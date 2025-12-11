@echo off
echo Creating directory structure...

mkdir src\gogidix_ai 2>nul
mkdir src\gogidix_ai\gateway 2>nul
mkdir src\gogidix_ai\property_intelligence 2>nul
mkdir src\gogidix_ai\conversational_ai 2>nul
mkdir src\gogidix_ai\analytics 2>nul
mkdir src\gogidix_ai\core 2>nul

echo ✓ Directory structure created
echo Creating __init__.py files...

echo. > src\__init__.py
echo. > src\gogidix_ai\__init__.py
echo. > src\gogidix_ai\gateway\__init__.py
echo. > src\gogidix_ai\property_intelligence\__init__.py
echo. > src\gogidix_ai\conversational_ai\__init__.py
echo. > src\gogidix_ai\analytics\__init__.py
echo. > src\gogidix_ai\core\__init__.py

echo ✓ __init__.py files created
echo Creating basic service files...

echo Creating core config...
(
echo from pydantic_settings import BaseSettings
echo.
echo class Settings^(BaseSettings^):
echo     ENVIRONMENT: str = "native"
echo     DEBUG: bool = True
echo     VERSION: str = "1.0.0"
echo.
echo     class Config:
echo         env_file = ".env.native"
) > src\gogidix_ai\core\config.py

echo Creating logging module...
(
echo import logging
echo.
.echo def get_logger^(name: str^):
echo     return logging.getLogger^(name^)
) > src\gogidix_ai\core\logging.py

echo Creating exceptions...
(
echo class GogidixException^(Exception^):
echo     pass
echo.
.echo class ValidationError^(GogidixException^):
echo     pass
) > src\gogidix_ai\core\exceptions.py

echo Creating service bases...
(
echo from fastapi import FastAPI
echo from src.gogidix_ai.core.config import get_settings
echo.
.echo settings = get_settings^(^)
echo app = FastAPI^(title="Gogidix AI Gateway"^)
echo.
.echo @app.get^("/health"^)
.echo async def health^(^):
.echo     return {"status": "healthy", "service": "AI Gateway"}
) > src\gogidix_ai\gateway\main.py

(
echo from fastapi import FastAPI
echo.
.echo app = FastAPI^(title="Property Intelligence"^)
echo.
.echo @app.get^("/health"^)
.echo async def health^(^):
.echo     return {"status": "healthy", "service": "Property Intelligence"}
) > src\gogidix_ai\property_intelligence\main.py

(
echo from fastapi import FastAPI
echo.
echo app = FastAPI^(title="Conversational AI"^)
echo.
.echo @app.get^("/health"^)
echo async def health^(^):
echo     return {"status": "healthy", "service": "Conversational AI"}
) > src\gogidix_ai\conversational_ai\main.py

(
echo from fastapi import FastAPI
echo.
echo app = FastAPI^(title="Analytics"^)
echo.
.echo @app.get^("/health"^)
echo async def health^(^):
echo     return {"status": "healthy", "service": "Analytics"}
) > src\gogidix_ai\analytics\main.py

echo ✓ Basic service files created
echo.
echo ✅ Structure creation complete!
echo.
echo Now you can run: .\start_native_fixed.bat