# Local Deployment Checklist

## Pre-Deployment Checklist

- [ ] Docker Desktop is installed
- [ ] Docker Desktop is running (check system tray)
- [ ] At least 16GB RAM available
- [ ] At least 20GB free disk space
- [ ] PowerShell is available (for Windows)
- [ ] Git Bash or WSL (optional)

## Quick Start Commands

### 1. Open PowerShell/Terminal
```powershell
# For PowerShell
cd C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\local-deployment

# For Git Bash
cd /c/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/local-deployment
```

### 2. Start Services
```powershell
# Option 1: Run the PowerShell script
powershell -ExecutionPolicy Bypass -File .\start-local.ps1

# Option 2: Use Docker Compose directly
docker-compose up -d
```

### 3. Verify Services
```powershell
# Check all containers are running
docker-compose ps

# Wait for all services to be healthy (can take 2-5 minutes)
docker-compose ps
```

## Service Verification

Once all containers are running, test these URLs in your browser:

### Core Services
- [ ] http://localhost:8080 - API Gateway
- [ ] http://localhost:8888 - Config Server
- [ ] http://localhost:8761 - Eureka Discovery
- [ ] http://localhost:9411 - Zipkin Tracing

### Monitoring
- [ ] http://localhost:9090 - Prometheus
- [ ] http://localhost:3000 - Grafana (admin/admin)
- [ ] http://localhost:5601 - Kibana
- [ ] http://localhost:16686 - Jaeger

### Security
- [ ] http://localhost:8081 - Keycloak (admin/admin)
- [ ] http://localhost:8200 - Vault (token: root-token)

### Databases & Storage
- [ ] localhost:5432 - PostgreSQL (postgres/postgres)
- [ ] localhost:6379 - Redis
- [ ] http://localhost:9001 - MinIO (minioadmin/minioadmin)
- [ ] http://localhost:15672 - RabbitMQ (guest/guest)

### Health Checks (run in terminal)
```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Config Server
curl http://localhost:8888/actuator/health

# Should return: {"status":"UP"}
```

## Troubleshooting

### If Docker Compose fails:
1. Make sure Docker Desktop is running
2. Check if ports are already in use:
   ```bash
   netstat -ano | findstr :8080
   ```
3. Stop and restart:
   ```bash
   docker-compose down
   docker-compose up -d
   ```

### If services are unhealthy:
1. Check logs:
   ```bash
   docker-compose logs [service-name]
   ```
2. Give it more time - some services need 2-3 minutes
3. Restart specific service:
   ```bash
   docker-compose restart [service-name]
   ```

### Common Port Conflicts
- 8080: API Gateway (change if needed)
- 5432: PostgreSQL
- 6379: Redis
- 9090: Prometheus

## Success Criteria

You're all set if:
- [ ] All `docker-compose ps` containers show "Up" status
- [ ] Health endpoints return `{"status":"UP"}`
- [ ] Grafana dashboard loads at http://localhost:3000
- [ ] Keycloak login page loads at http://localhost:8081
- [ ] Prometheus targets are up at http://localhost:9090/targets

## Next Steps

1. **Explore the Application**
   - Try API endpoints with Postman or curl
   - Check out the Grafana dashboards
   - View traces in Zipkin/Jaeger

2. **Run Integration Tests**
   ```bash
   cd ..\java-services\api-gateway
   mvn test
   ```

3. **Deploy to Staging**
   - Use Kubernetes manifests in ../kubernetes
   - Or Terraform configs in ../terraform

## Need Help?

1. Check the detailed guide: [LOCAL_DEPLOYMENT_GUIDE.md](./LOCAL_DEPLOYMENT_GUIDE.md)
2. Review service logs: `docker-compose logs [service]`
3. Check Docker Desktop resources allocation