# Gogidix Centralized Dashboard - Docker Setup

This directory contains the Docker configuration for the Gogidix Centralized Dashboard domain, including all Java backend services, Node.js web services, databases, and monitoring tools.

## 🏗️ Architecture

### Services Overview

#### Java Backend Services (Ports 8001-8009)
- **agent-dashboard-service**:8001 - Real estate agent dashboard
- **alert-management-service**:8002 - Alert and notification management
- **analytics-service**:8003 - Data analytics and business intelligence
- **centralized-dashboard**:8004 - Main centralized dashboard
- **custom-dashboard-builder**:8005 - Custom dashboard creation tools
- **executive-dashboard**:8006 - Executive-level reporting
- **metrics-service**:8007 - Metrics collection and analysis
- **provider-dashboard-service**:8008 - Service provider dashboard
- **reporting-service**:8009 - Report generation and management

#### Node.js Web Services (Ports 8010-8017)
- **alert-center-web**:8010 - Alert center web interface
- **analytics-dashboard-web**:8011 - Analytics dashboard frontend
- **custom-report-builder**:8012 - Custom report building interface
- **dashboard-web**:8013 - Main dashboard web interface
- **executive-dashboard-web**:8014 - Executive dashboard frontend
- **real-time-dashboard**:8015 - Real-time dashboard interface
- **shared-components-web**:8016 - Shared UI components service
- **visualization-web**:8017 - Data visualization interface

#### Infrastructure Services
- **postgres-centralized**:5432 - PostgreSQL database
- **redis-cache**:6379 - Redis cache
- **nginx-proxy**:80,443 - Reverse proxy and load balancer
- **prometheus**:9090 - Metrics collection
- **grafana**:3000 - Monitoring dashboard

## 🚀 Quick Start

### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+
- Make (optional, for convenient commands)

### Development Environment
```bash
# Quick start with hot reload
make dev

# Or manually:
docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d
```

### Production Environment
```bash
# Quick start for production
make prod

# Or manually:
docker-compose up -d
```

### Access Services
After starting services:

#### Development Access
- **Main Dashboard**: http://localhost:8013
- **API Gateway**: http://localhost:8004
- **Grafana**: http://localhost:3000 (admin/gogidix_admin)
- **Prometheus**: http://localhost:9090

#### Production Access
- **Main Dashboard**: http://localhost:80
- **Grafana**: http://localhost:3000 (admin/gogidix_admin)
- **Prometheus**: http://localhost:9090

## 🛠️ Management Commands

### Using Make (Recommended)
```bash
# Show all available commands
make help

# Service management
make up              # Start all services (production)
make up-dev          # Start all services (development)
make down            # Stop all services
make status          # Show service status
make health          # Check service health

# Build commands
make build           # Build all images
make build-java      # Build only Java services
make build-nodejs    # Build only Node.js services

# Logs
make logs            # Show all logs
make logs-java       # Show Java service logs
make logs-nodejs     # Show Node.js service logs
make logs-databases  # Show database logs

# Development
make dev             # Start development environment
make test            # Run integration tests
make test-integration # Run domain integration tests

# Database management
make db-connect      # Connect to PostgreSQL
make redis-connect   # Connect to Redis
make db-migrate      # Run database migrations

# Monitoring
make monitor         # Open monitoring dashboards
```

### Manual Docker Compose Commands
```bash
# Build images
docker-compose build

# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f [service-name]

# Execute commands in containers
docker-compose exec [service-name] [command]

# Scale services
docker-compose up -d --scale agent-dashboard-service=3
```

## 📁 Configuration

### Environment Variables
Edit `.env` file to customize:
- Database credentials
- Service ports
- Security settings
- Monitoring configuration

### Service Configuration
- **Nginx**: `config/nginx/nginx.conf`
- **Prometheus**: `config/prometheus/prometheus.yml`
- **Grafana**: `config/grafana/`

### Profiles
- **Development**: Uses `docker-compose.override.yml` with hot reload
- **Production**: Uses only `docker-compose.yml` with optimized settings

## 🔍 Monitoring

### Grafana Dashboards
Access Grafana at http://localhost:3000 with credentials:
- **Username**: admin
- **Password**: gogidix_admin

Available dashboards:
- Service Health
- Performance Metrics
- Resource Usage
- Error Tracking

### Prometheus Metrics
Access Prometheus at http://localhost:9090 for:
- Custom metrics collection
- Alerting rules
- Service discovery
- Query interface

## 🧪 Testing

### Running Tests
```bash
# Run all tests
make test

# Run integration tests only
make test-integration

# Test specific service
docker-compose exec agent-dashboard-service mvn test
```

### Health Checks
All services include health checks:
```bash
# Check all services
make health

# Check specific service
curl http://localhost:8001/actuator/health
```

## 🔧 Development

### Hot Reload
Development mode includes:
- **Java Services**: Automatic Maven compilation and restart
- **Node.js Services**: Nodemon for automatic restart on file changes
- **Volume Mounts**: Live code editing

### Database Development
```bash
# Start only databases
make up-databases

# Connect to database
make db-connect

# View database logs
make logs-databases
```

### Service Development
```bash
# Start only Java services
make up-java

# Start only Node.js services
make up-nodejs

# View logs for specific service
make logs-agent-dashboard-service
```

## 🚨 Troubleshooting

### Common Issues

#### Port Conflicts
If ports are already in use, modify `docker-compose.yml`:
```yaml
ports:
  - "8018:8013"  # Change to available port
```

#### Memory Issues
Increase Docker memory allocation or reduce service limits:
```yaml
environment:
  JAVA_OPTS: "-Xmx256m -Xms128m"  # Reduce memory usage
```

#### Database Connection Issues
Ensure database is healthy before starting services:
```bash
make up-databases
sleep 10
make up-java
```

#### Service Not Starting
Check logs and health status:
```bash
make status
make logs [service-name]
```

### Debug Mode
Enable debug logging:
```bash
# Java services
environment:
  LOGGING_LEVEL_ROOT: DEBUG

# Node.js services
environment:
  DEBUG: "service-name:*"
```

## 🔒 Security

### Production Security Checklist
- [ ] Change default passwords in `.env`
- [ ] Configure SSL certificates
- [ ] Set up proper firewall rules
- [ ] Enable security headers in Nginx
- [ ] Configure rate limiting
- [ ] Set up monitoring alerts
- [ ] Review API security policies

### Network Security
- Services run in isolated Docker network
- Database not exposed externally
- Rate limiting configured in Nginx
- Security headers enabled by default

## 📊 Performance

### Optimization Tips
- Use production builds (`make prod`)
- Enable gzip compression in Nginx
- Configure connection pooling
- Monitor resource usage in Grafana
- Scale services based on load

### Scaling Services
```bash
# Scale specific services
docker-compose up -d --scale agent-dashboard-service=3

# Or use make command
make scale SERVICE=agent-dashboard-service REPLICAS=3
```

## 🔄 Backup and Recovery

### Database Backup
```bash
# Create backup
make backup

# Manual backup
docker-compose exec postgres-centralized pg_dump -U gogidix_user centralized_dashboard > backup.sql
```

### Data Recovery
```bash
# Restore from backup
docker-compose exec -T postgres-centralized psql -U gogidix_user centralized_dashboard < backup.sql
```

## 📚 Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot with Docker](https://spring.io/guides/gs/spring-boot-docker/)
- [Node.js Best Practices](https://github.com/goldbergyoni/nodebestpractices)
- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)

## 🆘 Support

For issues and support:
1. Check logs: `make logs`
2. Verify health: `make health`
3. Review configuration: Check `.env` and service configs
4. Consult troubleshooting section above

---

**Generated with**: Gogidix Enterprise Solutions
**Version**: 1.0.0
**Last Updated**: November 30, 2025