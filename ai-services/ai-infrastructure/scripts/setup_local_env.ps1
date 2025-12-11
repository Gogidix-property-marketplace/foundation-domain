# Local Development Environment Setup Script for Windows PowerShell
# This script sets up the Gogidix AI Services for local development on Windows

Write-Host "🚀 Setting up Gogidix AI Services Local Development Environment" -ForegroundColor Green
Write-Host "==============================================================" -ForegroundColor Green

# Check prerequisites
Write-Host ""
Write-Host "Checking prerequisites..." -ForegroundColor Yellow

# Check if Docker is installed
try {
    $dockerVersion = docker --version
    Write-Host "✓ Docker found: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not installed. Please install Docker Desktop for Windows first." -ForegroundColor Red
    Write-Host "Download from: https://www.docker.com/products/docker-desktop" -ForegroundColor Cyan
    exit 1
}

# Check if Docker is running
try {
    docker info | Out-Null
    Write-Host "✓ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Check if Python is installed
try {
    $pythonVersion = python --version
    Write-Host "✓ Python found: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Python is not installed. Please install Python 3.9 or higher." -ForegroundColor Red
    Write-Host "Download from: https://www.python.org/downloads/" -ForegroundColor Cyan
    exit 1
}

# Check if pip is installed
try {
    $pipVersion = pip --version
    Write-Host "✓ Pip found: $pipVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ pip is not installed. Please install pip first." -ForegroundColor Red
    exit 1
}

# Create necessary directories
Write-Host ""
Write-Host "Creating directories..." -ForegroundColor Yellow

$directories = @(
    "data",
    "models\property_valuation_v1",
    "logs",
    "monitoring\grafana\dashboards",
    "monitoring\grafana\datasources",
    "nginx\ssl",
    "scripts",
    "temp"
)

foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}
Write-Host "✓ Directories created" -ForegroundColor Green

# Create environment file
Write-Host ""
Write-Host "Creating environment file..." -ForegroundColor Yellow

$envFile = ".env"
if (!(Test-Path $envFile)) {
    $envContent = @"
# Database Configuration
DATABASE_URL=postgresql://gogidix:gogidix123@localhost:5432/gogidix_ai
REDIS_URL=redis://localhost:6379

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Elasticsearch Configuration
ELASTICSEARCH_URL=http://localhost:9200

# MLflow Configuration
MLFLOW_TRACKING_URI=http://localhost:5000

# Service Configuration
ENVIRONMENT=development
DEBUG=true
LOG_LEVEL=INFO

# Authentication
API_KEY=sk-gogidix-local-1234567890abcdef
JWT_SECRET=your-super-secret-jwt-key-change-in-production
TOKEN_ISSUER=https://api.gogidix.com
TOKEN_AUDIENCE=gogidix-api

# GPU Support
GPU_ENABLED=false

# OpenAI API (Optional - for enhanced NLP features)
OPENAI_API_KEY=your-openai-api-key-here

# Hugging Face Token (Optional - for custom models)
HUGGINGFACE_TOKEN=your-huggingface-token-here

# MinIO/S3 Configuration (Optional)
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=gogidix-ai-data

# File Paths
ASSETS_DIR=./assets
MODELS_DIR=./models
LOGS_DIR=./logs

# API Gateway Configuration
GATEWAY_URL=http://localhost:8000
SERVICE_NAME=ai-gateway
SERVICE_ID=gateway-local-001
SERVICE_HOST=localhost
SERVICE_PORT=8000

# Rate Limiting
RATE_LIMIT_ENABLED=true
RATE_LIMIT_DEFAULT=1000
RATE_LIMIT_WINDOW=3600
RATE_LIMIT_BURST=100

# Cache Configuration
CACHE_ENABLED=true
CACHE_TTL=300
CACHE_MAX_SIZE=1000

# Monitoring
METRICS_ENABLED=true
TRACING_ENABLED=false
LOG_REQUESTS=true
"@
    $envContent | Out-File -FilePath $envFile -Encoding UTF8
    Write-Host "✓ .env file created" -ForegroundColor Green
} else {
    Write-Host "⚠ .env file already exists. Skipping creation." -ForegroundColor Yellow
}

# Create monitoring configuration
Write-Host ""
Write-Host "Setting up monitoring..." -ForegroundColor Yellow

# Prometheus configuration
$prometheusConfig = @"
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'ai-gateway'
    static_configs:
      - targets: ['ai-gateway:8000']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'property-intelligence'
    static_configs:
      - targets: ['property-intelligence:8001']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'conversational-ai'
    static_configs:
      - targets: ['conversational-ai:8002']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'analytics'
    static_configs:
      - targets: ['analytics:8003']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'ml-platform'
    static_configs:
      - targets: ['ml-platform:8004']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'ethical-ai'
    static_configs:
      - targets: ['ethical-ai:8005']
    metrics_path: /metrics
    scrape_interval: 10s
"@
$prometheusConfig | Out-File -FilePath "monitoring\prometheus.yml" -Encoding UTF8

# Grafana datasource configuration
$grafanaDatasource = @"
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
"@
$grafanaDatasource | Out-File -FilePath "monitoring\grafana\datasources\prometheus.yml" -Encoding UTF8

Write-Host "✓ Monitoring configuration created" -ForegroundColor Green

# Create database initialization script
Write-Host ""
Write-Host "Setting up database..." -ForegroundColor Yellow

$dbInit = @"
-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS ai_services;
CREATE SCHEMA IF NOT EXISTS mlflow;

-- Grant permissions
GRANT ALL ON SCHEMA ai_services TO gogidix;
GRANT ALL ON SCHEMA mlflow TO gogidix;
"@
$dbInit | Out-File -FilePath "scripts\init-db.sql" -Encoding UTF8
Write-Host "✓ Database initialization script created" -ForegroundColor Green

# Create Windows batch files for easy execution
Write-Host ""
Write-Host "Creating startup scripts..." -ForegroundColor Yellow

# Start script
$startScript = @"
@echo off
echo 🚀 Starting Gogidix AI Services...

REM Start infrastructure services
echo Starting infrastructure...
docker-compose -f docker-compose.local.yml up -d postgres redis kafka zookeeper elasticsearch

REM Wait for services
echo Waiting for services to be ready...
timeout /t 10 /nobreak

REM Start MLflow
echo Starting MLflow...
docker-compose -f docker-compose.local.yml up -d mlflow

REM Start monitoring
echo Starting monitoring...
docker-compose -f docker-compose.local.yml up -d prometheus grafana

REM Start core services
echo Starting core AI services...
docker-compose -f docker-compose.local.yml up -d ai-gateway

REM Wait for gateway
echo Waiting for AI Gateway...
timeout /t 5 /nobreak

REM Start all services
echo Starting all AI services...
docker-compose -f docker-compose.local.yml up -d property-intelligence conversational-ai analytics ml-platform ethical-ai

REM Generate synthetic data
echo Generating synthetic data...
docker-compose -f docker-compose.local.yml up data-processor

echo.
echo ✅ All services started!
echo.
echo 📊 Service URLs:
echo   • AI Gateway:         http://localhost:8000
echo   • Property Intel:     http://localhost:8001
echo   • Conversational AI:  http://localhost:8002
echo   • Analytics:          http://localhost:8003
echo   • ML Platform:        http://localhost:8004
echo   • Ethical AI:         http://localhost:8005
echo   • MLflow UI:          http://localhost:5000
echo   • Grafana Dashboard:  http://localhost:3000 (admin/admin)
echo   • Prometheus:         http://localhost:9090
echo.
echo 📝 API Documentation:
echo   • Swagger UI:         http://localhost:8000/docs
echo   • ReDoc:              http://localhost:8000/redoc
echo.
echo To train models:
echo   docker-compose -f docker-compose.local.yml up --build model-trainer
echo.
echo To stop all services:
echo   docker-compose -f docker-compose.local.yml down
echo.
pause
"@
$startScript | Out-File -FilePath "start_local.bat" -Encoding ASCII

# Stop script
$stopScript = @"
@echo off
echo 🛑 Stopping Gogidix AI Services...
docker-compose -f docker-compose.local.yml down
echo ✅ All services stopped!
pause
"@
$stopScript | Out-File -FilePath "stop_local.bat" -Encoding ASCII

# Train models script
$trainScript = @"
@echo off
echo 🤖 Training property valuation model...
docker-compose -f docker-compose.local.yml up --build model-trainer
echo ✅ Model training complete!
echo Check the models\ directory for trained models.
pause
"@
$trainScript | Out-File -FilePath "train_models.bat" -Encoding ASCII

# Test API script
$testScript = @"
@echo off
echo 🧪 Testing API endpoints...

REM Check if services are running
curl -s http://localhost:8000/health >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Services not running. Please run start_local.bat first.
    pause
    exit /b 1
)

REM Test health endpoint
echo Testing health endpoint...
curl -s http://localhost:8000/health

REM Test property valuation
echo.
echo Testing property valuation...
curl -X POST http://localhost:8000/api/v1/property-valuation ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: sk-gogidix-local-1234567890abcdef" ^
  -d "{""property_type"": ""apartment"", ""bedrooms"": 2, ""bathrooms"": 2, ""square_feet"": 1200, ""year_built"": 2010, ""city"": ""New York"", ""state"": ""NY""}"

echo.
echo ✅ API tests complete!
pause
"@
$testScript | Out-File -FilePath "test_api.bat" -Encoding ASCII

Write-Host "✓ Startup scripts created" -ForegroundColor Green

# Install Python dependencies
Write-Host ""
Write-Host "Installing Python dependencies..." -ForegroundColor Yellow

try {
    # Create requirements.txt if not exists
    if (!(Test-Path "requirements.txt")) {
        $requirements = @"
fastapi>=0.104.0
uvicorn[standard]>=0.24.0
pydantic>=2.4.0
pydantic-settings>=2.0.0
sqlalchemy>=2.0.0
alembic>=1.12.0
psycopg2-binary>=2.9.0
redis>=5.0.0
kafka-python>=2.0.0
elasticsearch>=8.10.0
prometheus-client>=0.18.0
mlflow>=2.7.0
scikit-learn>=1.3.0
pandas>=2.1.0
numpy>=1.24.0
joblib>=1.3.0
shap>=0.42.0
opencv-python>=4.8.0
pillow>=10.0.0
transformers>=4.34.0
torch>=2.1.0
tensorflow>=2.14.0
xgboost>=1.7.0
lightgbm>=4.1.0
plotly>=5.17.0
jinja2>=3.1.0
passlib>=1.7.0
python-jose[cryptography]>=3.3.0
python-multipart>=0.0.6
aiofiles>=23.2.0
httpx>=0.25.0
websockets>=12.0
python-dotenv>=1.0.0
"@
        $requirements | Out-File -FilePath "requirements.txt" -Encoding UTF8
    }

    # Install dependencies
    pip install -r requirements.txt
    Write-Host "✓ Python dependencies installed" -ForegroundColor Green
} catch {
    Write-Host "⚠ Failed to install Python dependencies. You may need to run this manually." -ForegroundColor Yellow
    Write-Host "Run: pip install -r requirements.txt" -ForegroundColor Cyan
}

# Final steps
Write-Host ""
Write-Host "🎉 Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Start the services:      .\start_local.bat" -ForegroundColor White
Write-Host "2. Train the models:        .\train_models.bat" -ForegroundColor White
Write-Host "3. Test the APIs:          .\test_api.bat" -ForegroundColor White
Write-Host "4. Access Jupyter Lab:     http://localhost:8888" -ForegroundColor White
Write-Host "5. View Grafana:           http://localhost:3000" -ForegroundColor White
Write-Host ""
Write-Host "⚠ Note: The first start may take a few minutes as Docker images are downloaded." -ForegroundColor Yellow
Write-Host ""
Write-Host "📚 Useful commands:" -ForegroundColor Cyan
Write-Host "  • View logs:          docker-compose -f docker-compose.local.yml logs -f [service-name]" -ForegroundColor White
Write-Host "  • Access shell:       docker exec -it gogidix-ai-gateway bash" -ForegroundColor White
Write-Host "  • Stop services:      .\stop_local.bat" -ForegroundColor White
Write-Host "  • Rebuild services:   docker-compose -f docker-compose.local.yml up --build" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Configuration:" -ForegroundColor Cyan
Write-Host "  • Edit .env file to change settings" -ForegroundColor White
Write-Host "  • Add OPENAI_API_KEY for enhanced NLP features" -ForegroundColor White
Write-Host "  • Set GPU_ENABLED=true if you have NVIDIA GPU and Docker GPU support" -ForegroundColor White
Write-Host ""
Write-Host "✅ Local development environment is ready! 🚀" -ForegroundColor Green

# Pause for user to see the output
Write-Host ""
Write-Host "Press any key to continue..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")