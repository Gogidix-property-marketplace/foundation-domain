# Production Deployment Guide

## Table of Contents
- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [Infrastructure Deployment](#infrastructure-deployment)
- [Application Deployment](#application-deployment)
- [Configuration](#configuration)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software
- Kubernetes 1.24+ with GPU support
- NVIDIA GPU Operator
- Helm 3.8+
- kubectl 1.24+
- Docker 20.10+
- Python 3.9+
- Node.js 18+ (for frontend)

### Hardware Requirements

#### Minimum Requirements
- **Control Plane**: 3 nodes, 4 vCPUs, 8GB RAM each
- **Worker Nodes**: 3 nodes, 8 vCPUs, 32GB RAM each
- **GPU Nodes**: 2 nodes with NVIDIA Tesla T4 or V100
- **Storage**: 500GB SSD for etcd, 2TB for persistent storage
- **Network**: 10Gbps between nodes

#### Recommended Production Setup
- **Control Plane**: 5 nodes, 8 vCPUs, 16GB RAM each
- **Worker Nodes**: 10+ nodes, 16 vCPUs, 64GB RAM each
- **GPU Nodes**: 4+ nodes with NVIDIA A100 or H100
- **Storage**: 1TB NVMe for etcd, 10TB+ for persistent storage
- **Network**: 25Gbps+ with redundant paths

## Environment Setup

### 1. Clone Repository
```bash
git clone https://github.com/gogidix/ai-services.git
cd ai-services
```

### 2. Install Dependencies
```bash
# Python dependencies
pip install -r requirements.txt
pip install -r requirements-prod.txt

# Kubernetes dependencies
kubectl apply -f https://github.com/kubernetes-sigs/gpu-operator/releases/latest/gpu-operator.yaml
```

### 3. Configure Namespace
```bash
kubectl create namespace gogidix-ai
kubectl label namespace gogidix-ai name=gogidix-ai
```

## Infrastructure Deployment

### 1. NVIDIA GPU Operator
```bash
helm repo add nvidia https://nvidia.github.io/gpu-operator
helm repo update

helm install gpu-operator nvidia/gpu-operator \
  --namespace gpu-operator \
  --create-namespace \
  --set devicePlugin.enabled=true \
  --set migManager.enabled=true \
  --set toolkit.enabled=true
```

### 2. Istio Service Mesh (Optional)
```bash
istioctl install --set profile=prod -y
kubectl label namespace gogidix-ai istio-injection=enabled
```

### 3. Cert-Manager for TLS
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.12.0/cert-manager.yaml
```

### 4. Prometheus Stack
```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --values monitoring/prometheus-values.yaml
```

### 5. ELK Stack for Logging
```bash
helm repo add elastic https://helm.elastic.co
helm repo update

helm install elasticsearch elastic/elasticsearch \
  --namespace logging \
  --create-namespace \
  --values logging/elasticsearch-values.yaml

helm install kibana elastic/kibana \
  --namespace logging \
  --values logging/kibana-values.yaml
```

## Application Deployment

### 1. Create Secrets
```bash
# Create namespace
kubectl create namespace gogidix-ai

# Database secrets
kubectl create secret generic postgres-secrets \
  --from-literal=username=postgres_user \
  --from-literal=password=postgres_password \
  --namespace gogidix-ai

# API keys
kubectl create secret generic api-keys \
  --from-literal=openai-api-key=your_openai_key \
  --from-literal=google-maps-api-key=your_google_maps_key \
  --namespace gogidix-ai

# JWT secrets
kubectl create secret generic jwt-secrets \
  --from-literal=secret-key=your_jwt_secret_key \
  --namespace gogidix-ai
```

### 2. Deploy Data Infrastructure
```bash
# PostgreSQL
kubectl apply -f kubernetes/data/postgresql.yaml

# Redis
kubectl apply -f kubernetes/data/redis.yaml

# Kafka
kubectl apply -f kubernetes/data/kafka.yaml

# Elasticsearch
kubectl apply -f kubernetes/data/elasticsearch.yaml

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n gogidix-ai --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n gogidix-ai --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n gogidix-ai --timeout=300s
```

### 3. Deploy ML Infrastructure
```bash
# MLflow
kubectl apply -f kubernetes/ml-platform/01-mlflow.yaml

# Kubeflow
kubectl apply -f kubernetes/ml-platform/02-kubeflow.yaml

# TFX
kubectl apply -f kubernetes/ml-platform/03-tfx.yaml

# Katib for hyperparameter tuning
kubectl apply -f kubernetes/ml-platform/04-katib.yaml
```

### 4. Deploy AI Services
```bash
# AI Gateway
kubectl apply -f kubernetes/01-ai-gateway.yaml

# Property Intelligence Service
kubectl apply -f kubernetes/02-property-intelligence.yaml

# Conversational AI Service
kubectl apply -f kubernetes/03-conversational-ai.yaml

# Analytics Service
kubectl apply -f kubernetes/analytics/analytics-deployment.yaml

# Ethical AI Service
kubectl apply -f kubernetes/04-ethical-ai.yaml

# ML Platform Service
kubectl apply -f kubernetes/ml-platform/ml-platform-deployment.yaml
```

### 5. Configure Ingress
```bash
# Install NGINX Ingress Controller
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace

# Deploy Ingress resources
kubectl apply -f kubernetes/ingress/
```

### 6. Configure Autoscaling
```bash
# Cluster Autoscaler
helm install cluster-autoscaler \
  cluster-autoscaler/cluster-autoscaler \
  --namespace kube-system \
  --set autoDiscovery.clusterName=gogidix-ai-cluster \
  --set scaleDownDelayAfterAdd=10m \
  --set scaleDownUnneededTime=10m

# Check HPA status
kubectl get hpa -n gogidix-ai
```

## Configuration

### Environment Variables
Create `config/prod.env`:

```bash
# Application Settings
APP_NAME=Gogidix AI Services
ENVIRONMENT=production
LOG_LEVEL=INFO
DEBUG=false

# Database Configuration
DATABASE_URL=postgresql://postgres_user:postgres_password@postgres:5432/gogidix_ai
REDIS_URL=redis://redis:6379/0

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_TOPIC_PREFIX=gogidix-ai

# API Gateway
API_GATEWAY_URL=https://api.gogidix.com
SERVICE_NAME=ai-services
SERVICE_ID=ai-services-prod
GATEWAY_API_KEY=your_production_api_key

# GPU Settings
GPU_ENABLED=true
CUDA_VISIBLE_DEVICES=0,1,2,3

# ML Configuration
MODEL_REGISTRY_URL=http://mlflow:5000
MODEL_SERVING_URL=http://model-serving:8080

# External APIs
OPENAI_API_KEY=your_openai_key
GOOGLE_MAPS_API_KEY=your_google_maps_key

# Ethical AI
ETHICAL_AI_ENABLED=true
BIAS_DETECTION_ENABLED=true
COMPLIANCE_MONITORING_ENABLED=true
```

### TLS Configuration
```bash
# Create TLS certificate
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: gogidix-ai-tls
  namespace: gogidix-ai
spec:
  secretName: gogidix-ai-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
    - api.gogidix.com
    - ai.gogidix.com
EOF
```

## Monitoring

### 1. Prometheus Metrics
```bash
# Access Prometheus dashboard
kubectl port-forward -n monitoring svc/prometheus-server 9090:80

# Service-specific metrics
curl http://prometheus-server.monitoring.svc.cluster.local:9090/metrics
```

### 2. Grafana Dashboards
```bash
# Access Grafana
kubectl port-forward -n monitoring svc/grafana 3000:80

# Import dashboards
kubectl apply -f monitoring/grafana-dashboards/
```

### 3. Health Checks
```bash
# Check all services
kubectl get pods -n gogidix-ai

# Service health endpoints
curl https://api.gogidix.com/health
curl https://api.gogidix.com/metrics
```

### 4. Alerting Rules
```bash
# Apply Prometheus rules
kubectl apply -f monitoring/alert-rules/

# Configure AlertManager
kubectl apply -f monitoring/alertmanager/
```

## Security

### 1. Network Policies
```bash
# Apply network policies
kubectl apply -f security/network-policies.yaml
```

### 2. Pod Security Policies
```bash
# Apply Pod Security Standards
kubectl apply -f security/pod-security-standards.yaml
```

### 3. RBAC
```bash
# Create service accounts
kubectl apply -f security/rbac.yaml
```

## Backup and Recovery

### 1. Database Backup
```bash
# Create backup CronJob
kubectl apply -f backup/postgres-backup.yaml

# Manual backup
kubectl exec -it postgres-0 -n gogidix-ai -- pg_dumpall -U postgres_user > backup.sql
```

### 2. Model Backup
```bash
# Backup MLflow models
kubectl exec -it deployment/mlflow -n gogidix-ai -- \
  tar -czf /tmp/models-backup.tar.gz /mlflow/models

# Copy backup
kubectl cp mlflow-xxx:/tmp/models-backup.tar.gz ./models-backup.tar.gz
```

## Performance Tuning

### 1. Resource Allocation
```yaml
# Example: High-performance deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-gateway-hp
spec:
  replicas: 5
  template:
    spec:
      containers:
      - name: ai-gateway
        resources:
          requests:
            cpu: "2"
            memory: "4Gi"
            nvidia.com/gpu: "0"
          limits:
            cpu: "4"
            memory: "8Gi"
            nvidia.com/gpu: "0"
```

### 2. Node Affinity
```yaml
# Schedule GPU workloads on GPU nodes
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      nodeSelectorTerms:
      - matchExpressions:
        - key: accelerator
          operator: Exists
        - key: nvidia.com/gpu.present
          operator: Exists
```

## Troubleshooting

### Common Issues

#### 1. Pods Not Starting
```bash
# Check pod events
kubectl describe pod <pod-name> -n gogidix-ai

# Check logs
kubectl logs <pod-name> -n gogidix-ai -f

# Check resource usage
kubectl top nodes
kubectl top pods -n gogidix-ai
```

#### 2. GPU Issues
```bash
# Check GPU nodes
kubectl get nodes -l accelerator=nvidia-tesla-v100

# Check GPU operator
kubectl logs -n gpu-operator deployment/gpu-operator

# Verify GPU in pod
kubectl exec -it <gpu-pod> -- nvidia-smi
```

#### 3. Network Issues
```bash
# Test service connectivity
kubectl exec -it <pod> -- nc -zv <service-name> 80

# Check DNS
kubectl exec -it <pod> -- nslookup kubernetes.default

# Check network policies
kubectl get networkpolicy -n gogidix-ai
```

#### 4. Database Issues
```bash
# Check database connection
kubectl exec -it postgres-0 -n gogidix-ai -- psql -U postgres_user -d gogidix_ai

# Check Redis
kubectl exec -it redis-0 -n gogidix-ai -- redis-cli ping
```

### Emergency Procedures

#### 1. Service Recovery
```bash
# Restart deployment
kubectl rollout restart deployment/ai-gateway -n gogidix-ai

# Scale up quickly
kubectl scale deployment ai-gateway --replicas=10 -n gogidix-ai

# Rollback to previous version
kubectl rollout undo deployment/ai-gateway -n gogidix-ai
```

#### 2. Emergency Shutdown
```bash
# Scale down all services
kubectl scale deployment --all --replicas=0 -n gogidix-ai

# Force delete stuck pods
kubectl delete pod --all --force --grace-period=0 -n gogidix-ai
```

## Maintenance

### 1. Rolling Updates
```bash
# Update with zero downtime
kubectl set image deployment/ai-gateway \
  ai-gateway=gogidix/ai-gateway:v1.1.0 \
  -n gogidix-ai

# Monitor rollout status
kubectl rollout status deployment/ai-gateway -n gogidix-ai
```

### 2. Certificate Renewal
```bash
# Check certificate expiration
kubectl describe certificate gogidix-ai-tls -n gogidix-ai

# Manual renewal
kubectl delete certificate gogidix-ai-tls -n gogidix-ai
kubectl apply -f kubernetes/certificates/
```

## Performance Benchmarks

### Expected Performance Metrics
- **API Response Time**: <100ms (p95)
- **Model Inference**: <500ms (CPU), <100ms (GPU)
- **Throughput**: 10,000 RPS per service
- **Availability**: 99.9% uptime
- **GPU Utilization**: >80% during peak
- **Memory Usage**: <70% allocated

### Load Testing
```bash
# Install k6
brew install k6

# Run load test
k6 run --out json=results.json tests/load-test.js
```

## Support

### 1. Documentation
- [API Documentation](https://docs.gogidix.com/api)
- [Architecture Guide](https://docs.gogidix.com/architecture)
- [Best Practices](https://docs.gogidix.com/best-practices)

### 2. Monitoring Dashboards
- Grafana: https://grafana.gogidix.com
- Kibana: https://kibana.gogidix.com
- MLflow: https://mlflow.gogidix.com

### 3. Alert Contacts
- On-call Engineering: oncall@gogidix.com
- Infrastructure Team: infra@gogidix.com
- Security Team: security@gogidix.com