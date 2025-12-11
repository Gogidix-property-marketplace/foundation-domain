"""
Enterprise Configuration Management

Production-grade configuration with environment-specific settings,
security best practices, and validation.
"""

import os
from functools import lru_cache
from typing import Any, Dict, List, Optional, Union

from pydantic import BaseSettings, Field, field_validator
from pydantic_settings import BaseSettings as PydanticBaseSettings
from pydantic import SecretStr


class Settings(BaseSettings):
    """Production configuration with validation and security defaults."""

    # Application Core
    APP_NAME: str = Field(default="Gogidix AI Services", env="APP_NAME")
    APP_VERSION: str = Field(default="1.0.0", env="APP_VERSION")
    ENVIRONMENT: str = Field(default="development", env="ENVIRONMENT")
    DEBUG: bool = Field(default=False, env="DEBUG")
    LOG_LEVEL: str = Field(default="INFO", env="LOG_LEVEL")

    # API Configuration
    API_V1_PREFIX: str = Field(default="/api/v1", env="API_V1_PREFIX")
    API_HOST: str = Field(default="0.0.0.0", env="API_HOST")
    API_PORT: int = Field(default=8000, env="API_PORT")
    API_WORKERS: int = Field(default=1, env="API_WORKERS")
    API_TIMEOUT: int = Field(default=30000, env="API_TIMEOUT")  # 30 seconds

    # Security Configuration
    SECRET_KEY: str = Field(..., env="SECRET_KEY")
    ALGORITHM: str = Field(default="HS256", env="ALGORITHM")
    ACCESS_TOKEN_EXPIRE_MINUTES: int = Field(default=30, env="ACCESS_TOKEN_EXPIRE_MINUTES")
    REFRESH_TOKEN_EXPIRE_DAYS: int = Field(default=7, env="REFRESH_TOKEN_EXPIRE_DAYS")

    # CORS Configuration
    CORS_ORIGINS: List[str] = Field(
        default=["http://localhost:3000"],
        env="CORS_ORIGINS"
    )
    CORS_ALLOW_CREDENTIALS: bool = Field(default=True, env="CORS_ALLOW_CREDENTIALS")
    CORS_ALLOW_METHODS: List[str] = Field(
        default=["*"],
        env="CORS_ALLOW_METHODS"
    )
    CORS_ALLOW_HEADERS: List[str] = Field(
        default=["*"],
        env="CORS_ALLOW_HEADERS"
    )

    # Database Configuration
    DATABASE_URL: str = Field(..., env="DATABASE_URL")
    DATABASE_POOL_SIZE: int = Field(default=20, env="DATABASE_POOL_SIZE")
    DATABASE_MAX_OVERFLOW: int = Field(default=30, env="DATABASE_MAX_OVERFLOW")
    DATABASE_POOL_TIMEOUT: int = Field(default=30, env="DATABASE_POOL_TIMEOUT")

    # Redis Configuration
    REDIS_URL: str = Field(..., env="REDIS_URL")
    REDIS_MAX_CONNECTIONS: int = Field(default=100, env="REDIS_MAX_CONNECTIONS")
    REDIS_PASSWORD: Optional[str] = Field(default=None, env="REDIS_PASSWORD")

    # Kafka Configuration
    KAFKA_BOOTSTRAP_SERVERS: List[str] = Field(
        default=["localhost:9092"],
        env="KAFKA_BOOTSTRAP_SERVERS"
    )
    KAFKA_TOPIC_PREFIX: str = Field(default="gogidix-ai", env="KAFKA_TOPIC_PREFIX")
    KAFKA_CONSUMER_GROUP: str = Field(default="ai-services", env="KAFKA_CONSUMER_GROUP")
    KAFKA_AUTO_OFFSET_RESET: str = Field(default="latest", env="KAFKA_AUTO_OFFSET_RESET")

    # Elasticsearch Configuration
    ELASTICSEARCH_HOSTS: List[str] = Field(
        default=["localhost:9200"],
        env="ELASTICSEARCH_HOSTS"
    )
    ELASTICSEARCH_INDEX_PREFIX: str = Field(
        default="gogidix-ai",
        env="ELASTICSEARCH_INDEX_PREFIX"
    )
    ELASTICSEARCH_TIMEOUT: int = Field(default=30, env="ELASTICSEARCH_TIMEOUT")

    # Vector Database Configuration (Milvus)
    MILVUS_HOST: str = Field(default="localhost", env="MILVUS_HOST")
    MILVUS_PORT: int = Field(default=19530, env="MILVUS_PORT")
    MILVUS_COLLECTION_PREFIX: str = Field(default="gogidix_ai_", env="MILVUS_COLLECTION_PREFIX")

    # Model Configuration
    MODEL_REGISTRY_URL: str = Field(default="http://mlflow:5000", env="MODEL_REGISTRY_URL")
    MODEL_SERVING_URL: str = Field(default="http://model-serving:8080", env="MODEL_SERVING_URL")
    DEFAULT_MODEL_TIMEOUT: int = Field(default=5000, env="DEFAULT_MODEL_TIMEOUT")  # 5 seconds
    MAX_BATCH_SIZE: int = Field(default=32, env="MAX_BATCH_SIZE")
    MAX_CONCURRENT_REQUESTS: int = Field(default=1000, env="MAX_CONCURRENT_REQUESTS")

    # GPU Configuration
    GPU_ENABLED: bool = Field(default=False, env="GPU_ENABLED")
    GPU_MEMORY_LIMIT: Optional[str] = Field(default=None, env="GPU_MEMORY_LIMIT")
    CUDA_VISIBLE_DEVICES: Optional[str] = Field(default=None, env="CUDA_VISIBLE_DEVICES")

    # Rate Limiting Configuration
    RATE_LIMIT_ENABLED: bool = Field(default=True, env="RATE_LIMIT_ENABLED")
    RATE_LIMIT_REQUESTS_PER_MINUTE: int = Field(
        default=60,
        env="RATE_LIMIT_REQUESTS_PER_MINUTE"
    )
    RATE_LIMIT_BURST_SIZE: int = Field(default=10, env="RATE_LIMIT_BURST_SIZE")

    # Caching Configuration
    CACHE_TTL_SECONDS: int = Field(default=3600, env="CACHE_TTL_SECONDS")  # 1 hour
    CACHE_MAX_SIZE: int = Field(default=10000, env="CACHE_MAX_SIZE")

    # Monitoring Configuration
    METRICS_ENABLED: bool = Field(default=True, env="METRICS_ENABLED")
    METRICS_PORT: int = Field(default=9090, env="METRICS_PORT")
    JAEGER_ENDPOINT: Optional[str] = Field(default=None, env="JAEGER_ENDPOINT")
    PROMETHEUS_MULTIPROC_DIR: str = Field(
        default="/tmp/prometheus_multiproc_dir",
        env="PROMETHEUS_MULTIPROC_DIR"
    )

    # External API Configuration
    GOOGLE_MAPS_API_KEY: Optional[str] = Field(default=None, env="GOOGLE_MAPS_API_KEY")
    WEATHER_API_KEY: Optional[str] = Field(default=None, env="WEATHER_API_KEY")
    PROPERTY_DATA_API_KEY: Optional[str] = Field(default=None, env="PROPERTY_DATA_API_KEY")

    # API Gateway Configuration
    API_GATEWAY_URL: str = Field(default="https://api.gogidix.com", env="API_GATEWAY_URL")
    API_GATEWAY_TIMEOUT: int = Field(default=30, env="API_GATEWAY_TIMEOUT")
    API_GATEWAY_RETRY_ATTEMPTS: int = Field(default=3, env="API_GATEWAY_RETRY_ATTEMPTS")

    # Service Identity for Gateway
    SERVICE_NAME: str = Field(..., env="SERVICE_NAME")
    SERVICE_ID: str = Field(..., env="SERVICE_ID")
    SERVICE_VERSION: str = Field(default="1.0.0", env="SERVICE_VERSION")
    SERVICE_HOST: str = Field(default="localhost", env="SERVICE_HOST")
    SERVICE_PORT: int = Field(default=8000, env="SERVICE_PORT")

    # Gateway Authentication
    GATEWAY_API_KEY: SecretStr = Field(..., env="GATEWAY_API_KEY")
    JWT_SECRET: Optional[str] = Field(None, env="JWT_SECRET")
    JWT_ALGORITHM: str = Field(default="RS256", env="JWT_ALGORITHM")
    TOKEN_ISSUER: str = Field(default="https://auth.gogidix.com", env="TOKEN_ISSUER")
    TOKEN_AUDIENCE: str = Field(default="gogidix-api", env="TOKEN_AUDIENCE")

    # Service Registry
    SERVICE_REGISTRY_URL: str = Field(
        default="https://registry.gogidix.com",
        env="SERVICE_REGISTRY_URL"
    )
    SERVICE_REGISTRY_HEARTBEAT_INTERVAL: int = Field(
        default=30,
        env="SERVICE_REGISTRY_HEARTBEAT_INTERVAL"
    )

    # AI Model Paths
    MODEL_BASE_PATH: str = Field(default="/app/models", env="MODEL_BASE_PATH")
    PROPERTY_VALUATION_MODEL_PATH: str = Field(
        default="/app/models/property_valuation",
        env="PROPERTY_VALUATION_MODEL_PATH"
    )
    IMAGE_RECOGNITION_MODEL_PATH: str = Field(
        default="/app/models/image_recognition",
        env="IMAGE_RECOGNITION_MODEL_PATH"
    )
    NLP_MODEL_PATH: str = Field(
        default="/app/models/nlp",
        env="NLP_MODEL_PATH"
    )
    CHATBOT_MODEL_PATH: str = Field(
        default="/app/models/chatbot",
        env="CHATBOT_MODEL_PATH"
    )

    # Feature Store Configuration
    FEATURE_STORE_URL: str = Field(default="http://feature-store:8080", env="FEATURE_STORE_URL")
    FEATURE_REFRESH_INTERVAL: int = Field(default=3600, env="FEATURE_REFRESH_INTERVAL")

    # Security Headers
    SECURITY_HEADERS: Dict[str, str] = Field(
        default={
            "X-Content-Type-Options": "nosniff",
            "X-Frame-Options": "DENY",
            "X-XSS-Protection": "1; mode=block",
            "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
            "Content-Security-Policy": "default-src 'self'",
        }
    )

    # Health Check Configuration
    HEALTH_CHECK_INTERVAL: int = Field(default=30, env="HEALTH_CHECK_INTERVAL")
    HEALTH_CHECK_TIMEOUT: int = Field(default=5, env="HEALTH_CHECK_TIMEOUT")

    # OpenAI Configuration (for advanced AI features)
    OPENAI_API_KEY: Optional[str] = Field(default=None, env="OPENAI_API_KEY")
    OPENAI_MODEL: str = Field(default="gpt-4", env="OPENAI_MODEL")
    OPENAI_MAX_TOKENS: int = Field(default=2048, env="OPENAI_MAX_TOKENS")
    OPENAI_TEMPERATURE: float = Field(default=0.7, env="OPENAI_TEMPERATURE")

    # Ethical AI Configuration
    ETHICAL_AI_ENABLED: bool = Field(default=True, env="ETHICAL_AI_ENABLED")
    BIAS_DETECTION_ENABLED: bool = Field(default=True, env="BIAS_DETECTION_ENABLED")
    EXPLAINABILITY_ENABLED: bool = Field(default=True, env="EXPLAINABILITY_ENABLED")
    COMPLIANCE_MONITORING_ENABLED: bool = Field(default=True, env="COMPLIANCE_MONITORING_ENABLED")

    # Bias Detection Settings
    PROTECTED_ATTRIBUTES: List[str] = Field(
        default=["gender", "race", "ethnicity", "age", "disability", "religion", "sexual_orientation"],
        env="PROTECTED_ATTRIBUTES"
    )
    FAIRNESS_THRESHOLDS: Dict[str, float] = Field(
        default={
            "demographic_parity": 0.1,
            "equalized_odds": 0.15,
            "equal_opportunity": 0.1,
            "individual_fairness": 0.05,
            "geographic_fairness": 0.2
        },
        env="FAIRNESS_THRESHOLDS"
    )
    BIAS_MITIGATION_STRATEGY: str = Field(default="preprocessing", env="BIAS_MITIGATION_STRATEGY")

    # Explainability Settings
    EXPLANATION_METHODS: List[str] = Field(
        default=["shap", "lime", "feature_importance", "surrogate"],
        env="EXPLANATION_METHODS"
    )
    EXPLANATION_CONFIDENCE_THRESHOLD: float = Field(default=0.7, env="EXPLANATION_CONFIDENCE_THRESHOLD")
    COUNTERFACTUAL_ENABLED: bool = Field(default=True, env="COUNTERFACTUAL_ENABLED")

    # Compliance Settings
    COMPLIANCE_STANDARDS: List[str] = Field(
        default=["ai_act_high_risk", "gdpr_article_22", "iso_iec_42001", "nist_ai_rmf"],
        env="COMPLIANCE_STANDARDS"
    )
    ASSESSMENT_FREQUENCY_DAYS: int = Field(default=90, env="ASSESSMENT_FREQUENCY_DAYS")
    ETHICAL_SCORE_THRESHOLD: float = Field(default=70.0, env="ETHICAL_SCORE_THRESHOLD")
    AUTO_BLOCK_UNETHICAL: bool = Field(default=False, env="AUTO_BLOCK_UNETHICAL")

    # Documentation and Reporting
    ETHICAL_REPORTS_PATH: str = Field(default="./ethical_reports", env="ETHICAL_REPORTS_PATH")
    DOCUMENTATION_RETENTION_DAYS: int = Field(default=2555, env="DOCUMENTATION_RETENTION_DAYS")  # 7 years
    AUDIT_LOG_ENABLED: bool = Field(default=True, env="AUDIT_LOG_ENABLED")

    # Ethical AI Monitoring
    CONTINUOUS_MONITORING_ENABLED: bool = Field(default=True, env="CONTINUOUS_MONITORING_ENABLED")
    MONITORING_INTERVAL_MINUTES: int = Field(default=60, env="MONITORING_INTERVAL_MINUTES")
    ETHICAL_ALERT_EMAILS: List[str] = Field(default=[], env="ETHICAL_ALERT_EMAILS")

    @field_validator("CORS_ORIGINS", mode="before")
    @classmethod
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        """Parse CORS origins from string or list."""
        if isinstance(v, str):
            return [i.strip() for i in v.split(",")]
        return v

    @field_validator("KAFKA_BOOTSTRAP_SERVERS", mode="before")
    @classmethod
    def assemble_kafka_servers(cls, v: Union[str, List[str]]) -> List[str]:
        """Parse Kafka bootstrap servers from string or list."""
        if isinstance(v, str):
            return [i.strip() for i in v.split(",")]
        return v

    @field_validator("ELASTICSEARCH_HOSTS", mode="before")
    @classmethod
    def assemble_elasticsearch_hosts(cls, v: Union[str, List[str]]) -> List[str]:
        """Parse Elasticsearch hosts from string or list."""
        if isinstance(v, str):
            return [i.strip() for i in v.split(",")]
        return v

    @field_validator("PROTECTED_ATTRIBUTES", mode="before")
    @classmethod
    def assemble_protected_attributes(cls, v: Union[str, List[str]]) -> List[str]:
        """Parse protected attributes from string or list."""
        if isinstance(v, str):
            return [i.strip() for i in v.split(",")]
        return v

    @field_validator("EXPLANATION_METHODS", mode="before")
    @classmethod
    def assemble_explanation_methods(cls, v: Union[str, List[str]]) -> List[str]:
        """Parse explanation methods from string or list."""
        if isinstance(v, str):
            return [i.strip() for i in v.split(",")]
        return v

    @field_validator("COMPLIANCE_STANDARDS", mode="before")
    @classmethod
    def assemble_compliance_standards(cls, v: Union[str, List[str]]) -> List[str]:
        """Parse compliance standards from string or list."""
        if isinstance(v, str):
            return [i.strip() for i in v.split(",")]
        return v

    @field_validator("ETHICAL_ALERT_EMAILS", mode="before")
    @classmethod
    def assemble_ethical_alert_emails(cls, v: Union[str, List[str]]) -> List[str]:
        """Parse ethical alert emails from string or list."""
        if isinstance(v, str):
            return [i.strip() for i in v.split(",")]
        return v

    @field_validator("ENVIRONMENT")
    @classmethod
    def validate_environment(cls, v: str) -> str:
        """Validate environment value."""
        allowed = ["development", "staging", "production", "testing"]
        if v not in allowed:
            raise ValueError(f"ENVIRONMENT must be one of: {allowed}")
        return v

    @field_validator("LOG_LEVEL")
    @classmethod
    def validate_log_level(cls, v: str) -> str:
        """Validate log level."""
        allowed = ["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]
        if v.upper() not in allowed:
            raise ValueError(f"LOG_LEVEL must be one of: {allowed}")
        return v.upper()

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True

    @property
    def is_development(self) -> bool:
        """Check if running in development environment."""
        return self.ENVIRONMENT == "development"

    @property
    def is_production(self) -> bool:
        """Check if running in production environment."""
        return self.ENVIRONMENT == "production"

    @property
    def is_gpu_available(self) -> bool:
        """Check if GPU is enabled and available."""
        return self.GPU_ENABLED and os.environ.get("CUDA_VISIBLE_DEVICES") != ""

    def get_database_url(self, async_driver: bool = True) -> str:
        """Get database URL with appropriate driver."""
        if async_driver and not self.DATABASE_URL.startswith("postgresql+asyncpg://"):
            return self.DATABASE_URL.replace("postgresql://", "postgresql+asyncpg://")
        return self.DATABASE_URL

    def get_model_config(self, model_type: str) -> Dict[str, Any]:
        """Get configuration for specific model type."""
        configs = {
            "property_valuation": {
                "path": self.PROPERTY_VALUATION_MODEL_PATH,
                "timeout": self.DEFAULT_MODEL_TIMEOUT,
                "batch_size": self.MAX_BATCH_SIZE,
            },
            "image_recognition": {
                "path": self.IMAGE_RECOGNITION_MODEL_PATH,
                "timeout": self.DEFAULT_MODEL_TIMEOUT * 2,
                "batch_size": self.MAX_BATCH_SIZE // 2,
            },
            "nlp": {
                "path": self.NLP_MODEL_PATH,
                "timeout": self.DEFAULT_MODEL_TIMEOUT // 2,
                "batch_size": self.MAX_BATCH_SIZE * 2,
            },
            "chatbot": {
                "path": self.CHATBOT_MODEL_PATH,
                "timeout": self.DEFAULT_MODEL_TIMEOUT * 3,
                "batch_size": 1,  # Chatbot processes one request at a time
            },
        }
        return configs.get(model_type, {})

    @property
    def api_gateway(self) -> Dict[str, Any]:
        """Get API Gateway configuration."""
        return {
            "url": self.API_GATEWAY_URL,
            "timeout": self.API_GATEWAY_TIMEOUT,
            "retry_attempts": self.API_GATEWAY_RETRY_ATTEMPTS,
            "service_name": self.SERVICE_NAME,
            "service_id": self.SERVICE_ID,
            "service_version": self.SERVICE_VERSION,
            "service_host": self.SERVICE_HOST,
            "service_port": self.SERVICE_PORT,
            "api_key": self.GATEWAY_API_KEY,
            "jwt_algorithm": self.JWT_ALGORITHM,
            "token_issuer": self.TOKEN_ISSUER,
            "token_audience": self.TOKEN_AUDIENCE,
        }

    @property
    def service_registry(self) -> Dict[str, Any]:
        """Get service registry configuration."""
        return {
            "url": self.SERVICE_REGISTRY_URL,
            "heartbeat_interval": self.SERVICE_REGISTRY_HEARTBEAT_INTERVAL,
        }


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()