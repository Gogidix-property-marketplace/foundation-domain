#!/bin/bash

# Deploy HashiCorp Vault for Secret Management
# This script deploys Vault with complete configuration for Gogidix

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🔐 Deploying HashiCorp Vault${NC}"
echo -e "${BLUE}    Secret Management System${NC}"
echo -e "${BLUE}========================================${NC}"

# Variables
VAULT_VERSION="1.14.0"
NAMESPACE="security"
VAULT_CLUSTER_NAME="vault-cluster"
VAULT_MODE="ha"

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

    # Check cert-manager
    if ! kubectl get namespace cert-manager &> /dev/null; then
        echo -e "${RED}Error: cert-manager is not installed. Please install cert-manager first.${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ Prerequisites check passed${NC}"
}

# Create namespace
create_namespace() {
    echo -e "${YELLOW}Creating namespace...${NC}"

    kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

    # Add labels for Istio injection and Vault
    kubectl label namespace ${NAMESPACE} istio-injection=enabled --overwrite
    kubectl label namespace ${NAMESPACE} name=vault --overwrite

    echo -e "${GREEN}✓ Namespace created${NC}"
}

# Install Vault using Helm
install_vault() {
    echo -e "${YELLOW}Installing Vault...${NC}"

    # Add HashiCorp helm repository
    helm repo add hashicorp https://helm.releases.hashicorp.com
    helm repo update

    # Create values file for Vault
    cat > vault-values.yaml <<EOF
# Global Configuration
global:
  enabled: true
  tlsDisable: false

# Server Configuration
server:
  image:
    repository: hashicorp/vault
    tag: "${VAULT_VERSION}"

  # HA Configuration
  ha:
    enabled: true
    replicas: 3
    config: |
      ui = true
      listener "tcp" {
        address = "[::]:8200"
        cluster_address = "[::]:8201"
        tls_cert_file = "/vault/userconfig/vault-server-tls/tls.crt"
        tls_key_file  = "/vault/userconfig/vault-server-tls/tls.key"
        tls_client_ca_file = "/vault/userconfig/vault-server-tls/ca.crt"
      }
      storage "consul" {
        path = "vault/"
        address = "consul.consul.svc.cluster.local:8501"
        scheme = "https"
        tls_ca_file = "/vault/userconfig/vault-server-tls/ca.crt"
      }
      service_registration "kubernetes" {}
      api_addr = "https://vault.${NAMESPACE}.svc.cluster.local:8200"
      cluster_addr = "https://vault-${VAULT_CLUSTER_NAME}:8201"
      disable_mlock = true

  # Resources
  resources:
    requests:
      cpu: 500m
      memory: 1Gi
    limits:
      cpu: 1000m
      memory: 2Gi

  # Affinity
  affinity: |
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchLabels:
              app.kubernetes.io/name: vault
              app.kubernetes.io/instance: vault
          topologyKey: kubernetes.io/hostname

  # Update Strategy
  updateStrategyType: "OnDelete"

  # Readiness Probe
  readinessProbe:
    enabled: true
    path: "/v1/sys/health?standbyok=true&perfstandbyok=true"
    initialDelaySeconds: 5
    periodSeconds: 5

  # Liveness Probe
  livenessProbe:
    enabled: true
    path: "/v1/sys/health?standbyok=true&perfstandbyok=true"
    initialDelaySeconds: 60
    periodSeconds: 10

  # Extra Environment Variables
  extraEnvironmentVars:
    VAULT_ADDR: "https://localhost:8200"
    VAULT_API_ADDR: "https://vault.${NAMESPACE}.svc.cluster.local:8200"

  # Extra Volumes
  extraVolumes:
    - type: secret
      name: vault-server-tls
      path: /vault/userconfig/vault-server-tls

# UI Configuration
ui:
  enabled: true
  externalPort: 443

# Ingress Configuration
ingress:
  enabled: true
  activeService: false
  hosts:
    - host: vault.gogidix.com
      paths:
        - path: /
          backend:
            serviceName: vault-ui
            servicePort: 8200
  tls:
    - secretName: vault-tls
      hosts:
        - vault.gogidix.com

# Agent Injector Configuration
injector:
  enabled: true
  image:
    repository: hashicorp/vault-k8s
    tag: "1.0.0"
  authPath: auth/kubernetes
  replicas: 2

  # Resources
  resources:
    requests:
      cpu: 250m
      memory: 256Mi
    limits:
      cpu: 500m
      memory: 512Mi

  # Webhook Configuration
  webhook:
    failurePolicy: Ignore

  # Metrics Configuration
  metrics:
    enabled: true

# Audit Device Configuration
serverAudit:
  audit: |
    audit {
      enabled = "true"
      path = "/vault/audit/file"
      type = "file"
      format = "json"
      options {
        file_path = "/var/log/vault/audit.log"
        mode = "0644"
      }
    }

# Telemetry Configuration
serverTelemetry:
  prometheus:
    enabled: true
    metricsPath: /v1/sys/metrics
    namespace: vault

# Auto-unseal Configuration (Using AWS KMS)
# Uncomment and configure if using AWS KMS
# server:
#   extraArgs: "-dev-kms"
#   ha:
#     config: |
#       seal "awskms" {
#         region = "us-east-1"
#         kms_key_id = "your-kms-key-id"
#       }

# Network Policies
networkPolicy:
  enabled: true

# Pod Disruption Budget
podDisruptionBudget:
  enabled: true
  minAvailable: 1

# Service Account
serviceAccount:
  annotations:
    iam.amazonaws.com/role: "vault-role"  # AWS IAM role for Vault
EOF

    # Install Vault
    helm install vault hashicorp/vault \
        --namespace ${NAMESPACE} \
        --values vault-values.yaml \
        --wait \
        --timeout 10m

    echo -e "${GREEN}✓ Vault installed${NC}"
}

# Create TLS certificates
create_tls_certificates() {
    echo -e "${YELLOW}Creating TLS certificates...${NC}"

    # Create certificate issuer
    cat > vault-issuer.yaml <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: vault-letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: "admin@gogidix.com"
    privateKeySecretRef:
      name: vault-issuer-account-key
    solvers:
    - selector: {}
      http01:
        ingress:
          class: istio
EOF

    kubectl apply -f vault-issuer.yaml

    # Create certificate for Vault
    cat > vault-certificate.yaml <<EOF
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: vault-server-tls
  namespace: ${NAMESPACE}
spec:
  secretName: vault-server-tls
  dnsNames:
    - vault.${NAMESPACE}.svc.cluster.local
    - vault.gogidix.com
    - localhost
  issuerRef:
    name: vault-letsencrypt-prod
    kind: ClusterIssuer
EOF

    kubectl apply -f vault-certificate.yaml

    echo -e "${GREEN}✓ TLS certificates created${NC}"
}

# Initialize and unseal Vault
initialize_vault() {
    echo -e "${YELLOW}Initializing Vault...${NC}"

    # Wait for Vault pods to be ready
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=vault -n ${NAMESPACE} --timeout=300s

    # Get the first Vault pod
    VAULT_POD=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=vault -o jsonpath='{.items[0].metadata.name}')

    # Port-forward to access Vault
    kubectl port-forward -n ${NAMESPACE} ${VAULT_POD} 8200:8200 &
    PF_PID=$!
    sleep 10

    # Initialize Vault
    echo -e "${BLUE}Initializing Vault cluster...${NC}"
    INIT_OUTPUT=$(kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault operator init -key-shares=5 -key-threshold=3 -format=json)

    # Save unseal keys and root token
    echo $INIT_OUTPUT > vault-init.json

    # Extract unseal keys and root token
    UNSEAL_KEYS=$(echo $INIT_OUTPUT | jq -r '.unseal_keys_b64[]')
    ROOT_TOKEN=$(echo $INIT_OUTPUT | jq -r '.root_token')

    # Unseal Vault
    echo -e "${BLUE}Unsealing Vault...${NC}"
    for key in $(echo $UNSEAL_KEYS | tr ' ' '\n'); do
        kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault operator unseal $key
    done

    # Unseal other pods
    for pod in $(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=vault -o jsonpath='{.items[1:].metadata.name}'); do
        for key in $(echo $UNSEAL_KEYS | tr ' ' '\n' | head -3); do
            kubectl exec -n ${NAMESPACE} $pod -- vault operator unseal $key
        done
    done

    # Login with root token
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault login $ROOT_TOKEN

    # Kill port-forward
    kill $PF_PID

    echo -e "${GREEN}✓ Vault initialized and unsealed${NC}"
}

# Configure Vault policies and secrets
configure_vault() {
    echo -e "${YELLOW}Configuring Vault policies and secrets...${NC}"

    # Get a Vault pod
    VAULT_POD=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=vault -o jsonpath='{.items[0].metadata.name}')

    # Enable Kubernetes auth
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault auth enable kubernetes

    # Configure Kubernetes auth
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- bash -c '
        vault write auth/kubernetes/config \
            token_reviewer_jwt="$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)" \
            kubernetes_host="https://$KUBERNETES_SERVICE_HOST:$KUBERNETES_SERVICE_PORT_HTTPS" \
            kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    '

    # Create policies
    cat > vault-policies.yaml <<EOF
# Policy for API Gateway
path "secret/data/api-gateway/*" {
  capabilities = ["read", "list"]
}

path "secret/data/database/*" {
  capabilities = ["read"]
}

# Policy for Property Service
path "secret/data/property-service/*" {
  capabilities = ["read", "list"]
}

# Policy for User Service
path "secret/data/user-service/*" {
  capabilities = ["read", "list"]
}

# Policy for Payment Service
path "secret/data/payment-service/*" {
  capabilities = ["read", "list"]
}

path "pki_int/issue/payment-*" {
  capabilities = ["create", "update"]
}

# Policy for monitoring
path "sys/health" {
  capabilities = ["read", "sudo"]
}

path "sys/metrics" {
  capabilities = ["read"]
}
EOF

    # Apply policies
    while IFS= read -r line; do
        if [[ "$line" != "#" && -n "$line" ]]; then
            kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault policy write "$line"
        fi
    done < vault-policies.yaml

    # Create database secrets engine
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault secrets enable -path=database kv-v2

    # Configure PostgreSQL database
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write database/config/postgresql \
        plugin_name=postgresql-database-plugin \
        allowed_roles="property-service,user-service,payment-service" \
        connection_url="postgresql://{{username}}:{{password}}@postgres.gogidix-database.svc.cluster.local:5432/gogidix_property" \
        username="gogidix_admin" \
        password="gogidix123"

    # Create database roles
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write database/roles/property-service \
        db_name=postgresql \
        creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT SELECT, INSERT, UPDATE, DELETE ON properties TO \"{{name}}\";" \
        default_ttl="1h" \
        max_ttl="24h"

    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write database/roles/user-service \
        db_name=postgresql \
        creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT SELECT, INSERT, UPDATE, DELETE ON users TO \"{{name}}\";" \
        default_ttl="1h" \
        max_ttl="24h"

    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write database/roles/payment-service \
        db_name=postgresql \
        creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT SELECT, INSERT, UPDATE, DELETE ON payments TO \"{{name}}\";" \
        default_ttl="1h" \
        max_ttl="24h"

    # Create PKI secrets engine
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault secrets enable pki_int
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault secrets tune -max-lease-ttl=87600h pki_int

    # Configure PKI
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write -field=certificate pki_int/root/generate/internal \
        common_name="Gogidix Internal CA" \
        ttl=87600h

    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write pki_int/config/urls \
        issuing_certificates="http://vault:8200/v1/pki_int/ca" \
        crl_distribution_points="http://vault:8200/v1/pki_int/crl"

    # Create PKI roles
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write pki_int/roles/payment-service \
        allowed_domains="payment-service,gogidix-payment" \
        allow_subdomains=true \
        max_ttl="72h"

    # Store API keys
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault kv put secret/api-gateway/stripe \
        api_key="sk_test_51234567890abcdef" \
        webhook_secret="whsec_test_abcdef1234567890"

    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault kv put secret/api-gateway/paypal \
        client_id="AQ1234567890abcdef" \
        client_secret="EFGH1234567890ijklmnopqrstuvwxyz" \
        webhook_id="WH-1ABC123XYZ456" \
        webhook_token="token1234567890"

    # Enable audit logging
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault audit enable file file_path=/var/log/vault/audit.log

    echo -e "${GREEN}✓ Vault configured${NC}"
}

# Create Kubernetes auth roles
create_auth_roles() {
    echo -e "${YELLOW}Creating Kubernetes auth roles...${NC}"

    VAULT_POD=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=vault -o jsonpath='{.items[0].metadata.name}')

    # API Gateway role
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write auth/kubernetes/role/api-gateway \
        bound_service_account_names=api-gateway \
        bound_service_account_namespaces=api-gateway \
        policies=api-gateway \
        ttl=24h

    # Property Service role
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write auth/kubernetes/role/property-service \
        bound_service_account_names=property-service \
        bound_service_account_namespaces=property-service \
        policies=property-service \
        ttl=24h

    # User Service role
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write auth/kubernetes/role/user-service \
        bound_service_account_names=user-service \
        bound_service_account_namespaces=user-service \
        policies=user-service \
        ttl=24h

    # Payment Service role
    kubectl exec -n ${NAMESPACE} ${VAULT_POD} -- vault write auth/kubernetes/role/payment-service \
        bound_service_account_names=payment-service \
        bound_service_account_namespaces=payment-service \
        policies=payment-service \
        ttl=24h

    echo -e "${GREEN}✓ Kubernetes auth roles created${NC}"
}

# Configure auto-renewal
setup_renewal() {
    echo -e "${YELLOW}Setting up certificate renewal...${NC}"

    # Create renewal script as ConfigMap
    cat > vault-renewal-cm.yaml <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: vault-renewal-script
  namespace: ${NAMESPACE}
data:
  renew-certs.sh: |
    #!/bin/bash
    set -e

    # Renew database credentials
    vault lease renew database/creds/property-service
    vault lease renew database/creds/user-service
    vault lease renew database/creds/payment-service

    # Renew PKI certificates
    vault lease renew pki_int/issue/payment-service
EOF

    kubectl apply -f vault-renewal-cm.yaml

    # Create CronJob for renewal
    cat > vault-renewal-cronjob.yaml <<EOF
apiVersion: batch/v1
kind: CronJob
metadata:
  name: vault-renewal
  namespace: ${NAMESPACE}
spec:
  schedule: "0 */12 * * *"  # Every 12 hours
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: vault-renewal
          containers:
          - name: vault-renewal
            image: hashicorp/vault:${VAULT_VERSION}
            command:
            - /bin/bash
            - /scripts/renew-certs.sh
            env:
            - name: VAULT_ADDR
              value: "https://vault.${NAMESPACE}.svc.cluster.local:8200"
            - name: VAULT_TOKEN
              valueFrom:
                secretKeyRef:
                  name: vault-renewal-token
                  key: token
            volumeMounts:
            - name: scripts
              mountPath: /scripts
              readOnly: true
          volumes:
          - name: scripts
            configMap:
              name: vault-renewal-script
              defaultMode: 0755
          restartPolicy: OnFailure
EOF

    kubectl apply -f vault-renewal-cronjob.yaml

    echo -e "${GREEN}✓ Certificate renewal setup complete${NC}"
}

# Create monitoring
create_monitoring() {
    echo -e "${YELLOW}Setting up monitoring...${NC}"

    # Create ServiceMonitor
    cat > vault-servicemonitor.yaml <<EOF
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: vault
  namespace: ${NAMESPACE}
  labels:
    team: gogidix
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: vault
  endpoints:
  - port: http
    path: /v1/sys/metrics
    interval: 30s
    scrapeTimeout: 10s
EOF

    kubectl apply -f vault-servicemonitor.yaml

    # Create Grafana dashboard
    cat > vault-dashboard.yaml <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: vault-dashboard
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  vault.json: |
    {
      "dashboard": {
        "id": null,
        "title": "HashiCorp Vault",
        "tags": ["vault", "security", "secrets"],
        "timezone": "browser",
        "panels": [
          {
            "title": "Sealed Status",
            "type": "singlestat",
            "targets": [
              {
                "expr": "vault_core_unsealed",
                "legendFormat": "Unsealed"
              }
            ]
          },
          {
            "title": "Request Rate",
            "type": "graph",
            "targets": [
              {
                "expr": "rate(vault_core_handle_request_count[5m])",
                "legendFormat": "{{method}} {{namespace}}"
              }
            ]
          },
          {
            "title": "Response Time",
            "type": "graph",
            "targets": [
              {
                "expr": "histogram_quantile(0.95, rate(vault_core_handle_request_duration_seconds_bucket[5m]))",
                "legendFormat": "95th percentile"
              }
            ]
          },
          {
            "title": "Active Leases",
            "type": "singlestat",
            "targets": [
              {
                "expr": "vault_sys_lease_count",
                "legendFormat": "Active Leases"
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

    kubectl apply -f vault-dashboard.yaml

    echo -e "${GREEN}✓ Monitoring configured${NC}"
}

# Display information
show_info() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${GREEN}✅ HashiCorp Vault deployment complete!${NC}"
    echo -e "${BLUE}========================================${NC}\n"

    echo -e "${YELLOW}Vault URLs:${NC}"
    echo -e "  • UI:               ${GREEN}https://vault.gogidix.com${NC}"
    echo -e "  • API:              ${GREEN}https://vault.gogidix.com/v1${NC}"

    echo -e "\n${YELLOW}Important Files:${NC}"
    echo -e "  • Unseal keys & root token saved in: ${BLUE}vault-init.json${NC}"
    echo -e "    ⚠️  Keep this file secure and offline!"

    echo -e "\n${YELLOW}First Steps:${NC}"
    echo -e "  1. Log in to Vault UI with root token from vault-init.json"
    echo -e "  2. Enable additional auth methods as needed"
    echo -e "  3. Configure additional secret engines"
    echo -e "  4. Create application-specific policies"

    echo -e "\n${YELLOW}Useful Commands:${NC}"
    echo -e "  • Vault status: ${BLUE}kubectl exec -n ${NAMESPACE} \$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=vault -o jsonpath='{.items[0].metadata.name}') -- vault status${NC}"
    echo -e "  • Get logs:     ${BLUE}kubectl logs -n ${NAMESPACE} -l app.kubernetes.io/name=vault -f${NC}"
    echo -e "  • Unseal Vault: ${BLUE}kubectl exec -n ${NAMESPACE} \$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=vault -o jsonpath='{.items[0].metadata.name}') -- vault operator unseal <unseal-key>${NC}"
}

# Cleanup function
cleanup() {
    echo -e "${YELLOW}Cleaning up temporary files...${NC}"
    rm -f vault-values.yaml vault-issuer.yaml vault-certificate.yaml vault-policies.yaml \
          vault-renewal-cm.yaml vault-renewal-cronjob.yaml vault-servicemonitor.yaml vault-dashboard.yaml
}

# Main execution
main() {
    check_prerequisites
    create_namespace
    install_vault
    create_tls_certificates
    initialize_vault
    configure_vault
    create_auth_roles
    setup_renewal
    create_monitoring
    show_info
    cleanup

    echo -e "\n${GREEN}✅ HashiCorp Vault is ready!${NC}"
}

# Handle script interruption
trap 'echo -e "\n${RED}Script interrupted. Cleaning up...${NC}"; cleanup; exit 1' INT

# Run main function
main "$@"