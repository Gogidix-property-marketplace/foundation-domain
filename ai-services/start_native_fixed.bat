@echo off
echo 🚀 Starting Gogidix AI Services (Native Mode - FIXED)
echo =================================================

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Set environment variables - This is critical!
set PYTHONPATH=%CD%
set ENV_FILE=.env.native

REM Create logs directory
if not exist logs mkdir logs

REM Start AI Gateway (Main Service)
echo Starting AI Gateway...
start "AI Gateway" cmd /k "cd %CD% && uvicorn src.gogidix_ai.gateway.main:app --host 0.0.0.0 --port 8000 --reload --log-level info"

timeout /t 2 /nobreak

REM Start Property Intelligence Service
echo Starting Property Intelligence...
start "Property Intelligence" cmd /k "cd %CD% && uvicorn src.gogidix_ai.property_intelligence.main:app --host 0.0.0.0 --port 8001 --reload --log-level info"

timeout /t 2 /nobreak

REM Start Conversational AI Service
echo Starting Conversational AI...
start "Conversational AI" cmd /k "cd %CD% && uvicorn src.gogidix_ai.conversational_ai.main:app --host 0.0.0.0 --port 8002 --reload --log-level info"

timeout /t 2 /nobreak

REM Start Analytics Service
echo Starting Analytics...
start "Analytics" cmd /k "cd %CD% && uvicorn src.gogidix_ai.analytics.main:app --host 0.0.0.0 --port 8003 --reload --log-level info"

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