# Gogidix Property Marketplace - Infrastructure Documentation

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Infrastructure Components](#infrastructure-components)
4. [Deployment Guides](#deployment-guides)
5. [Monitoring & Observability](#monitoring--observability)
6. [Security](#security)
7. [Backup & Disaster Recovery](#backup--disaster-recovery)
8. [Development Workflow](#development-workflow)
9. [Troubleshooting](#troubleshooting)
10. [Best Practices](#best-practices)

## Overview

The Gogidix Property Marketplace is built on a modern, cloud-native architecture using microservices. This infrastructure provides enterprise-grade scalability, reliability, and security.

### Key Features

- **Microservices Architecture**: 65+ services distributed across domains
- **Service Mesh**: Istio for service communication and traffic management
- **Container Orchestration**: Kubernetes for deployment and scaling
- **Observability**: Complete monitoring with Prometheus, Grafana, Jaeger, and ELK
- **Security**: Zero-trust network with mTLS, WAF, and identity management
- **High Availability**: Multi-zone deployment with automatic failover
- **GitOps**: Infrastructure as Code with Terraform and ArgoCD

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Internet                                │
└───────────────────────┬───────────────────────────────────────┘
                        │
┌───────────────────────▼───────────────────────────────────────┐
│                  CDN / CloudFlare WAF                         │
└───────────────────────┬───────────────────────────────────────┘
                        │
┌───────────────────────▼───────────────────────────────────────┐
│                     Istio Ingress                              │
│                 (Grafana, Kibana, Prometheus)                 │
└───────────────────────┬───────────────────────────────────────┘
                        │
              ┌─────────▼──────────┐
              │    API Gateway     │
              │ (Spring Cloud)     │
              └─────────┬──────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
│   Frontend   │ │   Core      │ │  Support   │
│              │ │ Services   │ │ Services   │
│ - React App  │ │ - Property  │ │ - Payment  │
│ - Mobile App │ │ - User      │ │ - Search   │
│              │ │ - Booking   │ │ - AI/ML    │
└──────────────┘ └─────────────┘ └────────────┘
```

### Data Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Data Layer                                 │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ PostgreSQL   │  │     Redis    │  │    Elasticsearch    │  │
│  │ - Primary    │  │ - Cache      │  │ - Search & Analytics│  │
│  │ - Read Replicas│ │ - Session    │  │ - Log Aggregation   │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │    Kafka     │  │  MinIO/S3    │  │      Vault          │  │
│  │ - Events     │  │ - Object     │  │ - Secrets           │  │
│  │ - Streaming  │  │   Storage    │  │ - Encryption        │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Infrastructure Components

### 1. API Gateway

**Technology**: Spring Cloud Gateway with Spring Boot 3.2.2

**Features**:
- Advanced routing (header, query, body-based)
- Distributed rate limiting with Redis
- Circuit breaking with Resilience4j
- API versioning and lifecycle management
- Web Application Firewall (WAF)
- Request/Response transformation
- Authentication and authorization

**Configuration**:
- Port: 8080
- Management: 8081
- Prometheus metrics: `/actuator/prometheus`
- Health check: `/actuator/health`

### 2. Service Mesh (Istio)

**Version**: 1.18+

**Components**:
- **Pilot**: Service discovery and traffic management
- **Citadel**: Security and mTLS
- **Galley**: Configuration validation
- **Mixer**: Policy enforcement (deprecated in 1.18)

**Features**:
- Automatic mTLS encryption
- Traffic splitting (canary deployments)
- Retry and timeout policies
- Distributed tracing
- Metrics collection

### 3. Identity Management (Keycloak)

**Version**: 23.0.0

**Features**:
- OAuth 2.0 and OpenID Connect
- SAML support
- Social login integration
- Multi-factor authentication
- Role-based access control (RBAC)
- User federation

**Realm Configuration**:
- Name: `gogidix-property`
- Roles: admin, agent, owner, user
- Clients: frontend, api-gateway, microservices

### 4. Secret Management (HashiCorp Vault)

**Version**: 1.14.0

**Features**:
- Dynamic secrets generation
- Data encryption
- Identity-based access
- Audit logging
- Auto-unsealing with AWS KMS

**Secret Engines**:
- `database/`: PostgreSQL dynamic credentials
- `pki/`: Certificate management
- `kv/`: Key-value storage
- `transit/`: Encryption as a service

### 5. Monitoring Stack

#### Prometheus
- **Version**: v2.45.0
- **HA Deployment**: 3 replicas
- **Retention**: 30 days, 100GB
- **Remote Write**: To long-term storage

#### Grafana
- **Version**: 10.1.0
- **Dashboards**: Pre-configured for all services
- **Authentication**: OAuth via Keycloak
- **Alerting**: Integrated with Alertmanager

#### Alertmanager
- **Routes**: Email, Slack, PagerDuty
- **Groups**: By service and severity
- **Inhibition**: Prevent alert storms

#### ELK Stack
- **Elasticsearch**: 8.11.3 (3 master, 6 data, 3 coordinating nodes)
- **Kibana**: Log visualization and analysis
- **Logstash**: Log processing and enrichment
- **Filebeat**: Log collection from all nodes

#### Jaeger
- **Version**: 1.50
- **Storage**: Elasticsearch
- **Sampling**: 0.1% in production
- **UI**: Distributed tracing visualization

### 6. Message Queuing

#### Kafka
- **Version**: 7.4.0
- **Topics**: Partitioned by service
- **Replication**: 3x
- **Retention**: 7 days

#### RabbitMQ
- **Version**: 3.12
- **Policies**: HA for critical queues
- **Management**: Enabled on port 15672

### 7. Storage

#### PostgreSQL
- **Version**: 15
- **HA**: Patroni with automatic failover
- **Backups**: pgBaseRunner with WAL-E
- **Read Replicas**: 3 instances

#### Redis
- **Version**: 7
- **Mode**: Cluster (6 nodes)
- **Persistence**: RDB + AOF
- **Sharding**: Automatic

#### MinIO/S3
- **Version**: RELEASE.2023-11-01T01-56-33Z
- **Replication**: Erasure coding
- **Lifecycle**: Automated tiering

## Deployment Guides

### Local Development

See [local-deployment/README.md](local-deployment/README.md)

### Staging Environment

**Infrastructure**: AWS EKS
**Node Type**: t3.large (2 vCPU, 8GB RAM)
**Nodes**: 3 (auto-scaling to 10)
**Regions**: us-east-1

Deploy with:
```bash
cd terraform/environments/staging
terraform init
terraform apply -auto-approve
```

### Production Environment

**Infrastructure**: AWS EKS
**Node Type**: m5.xlarge (4 vCPU, 16GB RAM)
**Nodes**: 6 (auto-scaling to 50)
**Regions**: Multi-AZ in us-east-1

Deploy with:
```bash
cd terraform/environments/production
terraform init
terraform apply -auto-approve
```

### Blue-Green Deployment

1. Deploy new version to green namespace
2. Run smoke tests
3. Switch traffic with Istio
4. Monitor for issues
5. Promote blue to previous

### Canary Deployments

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: reviews
spec:
  http:
  - match:
    - headers:
        canary:
          exact: "true"
    route:
    - destination:
        host: reviews
        subset: v2
      weight: 100
  - route:
    - destination:
        host: reviews
        subset: v1
      weight: 90
    - destination:
        host: reviews
        subset: v2
      weight: 10
```

## Monitoring & Observability

### Metrics Collection

All services expose Prometheus metrics at `/actuator/prometheus`

Key metrics:
- `http_requests_total`: Request count by status
- `http_request_duration_seconds`: Request latency
- `jvm_memory_used_bytes`: JVM memory usage
- `hikaricp_connections_active`: Database connections
- `custom_business_metrics_*`: Domain-specific metrics

### Logging

Standard log format:
```json
{
  "timestamp": "2024-01-28T10:30:45.123Z",
  "level": "INFO",
  "service": "property-service",
  "trace_id": "abc123",
  "span_id": "def456",
  "message": "Property created",
  "user_id": "user-789",
  "property_id": "prop-456"
}
```

### Tracing

Distributed tracing with OpenTelemetry:
- Automatic trace propagation
- Custom spans for business operations
- Jaeger UI for trace visualization
- Correlation with logs and metrics

### Alerting Rules

Critical alerts:
- Service down
- Error rate > 5%
- P95 latency > 1s
- Disk usage > 85%
- Memory usage > 90%

Warning alerts:
- High GC time
- Database connection pool saturation
- Queue backlog
- Certificate expiration

## Security

### Network Security

- **Zero Trust**: All traffic encrypted with mTLS
- **Network Policies**: Namespace-level isolation
- **Egress Controls**: Allowed outbound traffic only
- **WAF Rules**: OWASP Top 10 protection

### Authentication & Authorization

- **OAuth 2.0**: Via Keycloak
- **JWT Tokens**: Signed with RSA keys
- **Fine-grained permissions**: Policy-based access control
- **Service Accounts**: For service-to-service communication

### Secret Management

- **Vault**: Centralized secret storage
- **Dynamic secrets**: Short-lived credentials
- **Encryption**: AES-256-GCM
- **Audit trails**: All access logged

### Compliance

- **GDPR**: Data protection for EU users
- **SOC 2**: Security controls documentation
- **PCI DSS**: Payment card security
- **HIPAA**: Healthcare data protection

## Backup & Disaster Recovery

### Backup Strategy

1. **Database Backups**
   - Continuous WAL archiving
   - Daily full backups
   - Point-in-time recovery (30 days)

2. **Volume Snapshots**
   - EBS snapshots every 6 hours
   - Cross-region replication
   - 90-day retention

3. **Configuration Backups**
   - Git version control
   - Terraform state
   - Kubernetes resources

### Recovery Objectives

| Service | RTO | RPO |
|---------|-----|-----|
| Critical | < 30 min | < 15 min |
| Important | < 2 hours | < 1 hour |
| Standard | < 4 hours | < 4 hours |

### Disaster Recovery Plan

See [backup/disaster-recovery-plan.md](backup/disaster-recovery-plan.md)

## Development Workflow

### Code Repository Structure

```
gogidix-property-marketplace/
├── Gogidix-Domain/
│   ├── foundation-domain/
│   │   ├── shared-infrastructure/
│   │   ├── java-services/
│   │   ├── web-ui/
│   │   └── mobile-app/
│   ├── property-domain/
│   ├── user-domain/
│   └── payment-domain/
├── deployment/
│   ├── terraform/
│   ├── helm-charts/
│   └── kubernetes/
└── docs/
    ├── architecture/
    ├── api/
    └── runbooks/
```

### CI/CD Pipeline

1. **Commit**: Push to feature branch
2. **Build**: Compile and test
3. **Security Scan**: SAST, dependency check
4. **Package**: Docker image build
5. **Deploy**: To staging environment
6. **Test**: Integration and E2E tests
7. **Promote**: To production (manual approval)

### Branching Strategy

- **main**: Production code
- **develop**: Integration branch
- **feature/***: New features
- **hotfix/***: Critical fixes
- **release/***: Release preparation

## Troubleshooting

### Common Issues

#### Service Not Starting
1. Check pod status: `kubectl get pods`
2. View logs: `kubectl logs -f <pod>`
3. Check events: `kubectl describe pod <pod>`
4. Verify resources: CPU/memory limits

#### Database Connection Issues
1. Check secret: `kubectl get secret db-creds`
2. Test connectivity: `kubectl exec -it <pod> -- psql`
3. Check pool settings: HikariCP metrics
4. Verify network policies

#### High Latency
1. Check resources: CPU/memory throttling
2. Review metrics: GC time, thread pool
3. Trace requests: Jaeger UI
4. Check dependencies: Downstream services

#### Memory Leaks
1. Heap dump: `kubectl exec -it <pod> -- jcmd <pid> GC.heap_dump`
2. Analyze with MAT or JProfiler
3. Check for unclosed resources
4. Review thread dumps

### Debugging Tools

- **kubectl**: Cluster debugging
- **stern**: Multi-pod log tailing
- **k9s**: Interactive cluster viewer
- **lens**: Kubernetes IDE
- **skaffold**: Development workflow
- **telepresence**: Local development

## Best Practices

### Development

1. **12-Factor App**
   - Externalize configuration
   - Stateless processes
   - Self-contained services

2. **Code Quality**
   - Unit tests > 80% coverage
   - Code reviews mandatory
   - Static analysis with SonarQube

3. **Documentation**
   - API specs with OpenAPI
   - Architecture decision records (ADRs)
   - Runbooks for operations

### Operations

1. **Observability**
   - Structured logging
   - Business metrics
   - Custom dashboards

2. **Reliability**
   - Graceful degradation
   - Bulkheads isolation
   - Timeout and retry policies

3. **Performance**
   - Async processing
   - Caching strategies
   - Resource optimization

### Security

1. **Shift Left**
   - Security in CI/CD
   - Dependency scanning
   - Threat modeling

2. **Defense in Depth**
   - Multiple layers of security
   - Principle of least privilege
   - Regular security audits

## Glossary

| Term | Definition |
|------|------------|
| API Gateway | Entry point for all client requests |
| Circuit Breaker | Fault tolerance pattern for service calls |
| Ingress Controller | Manages external access to services |
| Service Mesh | Dedicated infrastructure layer for service communication |
| Sidecar | Container that runs alongside the main application container |
| Canary Release | Deployment strategy that rolls out changes to a small subset |
| Blue-Green Deployment | Deployment strategy with two identical environments |
| Observability | Ability to understand system's internal state from external data |
| SLO (Service Level Objective) | Target level for a service's reliability |
| SLI (Service Level Indicator) | Metric that measures service reliability |

## Contact

- **Infrastructure Team**: infra@gogidix.com
- **Security Team**: security@gogidix.com
- **On-call Rotation**: ops@gogidix.com
- **Documentation**: docs@gogidix.com
- **Support Portal**: https://support.gogidix.com

---

*Last updated: January 28, 2024*
*Version: 1.0.0*