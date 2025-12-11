# Gogidix Property Marketplace - Local Development Environment

This directory contains the complete local development environment for the Gogidix Property Marketplace application. It includes all infrastructure and application services configured to run locally using Docker Compose.

## Prerequisites

- Docker Desktop installed and running (minimum 4GB RAM, 10GB disk space)
- Docker Compose (included with Docker Desktop)
- PowerShell (for Windows) or Bash (for Linux/Mac)
- At least 8GB system RAM (16GB recommended)
- Git

## Quick Start

### Windows

1. Open PowerShell as Administrator
2. Navigate to this directory
3. Run the startup script:

```powershell
.\start-local.ps1
```

### Linux/Mac

1. Open Terminal
2. Navigate to this directory
3. Make the script executable and run:

```bash
chmod +x start-local.sh
./start-local.sh
```

## Architecture Overview

The local deployment includes:

### Infrastructure Services
- **PostgreSQL**: Primary database (port 5432)
- **Redis**: Cache and session storage (port 6379)
- **Kafka**: Message broker (port 9092)
- **Elasticsearch**: Search and analytics (port 9200)
- **Kibana**: Log visualization (port 5601)
- **Zookeeper**: Kafka coordination (port 2181)

### Application Services
- **API Gateway**: Main entry point (port 8080)
- **Property Service**: Property management (port 8082)
- **User Service**: User management (port 8083)
- **Payment Service**: Payment processing (port 8084)
- **Rate Limiting Service**: API rate limiting (port 8085)
- **Circuit Breaker Service**: Fault tolerance (port 8086)

### Monitoring & Observability
- **Prometheus**: Metrics collection (port 9090)
- **Grafana**: Metrics dashboards (port 3000)
- **Jaeger**: Distributed tracing (port 16686)

### Additional Services
- **Keycloak**: Identity management (port 8080/auth)
- **Vault**: Secret management (port 8200)
- **Consul**: Service discovery (port 8500)
- **RabbitMQ**: Message queue (ports 5672, 15672)
- **MinIO**: S3-compatible storage (ports 9000, 9001)
- **Nginx**: Reverse proxy (ports 80, 443)

## Accessing Services

### Application
- **API Gateway**: http://localhost/api/
- **Swagger Documentation**: http://localhost/swagger-ui.html

### Monitoring
- **Grafana Dashboard**: http://localhost:3000
  - Username: `admin`
  - Password: `gogidix123`
- **Prometheus**: http://localhost:9090
- **Kibana Logs**: http://localhost:5601
- **Jaeger Tracing**: http://localhost:16686

### Infrastructure
- **Keycloak Admin Console**: http://localhost:8080
  - Username: `admin`
  - Password: `gogidix123`
- **MinIO Console**: http://localhost:9001
  - Username: `minioadmin`
  - Password: `minioadmin123`
- **RabbitMQ Management**: http://localhost:15672
  - Username: `gogidix`
  - Password: `gogidix123`
- **Consul UI**: http://localhost:8500
- **Vault UI**: http://localhost:8200

## Development Workflow

### Building Applications

Before starting services, build the Java applications:

```bash
# Build all services
./build-all.sh

# Or build individual services
cd ../java-services/api-gateway
mvn clean package -DskipTests
```

### Running Services

1. **Start all services**:
   ```bash
   docker-compose up -d
   ```

2. **Start specific service**:
   ```bash
   docker-compose up -d api-gateway
   ```

3. **View logs**:
   ```bash
   docker-compose logs -f api-gateway
   ```

4. **Stop all services**:
   ```bash
   docker-compose down
   ```

5. **Scale a service**:
   ```bash
   docker-compose up -d --scale api-gateway=3
   ```

### Database Access

Connect to PostgreSQL:
- Host: localhost
- Port: 5432
- Database: gogidix_property
- Username: gogidix_admin
- Password: gogidix123

Using psql:
```bash
psql -h localhost -p 5432 -U gogidix_admin -d gogidix_property
```

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and modify as needed:

```bash
cp .env.example .env
```

Key variables:
- `COMPOSE_PROJECT_NAME`: Project name for containers
- `POSTGRES_PASSWORD`: PostgreSQL password
- `REDIS_PASSWORD`: Redis password
- `KEYCLOAK_ADMIN_PASSWORD`: Keycloak admin password

### Custom Configuration

- **Docker Compose**: Modify `docker-compose.yml`
- **Prometheus**: Edit `monitoring/prometheus/prometheus.yml`
- **Grafana**: Add dashboards to `monitoring/grafana/provisioning/dashboards/`
- **Nginx**: Update `nginx/nginx.conf`

## Troubleshooting

### Port Conflicts

If ports are already in use:
1. Stop services using those ports
2. Or modify ports in `docker-compose.yml`

### Out of Memory

Increase Docker Desktop memory allocation:
1. Open Docker Desktop
2. Go to Settings > Resources
3. Set memory to at least 8GB

### Service Health Issues

1. Check service logs:
   ```bash
   docker-compose logs -f [service-name]
   ```

2. Verify dependencies:
   ```bash
   docker-compose ps
   ```

3. Restart services:
   ```bash
   docker-compose restart [service-name]
   ```

### Common Issues

**SSL Certificate Errors**
- SSL is self-signed for local development
- Browsers will show security warnings (can be bypassed)

**Windows File Sharing Issues**
- Ensure the project directory is shared in Docker Desktop
- File sharing is in Settings > Resources > File Sharing

**Permission Issues (Linux/Mac)**
- Run scripts with appropriate permissions
- May need `sudo` for some operations

## Performance Tuning

### Development Profile

Services are configured for development with:
- Enabled debug logging
- Lower resource limits
- Hot reload enabled where possible

### Production Mode

To simulate production:
1. Set `SPRING_PROFILES_ACTIVE=prod`
2. Increase resource limits in `docker-compose.yml`
3. Enable all security features

## Testing

### Running Tests

```bash
# Run all tests
./test-all.sh

# Run service-specific tests
cd ../java-services/api-gateway
mvn test
```

### Load Testing

Use included scripts:

```bash
# Simple load test
./scripts/load-test.sh

# Generate load with k6
k6 run scripts/load-test.js
```

## Contributing

### Adding New Services

1. Add service to `docker-compose.yml`
2. Update monitoring configuration
3. Add health check to startup script
4. Update documentation

### Configuration Management

- Development config in `/config/dev/`
- Production config in `/config/prod/`
- Secrets managed through Vault

## Security

- All services communicate through internal Docker network
- External access only through Nginx proxy
- Authentication handled by Keycloak
- Secrets managed by Vault

## Backup & Recovery

### Manual Backup

```bash
# Backup databases
docker-compose exec postgres pg_dump -U gogidix_admin gogidix_property > backup.sql

# Backup volumes
docker run --rm -v gogidix_postgres_data:/data -v $(pwd):/backup ubuntu tar cvf /backup/postgres-backup.tar /data
```

### Restore

```bash
# Restore database
docker-compose exec -T postgres psql -U gogidix_admin -d gogidix_property < backup.sql
```

## Getting Help

- Check logs for errors
- Refer to service documentation in parent directories
- Open an issue in the project repository

## Cleanup

To completely reset the environment:

```bash
# Stop and remove containers
docker-compose down -v

# Remove all images
docker system prune -a

# Reset networks
docker network prune
```