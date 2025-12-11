# Gogidix Property Marketplace - Local Deployment Guide

## Quick Start

### Prerequisites
1. **Docker Desktop** (Latest version)
   - Download from https://www.docker.com/products/docker-desktop
   - Start Docker Desktop after installation
   - Ensure it's running (you should see the Docker icon in your system tray)

2. **System Resources**
   - Minimum 16GB RAM
   - Minimum 20GB free disk space
   - 4+ CPU cores recommended

### Starting the Services

#### Option 1: Using PowerShell Script (Recommended for Windows)
```powershell
cd C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\local-deployment
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

#### Option 2: Using Bash/WSL
```bash
cd /c/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/local-deployment
./start-local.sh
```

#### Option 3: Manual Docker Compose
```bash
cd C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\local-deployment
docker-compose up -d
```

### Service URLs

Once services are running, access them at:

**Core Infrastructure**
- **API Gateway**: http://localhost:8080
- **Config Server**: http://localhost:8888
- **Eureka Discovery**: http://localhost:8761
- **Zipkin Tracing**: http://localhost:9411

**Database & Storage**
- **PostgreSQL**: localhost:5432
  - User: postgres
  - Password: postgres
- **Redis**: localhost:6379
- **MinIO Console**: http://localhost:9001
  - User: minioadmin
  - Password: minioadmin

**Monitoring & Observability**
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000
  - User: admin
  - Password: admin
- **Kibana**: http://localhost:5601
- **Jaeger**: http://localhost:16686

**Security**
- **Keycloak**: http://localhost:8081
  - User: admin
  - Password: admin
- **Vault**: http://localhost:8200
  - Token: root-token

**Message Queue**
- **RabbitMQ Management**: http://localhost:15672
  - User: guest
  - Password: guest

### Verifying Deployment

#### 1. Check Service Status
```bash
# Check all running containers
docker-compose ps

# Check logs for specific service
docker-compose logs -f [service-name]
```

#### 2. Health Checks
```bash
# API Gateway Health
curl http://localhost:8080/actuator/health

# Config Server Health
curl http://localhost:8888/actuator/health

# Eureka Health
curl http://localhost:8761/actuator/health
```

#### 3. Monitor Application Logs
```bash
# API Gateway logs
docker-compose logs -f api-gateway

# Rate Limiting Service logs
docker-compose logs -f rate-limiting-service

# Dashboard Integration Service logs
docker-compose logs -f dashboard-integration-service
```

### Common Issues and Solutions

#### Issue 1: Docker Desktop not running
**Solution**: Start Docker Desktop manually from Start Menu or desktop shortcut

#### Issue 2: Port already in use
**Error**: `Port 8080 is already allocated`
**Solution**:
```bash
# Find what's using the port
netstat -ano | findstr :8080

# Or change the port in docker-compose.yml
```

#### Issue 3: Out of memory errors
**Solution**:
1. Increase Docker Desktop memory allocation (Settings > Resources)
2. Or reduce the number of services running:
```bash
docker-compose up -d postgres redis elasticsearch
```

#### Issue 4: Services taking too long to start
**Solution**: Some services like Elasticsearch need more time:
```bash
# Wait for all services to be healthy
docker-compose up --wait
```

#### Issue 5: Permission denied errors
**Solution**: Run PowerShell as Administrator

### Development Workflow

#### 1. Start Infrastructure Only
```bash
# Start just databases and message queues
docker-compose up -d postgres redis rabbitmq elasticsearch

# Start monitoring
docker-compose up -d prometheus grafana
```

#### 2. Run Application Services Locally
```bash
# Navigate to service directory
cd ..\java-services\api-gateway

# Run with Maven
mvn spring-boot:run

# Or run from IDE
```

#### 3. Integration Testing
```bash
# Test API Gateway with health check
curl http://localhost:8080/actuator/health

# Test rate limiting
curl -H "X-API-Key: test-key" http://localhost:8080/api/v1/test

# Test circuit breaker
curl http://localhost:8080/api/v1/test/circuit
```

### Environment Variables

Create a `.env` file in the local-deployment directory:
```env
# Database Configuration
POSTGRES_DB=gogidix
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Elasticsearch
ELASTIC_PASSWORD=elastic123

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin

# Vault
VAULT_DEV_ROOT_TOKEN_ID=root-token
```

### Monitoring and Debugging

#### Grafana Dashboards
1. Open http://localhost:3000
2. Login with admin/admin
3. Import dashboards from `grafana/dashboards` directory

#### Application Metrics
- Prometheus metrics: http://localhost:8080/actuator/prometheus
- Health endpoints: http://localhost:8080/actuator/health
- Custom metrics: http://localhost:8080/actuator/metrics

#### Distributed Tracing
1. Open http://localhost:9411 (Zipkin)
2. Search for traces by service name
3. View service dependencies

#### Logs Analysis
1. Open Kibana at http://localhost:5601
2. Create index pattern: `logstash-*`
3. View and analyze application logs

### Performance Testing

#### Load Testing with Apache Bench
```bash
# Basic load test
ab -n 1000 -c 10 http://localhost:8080/actuator/health

# With API key
ab -n 1000 -c 10 -H "X-API-Key: test-key" http://localhost:8080/api/v1/test
```

#### Rate Limiting Test
```bash
# Should be rate limited after 10 requests
for i in {1..15}; do
  curl -H "X-API-Key: test-key" http://localhost:8080/api/v1/test
done
```

### Stopping Services

#### Stop All Services
```bash
docker-compose down
```

#### Stop and Remove Volumes
```bash
docker-compose down -v
```

#### Stop and Remove Including Images
```bash
docker-compose down -v --rmi all
```

### Cleanup

#### Remove All Docker Resources
```bash
# Remove all containers
docker rm -f $(docker ps -aq)

# Remove all images
docker rmi -f $(docker images -q)

# Clean up unused resources
docker system prune -a
```

## Production Deployment

### Kubernetes Deployment
For production deployment, use the provided Kubernetes manifests:
```bash
cd ../kubernetes
kubectl apply -f namespace.yaml
kubectl apply -f secrets.yaml
kubectl apply -f configmaps.yaml
kubectl apply -f services.yaml
kubectl apply -f deployments.yaml
```

### Terraform Infrastructure
To deploy AWS infrastructure:
```bash
cd ../terraform
terraform init
terraform plan
terraform apply
```

## Support

For issues:
1. Check Docker Desktop is running
2. Verify port availability
3. Check service logs: `docker-compose logs [service]`
4. Refer to service documentation in the README files

## Next Steps

After successful local deployment:
1. Explore the service endpoints and dashboards
2. Run the integration tests
3. Deploy to staging environment
4. Configure production monitoring and alerts