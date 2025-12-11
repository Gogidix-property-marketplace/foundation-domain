#!/bin/bash

# Deploy Keycloak Identity Management for Gogidix Property Marketplace
# This script deploys Keycloak with complete configuration

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🔐 Deploying Keycloak Identity${NC}"
echo -e "${BLUE}    Management System${NC}"
echo -e "${BLUE}========================================${NC}"

# Variables
KEYCLOAK_VERSION="23.0.0"
NAMESPACE="identity"
KEYCLOAK_ADMIN="admin"
KEYCLOAK_ADMIN_PASSWORD="gogidix123"
DB_HOST="postgres.gogidix-database.svc.cluster.local"
DB_NAME="keycloak"
DB_USER="keycloak"
DB_PASSWORD="keycloak123"

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"

    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}Error: kubectl is not installed${NC}"
        exit 1
    fi

    # Check helm
    if ! command -v helm &> /dev/null; then
        echo -e "${RED}Error: helm is not installed${NC}"
        exit 1
    fi

    # Check cluster connection
    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ Prerequisites check passed${NC}"
}

# Create namespace
create_namespace() {
    echo -e "${YELLOW}Creating namespace...${NC}"

    kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

    # Add labels for Istio injection
    kubectl label namespace ${NAMESPACE} istio-injection=enabled --overwrite

    echo -e "${GREEN}✓ Namespace created${NC}"
}

# Install Keycloak using Helm
install_keycloak() {
    echo -e "${YELLOW}Installing Keycloak...${NC}"

    # Add Keycloak helm repository
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo update

    # Create values file for Keycloak
    cat > keycloak-values.yaml <<EOF
# Keycloak Configuration
auth:
  adminUser: ${KEYCLOAK_ADMIN}
  adminPassword: ${KEYCLOAK_ADMIN_PASSWORD}
  managementUser: manager
  managementPassword: gogidix123

# Database Configuration
externalDatabase:
  host: ${DB_HOST}
  port: 5432
  user: ${DB_USER}
  password: ${DB_PASSWORD}
  database: ${DB_NAME}

# Ingress Configuration
ingress:
  enabled: true
  hostname: keycloak.gogidix.com
  ingressClassName: istio
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    networking.istio.io/gateway: istio-system/ingressgateway
  tls: true

# Production Configuration
production: true
disableBootstrap: true

# Proxy Configuration
proxy: edge

# Cache Configuration
cache:
  stack: kubernetes

# Health Configuration
startupScripts:
  # Custom realm configuration script
  realm-config.sh: |
    #!/bin/bash
    /opt/keycloak/bin/kcadm.sh config credentials \
      --server http://localhost:8080/auth \
      --realm master \
      --user ${KEYCLOAK_ADMIN} \
      --password ${KEYCLOAK_ADMIN_PASSWORD}

    # Import realm configuration
    /opt/keycloak/bin/kcadm.sh create realms \
      -f /tmp/realm-config.json \
      || echo "Realm might already exist"

# Metrics Configuration
metrics:
  enabled: true

# Prometheus Configuration
serviceMonitor:
  enabled: true
  namespace: ${NAMESPACE}
  labels:
    team: gogidix

# Resources Configuration
resources:
  requests:
    cpu: 1000m
    memory: 2Gi
  limits:
    cpu: 2000m
    memory: 4Gi

# High Availability Configuration
replicaCount: 3

# JVM Configuration
jvmJavaOpts: >-
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseCGroupMemoryLimitForHeap
  -XX:MaxRAMPercentage=75.0

# Liveness and Readiness Probes
livenessProbe:
  initialDelaySeconds: 300
  periodSeconds: 30
  timeoutSeconds: 5
  failureThreshold: 3
  successThreshold: 1

readinessProbe:
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
  successThreshold: 1

# Pod Disruption Budget
podDisruptionBudget:
  enabled: true
  minAvailable: 1

# Network Policies
networkPolicy:
  enabled: true

# Pod Anti-Affinity
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchLabels:
              app.kubernetes.io/name: keycloak
          topologyKey: kubernetes.io/hostname

# TLS Configuration
tls:
  enabled: true
  autoGenerate: false

# Themes Configuration
extraVolumeMounts:
  - name: themes
    mountPath: /opt/keycloak/themes/gogidix
    readOnly: true

extraVolumes:
  - name: themes
    emptyDir: {}

# Extra Init Containers
extraInitContainers:
  - name: import-themes
    image: busybox:1.35
    command:
      - /bin/sh
      - -c
      - |
        echo "Setting up custom themes..."
        mkdir -p /themes/gogidix/{login,account}
        echo "Custom theme setup complete"
    volumeMounts:
      - name: themes
        mountPath: /themes
EOF

    # Install Keycloak
    helm install keycloak bitnami/keycloak \
        --namespace ${NAMESPACE} \
        --values keycloak-values.yaml \
        --version 21.3.0 \
        --wait \
        --timeout 10m

    echo -e "${GREEN}✓ Keycloak installed${NC}"
}

# Configure realm and clients
configure_keycloak() {
    echo -e "${YELLOW}Configuring Keycloak realm and clients...${NC}"

    # Wait for Keycloak to be ready
    echo -e "${BLUE}Waiting for Keycloak to be ready...${NC}"
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=keycloak -n ${NAMESPACE} --timeout=300s

    # Get Keycloak pod
    KEYCLOAK_POD=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}')

    # Copy realm configuration file to pod
    kubectl cp realm-config.json ${NAMESPACE}/${KEYCLOAK_POD}:/tmp/realm-config.json

    # Execute configuration script
    kubectl exec -n ${NAMESPACE} ${KEYCLOAK_POD} -- bash -c '
        # Wait for Keycloak to be fully ready
        until curl -f http://localhost:8080/auth/realms/master; do
            echo "Waiting for Keycloak to start..."
            sleep 5
        done

        # Configure admin credentials
        /opt/keycloak/bin/kcadm.sh config credentials \
            --server http://localhost:8080/auth \
            --realm master \
            --user '${KEYCLOAK_ADMIN}' \
            --password '${KEYCLOAK_ADMIN_PASSWORD}'

        # Import realm configuration
        echo "Importing realm configuration..."
        /opt/keycloak/bin/kcadm.sh create realms \
            -f /tmp/realm-config.json || \
        /opt/keycloak/bin/kcadm.sh update realms/gogidix-property \
            -f /tmp/realm-config.json

        echo "Creating test users..."
        # Create admin user
        /opt/keycloak/bin/kcadm.sh create users \
            -r gogidix-property \
            -s username=admin \
            -s firstName=Admin \
            -s lastName=User \
            -s email=admin@gogidix.com \
            -s enabled=true || echo "Admin user might already exist"

        # Set admin password
        ADMIN_USER_ID=$( /opt/keycloak/bin/kcadm.sh get users \
            -r gogidix-property \
            -q username=admin \
            | jq -r '.[0].id' )
        if [ "$ADMIN_USER_ID" != "null" ]; then
            /opt/keycloak/bin/kcadm.sh set-password \
                -r gogidix-property \
                --userid ${ADMIN_USER_ID} \
                --new-password Admin123!
        fi

        # Create agent user
        /opt/keycloak/bin/kcadm.sh create users \
            -r gogidix-property \
            -s username=agent \
            -s firstName=Agent \
            -s lastName=User \
            -s email=agent@gogidix.com \
            -s enabled=true || echo "Agent user might already exist"

        # Set agent password
        AGENT_USER_ID=$( /opt/keycloak/bin/kcadm.sh get users \
            -r gogidix-property \
            -q username=agent \
            | jq -r '.[0].id' )
        if [ "$AGENT_USER_ID" != "null" ]; then
            /opt/keycloak/bin/kcadm.sh set-password \
                -r gogidix-property \
                --userid ${AGENT_USER_ID} \
                --new-password Agent123!
            # Assign agent role
            /opt/keycloak/bin/kcadm.sh add-roles \
                -r gogidix-property \
                --uusername agent \
                --rolename agent
        fi

        # Create regular user
        /opt/keycloak/bin/kcadm.sh create users \
            -r gogidix-property \
            -s username=user \
            -s firstName=Regular \
            -s lastName=User \
            -s email=user@gogidix.com \
            -s enabled=true || echo "User might already exist"

        # Set user password
        USER_ID=$( /opt/keycloak/bin/kcadm.sh get users \
            -r gogidix-property \
            -q username=user \
            | jq -r '.[0].id' )
        if [ "$USER_ID" != "null" ]; then
            /opt/keycloak/bin/kcadm.sh set-password \
                -r gogidix-property \
                --userid ${USER_ID} \
                --new-password User123!
            # Assign user role
            /opt/keycloak/bin/kcadm.sh add-roles \
                -r gogidix-property \
                --uusername user \
                --rolename user
        fi

        echo "Keycloak configuration completed!"
    '

    echo -e "${GREEN}✓ Keycloak configured${NC}"
}

# Create monitoring configuration
create_monitoring() {
    echo -e "${YELLOW}Setting up monitoring...${NC}"

    # Create ServiceMonitor for Keycloak
    cat > keycloak-servicemonitor.yaml <<EOF
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: keycloak
  namespace: ${NAMESPACE}
  labels:
    team: gogidix
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: keycloak
  endpoints:
  - port: http
    path: /auth/realms/master/metrics
    interval: 30s
    scrapeTimeout: 10s
EOF

    kubectl apply -f keycloak-servicemonitor.yaml

    # Create Grafana dashboard
    cat > keycloak-dashboard.yaml <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: keycloak-dashboard
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  keycloak.json: |
    {
      "dashboard": {
        "id": null,
        "title": "Keycloak Identity Management",
        "tags": ["keycloak", "identity", "auth"],
        "timezone": "browser",
        "panels": [
          {
            "title": "Active Sessions",
            "type": "singlestat",
            "targets": [
              {
                "expr": "keycloak_statistics_active_sessions",
                "legendFormat": "Active Sessions"
              }
            ]
          },
          {
            "title": "Login Attempts",
            "type": "graph",
            "targets": [
              {
                "expr": "rate(keycloak_logins_total[5m])",
                "legendFormat": "Login Rate"
              }
            ]
          },
          {
            "title": "Failed Logins",
            "type": "graph",
            "targets": [
              {
                "expr": "rate(keycloak_login_failed_total[5m])",
                "legendFormat": "Failed Login Rate"
              }
            ]
          },
          {
            "title": "User Registrations",
            "type": "graph",
            "targets": [
              {
                "expr": "increase(keycloak_registrations_total[5m])",
                "legendFormat": "Registrations"
              }
            ]
          }
        ],
        "time": {
          "from": "now-1h",
          "to": "now"
        },
        "refresh": "30s"
      }
    }
EOF

    kubectl apply -f keycloak-dashboard.yaml

    echo -e "${GREEN}✓ Monitoring configured${NC}"
}

# Create backup configuration
setup_backup() {
    echo -e "${YELLOW}Setting up backup configuration...${NC}"

    # Create backup script
    cat > keycloak-backup.yaml <<EOF
apiVersion: batch/v1
kind: CronJob
metadata:
  name: keycloak-backup
  namespace: ${NAMESPACE}
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: keycloak-backup
            image: postgres:15-alpine
            env:
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: keycloak-postgresql
                  key: postgres-password
            command:
            - /bin/bash
            - -c
            - |
              DATE=\$(date +%Y%m%d_%H%M%S)
              BACKUP_FILE="/backup/keycloak-backup-\${DATE}.sql"

              pg_dump -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} > \${BACKUP_FILE}

              # Upload to S3 (if configured)
              if [ -n "\${AWS_S3_BUCKET}" ]; then
                aws s3 cp \${BACKUP_FILE} s3://\${AWS_S3_BUCKET}/keycloak-backups/
              fi

              # Clean up local files older than 7 days
              find /backup -name "*.sql" -mtime +7 -delete
            volumeMounts:
            - name: backup-storage
              mountPath: /backup
            env:
            - name: AWS_S3_BUCKET
              value: "gogidix-backup-bucket"
          volumes:
          - name: backup-storage
            persistentVolumeClaim:
              claimName: keycloak-backup-pvc
          restartPolicy: OnFailure
EOF

    kubectl apply -f keycloak-backup.yaml

    # Create PVC for backups
    cat > keycloak-backup-pvc.yaml <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: keycloak-backup-pvc
  namespace: ${NAMESPACE}
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: fast-ssd
  resources:
    requests:
      storage: 10Gi
EOF

    kubectl apply -f keycloak-backup-pvc.yaml

    echo -e "${GREEN}✓ Backup configuration created${NC}"
}

# Display information
show_info() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${GREEN}✅ Keycloak deployment complete!${NC}"
    echo -e "${BLUE}========================================${NC}\n"

    echo -e "${YELLOW}Keycloak URLs:${NC}"
    echo -e "  • Admin Console:     ${GREEN}https://keycloak.gogidix.com/auth/admin/${NC}"
    echo -e "  • Account Console:   ${GREEN}https://keycloak.gogidix.com/auth/realms/gogidix-property/account/${NC}"
    echo -e "  • Well-Known Config: ${GREEN}https://keycloak.gogidix.com/auth/realms/gogidix-property/.well-known/openid_configuration${NC}"

    echo -e "\n${YELLOW}Default Credentials:${NC}"
    echo -e "  • Admin Username:    ${BLUE}${KEYCLOAK_ADMIN}${NC}"
    echo -e "  • Admin Password:    ${BLUE}${KEYCLOAK_ADMIN_PASSWORD}${NC}"
    echo -e "  • Test User:         ${BLUE}user@gogidix.com${NC} / ${BLUE}User123!${NC}"
    echo -e "  • Test Agent:        ${BLUE}agent@gogidix.com${NC} / ${BLUE}Agent123!${NC}"

    echo -e "\n${YELLOW}API Endpoints:${NC}"
    echo -e "  • Token Endpoint:    ${GREEN}https://keycloak.gogidix.com/auth/realms/gogidix-property/protocol/openid-connect/token${NC}"
    echo -e "  • User Info:         ${GREEN}https://keycloak.gogidix.com/auth/realms/gogidix-property/protocol/openid-connect/userinfo${NC}"
    echo -e "  • JWKS:              ${GREEN}https://keycloak.gogidix.com/auth/realms/gogidix-property/protocol/openid-connect/certs${NC}"

    echo -e "\n${YELLOW}Useful Commands:${NC}"
    echo -e "  • Get admin token: ${BLUE}kubectl exec -n ${NAMESPACE} \$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}') -- /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user ${KEYCLOAK_ADMIN} --password ${KEYCLOAK_ADMIN_PASSWORD}${NC}"
    echo -e "  • List users:       ${BLUE}kubectl exec -n ${NAMESPACE} \$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}') -- /opt/keycloak/bin/kcadm.sh get users -r gogidix-property${NC}"
    echo -e "  • Check logs:       ${BLUE}kubectl logs -n ${NAMESPACE} -l app.kubernetes.io/name=keycloak -f${NC}"
}

# Cleanup function
cleanup() {
    echo -e "${YELLOW}Cleaning up temporary files...${NC}"
    rm -f keycloak-values.yaml keycloak-servicemonitor.yaml keycloak-dashboard.yaml keycloak-backup.yaml keycloak-backup-pvc.yaml
}

# Main execution
main() {
    check_prerequisites
    create_namespace
    install_keycloak
    configure_keycloak
    create_monitoring
    setup_backup
    show_info
    cleanup

    echo -e "\n${GREEN}✅ Keycloak Identity Management System is ready!${NC}"
}

# Handle script interruption
trap 'echo -e "\n${RED}Script interrupted. Cleaning up...${NC}"; cleanup; exit 1' INT

# Run main function
main "$@"