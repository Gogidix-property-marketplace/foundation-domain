#!/bin/bash

# Deploy Velero for Backup and Disaster Recovery
# This script deploys Velero with complete backup configuration for Gogidix

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}💾 Deploying Velero Backup &${NC}"
echo -e "${BLUE}    Disaster Recovery System${NC}"
echo -e "${BLUE}========================================${NC}"

# Variables
VELERO_VERSION="v1.10.1"
NAMESPACE="velero"
BUCKET_NAME="gogidix-backup-bucket"
BACKUP_SCHEDULE="0 2 * * *"  # Daily at 2 AM
RETENTION_PERIOD="30d"
AWS_REGION="us-east-1"

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"

    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}Error: kubectl is not installed${NC}"
        exit 1
    fi

    # Check velero CLI
    if ! command -v velero &> /dev/null; then
        echo -e "${YELLOW}Velero CLI not found. Installing...${NC}"
        install_velero_cli
    else
        echo -e "${GREEN}✓ Velero CLI found${NC}"
    fi

    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        echo -e "${RED}Error: AWS CLI is not installed${NC}"
        exit 1
    fi

    # Check cluster connection
    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
        exit 1
    fi

    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        echo -e "${RED}Error: AWS credentials not configured${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ Prerequisites check passed${NC}"
}

# Install Velero CLI
install_velero_cli() {
    echo -e "${YELLOW}Installing Velero CLI...${NC}"

    # Download and install Velero CLI
    wget -q https://github.com/vmware-tanzu/velero/releases/download/${VELERO_VERSION}/velero-${VELERO_VERSION}-linux-amd64.tar.gz
    tar -xzf velero-${VELERO_VERSION}-linux-amd64.tar.gz
    sudo mv velero-${VELERO_VERSION}-linux-amd64/velero /usr/local/bin/

    # Verify installation
    velero version --client-only

    echo -e "${GREEN}✓ Velero CLI installed${NC}"
}

# Create S3 bucket
create_s3_bucket() {
    echo -e "${YELLOW}Creating S3 bucket...${NC}"

    # Check if bucket already exists
    if aws s3 ls "s3://${BUCKET_NAME}" 2>&1 | grep -q "NoSuchBucket"; then
        aws s3 mb "s3://${BUCKET_NAME}" --region ${AWS_REGION}
        echo -e "${GREEN}✓ S3 bucket created${NC}"
    else
        echo -e "${GREEN}✓ S3 bucket already exists${NC}"
    fi

    # Enable versioning
    aws s3api put-bucket-versioning \
        --bucket ${BUCKET_NAME} \
        --versioning-configuration Status=Enabled

    # Set up lifecycle policy
    aws s3api put-bucket-lifecycle-configuration \
        --bucket ${BUCKET_NAME} \
        --lifecycle-configuration '{
            "Rules": [
                {
                    "Status": "Enabled",
                    "Filter": {"Prefix": ""},
                    "Transitions": [
                        {
                            "Days": 30,
                            "StorageClass": "STANDARD_IA"
                        },
                        {
                            "Days": 90,
                            "StorageClass": "GLACIER"
                        },
                        {
                            "Days": 365,
                            "StorageClass": "DEEP_ARCHIVE"
                        }
                    ],
                    "ID": "VeleroBackupLifecycle"
                }
            ]
        }'

    echo -e "${GREEN}✓ S3 bucket configured${NC}"
}

# Create IAM user and policy
create_iam_resources() {
    echo -e "${YELLOW}Creating IAM user and policy...${NC}"

    # Create IAM policy
    cat > velero-policy.json <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:DescribeVolumes",
                "ec2:DescribeSnapshots",
                "ec2:CreateTags",
                "ec2:CreateVolume",
                "ec2:CreateSnapshot",
                "ec2:DeleteSnapshot"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:PutObject",
                "s3:AbortMultipartUpload",
                "s3:ListMultipartUploadParts"
            ],
            "Resource": [
                "arn:aws:s3:::${BUCKET_NAME}/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::${BUCKET_NAME}"
            ]
        }
    ]
}
EOF

    # Create policy
    aws iam create-policy \
        --policy-name VeleroBackupPolicy \
        --policy-document file://velero-policy.json \
        --description "Policy for Velero backup and restore operations" || \
        echo "Policy might already exist"

    # Create IAM user
    aws iam create-user \
        --user-name velero-backup-user || \
        echo "User might already exist"

    # Attach policy to user
    aws iam attach-user-policy \
        --user-name velero-backup-user \
        --policy-arn arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):policy/VeleroBackupPolicy

    # Create access key
    ACCESS_KEY_OUTPUT=$(aws iam create-access-key \
        --user-name velero-backup-user \
        --query '[AccessKey.AccessKeyId, AccessKey.SecretAccessKey]' \
        --output text)

    ACCESS_KEY_ID=$(echo $ACCESS_KEY_OUTPUT | awk '{print $1}')
    SECRET_ACCESS_KEY=$(echo $ACCESS_KEY_OUTPUT | awk '{print $2}')

    echo -e "${GREEN}✓ IAM resources created${NC}"

    # Save credentials to file
    cat > velero-credentials <<EOF
[default]
aws_access_key_id = ${ACCESS_KEY_ID}
aws_secret_access_key = ${SECRET_ACCESS_KEY}
EOF
}

# Create namespace
create_namespace() {
    echo -e "${YELLOW}Creating namespace...${NC}"

    kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

    echo -e "${GREEN}✓ Namespace created${NC}"
}

# Install Velero
install_velero() {
    echo -e "${YELLOW}Installing Velero...${NC}"

    # Install Velero
    velero install \
        --provider aws \
        --bucket ${BUCKET_NAME} \
        --secret-file ./velero-credentials \
        --use-restic \
        --default-backup-ttl ${RETENTION_PERIOD} \
        --plugins velero/velero-plugin-for-aws:v1.5.0 \
        --namespace ${NAMESPACE} \
        --wait

    echo -e "${GREEN}✓ Velero installed${NC}"
}

# Create backup schedules
create_backup_schedules() {
    echo -e "${YELLOW}Creating backup schedules...${NC}"

    # Daily backup of all namespaces
    velero create schedule "daily-full-backup" \
        --schedule="${BACKUP_SCHEDULE}" \
        --ttl ${RETENTION_PERIOD} \
        --include-namespaces="*" \
        --default-volumes-to-restic=true \
        --description="Daily full backup of all namespaces"

    # Hourly backup of application data
    velero create schedule "hourly-app-backup" \
        --schedule="@hourly" \
        --ttl "7d" \
        --include-namespaces="gogidix-workloads,gogidix-database" \
        --default-volumes-to-restic=false \
        --description="Hourly backup of application data"

    # Weekly backup with longer retention
    velero create schedule "weekly-extended-backup" \
        --schedule="0 3 * * 0" \
        --ttl "90d" \
        --include-namespaces="*" \
        --default-volumes-to-restic=true \
        --description="Weekly backup with extended retention"

    echo -e "${GREEN}✓ Backup schedules created${NC}"
}

# Create backup location and restic repository
configure_backup_storage() {
    echo -e "${YELLOW}Configuring backup storage...${NC}"

    # Create additional backup location for cross-region
    velero backup-location create cross-region-backup \
        --provider aws \
        --bucket ${BUCKET_NAME}-cross-region \
        --config region=${AWS_REGION} \
        --access-mode=ReadWrite \
        --credential-file ./velero-credentials \
        --namespace ${NAMESPACE} || \
        echo "Cross-region backup location might already exist"

    # Create restic repositories for persistent volumes
    for ns in gogidix-database gogidix-workloads gogidix-monitoring; do
        velero restic repo create \
            --namespace ${ns} \
            --operator-type deployment \
            --operator-name velero
    done

    echo -e "${GREEN}✓ Backup storage configured${NC}"
}

# Create restore test job
create_restore_test() {
    echo -e "${YELLOW}Creating restore test job...${NC}"

    cat > velero-restore-test.yaml <<EOF
apiVersion: batch/v1
kind: CronJob
metadata:
  name: velero-restore-test
  namespace: ${NAMESPACE}
spec:
  schedule: "0 4 * * 0"  # Weekly on Sunday at 4 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: restore-test
            image: velero/velero:${VELERO_VERSION}
            command:
            - /bin/bash
            - -c
            - |
              # Get latest backup
              BACKUP_NAME=\$(velero backup get --output json | jq -r '.items[] | select(.status.phase=="Completed") | .metadata.name' | head -1)

              if [ -n "\$BACKUP_NAME" ]; then
                # Create test restore
                RESTORE_NAME="test-restore-\$(date +%Y%m%d%H%M%S)"
                velero create restore \${RESTORE_NAME} \
                    --from-backup \${BACKUP_NAME} \
                    --namespace-mappings "gogidix-workloads:gogidix-workloads-test" \
                    --wait

                # Verify restore
                if [ \$(velero restore get \${RESTORE_NAME} --output json | jq -r '.status.phase') == "Completed" ]; then
                  echo "Restore test successful"
                  # Clean up test restore
                  velero restore delete \${RESTORE_NAME} --confirm
                  kubectl delete namespace gogidix-workloads-test --ignore-not-found
                else
                  echo "Restore test failed"
                  exit 1
                fi
              else
                echo "No completed backup found"
              fi
            env:
            - name: VELERO_NAMESPACE
              value: ${NAMESPACE}
          restartPolicy: OnFailure
          serviceAccountName: velero
EOF

    kubectl apply -f velero-restore-test.yaml

    echo -e "${GREEN}✓ Restore test job created${NC}"
}

# Create monitoring for Velero
create_monitoring() {
    echo -e "${YELLOW}Setting up monitoring...${NC}"

    # Create ServiceMonitor for Velero
    cat > velero-servicemonitor.yaml <<EOF
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: velero
  namespace: ${NAMESPACE}
  labels:
    team: gogidix
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: velero
  endpoints:
  - port: http-monitoring
    interval: 30s
    path: /metrics
EOF

    kubectl apply -f velero-servicemonitor.yaml

    # Create Grafana dashboard
    cat > velero-dashboard.yaml <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: velero-dashboard
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  velero.json: |
    {
      "dashboard": {
        "id": null,
        "title": "Velero Backup & Restore",
        "tags": ["velero", "backup", "disaster-recovery"],
        "timezone": "browser",
        "panels": [
          {
            "title": "Backup Success Rate",
            "type": "singlestat",
            "targets": [
              {
                "expr": "sum(rate(velero_backup_success_total[5m])) / sum(rate(velero_backup_attempt_total[5m])) * 100",
                "legendFormat": "Success Rate %"
              }
            ],
            "valueMaps": [
              {
                "value": "null",
                "text": "N/A"
              }
            ]
          },
          {
            "title": "Backup Duration",
            "type": "graph",
            "targets": [
              {
                "expr": "histogram_quantile(0.95, rate(velero_backup_duration_seconds_bucket[5m]))",
                "legendFormat": "95th percentile"
              }
            ]
          },
          {
            "title": "Backups by Schedule",
            "type": "graph",
            "targets": [
              {
                "expr": "sum by (schedule) (increase(velero_backup_total[5m]))",
                "legendFormat": "{{schedule}}"
              }
            ]
          },
          {
            "title": "Restic Operation Rate",
            "type": "graph",
            "targets": [
              {
                "expr": "sum by (operation) (rate(velero_restic_operation_total[5m]))",
                "legendFormat": "{{operation}}"
              }
            ]
          },
          {
            "title": "Volume Snapshots",
            "type": "singlestat",
            "targets": [
              {
                "expr": "sum(velero_volume_snapshot_info)",
                "legendFormat": "Total Snapshots"
              }
            ]
          }
        ],
        "time": {
          "from": "now-24h",
          "to": "now"
        },
        "refresh": "30s"
      }
    }
EOF

    kubectl apply -f velero-dashboard.yaml

    # Create Prometheus rules
    cat > velero-prometheus-rules.yaml <<EOF
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: velero-rules
  namespace: ${NAMESPACE}
  labels:
    team: gogidix
spec:
  groups:
  - name: velero.rules
    rules:
    - alert: VeleroBackupFailed
      expr: rate(velero_backup_failed_total[5m]) > 0
      for: 2m
      labels:
        severity: critical
        service: velero
      annotations:
        summary: "Velero backup failure detected"
        description: "Velero backup has failed {{ $value }} times in the last 5 minutes"

    - alert: VeleroRestoreFailed
      expr: rate(velero_restore_failed_total[5m]) > 0
      for: 2m
      labels:
        severity: critical
        service: velero
      annotations:
        summary: "Velero restore failure detected"
        description: "Velero restore has failed {{ $value }} times in the last 5 minutes"

    - alert: VeleroBackupStale
      expr: time() - velero_backup_last_success_timestamp > 86400
      for: 0m
      labels:
        severity: warning
        service: velero
      annotations:
        summary: "No successful Velero backup in 24 hours"
        description: "Last successful backup was {{ $value | humanizeDuration }} ago"
EOF

    kubectl apply -f velero-prometheus-rules.yaml

    echo -e "${GREEN}✓ Monitoring configured${NC}"
}

# Create disaster recovery plan
create_dr_plan() {
    echo -e "${YELLOW}Creating disaster recovery plan...${NC}"

    cat > disaster-recovery-plan.md <<'EOF'
# Gogidix Property Marketplace - Disaster Recovery Plan

## Overview
This document outlines the disaster recovery procedures for the Gogidix Property Marketplace infrastructure using Velero.

## Backup Strategy

### Backup Types
1. **Daily Full Backups**: Complete backup of all namespaces with 30-day retention
2. **Hourly Application Backups**: Application data only with 7-day retention
3. **Weekly Extended Backups**: Complete backup with 90-day retention

### Backup Locations
- **Primary**: AWS S3 bucket in us-east-1 (gogidix-backup-bucket)
- **Secondary**: AWS S3 bucket in us-west-2 (gogidix-backup-bucket-cross-region)

## Disaster Recovery Scenarios

### Scenario 1: Single Pod/Deployment Failure
**Impact**: Minimal
**Recovery Time**: < 5 minutes
**Steps**:
1. Identify failed pod: `kubectl get pods --all-namespaces`
2. Delete the pod: `kubectl delete pod <pod-name> -n <namespace>`
3. Kubernetes will automatically recreate the pod

### Scenario 2: Namespace Level Failure
**Impact**: Service-specific
**Recovery Time**: < 30 minutes
**Steps**:
1. Identify affected namespace
2. Delete the namespace: `kubectl delete namespace <namespace>`
3. Restore from latest backup: `velero create restore --from-backup <backup-name> --wait`

### Scenario 3: Cluster-Level Disaster
**Impact**: Complete service outage
**Recovery Time**: < 2 hours
**Steps**:
1. Provision new Kubernetes cluster
2. Install Velero: `velero install --provider aws --bucket gogidix-backup-bucket ...`
3. Restore all namespaces: `velero create restore --from-backup <latest-backup> --wait`
4. Verify all services are running
5. Update DNS to point to new cluster

### Scenario 4: Data Corruption
**Impact**: Potential data loss
**Recovery Time**: < 1 hour
**Steps**:
1. Identify point in time before corruption
2. Restore to specific backup: `velero create restore --from-backup <backup-name> --wait`
3. Verify data integrity
4. Resume normal operations

## Recovery Procedures

### Pre-Recovery Checklist
- [ ] Identify root cause of disaster
- [ ] Ensure backup integrity
- [ ] Prepare new infrastructure (if needed)
- [ ] Notify stakeholders
- [ ] Prepare maintenance window

### Recovery Commands

#### List Available Backups
```bash
velero backup get
```

#### Describe Backup Details
```bash
velero backup describe <backup-name> --details
```

#### Restore from Backup
```bash
# Full restore
velero create restore --from-backup <backup-name> --wait

# Selective restore
velero create restore --from-backup <backup-name> \
    --include-namespaces namespace1,namespace2 \
    --wait

# Restore with namespace mapping
velero create restore --from-backup <backup-name> \
    --namespace-mappings "source-namespace:target-namespace" \
    --wait
```

### Post-Recovery Verification
1. Check all pods are running: `kubectl get pods --all-namespaces`
2. Verify service endpoints: `kubectl get svc --all-namespaces`
3. Test application functionality
4. Verify database connectivity
5. Check monitoring dashboards
6. Perform smoke tests on all critical paths

## RTO/RPO Metrics

| Service Level | Recovery Time Objective (RTO) | Recovery Point Objective (RPO) |
|---------------|------------------------------|------------------------------|
| Critical      | < 30 minutes                 | < 1 hour                     |
| Important     | < 2 hours                    | < 4 hours                    |
| Normal        | < 24 hours                   | < 24 hours                   |

## Contact Information

### Primary Contact
- **Name**: Infrastructure Team
- **Email**: infra@gogidix.com
- **Phone**: +1-555-0123

### Secondary Contact
- **Name**: Operations Team
- **Email**: ops@gogidix.com
- **Phone**: +1-555-0456

## Testing Schedule
- **Monthly**: Restore test for non-production environments
- **Quarterly**: Full disaster recovery drill
- **Annually**: Complete DR test with third-party validation

## Documentation Updates
- Review and update this plan quarterly
- Document all incidents and recovery procedures
- Maintain change log of infrastructure modifications

EOF

    echo -e "${GREEN}✓ Disaster recovery plan created${NC}"
}

# Display information
show_info() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${GREEN}✅ Velero deployment complete!${NC}"
    echo -e "${BLUE}========================================${NC}\n"

    echo -e "${YELLOW}Backup Configuration:${NC}"
    echo -e "  • Daily Full Backup:  ${GREEN}Every day at 2 AM${NC}"
    echo -e "  • Hourly App Backup:  ${GREEN}Every hour${NC}"
    echo -e "  • Weekly Extended:    ${GREEN}Every Sunday at 3 AM${NC}"
    echo -e "  • Retention Period:   ${GREEN}Daily: 30 days, Weekly: 90 days${NC}"

    echo -e "\n${YELLOW}Storage:${NC}"
    echo -e "  • S3 Bucket:          ${BLUE}${BUCKET_NAME}${NC}"
    echo -e "  • Region:             ${BLUE}${AWS_REGION}${NC}"
    echo -e "  • Cross-region Backup: ${BLUE}${BUCKET_NAME}-cross-region${NC}"

    echo -e "\n${YELLOW}Useful Commands:${NC}"
    echo -e "  • List backups:       ${BLUE}velero backup get${NC}"
    echo -e "  • List schedules:     ${BLUE}velero schedule get${NC}"
    echo -e "  • Create backup:      ${BLUE}velero backup create <name>${NC}"
    echo -e "  • Restore backup:     ${BLUE}velero restore create --from-backup <name>${NC}"
    echo -e "  • Check status:        ${BLUE}velero get backups,restores,schedules${NC}"
    echo -e "  • Describe backup:    ${BLUE}velero backup describe <name> --details${NC}"
    echo -e "  • Verify plugins:      ${BLUE}velero plugin get${NC}"

    echo -e "\n${YELLOW}Monitoring:${NC}"
    echo -e "  • Grafana Dashboard:  ${GREEN}Velero Backup & Restore${NC}"
    echo -e "  • Prometheus Metrics: ${GREEN}http://prometheus:9090${NC}"
    echo -e "  • Alert Rules:        ${GREEN}Velero alerts configured${NC}"

    echo -e "\n${YELLOW}Documentation:${NC}"
    echo -e "  • DR Plan:           ${BLUE}disaster-recovery-plan.md${NC}"
}

# Cleanup function
cleanup() {
    echo -e "${YELLOW}Cleaning up temporary files...${NC}"
    rm -f velero-policy.json velero-credentials velero-restore-test.yaml \
          velero-servicemonitor.yaml velero-dashboard.yaml velero-prometheus-rules.yaml
}

# Main execution
main() {
    check_prerequisites
    create_s3_bucket
    create_iam_resources
    create_namespace
    install_velero
    create_backup_schedules
    configure_backup_storage
    create_restore_test
    create_monitoring
    create_dr_plan
    show_info
    cleanup

    echo -e "\n${GREEN}✅ Velero Backup & Disaster Recovery is ready!${NC}"
}

# Handle script interruption
trap 'echo -e "\n${RED}Script interrupted. Cleaning up...${NC}"; cleanup; exit 1' INT

# Run main function
main "$@"