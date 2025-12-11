# Gogidix Foundation Domain

[![CI/CD](https://github.com/Gogidix-property-marketplace/foundation-domain/actions/workflows/ci-foundation-domain.yml/badge.svg)](https://github.com/Gogidix-property-marketplace/foundation-domain/actions/workflows/ci-foundation-domain.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The Foundation Domain provides core infrastructure services for the Gogidix Property Marketplace platform. It consists of 306+ microservices across 5 domains, built with Java, Node.js, and Python.

## 🏗️ Architecture Overview

### Domain Structure

```
foundation-domain/
├── ai-services/              # AI/ML services (142 services)
│   ├── backend-services/
│   │   ├── java-services/     # 81 Java services
│   │   ├── nodejs-services/   # 10 Node.js services
│   │   └── python-services/   # 50 Python services
│   └── frontend-dashboard/    # Next.js dashboard
├── shared-infrastructure/    # Core infrastructure (114 services)
│   └── backend-services/
│       ├── java-services/     # 82 Java services
│       └── nodejs-services/   # 32 Node.js services
├── centralized-dashboard/     # Dashboard services (27 services)
│   └── backend-services/
│       ├── java-services/     # 9 Java services
│       └── nodejs-services/   # 9 Node.js + 9 frontend services
├── central-configuration/     # Configuration services (11 services)
│   └── backend-services/
│       └── java-services/     # 11 Java services
└── shared-libraries/         # Reusable libraries (12 modules)
    └── java/                  # 12 shared Java libraries
```

### Technology Stack

| Technology | Version | Usage |
|------------|---------|-------|
| Java | 21 | Spring Boot microservices |
| Node.js | 20 | Express.js services & dashboards |
| Python | 3.11 | AI/ML services |
| Docker | Latest | Containerization |
| Kubernetes | 1.28+ | Orchestration |
| PostgreSQL | 14+ | Primary database |
| Redis | 7+ | Caching |
| Kafka | 3.5+ | Event streaming |

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Node.js 20+
- Python 3.11+
- Docker Desktop
- kubectl configured

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/Gogidix-property-marketplace/foundation-domain.git
   cd foundation-domain
   ```

2. **Build shared libraries**
   ```bash
   cd shared-libraries
   mvn clean install
   ```

3. **Start core services**
   ```bash
   # Start API Gateway
   cd shared-infrastructure/backend-services/java-services/api-gateway
   mvn spring-boot:run

   # Start Config Server
   cd ../config-server
   mvn spring-boot:run

   # Start Service Registry
   cd ../registry-service
   mvn spring-boot:run
   ```

4. **Run with Docker Compose**
   ```bash
   docker-compose up -d
   ```

## 📊 Service Count Summary

| Domain | Java | Node.js | Python | Frontend | Total |
|--------|------|---------|--------|----------|-------|
| AI Services | 81 | 10 | 50 | 1 | 142 |
| Shared Infrastructure | 82 | 32 | 0 | 0 | 114 |
| Centralized Dashboard | 9 | 9 | 0 | 9 | 27 |
| Central Configuration | 11 | 0 | 0 | 0 | 11 |
| Shared Libraries | 12 | 0 | 0 | 0 | 12 |
| **TOTAL** | **195** | **51** | **50** | **10** | **306** |

## 🔧 Configuration

### Environment Variables

Create `.env` file for local development:

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=foundation

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Services
API_GATEWAY_PORT=8080
CONFIG_SERVER_PORT=8888
REGISTRY_PORT=8761
```

### Kubernetes Deployment

Deploy to staging:

```bash
kubectl create namespace foundation-staging
kubectl apply -f k8s/
```

## 🧪 Testing

Run tests for all services:

```bash
# Java services
mvn test -pl shared-libraries
mvn test -pl shared-infrastructure/backend-services/java-services
mvn test -pl ai-services/backend-services/java-services

# Node.js services
npm run test --workspaces --if-present

# Python services
find . -name "pytest.ini" -exec dirname {} \; | xargs -I {} pytest {}
```

## 📖 Documentation

- [Architecture Guide](docs/architecture.md)
- [API Documentation](docs/api/)
- [Deployment Guide](docs/deployment.md)
- [CI/CD Pipeline](.github/README.md)

## 🔒 Security

- All services authenticate through the central authentication service
- API rate limiting implemented
- Secrets managed through Kubernetes secrets
- Security scanning integrated in CI/CD pipeline

## 📈 Monitoring

- Health endpoints: `/actuator/health`
- Metrics: Prometheus compatible endpoints
- Logging: Structured JSON logging
- Tracing: OpenTelemetry integration ready

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support, please contact the infrastructure team at:
- Email: infrastructure@gogidix.com
- Slack: #foundation-domain