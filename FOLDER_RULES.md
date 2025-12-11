# 📋 Foundation-Domain Folder Rules

## 🏗️ Domain Purpose
The Foundation-Domain serves as the foundational layer for the Gogidix Property Marketplace, providing core shared libraries, infrastructure components, and AI/ML services that support all other domains.

## 📁 Folder Structure Diagram

```
📁 foundation-domain/
├── 📁 shared-libraries/                    # Core shared libraries and components
│   ├── 📁 backend-services/              # Production-ready microservices
│   │   ├── 📁 java-services/            # Java microservices (10 services)
│   │   │   ├── 📁 gogidix-common-audit/      # Audit logging framework
│   │   │   ├── 📁 gogidix-common-cache/     # Distributed caching
│   │   │   ├── 📁 gogidix-common-client/    # API client libraries
│   │   │   ├── 📁 gogidix-common-core/      # Core utilities and base classes
│   │   │   ├── 📁 gogidix-common-messaging/ # Event-driven messaging
│   │   │   ├── 📁 gogidix-common-monitoring/# Application monitoring
│   │   │   ├── 📁 gogidix-common-persistence/# Database persistence layer
│   │   │   ├── 📁 gogidix-common-security/   # Security and authentication
│   │   │   ├── 📁 gogidix-common-testing/    # Testing framework utilities
│   │   │   └── 📁 gogidix-common-validation/ # Input validation framework
│   │   ├── 📁 deployment/                # Docker, Kubernetes, Helm configs
│   │   ├── 📁 docs/                     # Technical documentation
│   │   ├── 📁 testing/                  # Testing frameworks
│   │   └── 📁 training/                 # Team training materials
│   └── 📁 java-services/                # Legacy Java libraries (being phased out)
│
├── 📁 shared-infrastructure/               # Platform infrastructure components
│   ├── 📁 backend-services/              # Infrastructure services (83 total)
│   │   ├── 📁 java-services/            # Infrastructure Java services (65+)
│   │   │   ├── 📁 aggregation-service/      # Log and metrics aggregation
│   │   │   ├── 📁 api-gateway/             # API gateway and routing
│   │   │   ├── 📁 authentication-service/  # Identity and auth
│   │   │   ├── 📁 authorization-service/   # RBAC and permissions
│   │   │   ├── 📁 monitoring-service/      # System monitoring
│   │   │   ├── 📁 notification-service/    # Alerting and notifications
│   │   │   ├── 📁 registry-service/        # Service discovery
│   │   │   └── 📁 [57 more infrastructure services...]
│   │   └── 📁 nodejs-services/          # Infrastructure Node.js services (18)
│   │       ├── 📁 admin-console/           # System administration console
│   │       ├── 📁 alert-manager/          # Alert management UI
│   │       ├── 📁 monitoring-dashboard/   # Monitoring dashboard
│   │       └── 📁 [15 more Node.js services...]
│   ├── 📁 platform-services/            # Platform component services
│   │   ├── 📁 identity-services/         # Keycloak and identity management
│   │   ├── 📁 monitoring-services/       # Prometheus and monitoring
│   │   ├── 📁 logging-services/          # ELK stack logging
│   │   ├── 📁 security-services/         # Vault and security
│   │   └── 📁 backup-services/           # Velero backup solutions
│   ├── 📁 infrastructure/               # Infrastructure as code
│   │   ├── 📁 terraform/                 # Cloud infrastructure
│   │   ├── 📁 istio/                     # Service mesh configuration
│   │   └── 📁 cicd/                      # CI/CD pipeline definitions
│   ├── 📁 deployment/                   # Deployment configurations
│   ├── 📁 tools/                        # Development and operational tools
│   └── 📁 docs/                         # Infrastructure documentation
│
├── 📁 centralized-dashboard/               # Analytics and dashboard platform
│   ├── 📁 backend-services/              # Dashboard services (17 total)
│   │   ├── 📁 java-services/            # Backend analytics services (9)
│   │   │   ├── 📁 agent-dashboard-service/     # Real estate agent analytics
│   │   │   ├── 📁 alert-management-service/   # Alert and notification mgmt
│   │   │   ├── 📁 analytics-service/         # Core analytics engine
│   │   │   ├── 📁 centralized-dashboard/     # Main dashboard service
│   │   │   ├── 📁 custom-dashboard-builder/  # Custom dashboard builder
│   │   │   ├── 📁 executive-dashboard/       # Executive analytics
│   │   │   ├── 📁 metrics-service/          # Metrics collection
│   │   │   ├── 📁 provider-dashboard-service/# Provider analytics
│   │   │   └── 📁 reporting-service/        # Report generation
│   │   └── 📁 nodejs-services/          # Frontend dashboard services (8)
│   │       ├── 📁 alert-center-web/          # Alert management UI
│   │       ├── 📁 analytics-dashboard-web/   # Analytics dashboard UI
│   │       ├── 📁 custom-report-builder/     # Report builder UI
│   │       ├── 📁 dashboard-web/            # Main dashboard UI
│   │       ├── 📁 executive-dashboard-web/   # Executive dashboard UI
│   │       ├── 📁 real-time-dashboard/      # Real-time analytics UI
│   │       ├── 📁 shared-components-web/    # Shared UI components
│   │       └── 📁 visualization-web/        # Data visualization UI
│   ├── 📁 shared-components/             # Reusable dashboard components
│   │   ├── 📁 ui-components/              # React/Vue components
│   │   ├── 📁 chart-libraries/           # D3.js, Chart.js components
│   │   └── 📁 dashboard-templates/       # Dashboard templates
│   ├── 📁 docs/                         # Dashboard documentation
│   ├── 📁 tools/                        # Dashboard development tools
│   └── 📁 config/                       # Dashboard configurations
│
├── 📁 central-configuration/              # Configuration management platform
│   ├── 📁 backend-services/              # Configuration services (11 total)
│   │   ├── 📁 java-services/            # Java config services (11)
│   │   │   ├── 📁 AuditLoggingConfigService/    # Audit logging config
│   │   │   ├── 📁 BackupConfigService/           # Backup configuration
│   │   │   ├── 📁 ConfigManagementService/      # Central config mgmt
│   │   │   ├── 📁 DisasterRecoveryConfigService/# Disaster recovery config
│   │   │   ├── 📁 DynamicConfigService/         # Dynamic config service
│   │   │   ├── 📁 EnvironmentVarsService/       # Environment variables
│   │   │   ├── 📁 FeatureFlagsService/          # Feature flag management
│   │   │   ├── 📁 PolicyManagementService/      # Policy configuration
│   │   │   ├── 📁 RateLimitingService/         # Rate limiting config
│   │   │   ├── 📁 SecretsManagementService/     # Secret management
│   │   │   └── 📁 SecretsRotationService/      # Secret rotation
│   │   └── 📁 nodejs-services/          # Node.js config services (empty)
│   ├── 📁 build-tools/                  # Configuration build tools
│   │   ├── 📁 scripts/                    # Build and deployment scripts
│   │   └── 📁 ci-cd-templates/           # CI/CD pipeline templates
│   ├── 📁 shared-libraries/             # Configuration shared libraries
│   ├── 📁 dependency-management/        # Maven BOM and dependencies
│   ├── 📁 docs/                         # Configuration documentation
│   └── 📁 tools/                        # Configuration tools
│
└── 📁 ai-services/                        # AI and machine learning platform
    ├── 📁 backend-services/              # AI services (34 total)
    │   ├── 📁 java-services/            # Java AI services (27)
    │   │   ├── 📁 ai-anomaly-detection-service/  # Anomaly detection
    │   │   ├── 📁 ai-automated-tagging-service/  # Automated content tagging
    │   │   ├── 📁 ai-bi-analytics-service/       # Business intelligence
    │   │   ├── 📁 ai-categorization-service/     # Content categorization
    │   │   ├── 📁 ai-chatbot-service/           # Conversational AI
    │   │   ├── 📁 ai-computer-vision-service/   # Image/video analysis
    │   │   ├── 📁 ai-content-moderation-service/# Content moderation
    │   │   ├── 📁 ai-data-quality-service/      # Data quality management
    │   │   ├── 📁 ai-forecasting-service/       # Predictive forecasting
    │   │   ├── 📁 ai-fraud-detection-service/   # Fraud detection
    │   │   ├── 📁 ai-gateway-service/           # AI model gateway
    │   │   ├── 📁 ai-image-recognition-service/# Image recognition
    │   │   ├── 📁 ai-inference-service/         # Model inference
    │   │   ├── 📁 ai-matching-algorithm-service/# Matching algorithms
    │   │   ├── 📁 ai-model-management-service/  # ML model management
    │   │   ├── 📁 ai-nlp-processing-service/    # Natural language processing
    │   │   ├── 📁 ai-optimization-service/      # Process optimization
    │   │   ├── 📁 ai-personalization-service/  # User personalization
    │   │   ├── 📁 ai-predictive-analytics-service/# Predictive analytics
    │   │   ├── 📁 ai-pricing-engine-service/   # Dynamic pricing
    │   │   ├── 📁 ai-recommendation-service/    # Recommendation engine
    │   │   ├── 📁 ai-report-generation-service/ # Automated reports
    │   │   ├── 📁 ai-risk-assessment-service/  # Risk assessment
    │   │   ├── 📁 ai-search-optimization-service/# Search optimization
    │   │   ├── 📁 ai-sentiment-analysis-service/# Sentiment analysis
    │   │   ├── 📁 ai-speech-recognition-service/# Speech recognition
    │   │   └── 📁 ai-translation-service/        # Language translation
    │   ├── 📁 nodejs-services/          # Node.js AI services (7)
    │   │   ├── 📁 ai-dashboard-web/             # AI management dashboard
    │   │   ├── 📁 ai-training-service/          # Model training service
    │   │   ├── 📁 computer-vision-service/     # Computer vision API
    │   │   ├── 📁 data-quality-service/        # Data quality API
    │   │   ├── 📁 document-analysis-service/   # Document analysis API
    │   │   ├── 📁 ml-model-service/            # ML model serving
    │   │   └── 📁 natural-language-processing-service/# NLP API
    │   └── 📁 python-services/          # Python AI services (1)
    │       └── 📁 gogidix-ai-platform/       # Core Python AI platform
    ├── 📁 ai-infrastructure/             # AI platform infrastructure
    │   ├── 📁 models/                     # Trained ML models
    │   ├── 📁 training/                   # Training infrastructure
    │   ├── 📁 data/                       # AI data management
    │   └── 📁 deployment/                 # AI deployment configs
    ├── 📁 ai-platform/                  # Core AI platform components
    ├── 📁 ml-ops/                       # Machine learning operations
    ├── 📁 docs/                         # AI documentation
    └── 📁 tools/                        # AI development tools
```

## 🎯 Use Cases by Component

### 📦 Shared-Libraries
- **Core Services**: Reused across all domains for audit, caching, security, messaging
- **Deployment**: Container orchestration and Kubernetes management
- **Testing**: Shared testing frameworks and utilities

### 🏗️ Shared-Infrastructure
- **Platform Services**: Identity, monitoring, logging for entire platform
- **Infrastructure as Code**: Terraform configs for cloud resources
- **Service Mesh**: Istio configuration for microservices communication

### 📊 Centralized-Dashboard
- **Business Intelligence**: Analytics dashboards for stakeholders
- **Real-time Monitoring**: Live system and business metrics
- **Custom Reports**: User-configurable reporting tools

### ⚙️ Central-Configuration
- **Feature Flags**: Dynamic feature toggles across services
- **Secret Management**: Secure credential and configuration storage
- **Environment Management**: Configuration across dev/staging/prod

### 🤖 AI-Services
- **Property Intelligence**: Automated property valuation and analysis
- **User Personalization**: AI-driven recommendations and experiences
- **Operational Intelligence**: Fraud detection and anomaly identification

## 📋 Folder Rules & Guidelines

### ✅ **RULES FOR ALL AGENTS**

#### 🗂️ **Service Organization Rules**
1. **Services Must Be in backend-services/**
   - All microservices must be under `backend-services/java-services/` or `backend-services/nodejs-services/`
   - Never create service directories at domain root level

2. **Technology Stack Separation**
   - Java services → `backend-services/java-services/`
   - Node.js services → `backend-services/nodejs-services/`
   - Python services → `backend-services/python-services/`
   - Never mix different technologies in same service directory

3. **Service Naming Convention**
   - Format: `{domain}-{service-type}-service`
   - Examples: `user-management-service`, `property-listing-service`
   - Use kebab-case, never camelCase or spaces

#### 📁 **Directory Structure Rules**
1. **Consistent Structure Required**
   ```bash
   backend-services/
   ├── java-services/
   ├── nodejs-services/
   └── [optional] python-services/

   docs/
   ├── api-documentation/
   ├── user-guides/
   └── technical-docs/

   tools/
   ├── generators/
   ├── scripts/
   └── testing-utilities/
   ```

2. **No Duplicate Directories**
   - Never create duplicate service directories
   - If service exists, enhance it, don't create new one

3. **Documentation Organization**
   - All docs must be in centralized `docs/` directory
   - Never leave documentation scattered in service directories

#### 🏗️ **Infrastructure Rules**
1. **Infrastructure Code Separation**
   - Terraform → `infrastructure/terraform/`
   - Kubernetes → `infrastructure/kubernetes/`
   - Docker → `deployment/docker/`

2. **Configuration Management**
   - Environment configs → `config/environment-configs/`
   - Service configs → `config/service-configs/`
   - Never hardcode configurations in services

#### 📝 **Documentation Rules**
1. **README.md Required**
   - Every service must have README.md explaining purpose
   - Every major directory must have README.md with structure explanation

2. **API Documentation**
   - All APIs must be documented in `docs/api-documentation/`
   - Use OpenAPI/Swagger specifications

3. **Change Documentation**
   - All changes must be documented in CHANGELOG.md
   - Follow semantic versioning for breaking changes

#### 🚀 **Deployment Rules**
1. **Containerization Required**
   - Every service must have Dockerfile
   - Use multi-stage builds for optimization

2. **Kubernetes Ready**
   - Every service must have Kubernetes manifests
   - Include health checks and resource limits

3. **Environment Separation**
   - Separate configs for dev/staging/prod
   - Never share environments between services

#### 🔧 **Development Rules**
1. **Code Organization**
   - Follow Domain-Driven Design (DDD) principles
   - Separate application, domain, and infrastructure layers

2. **Testing Requirements**
   - Unit tests required for all business logic
   - Integration tests for all external dependencies
   - E2E tests for critical user journeys

3. **Security Standards**
   - Follow OWASP security guidelines
   - Implement proper authentication and authorization
   - Never commit secrets or credentials

#### 📊 **Monitoring Rules**
1. **Logging Standards**
   - Use structured logging (JSON format)
   - Include correlation IDs for request tracing
   - Never log sensitive information

2. **Metrics Required**
   - All services must expose health endpoints
   - Implement business metrics tracking
   - Use Prometheus/Grafana for monitoring

3. **Alerting Setup**
   - Critical services must have alerting configured
   - Include SLA monitoring
   - Set up escalation procedures

### ❌ **FORBIDDEN ACTIONS**
1. **Never create service directories outside backend-services/**
2. **Never mix different technologies in same service directory**
3. **Never hardcode configurations or credentials**
4. **Never skip documentation for new services**
5. **Never commit sensitive data or secrets**
6. **Never ignore naming conventions**
7. **Never create duplicate functionality**

### ✅ **REQUIRED ACTIONS**
1. **Always create README.md for new services**
2. **Always update this documentation when adding new components**
3. **Always follow the established folder structure**
4. **Always include proper testing and documentation**
5. **Always implement proper logging and monitoring**
6. **Always use semantic versioning**
7. **Always follow security best practices**

## 🚨 **ENFORCEMENT**

### 📋 **Code Review Checklist**
- [ ] Service is in correct `backend-services/` subdirectory
- [ ] Technology stack matches directory (Java/Node.js/Python)
- [ ] Service follows naming convention
- [ ] README.md exists and is comprehensive
- [ ] API documentation exists
- [ ] Tests are included and passing
- [ ] Security best practices implemented
- [ ] Logging and monitoring configured
- [ ] Docker and Kubernetes configs exist

### 🔍 **Automated Validation**
- CI/CD pipelines validate folder structure
- Automated tests check naming conventions
- Security scans for hardcoded secrets
- Documentation generation for APIs

## 📞 **Support**
For questions about these folder rules:
1. Check existing service examples for patterns
2. Review this documentation thoroughly
3. Consult with domain architecture team
4. Create GitHub issues for rule clarifications

---

**Last Updated**: 2025-01-30
**Version**: 1.0
**Next Review**: 2025-02-28