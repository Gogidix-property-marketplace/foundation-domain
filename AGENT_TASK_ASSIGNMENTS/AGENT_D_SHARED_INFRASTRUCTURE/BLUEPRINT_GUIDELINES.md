# BLUEPRINT GUIDELINES - API GATEWAY REFERENCE

## Current API Gateway Enhancement

The existing API Gateway serves as our primary blueprint and foundation:
```
C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\java-services\api-gateway
```

## Enhancement Strategy

### 1. API Gateway Architecture Enhancement

**Current Implementation Analysis**:
```java
// Existing API Gateway patterns to enhance
@RestController
@RequestMapping("/api/v1")
public class GatewayController {

    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        // Enhanced health check implementation
        return ResponseEntity.ok(healthCheckService.getHealth());
    }

    // Request routing patterns to enhance
    @RequestMapping(value = "/services/{service}/**")
    public ResponseEntity<?> routeRequest(
        @PathVariable String service,
        HttpServletRequest request) {
        // Enhanced routing with advanced features
        return gatewayService.route(service, request);
    }
}
```

**Enhancement Areas**:
- **Advanced Routing**: Header, query parameter, and body-based routing
- **Rate Limiting**: User, service, and global rate limiting
- **Circuit Breaking**: Enhanced circuit breaking with recovery
- **Load Balancing**: Advanced load balancing algorithms
- **Security**: Enhanced security with WAF capabilities
- **Monitoring**: Rich telemetry and analytics

### 2. Service Mesh Integration

**Service Mesh Patterns**:
```yaml
# Istio service mesh configuration following API Gateway patterns
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api-gateway-vs
spec:
  hosts:
  - api-gateway
  http:
  - match:
    - uri:
        prefix: "/api/v1/"
    route:
    - destination:
        host: api-gateway
        port:
          number: 8080
    fault:
      delay:
        percentage:
          value: 0.1
        fixedDelay: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
```

### 3. Infrastructure as Code (IaC) Patterns

**Terraform Structure Following API Gateway Patterns**:
```hcl
# Enhanced infrastructure following existing patterns
module "api_gateway" {
  source = "./modules/api-gateway"

  # Enhanced configuration
  name = "gogidix-api-gateway"

  # Advanced routing configuration
  routing_rules = [
    {
      path = "/api/v1/shared/*"
      service = "agent-a-services"
      weight = 100
    },
    {
      path = "/api/v1/dashboard/*"
      service = "agent-b-dashboard"
      weight = 100
    },
    {
      path = "/api/v1/ai/*"
      service = "agent-c-ai-services"
      weight = 100
    }
  ]

  # Enhanced security configuration
  security_config = {
    enable_waf = true
    rate_limiting = {
      requests_per_second = 1000
      burst = 5000
    }
    cors_policy = {
      allowed_origins = ["https://dashboard.gogidix.com"]
      allowed_methods = ["GET", "POST", "PUT", "DELETE"]
    }
  }
}
```

## Monitoring and Observability Enhancement

### Prometheus Metrics Enhancement
```yaml
# Enhanced metrics collection following API Gateway patterns
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'api-gateway'
    static_configs:
      - targets: ['api-gateway:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'service-mesh'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names: ["istio-system"]
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true

  - job_name: 'application-metrics'
    kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names: ["gogidix"]
    relabel_configs:
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
        action: keep
        regex: true
```

### Grafana Dashboard Enhancement
```json
{
  "dashboard": {
    "title": "Gogidix Infrastructure Overview",
    "panels": [
      {
        "title": "API Gateway Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{service}} - {{method}}"
          }
        ]
      },
      {
        "title": "Service Mesh Health",
        "type": "table",
        "targets": [
          {
            "expr": "istio_requests_total",
            "format": "table"
          }
        ]
      },
      {
        "title": "Infrastructure Resource Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(container_cpu_usage_seconds_total[5m])",
            "legendFormat": "{{pod}}"
          }
        ]
      }
    ]
  }
}
```

## Security Enhancement Patterns

### Zero Trust Architecture
```yaml
# Network policies following API Gateway security patterns
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: api-gateway-netpol
spec:
  podSelector:
    matchLabels:
      app: api-gateway
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: istio-ingressgateway
    - namespaceSelector:
        matchLabels:
          name: istio-system
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: agent-a-services
    - podSelector:
        matchLabels:
          app: agent-b-dashboard
    - podSelector:
        matchLabels:
          app: agent-c-ai-services
```

### Certificate Management
```yaml
# Enhanced certificate management
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: infrastructure@gogidix.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: gogidix-tls
spec:
  secretName: gogidix-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
  - api.gogidix.com
  - dashboard.gogidix.com
  - ai.gogidix.com
```

## DevOps Enhancement Patterns

### Enhanced CI/CD Pipeline
```yaml
# Enhanced GitHub Actions following API Gateway patterns
name: Enhanced Infrastructure Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test-and-validate:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Set up Terraform
      uses: hashicorp/setup-terraform@v2

    - name: Terraform fmt
      run: terraform fmt -check

    - name: Terraform init
      run: terraform init

    - name: Terraform validate
      run: terraform validate

    - name: Terraform plan
      run: terraform plan

    - name: Security scan
      uses: securecodewarrior/github-action-add-sarif@v1
      with:
        sarif-file: 'security-scan-results.sarif'

  deploy-infrastructure:
    needs: test-and-validate
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
    - uses: actions/checkout@v3

    - name: Deploy to production
      run: |
        terraform apply -auto-approve

    - name: Update monitoring
      run: |
        kubectl apply -f monitoring/

    - name: Run smoke tests
      run: |
        ./scripts/smoke-tests.sh
```

### GitOps with ArgoCD
```yaml
# ArgoCD application following API Gateway patterns
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: gogidix-infrastructure
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/gogidix/infrastructure
    targetRevision: HEAD
    path: environments/production
  destination:
    server: https://kubernetes.default.svc
    namespace: gogidix
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

## Performance Enhancement Patterns

### Auto-scaling Configuration
```yaml
# Enhanced HPA following API Gateway patterns
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 3
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
```

### Resource Optimization
```yaml
# Enhanced resource configuration
apiVersion: v1
kind: LimitRange
metadata:
  name: gogidix-limits
spec:
  limits:
  - default:
      cpu: "500m"
      memory: "512Mi"
    defaultRequest:
      cpu: "100m"
      memory: "128Mi"
    type: Container
  - max:
      cpu: "2"
      memory: "4Gi"
    min:
      cpu: "50m"
      memory: "64Mi"
    type: Container
```

## Backup and Disaster Recovery

### Automated Backup Configuration
```yaml
# Velero backup configuration following API Gateway patterns
apiVersion: velero.io/v1
kind: Schedule
metadata:
  name: gogidix-daily-backup
  namespace: velero
spec:
  schedule: "0 2 * * *"
  template:
    includedNamespaces:
    - gogidix
    - istio-system
    - monitoring
    ttl: "720h0m0s"
    storageLocation: aws-backup
    volumeSnapshotLocations:
    - aws-volume-snapshots
---
apiVersion: velero.io/v1
kind: BackupStorageLocation
metadata:
  name: aws-backup
  namespace: velero
spec:
  provider: aws
  objectStorage:
    bucket: gogidix-velero-backups
    prefix: backup
  config:
    region: us-west-2
```

## Compliance and Governance

### Policy as Code
```yaml
# Open Policy Agent (OPA) policies following API Gateway patterns
package gogidix.infrastructure

deny[msg] {
    input.kind == "Deployment"
    not input.spec.template.spec.securityContext.runAsNonRoot
    msg := "Containers must run as non-root user"
}

deny[msg] {
    input.kind == "Deployment"
    count(input.spec.template.spec.containers[_].resources.requests) == 0
    msg := "Containers must have resource requests defined"
}

deny[msg] {
    input.kind == "Service"
    input.spec.type == "LoadBalancer"
    not contains(input.metadata.annotations["service.beta.kubernetes.io/aws-load-balancer-internal"], "true")
    msg := "LoadBalancer services must be internal"
}
```

### Compliance Monitoring
```yaml
# Compliance monitoring configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: compliance-config
  namespace: monitoring
data:
  compliance-rules.yaml: |
    rules:
    - name: "SOC 2 - Encryption at Rest"
      description: "All persistent storage must be encrypted"
      check: |
        kubectl get pvc -o json | jq '.items[].spec.storageClassName' | grep -E "(encrypted|gp2-encrypted)"
      severity: "critical"

    - name: "SOC 2 - Network Segmentation"
      description: "Network policies must be defined"
      check: |
        kubectl get netpol -A | wc -l | grep -v "^0$"
      severity: "high"
```

## Integration Guidelines

### Agent Integration Patterns
```yaml
# Service integration following API Gateway patterns
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: agent-a-services
spec:
  host: agent-a-services
  trafficPolicy:
    loadBalancer:
      simple: LEAST_CONN
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 10
    circuitBreaker:
      consecutiveErrors: 3
      interval: 30s
      baseEjectionTime: 30s
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: agent-a-routing
spec:
  hosts:
  - api-gateway
  http:
  - match:
    - uri:
        prefix: "/api/v1/shared/"
    route:
    - destination:
        host: agent-a-services
        subset: v1
      weight: 90
    - destination:
        host: agent-a-services
        subset: v2
      weight: 10
    timeout: 30s
    retries:
      attempts: 3
      perTryTimeout: 10s
```

---

**Reference Document**: Enhanced API Gateway Implementation
**Last Updated**: 2025-11-28
**Review Frequency**: Monthly
**Approval Required**: CTO Architecture Review