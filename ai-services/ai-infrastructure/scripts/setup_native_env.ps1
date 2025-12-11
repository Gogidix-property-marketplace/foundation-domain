# Native Python Environment Setup Script (No Docker Required)
# This script sets up Gogidix AI Services using native Python only

Write-Host "🚀 Setting up Gogidix AI Services (Native Python Mode)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green

# Check Python
try {
    $pythonVersion = python --version
    Write-Host "✓ Python found: $pythonVersion" -ForegroundColor Green
    if (!($pythonVersion -match "3\.([9-9]|[1-9][0-9])")) {
        Write-Host "⚠ Python 3.9+ recommended. Current version may not support all features." -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Python is not installed. Please install Python 3.9+ from https://www.python.org/downloads/" -ForegroundColor Red
    exit 1
}

# Create directories
Write-Host ""
Write-Host "Creating directories..." -ForegroundColor Yellow
$directories = @(
    "data",
    "models\property_valuation_v1",
    "logs",
    "temp",
    "uploads"
)

foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}
Write-Host "✓ Directories created" -ForegroundColor Green

# Create virtual environment
Write-Host ""
Write-Host "Creating Python virtual environment..." -ForegroundColor Yellow

if (!(Test-Path "venv")) {
    python -m venv venv
    Write-Host "✓ Virtual environment created" -ForegroundColor Green
} else {
    Write-Host "⚠ Virtual environment already exists" -ForegroundColor Yellow
}

# Activate virtual environment
Write-Host ""
Write-Host "Activating virtual environment..." -ForegroundColor Yellow
& .\venv\Scripts\Activate.ps1
Write-Host "✓ Virtual environment activated" -ForegroundColor Green

# Install dependencies
Write-Host ""
Write-Host "Installing Python dependencies..." -ForegroundColor Yellow

# Create requirements.txt for native setup
$requirements = @"
# Core FastAPI
fastapi>=0.104.0
uvicorn[standard]>=0.24.0
pydantic>=2.4.0
pydantic-settings>=2.0.0

# Database (SQLite for native setup)
sqlalchemy>=2.0.0
alembic>=1.12.0

# HTTP Client
httpx>=0.25.0
requests>=2.31.0

# ML Libraries
scikit-learn>=1.3.0
pandas>=2.1.0
numpy>=1.24.0
joblib>=1.3.0
shap>=0.42.0

# Image Processing
opencv-python>=4.8.0
pillow>=10.0.0

# NLP Libraries
transformers>=4.34.0
torch>=2.1.0

# ML Tools
xgboost>=1.7.0
lightgbm>=4.1.0

# Visualization
plotly>=5.17.0
matplotlib>=3.7.0

# Utility Libraries
jinja2>=3.1.0
passlib>=1.7.0
python-jose[cryptography]>=3.3.0
python-multipart>=0.0.6
aiofiles>=23.2.0
websockets>=12.0
python-dotenv>=1.0.0
psutil>=5.9.0

# Testing
pytest>=7.4.0
pytest-asyncio>=0.21.0
"@
$requirements | Out-File -FilePath "requirements-native.txt" -Encoding UTF8

# Install requirements
pip install -r requirements-native.txt
Write-Host "✓ Dependencies installed" -ForegroundColor Green

# Create environment configuration
Write-Host ""
Write-Host "Creating configuration files..." -ForegroundColor Yellow

$envContent = @"
# Native Python Configuration (No Docker)
ENVIRONMENT=native
DEBUG=true
LOG_LEVEL=INFO

# Database Configuration (SQLite)
DATABASE_URL=sqlite:///./data/gogidix_ai.db

# Cache Configuration (In-memory)
CACHE_ENABLED=true
CACHE_TYPE=simple
CACHE_TTL=300

# Authentication
SECRET_KEY=your-super-secret-key-change-in-production
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30

# API Configuration
API_V1_STR=/api/v1
PROJECT_NAME=Gogidix AI Services
VERSION=1.0.0

# File Storage
UPLOAD_DIR=./uploads
MODELS_DIR=./models
DATA_DIR=./data
LOGS_DIR=./logs

# ML Configuration
ML_MODELS_PATH=./models
DEVICE=cpu  # Use 'cuda' if you have NVIDIA GPU
BATCH_SIZE=32
MAX_WORKERS=4

# OpenAI API (Optional)
OPENAI_API_KEY=your-openai-api-key-here

# Rate Limiting (Basic)
RATE_LIMIT_ENABLED=true
RATE_LIMIT_REQUESTS=100
RATE_LIMIT_WINDOW=60
"@
$envContent | Out-File -FilePath ".env.native" -Encoding UTF8

# Create native startup script
$startScript = @"
@echo off
echo 🚀 Starting Gogidix AI Services (Native Mode)
echo ============================================

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Set environment
set PYTHONPATH=%CD%
set ENV_FILE=.env.native

REM Create logs directory
if not exist logs mkdir logs

REM Start AI Gateway (Main Service)
echo Starting AI Gateway...
start "AI Gateway" cmd /k "uvicorn src.gogidix_ai.gateway.main:app --host 0.0.0.0 --port 8000 --reload --log-level info"

timeout /t 2 /nobreak

REM Start Property Intelligence Service
echo Starting Property Intelligence...
start "Property Intelligence" cmd /k "uvicorn src.gogidix_ai.property_intelligence.main:app --host 0.0.0.0 --port 8001 --reload --log-level info"

timeout /t 2 /nobreak

REM Start Conversational AI Service
echo Starting Conversational AI...
start "Conversational AI" cmd /k "uvicorn src.gogidix_ai.conversational_ai.main:app --host 0.0.0.0 --port 8002 --reload --log-level info"

timeout /t 2 /nobreak

REM Start Analytics Service
echo Starting Analytics...
start "Analytics" cmd /k "uvicorn src.gogidix_ai.analytics.main:app --host 0.0.0.0 --port 8003 --reload --log-level info"

echo.
echo ✅ Services are starting...
echo.
echo 📊 Service URLs:
echo   • AI Gateway:         http://localhost:8000
echo   • Property Intel:     http://localhost:8001
echo   • Conversational AI:  http://localhost:8002
echo   • Analytics:          http://localhost:8003
echo.
echo 📝 API Documentation:
echo   • Swagger UI:         http://localhost:8000/docs
echo   • ReDoc:              http://localhost:8000/redoc
echo.
echo 🛑 To stop services:
echo    Close all command windows or run stop_native.bat
echo.
echo The services will start in separate windows...
pause
"@
$startScript | Out-File -FilePath "start_native.bat" -Encoding ASCII

# Create stop script
$stopScript = @"
@echo off
echo 🛑 Stopping Gogidix AI Services...

REM Kill Python processes for our services
taskkill /F /IM python.exe /FI "WINDOWTITLE eq AI Gateway*" 2>nul
taskkill /F /IM python.exe /FI "WINDOWTITLE eq Property Intelligence*" 2>nul
taskkill /F /IM python.exe /FI "WINDOWTITLE eq Conversational AI*" 2>nul
taskkill /F /IM python.exe /FI "WINDOWTITLE eq Analytics*" 2>nul

echo ✅ Services stopped!
pause
"@
$stopScript | Out-File -FilePath "stop_native.bat" -Encoding ASCII

# Create test script
$testScript = @"
@echo off
echo 🧪 Testing Native API Services...

echo Testing AI Gateway health...
curl -s http://localhost:8000/health

echo.
echo Testing property valuation...
curl -X POST http://localhost:8000/api/v1/property-valuation ^
  -H "Content-Type: application/json" ^
  -d "{""property_type"": ""apartment"", ""bedrooms"": 2, ""bathrooms"": 2, ""square_feet"": 1200, ""city"": ""New York""}"

echo.
echo Testing chat endpoint...
curl -X POST http://localhost:8000/api/v1/chat ^
  -H "Content-Type: application/json" ^
  -d "{""message"": ""Hello, can you help me find a property?""}"

echo.
echo ✅ Tests completed!
pause
"@
$testScript | Out-File -FilePath "test_native.bat" -Encoding ASCII

# Create data generation script
$dataGenScript = @"
@echo off
echo 📊 Generating Synthetic Property Data...

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Run data generator
python data/synthetic_data_generator.py

echo ✅ Data generation complete!
echo Check the data/ directory for generated files.
pause
"@
$dataGenScript | Out-File -FilePath "generate_data.bat" -Encoding ASCII

# Create model training script
$trainScript = @"
@echo off
echo 🤖 Training Property Valuation Model...

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Check if data exists
if not exist "data\properties_sample_1000.parquet" (
    echo ⚠ No training data found. Generating sample data first...
    python data/synthetic_data_generator.py
)

REM Train model
python training/train_property_valuation.py --data data/properties_sample_1000.parquet

echo ✅ Model training complete!
echo Check the models/ directory for trained models.
pause
"@
$trainScript | Out-File -FilePath "train_native.bat" -Encoding ASCII

Write-Host "✓ Scripts created" -ForegroundColor Green

# Initialize database
Write-Host ""
Write-Host "Initializing database..." -ForegroundColor Yellow

# Create database init script
$dbInit = @"
import sqlite3
import os
from datetime import datetime

# Create database directory
os.makedirs('data', exist_ok=True)

# Connect to SQLite database
conn = sqlite3.connect('data/gogidix_ai.db')
cursor = conn.cursor()

# Create tables
cursor.executescript('''
    -- Properties table
    CREATE TABLE IF NOT EXISTS properties (
        id TEXT PRIMARY KEY,
        property_type TEXT,
        address TEXT,
        city TEXT,
        state TEXT,
        bedrooms INTEGER,
        bathrooms INTEGER,
        square_feet INTEGER,
        year_built INTEGER,
        price REAL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- Users table
    CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        email TEXT UNIQUE,
        hashed_password TEXT,
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- API keys table
    CREATE TABLE IF NOT EXISTS api_keys (
        id TEXT PRIMARY KEY,
        key_id TEXT UNIQUE,
        hashed_key TEXT,
        user_id TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id)
    );

    -- ML models table
    CREATE TABLE IF NOT EXISTS ml_models (
        id TEXT PRIMARY KEY,
        name TEXT,
        version TEXT,
        model_path TEXT,
        metrics TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
''')

conn.commit()
conn.close()

print("✅ Database initialized successfully")
"@
$dbInit | Out-File -FilePath "scripts\init_db_native.py" -Encoding UTF8

# Run database initialization
python scripts\init_db_native.py
Write-Host "✓ Database initialized" -ForegroundColor Green

# Create simple monitoring script
$monitorScript = @"
@echo off
echo 📊 Service Monitor
echo ================

:loop
cls
echo.
echo Checking services at %time%...
echo.

REM Check if services are responding
powershell -Command "try { Invoke-RestMethod -Uri 'http://localhost:8000/health' -TimeoutSec 2 | Out-Null; Write-Host '✅ AI Gateway (8000): Running' -ForegroundColor Green } catch { Write-Host '❌ AI Gateway (8000): Down' -ForegroundColor Red }"

powershell -Command "try { Invoke-RestMethod -Uri 'http://localhost:8001/health' -TimeoutSec 2 | Out-Null; Write-Host '✅ Property Intel (8001): Running' -ForegroundColor Green } catch { Write-Host '❌ Property Intel (8001): Down' -ForegroundColor Red }"

powershell -Command "try { Invoke-RestMethod -Uri 'http://localhost:8002/health' -TimeoutSec 2 | Out-Null; Write-Host '✅ Conversational AI (8002): Running' -ForegroundColor Green } catch { Write-Host '❌ Conversational AI (8002): Down' -ForegroundColor Red }"

powershell -Command "try { Invoke-RestMethod -Uri 'http://localhost:8003/health' -TimeoutSec 2 | Out-Null; Write-Host '✅ Analytics (8003): Running' -ForegroundColor Green } catch { Write-Host '❌ Analytics (8003): Down' -ForegroundColor Red }"

echo.
echo Memory Usage:
powershell -Command "Get-Process -Name python | Select-Object ProcessName, @{Name='Memory(MB)';Expression={[math]::Round($_.WorkingSet/1MB,2)}} | Format-Table -AutoSize"

echo.
echo Press Ctrl+C to stop monitoring
timeout /t 5 /nobreak
goto loop
"@
$monitorScript | Out-File -FilePath "monitor_services.bat" -Encoding ASCII

Write-Host "✓ Monitor script created" -ForegroundColor Green

# Summary
Write-Host ""
Write-Host "🎉 Native Python Setup Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Your lightweight setup is ready with:" -ForegroundColor Cyan
Write-Host "  ✅ SQLite Database (no PostgreSQL needed)" -ForegroundColor Gray
Write-Host "  ✅ In-memory Cache (no Redis needed)" -ForegroundColor Gray
Write-Host "  ✅ Fast Local APIs (no network overhead)" -ForegroundColor Gray
Write-Host "  ✅ Low Resource Usage (~500MB RAM)" -ForegroundColor Gray
Write-Host "  ❌ No Kafka, Elasticsearch, or Grafana" -ForegroundColor Yellow
Write-Host ""
Write-Host "Quick Start:" -ForegroundColor Cyan
Write-Host "1. Generate test data: .\generate_data.bat" -ForegroundColor White
Write-Host "2. Train ML models: .\train_native.bat" -ForegroundColor White
Write-Host "3. Start services: .\start_native.bat" -ForegroundColor White
Write-Host "4. Test APIs: .\test_native.bat" -ForegroundColor White
Write-Host "5. Monitor: .\monitor_services.bat" -ForegroundColor White
Write-Host ""
Write-Host "📊 Access Points:" -ForegroundColor Cyan
Write-Host "  • API Gateway: http://localhost:8000" -ForegroundColor White
Write-Host "  • API Docs: http://localhost:8000/docs" -ForegroundColor White
Write-Host "  • Services run in separate windows" -ForegroundColor White
Write-Host ""
Write-Host "✅ Ready to start! 🚀" -ForegroundColor Green

Write-Host ""
Write-Host "Press any key to continue..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")