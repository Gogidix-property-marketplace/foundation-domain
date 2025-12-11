# 🚀 Quick Start Guide - Gogidix Property Marketplace

## The Problem: Docker Desktop Not Running?

You're seeing connection errors because Docker Desktop is not started. Here's how to fix it:

### Step 1: Start Docker Desktop
1. Press `Win + S` and search for "Docker Desktop"
2. Click "Docker Desktop" to open it
3. Wait for the whale icon in your system tray to stop animating (this can take 2-3 minutes)
4. Once it's running, Docker will work properly

### Step 2: Verify Docker is Running
```powershell
docker --version
docker info
```

If these commands work without errors, you're ready to go!

## Quick Deployment Options

### Option A: Full Docker Deployment (Recommended)
```powershell
cd C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\local-deployment

# Start everything with one command
.\start-local.ps1

# Or manually
docker-compose up -d
```

### Option B: Java Services Only (No Docker)
If Docker is giving you trouble, run Java services directly:

```powershell
# Start Java services without Docker
.\start-java-services.ps1 -UseInMemory

# This will start the core services with in-memory database
```

### Option C: Minimal Test
Just want to test a few services?

```powershell
# Start only Config Server
cd ..\java-services\config-server
mvn spring-boot:run

# In a new terminal, start Eureka
cd ..\eureka-server
mvn spring-boot:run

# In a third terminal, start API Gateway
cd ..\api-gateway
mvn spring-boot:run
```

## Service URLs (When Running)

| Service | URL | How to Check |
|---------|-----|--------------|
| API Gateway | http://localhost:8080 | `curl http://localhost:8080/actuator/health` |
| Config Server | http://localhost:8888 | `curl http://localhost:8888/actuator/health` |
| Eureka | http://localhost:8761 | Open in browser |
| Grafana | http://localhost:3000 | Docker only (admin/admin) |

## Common Issues & Fixes

### ❌ "Cannot connect to Docker daemon"
**Fix**: Start Docker Desktop from Start Menu

### ❌ "Port 8080 is already in use"
**Fix**:
```powershell
# Find what's using the port
netstat -ano | findstr :8080

# Kill the process
taskkill /PID <PID> /F
```

### ❌ "Java not found"
**Fix**: Install Java 21 from https://adoptium.net/

### ❌ "Maven not found"
**Fix**: Install Maven 3.9+ from https://maven.apache.org/

## What Happens When You Start Services

1. **Config Server** starts on port 8888
   - Provides configuration to all services
   - Health check: http://localhost:8888/actuator/health

2. **Eureka Server** starts on port 8761
   - Service registry and discovery
   - Dashboard: http://localhost:8761

3. **API Gateway** starts on port 8080
   - Routes all API requests
   - Handles rate limiting and circuit breaking
   - Health check: http://localhost:8080/actuator/health

4. **Optional Services** (if using Docker)
   - PostgreSQL, Redis, Elasticsearch for data
   - Prometheus, Grafana for monitoring
   - Keycloak for authentication

## Testing Your Deployment

### 1. Check if services are running
```powershell
# List all Docker containers
docker-compose ps

# Or check Java processes
Get-Process java
```

### 2. Test health endpoints
```powershell
curl http://localhost:8888/actuator/health  # Config Server
curl http://localhost:8080/actuator/health  # API Gateway
```

### 3. View Eureka dashboard
Open http://localhost:8761 in your browser
- You should see all registered services

### 4. Test API Gateway
```powershell
# Basic test
curl http://localhost:8080/api/v1/test

# Test rate limiting
for i in {1..5}; do
  curl -H "X-API-Key: test-key" http://localhost:8080/api/v1/test
done
```

## Need More Help?

1. **Full Documentation**: [LOCAL_DEPLOYMENT_GUIDE.md](./LOCAL_DEPLOYMENT_GUIDE.md)
2. **Docker Issues**: [DOCKER_TROUBLESHOOTING.md](./DOCKER_TROUBLESHOOTING.md)
3. **Java Services**: [RUN_LOCAL_JAVA.md](./RUN_LOCAL_JAVA.md)
4. **Checklist**: [DEPLOYMENT_CHECKLIST.md](./DEPLOYMENT_CHECKLIST.md)

## Still Having Trouble?

1. Make sure Docker Desktop is completely started (wait 2-3 minutes)
2. Check if any antivirus is blocking Docker
3. Restart your computer after installing Docker
4. Try running PowerShell as Administrator

## Success Criteria

You're all set when:
- ✅ Docker Desktop is running (whale icon in system tray)
- ✅ Services start without errors
- ✅ Health endpoints return `{"status":"UP"}`
- ✅ Eureka dashboard shows registered services
- ✅ API Gateway responds to requests