@echo off
echo 🚀 GOGIDIX AI SERVICES - ONE CLICK START & TEST
echo =============================================

REM Activate virtual environment
call venv\Scripts\activate.bat

echo.
echo Starting AI Services...
start cmd /k "python single_service.py"

echo.
echo Waiting for services to start...
timeout /t 5 /nobreak

echo.
echo Running API Tests...
python test_apis.py

echo.
echo ✅ Done! Check the other window for the running server.
echo 🌐 Open http://localhost:8000/docs in your browser for interactive API testing
pause