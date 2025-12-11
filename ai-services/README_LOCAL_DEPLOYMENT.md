# Local Deployment Guide

This guide will help you deploy the Gogidix AI Services platform locally on Windows for testing and development.

## Prerequisites

### Required Software
1. **Docker Desktop for Windows** - [Download here](https://www.docker.com/products/docker-desktop)
   - Ensure WSL 2 is enabled
   - Allocate at least 8GB RAM to Docker
   - Enable file sharing for your project directory

2. **Python 3.9+** - [Download here](https://www.python.org/downloads/)
   - Add Python to PATH during installation
   - Install pip if not included

3. **Git** - [Download here](https://git-scm.com/download/win)

### System Requirements
- **RAM**: 16GB minimum (32GB recommended)
- **Storage**: 20GB free space
- **CPU**: 4 cores minimum (8 recommended)

## Quick Start (5 Minutes)

### 1. Open PowerShell as Administrator
```powershell
# Navigate to the project directory
cd C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\ai-services

# Run the setup script
.\scripts\setup_local_env.ps1
```

### 2. Start All Services
Once setup is complete:
```powershell
# This will start all services in the background
.\start_local.bat
```

### 3. Verify Deployment
Open your browser and test these URLs:

| Service | URL | Description |
|---------|-----|-------------|
| AI Gateway API | http://localhost:8000 | Main API endpoint |
| API Documentation | http://localhost:8000/docs | Interactive Swagger UI |
| Property Intelligence | http://localhost:8001 | Property analysis service |
| Conversational AI | http://localhost:8002 | AI chatbot service |
| Analytics | http://localhost:8003 | Analytics dashboard |
| ML Platform | http://localhost:8004 | ML model management |
| Ethical AI | http://localhost:8005 | Bias detection service |
| MLflow UI | http://localhost:5000 | ML experiment tracking |
| Grafana Dashboard | http://localhost:3000 | Monitoring (admin/admin) |
| Prometheus | http://localhost:9090 | Metrics collection |

### 4. Test the APIs
```powershell
# Run the test script
.\test_api.bat
```

## Detailed Deployment Steps

### Step 1: Clone the Repository
```powershell
git clone https://github.com/gogidix/ai-services.git
cd ai-services
```

### Step 2: Run Setup Script
```powershell
# Make sure PowerShell execution policy allows scripts
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Run the setup
.\scripts\setup_local_env.ps1
```

### Step 3: Build Docker Images (First Time Only)
```powershell
# This will take 10-15 minutes the first time
docker-compose -f docker-compose.local.yml build
```

### Step 4: Start Services
```powershell
# Start all services
docker-compose -f docker-compose.local.yml up -d

# Check status
docker-compose -f docker-compose.local.yml ps
```

### Step 5: Generate Test Data
```powershell
# Generate synthetic property data
docker-compose -f docker-compose.local.yml up data-processor

# Check if data was generated
dir data
```

### Step 6: Train ML Models
```powershell
# Train property valuation model
docker-compose -f docker-compose.local.yml up --build model-trainer

# Check models
dir models
```

## API Testing Examples

### Property Valuation
```powershell
curl -X POST http://localhost:8000/api/v1/property-valuation ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: sk-gogidix-local-1234567890abcdef" ^
  -d "{\"property_type\": \"apartment\", \"bedrooms\": 2, \"bathrooms\": 2, \"square_feet\": 1200, \"year_built\": 2010, \"city\": \"New York\", \"state\": \"NY\"}"
```

### Chat with AI
```powershell
curl -X POST http://localhost:8000/api/v1/chat ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: sk-gogidix-local-1234567890abcdef" ^
  -d "{\"message\": \"What are the best neighborhoods in NYC?\", \"conversation_id\": \"test-123\"}"
```

### Image Analysis
```powershell
# First, download a sample property image
curl -o sample_house.jpg https://example.com/property-image.jpg

# Analyze the image
curl -X POST http://localhost:8000/api/v1/property-intelligence/analyze-image ^
  -H "Authorization: Bearer sk-gogidix-local-1234567890abcdef" ^
  -F "file=@sample_house.jpg" ^
  -F "property_id=test-property-123"
```

## Development Workflow

### 1. Make Code Changes
Edit any Python files in the `src/` directory.

### 2. Rebuild and Restart
```powershell
# Stop services
.\stop_local.bat

# Rebuild with changes
docker-compose -f docker-compose.local.yml up --build

# Or restart specific service
docker-compose -f docker-compose.local.yml restart ai-gateway
```

### 3. View Logs
```powershell
# View all logs
docker-compose -f docker-compose.local.yml logs -f

# View specific service logs
docker-compose -f docker-compose.local.yml logs -f ai-gateway
```

### 4. Access Service Shell
```powershell
# Access AI Gateway container
docker exec -it gogidix-ai-gateway bash

# Inside container
cd /workspace
python -m pytest tests/
```

## Troubleshooting

### Common Issues

#### 1. Docker Not Starting
**Error**: `docker: command not found`
**Solution**:
- Install Docker Desktop for Windows
- Start Docker Desktop service
- Add Docker to PATH

#### 2. Port Already in Use
**Error**: `Port 8000 is already allocated`
**Solution**:
```powershell
# Find what's using the port
netstat -ano | findstr :8000

# Kill the process
taskkill /PID <PID> /F
```

#### 3. Out of Memory
**Error**: `Container killed due to memory limit`
**Solution**:
- Increase Docker memory limit in Docker Desktop settings
- Stop unused services: `docker-compose -f docker-compose.local.yml stop analytics`

#### 4. GPU Not Available
**Error**: `CUDA out of memory`
**Solution**:
- Set `GPU_ENABLED=false` in `.env` file
- Or install NVIDIA Docker Toolkit

#### 5. Database Connection Failed
**Error**: `Connection to postgres failed`
**Solution**:
```powershell
# Check if PostgreSQL is running
docker-compose -f docker-compose.local.yml ps postgres

# Restart database
docker-compose -f docker-compose.local.yml restart postgres
```

### Health Check Script
Create a health check script `health_check.ps1`:

```powershell
# Check all services
$services = @(
    @{name="AI Gateway"; url="http://localhost:8000/health"},
    @{name="Property Intel"; url="http://localhost:8001/health"},
    @{name="Conversational AI"; url="http://localhost:8002/health"},
    @{name="Analytics"; url="http://localhost:8003/health"},
    @{name="ML Platform"; url="http://localhost:8004/health"},
    @{name="Ethical AI"; url="http://localhost:8005/health"}
)

foreach ($service in $services) {
    try {
        $response = Invoke-RestMethod -Uri $service.url -Method GET -TimeoutSec 5
        Write-Host "✓ $($service.name): Healthy" -ForegroundColor Green
    } catch {
        Write-Host "✗ $($service.name): Unhealthy - $($_.Exception.Message)" -ForegroundColor Red
    }
}
```

## Performance Tips

### 1. Use SSD Storage
Docker containers run much faster on SSD drives.

### 2. Allocate Enough RAM
- Minimum: 8GB for Docker
- Recommended: 16GB+ for ML workloads

### 3. Enable Docker File Sharing
Make sure your project directory is shared in Docker Desktop settings.

### 4. Use .dockerignore
Add a `.dockerignore` file to speed up builds:

```
.git
.pytest_cache
__pycache__
*.pyc
.env
logs/
temp/
.vscode/
```

## Monitoring

### Grafana Dashboard Access
1. Go to http://localhost:3000
2. Login with admin/admin
3. View pre-configured dashboards:
   - Service Health
   - API Performance
   - Resource Usage
   - ML Model Metrics

### Custom Metrics
Add custom metrics in your code:
```python
from prometheus_client import Counter, Histogram

REQUEST_COUNT = Counter('requests_total', 'Total requests', ['method', 'endpoint'])
REQUEST_LATENCY = Histogram('request_duration_seconds', 'Request latency')
```

## Cleanup

### Remove All Data
```powershell
# Stop and remove containers
docker-compose -f docker-compose.local.yml down -v

# Remove all Docker images
docker rmi $(docker images -q "gogidix*")

# Clean up volumes
docker volume prune
```

### Reset to Clean State
```powershell
# Remove generated files
Remove-Item -Recurse -Force data\*
Remove-Item -Recurse -Force models\*
Remove-Item -Recurse -Force logs\*

# Recreate clean environment
.\scripts\setup_local_env.ps1
```

## Next Steps

1. **Explore the APIs**: Visit http://localhost:8000/docs
2. **Run Tests**: `pytest tests/`
3. **Develop New Features**: Edit files in `src/`
4. **Train Custom Models**: Use the ML Platform at http://localhost:8004
5. **Monitor Performance**: Check Grafana at http://localhost:3000

## Support

For issues or questions:
- Check logs: `docker-compose logs -f [service]`
- Review documentation: `/docs` directory
- Create an issue on GitHub

Happy coding! 🚀