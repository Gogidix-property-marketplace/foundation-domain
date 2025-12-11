"""
Native Configuration (No Docker)
Lightweight configuration for local development without Docker
"""

import os
from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings for native deployment."""

    # Basic Settings
    ENVIRONMENT: str = "native"
    DEBUG: bool = True
    LOG_LEVEL: str = "INFO"
    VERSION: str = "1.0.0"

    # Database (SQLite for native)
    DATABASE_URL: str = "sqlite:///./data/gogidix_ai.db"
    DATABASE_ECHO: bool = False

    # Cache (Simple in-memory cache)
    CACHE_ENABLED: bool = True
    CACHE_TYPE: str = "simple"  # or "redis" if Redis is installed
    CACHE_TTL: int = 300
    REDIS_URL: Optional[str] = None

    # Security
    SECRET_KEY: str = "your-super-secret-key-change-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    # API Settings
    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "Gogidix AI Services"
    DESCRIPTION: str = "AI-powered property marketplace platform"

    # File Storage
    UPLOAD_DIR: str = "./uploads"
    MODELS_DIR: str = "./models"
    DATA_DIR: str = "./data"
    LOGS_DIR: str = "./logs"

    # ML Settings
    ML_MODELS_PATH: str = "./models"
    DEVICE: str = "cpu"  # or "cuda" if available
    BATCH_SIZE: int = 32
    MAX_WORKERS: int = 4

    # Model Paths
    PROPERTY_VALUATION_MODEL: str = "./models/property_valuation_v1/model.joblib"
    IMAGE_ANALYSIS_MODEL: str = "./models/image_analysis_v1/model.h5"
    CHAT_MODEL_PATH: str = "./models/chat_v1/"

    # External APIs
    OPENAI_API_KEY: Optional[str] = None
    HUGGINGFACE_TOKEN: Optional[str] = None

    # Rate Limiting (Simple)
    RATE_LIMIT_ENABLED: bool = True
    RATE_LIMIT_REQUESTS: int = 100
    RATE_LIMIT_WINDOW: int = 60  # seconds

    # CORS
    BACKEND_CORS_ORIGINS: list[str] = [
        "http://localhost:3000",
        "http://localhost:8000",
        "http://127.0.0.1:8000",
        "*"
    ]

    # Monitoring (Basic)
    METRICS_ENABLED: bool = True
    LOG_REQUESTS: bool = True

    # Service URLs
    AI_GATEWAY_URL: str = "http://localhost:8000"
    PROPERTY_INTELLIGENCE_URL: str = "http://localhost:8001"
    CONVERSATIONAL_AI_URL: str = "http://localhost:8002"
    ANALYTICS_URL: str = "http://localhost:8003"

    @property
    def database_url_sync(self) -> str:
        """Sync database URL for SQLAlchemy."""
        return self.DATABASE_URL.replace("sqlite+aiosqlite://", "sqlite:///")

    class Config:
        env_file = ".env.native"
        case_sensitive = True


# Create global settings instance
settings = Settings()