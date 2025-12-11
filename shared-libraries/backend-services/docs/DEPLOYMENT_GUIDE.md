# ✨ GOGIDIX PLATFORM - DEPLOYMENT GUIDE ✨
# Comprehensive Production Deployment Manual
# Version: 1.0.0
# Last Updated: 2025-11-29

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Local Development Setup](#local-development-setup)
4. [Staging Deployment](#staging-deployment)
5. [Production Deployment](#production-deployment)
6. [CI/CD Pipeline](#cicd-pipeline)
7. [Monitoring Setup](#monitoring-setup)
8. [Troubleshooting](#troubleshooting)
9. [Rollback Procedures](#rollback-procedures)

## ✅ Prerequisites

### System Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **CPU** | 4 cores | 8 cores |
| **RAM** | 16GB | 32GB |
| **Storage** | 100GB SSD | 500GB SSD |
| **Network** | 1 Gbps | 10 Gbps |

### Software Requirements

- **Operating System**: Linux (Ubuntu 20.04+ or RHEL 8+)
- **Docker**: 24.0+
- **Kubernetes**: 1.28+
- **Helm**: 3.12+
- **kubectl**: 1.28+
- **Maven**: 3.9+
- **Java**: 21 (LTS)

### Required Accounts

- **Container Registry**: Docker Hub / AWS ECR / GCR
- **Git Provider**: GitHub / GitLab / Bitbucket
- **DNS Provider**: Route53 / CloudFlare / GoDaddy
- **Certificate Authority**: Let's Encrypt / Enterprise CA

## 🔧 Environment Setup

### 1. Clone Repository

```bash
git clone https://github.com/gogidix/platform.git
cd platform
```

### 2. Install Docker

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# CentOS/RHEL
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io
sudo systemctl start docker
sudo systemctl enable docker
```

### 3. Install Kubernetes

#### Option A: Minikube (Development)

```bash
# Install Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Start Minikube
minikube start --cpus=4 --memory=8192 --disk-size=100g
minikube addons enable ingress
minikube addons enable metrics-server
```

#### Option B: Kind (Local)

```bash
# Install Kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/

# Create cluster
kind create cluster --name gogidix --config=- <<EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
EOF
```

#### Option C: Production Cluster (GKE/EKS/AKS)

```bash
# Google Kubernetes Engine (GKE)
gcloud container clusters create gogidix-prod \
  --region=us-east1 \
  --node-locations=us-east1-a,us-east1-b,us-east1-c \
  --num-nodes=3 \
  --machine-type=e2-standard-4 \
  --enable-autoscaling \
  --min-nodes=3 \
  --max-nodes=10
```

### 4. Install Helm

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### 5. Install Required Tools

```bash
# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root kubectl /usr/local/bin/

# kubectx (alternative to kubectl)
kubectl krew install ctx
kubectl krew install ns
```

## 💻 Local Development Setup

### 1. Start Services

```bash
# Navigate to deployment directory
cd deployment

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f
```

### 2. Verify Services

```bash
# Check service health
curl http://localhost:8888/actuator/health

# Check API Gateway
curl http://localhost:8080/actuator/health

# Check individual services
curl http://localhost:8084/actuator/health  # Billing Service
curl http://localhost:8081/actuator/health  # User Service
```

### 3. Access Applications

- **API Gateway**: http://localhost:8080
- **Config Server**: http://localhost:8888
- **Service Registry**: http://localhost:8761
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090

### 4. Run Tests

```bash
# Build and test all services
mvn clean verify

# Run specific service tests
cd ../management-domain/java-services/billing-invoicing-service
mvn clean test
```

## 🚀 Staging Deployment

### 1. Prepare Environment Variables

Create `.env.staging`:

```bash
# Environment
ENVIRONMENT=staging

# Database Configuration
POSTGRES_HOST=staging-db.gogidix.com
POSTGRES_USER=gogidix_staging
POSTGRES_PASSWORD=${STAGING_DB_PASSWORD}

# Redis Configuration
REDIS_HOST=staging-redis.gogidix.com
REDIS_PASSWORD=${STAGING_REDIS_PASSWORD}

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=staging-kafka-1:9092,staging-kafka-2:9092,staging-kafka-3:9092

# JWT Configuration
GOGIDIX_JWT_SECRET=${STAGING_JWT_SECRET}

# External Services
STRIPE_API_KEY=${STAGING_STRIPE_KEY}
SENDGRID_API_KEY=${STAGING_SENDGRID_KEY}
```

### 2. Create Kubernetes Namespace

```yaml
# namespace-staging.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: gogidix-staging
  labels:
    environment: staging
    managed-by: gogidix-platform
```

```bash
kubectl apply -f namespace-staging.yaml
```

### 3. Deploy Infrastructure

```bash
# Deploy databases
kubectl apply -f k8s/infrastructure/postgres/
kubectl apply -f k8s/infrastructure/redis/
kubectl apply -f k8s/infrastructure/kafka/

# Wait for databases to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n gogidix-staging --timeout=300s
```

### 4. Deploy Monitoring

```bash
# Deploy monitoring stack
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace gogidix-monitoring \
  --create-namespace \
  --values k8s/monitoring/prometheus/values.yml

# Install Grafana dashboards
kubectl apply -f k8s/monitoring/grafana/dashboards/
```

### 5. Deploy Services

```bash
# Deploy core services
kubectl apply -f k8s/services/config-server/
kubectl apply -f k8s/services/service-registry/
kubectl apply -f k8s/services/api-gateway/

# Deploy business services
kubectl apply -f k8s/services/billing-invoicing-service/
kubectl apply -k8s/services/user-management-service/
kubectl apply -k8s/services/property-management-service/

# Wait for services to be ready
kubectl wait --for=condition=available deployment --all -n gogidix-staging --timeout=600s
```

### 6. Configure Ingress

```bash
# Install NGINX Ingress Controller
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace

# Apply Ingress rules
kubectl apply -f k8s/ingress/staging-ingress.yaml
```

## 🏭 Production Deployment

### 1. Security Hardening

#### Cluster Security

```yaml
# Pod Security Policy
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: gogidix-restrict
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - configMap
    - emptyDir
    - projected
    - secret
    - downwardAPI
    - persistentVolumeClaim
  runAsUser:
    rule: MustRunAsNonRoot
  seLinux:
    rule: RunAsAny
  fsGroup:
    rule: RunAsAny
```

#### Network Policies

```yaml
# Default deny all
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: gogidix-default-deny
  namespace: gogidix-prod
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress

# Allow internal traffic
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: gogidix-allow-internal
  namespace: gogidix-prod
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  ingress:
    - from:
      - namespaceSelector:
          matchLabels:
            name: gogidix-prod
  egress:
    - to:
      - namespaceSelector:
          matchLabels:
            name: gogidix-prod
```

### 2. Deploy with Blue-Green Strategy

```bash
# Deploy to green environment
kubectl apply -f k8s/services/ -n gogidix-prod-green

# Wait for green to be healthy
kubectl wait --for=condition=available deployment --all -n gogididix-prod-green --timeout=600s

# Switch traffic to green
kubectl patch service api-gateway -p '{"spec":{"selector":{"app":"api-gateway","version":"green"}}}'

# Verify switch
sleep 30

# Delete old blue environment
kubectl delete -f k8s/services/ -n gogidix-prod-blue
```

### 3. Apply Production Configurations

```bash
# Apply production secrets
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/configmaps/

# Deploy with production values
helm upgrade billing-invoicing-service ./charts/billing-invoicing-service \
  --namespace gogidix-prod \
  --values charts/billing-invoicing-service/values-prod.yml \
  --set image.tag=v1.0.0

# Deploy all services using automation script
./deployment/scripts/deploy-all.sh --env prod --version v1.0.0
```

### 4. Final Verification

```bash
# Health check all services
./deployment/scripts/health-check.sh --env prod

# Load test verification
./testing/performance/run-load-test.sh --target=https://api.gogogidix.com --users=1000

# Security scan
./testing/security/run-security-scan.sh --target=https://api.gogogidix.com
```

## 🔄 CI/CD Pipeline

### GitHub Actions Workflow

```yaml
name: Build and Deploy

on:
  push:
    branches: [main, develop]
    tags: ['v*']
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

    - name: Run tests
      run: mvn clean verify

    - name: Run security scan
      run: mvn dependency-check:check

    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-results
        path: target/surefire-reports/

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    if: github.event_name != 'pull_request'

    steps:
    - uses: actions/checkout@v4

    - name: Log in to Container Registry
      uses: docker/login-action@v3
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}

    - name: Build and push Docker image
      uses: docker/build-push-action@v5
      with:
        context: .
        platforms: linux/amd64,linux/arm64
        push: true
        tags: |
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest

  deploy-staging:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: staging

    steps:
    - uses: actions/checkout@v4

    - name: Configure kubectl
      uses: azure/k8s-set-context@v3
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG_STAGING }}

    - name: Deploy to staging
      run: |
        helm upgrade --install billing-invoicing-service ./charts/billing-invoicing-service \
          --namespace gogidix-staging \
          --set image.tag=${{ github.sha }} \
          --wait --timeout=10m

    - name: Run smoke tests
      run: |
        ./testing/smoke-tests/staging.sh

  deploy-production:
    needs: build-and-push
    runs-on: ubuntu-linux-large
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    environment: production

    steps:
    - uses: actions/checkout@v4

    - name: Configure kubectl
      uses: azure/k8s-set-context@v3
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG_PROD }}

    - name: Deploy to production
      run: |
        # Blue-green deployment
        ./deployment/scripts/blue-green-deploy.sh \
          --service billing-invoicing-service \
          --version ${{ github.sha }} \
          --namespace gogidix-prod

    - name: Run health checks
      run: |
        ./deployment/scripts/health-check.sh --env prod

    - name: Create release tag
      if: startsWith(github.ref, 'refs/tags/')
      uses: actions/create-release@v1
      with:
        tag_name: ${{ github.ref_name }}
        release_name: Release ${{ github.ref_name }}
        body: |
          🚀 Production deployment successful
          Service: ${{ github.repository }}
          Version: ${{ github.sha }}
        draft: false
        prerelease: false
```

## 📊 Monitoring Setup

### 1. Install Prometheus Operator

```bash
# Install Prometheus Operator
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install prometheus-operator prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --values monitoring/prometheus/operator-values.yaml
```

### 2. Configure Service Monitoring

```yaml
# monitoring/prometheus/service-monitors.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: billing-invoicing-service
  namespace: gogidix-prod
  labels:
    app: billing-invoicing-service
spec:
  selector:
    matchLabels:
      app: billing-invoicing-service
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
```

### 3. Set Up Grafana

```bash
# Install Grafana
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

helm install grafana grafana/grafana \
  --namespace monitoring \
  --create-namespace \
  --values monitoring/grafana/values.yaml \
  --set adminPassword=${GRAFANA_PASSWORD}

# Import dashboards
kubectl apply -f monitoring/grafana/dashboards/
```

### 4. Configure Alerting

```yaml
# monitoring/alertmanager/alertmanager.yaml
global:
  smtp_smarthost: localhost:587
  smtp_from: alerts@gogidix.com
  smtp_auth_username: ${SMTP_USERNAME}
  smtp_auth_password: ${SMTP_PASSWORD}

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'default'

receivers:
- name: 'default'
  email_configs:
  - to: 'ops-team@gogidix.com'
    subject: '[GOGIDIX] Alert: {{ .GroupLabels.alertname }}'
  slack_configs:
  - api_url: ${SLACK_WEBHOOK_URL}
    channel: '#alerts'
    title: 'Gogidix Alert'
    text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
```

## 🔧 Troubleshooting

### Common Issues

#### 1. Service Not Starting

```bash
# Check pod status
kubectl get pods -n gogidix-prod

# Describe pod for detailed info
kubectl describe pod <pod-name> -n gogidix-prod

# Check pod logs
kubectl logs <pod-name> -n gogidix-prod
```

#### 2. Database Connection Issues

```bash
# Check database secrets
kubectl get secret db-secret -n gogidix-prod -o yaml

# Test database connectivity
kubectl run postgres-client --image=postgres:15 --rm -it --restart=Never \
  --env="PGPASSWORD=$DB_PASSWORD" --env="PGUSER=$DB_USER" \
  --command="psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c 'SELECT 1'"
```

#### 3. Memory Issues

```bash
# Check pod resource usage
kubectl top pods -n gogidix-prod

# Check node resource usage
kubectl top nodes

# Adjust resource limits
kubectl patch deployment billing-invoicing-service -p '{"spec":{"template":{"spec":{"containers":[{"name":"billing-invoicing-service","resources":{"limits":{"memory":"2Gi"}}}]}}}}'
```

#### 4. Networking Issues

```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Check ingress rules
kubectl get ingress -n gogidix-prod

# Test service connectivity
kubectl run test-pod --image=curlimages/curl --rm -it \
  --command="curl -v http://api-gateway/actuator/health"
```

## 🔙 Rollback Procedures

### 1. Quick Rollback

```bash
# Rollback to previous deployment
kubectl rollout undo deployment/billing-invoicing-service -n gogidix-prod

# Check rollout status
kubectl rollout status deployment/billing-inilling-service -n gogidix-prod
```

### 2. Rollback to Specific Version

```bash
# Rollback to specific image tag
kubectl set image deployment/billing-invoicing-service \
  billing-invoicing-service=gogidix/billing-invoicing-service:v1.0.1 \
  -n gogidix-prod

# Restart pods with new image
kubectl rollout restart deployment/billing-invoicing-service -n gogidox-prod
```

### 3. Emergency Rollback

```bash
# Emergency stop all traffic
kubectl patch service api-gateway -p '{"spec":{"selector":{"app":"api-gateway","version":"blue"}}}'

# Scale down services
kubectl scale deployment billing-invoicing-service --replicas=0 -n gogidix-prod

# Restore from backup
./scripts/restore-database.sh --backup-id=backup_20251129_1200
```

## 📚 Additional Resources

### Documentation
- [API Reference](./docs/api/openapi.yml)
- [Architecture Guide](./docs/GOGIDIX_PLATFORM_TECHNICAL_DOCUMENTATION.md)
- [Security Guidelines](./docs/SECURITY_GUIDELINES.md)

### Tools
- [kubectx](https://kubectx.io/) - Kubernetes CLI alternative
- [Lens](https://k8slens.dev/) - Kubernetes IDE
- [K9s](https://k9scli.io/) - Kubernetes CLI TUI

### Training Materials
- [Shared Libraries Workshop](./training/workshops/)
- [API Design Guidelines](./training/api-design/)
- [Kubernetes Best Practices](./training/kubernetes/)

---

**Note**: This deployment guide is continuously updated. Check the repository for the latest version and updates.