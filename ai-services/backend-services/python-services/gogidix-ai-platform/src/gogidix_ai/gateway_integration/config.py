"""
Gateway Integration Configuration

Configuration settings and utilities for API Gateway integration
including service discovery, authentication, and routing.
"""

import os
from functools import lru_cache
from typing import Any, Dict, List, Optional, Union

from pydantic import BaseSettings, Field, field_validator
from pydantic_settings import BaseSettings as PydanticBaseSettings
from pydantic import SecretStr

from ..core.config import get_settings


class GatewaySettings(BaseSettings):
    """Configuration for API Gateway integration."""

    # Gateway Connection
    GATEWAY_URL: str = Field(
        default="https://api.gogidix.com",
        env="GATEWAY_URL"
    )
    GATEWAY_TIMEOUT: int = Field(default=30, env="GATEWAY_TIMEOUT")
    GATEWAY_RETRY_ATTEMPTS: int = Field(default=3, env="GATEWAY_RETRY_ATTEMPTS")

    # Service Identity
    SERVICE_NAME: str = Field(..., env="SERVICE_NAME")
    SERVICE_ID: str = Field(..., env="SERVICE_ID")
    SERVICE_VERSION: str = Field(default="1.0.0", env="SERVICE_VERSION")
    SERVICE_HOST: str = Field(default="localhost", env="SERVICE_HOST")
    SERVICE_PORT: int = Field(default=8000, env="SERVICE_PORT")

    # Authentication
    API_KEY: SecretStr = Field(..., env="API_KEY")
    JWT_SECRET: Optional[str] = Field(None, env="JWT_SECRET")
    JWT_ALGORITHM: str = Field(default="RS256", env="JWT_ALGORITHM")
    TOKEN_ISSUER: str = Field(default="https://auth.gogidix.com", env="TOKEN_ISSUER")
    TOKEN_AUDIENCE: str = Field(default="gogidix-api", env="TOKEN_AUDIENCE")

    # Service Registry
    REGISTRY_URL: str = Field(
        default="https://registry.gogidix.com",
        env="REGISTRY_URL"
    )
    REGISTRY_HEARTBEAT_INTERVAL: int = Field(default=30, env="REGISTRY_HEARTBEAT_INTERVAL")
    REGISTRY_DEREGISTRATION_DELAY: int = Field(default=30, env="REGISTRY_DEREGISTRATION_DELAY")

    # Rate Limiting
    RATE_LIMIT_ENABLED: bool = Field(default=True, env="RATE_LIMIT_ENABLED")
    RATE_LIMIT_DEFAULT: int = Field(default=1000, env="RATE_LIMIT_DEFAULT")
    RATE_LIMIT_WINDOW: int = Field(default=3600, env="RATE_LIMIT_WINDOW")
    RATE_LIMIT_BURST: int = Field(default=100, env="RATE_LIMIT_BURST")

    # Circuit Breaker
    CIRCUIT_BREAKER_ENABLED: bool = Field(default=True, env="CIRCUIT_BREAKER_ENABLED")
    CIRCUIT_BREAKER_THRESHOLD: int = Field(default=5, env="CIRCUIT_BREAKER_THRESHOLD")
    CIRCUIT_BREAKER_TIMEOUT: int = Field(default=60, env="CIRCUIT_BREAKER_TIMEOUT")

    # Caching
    CACHE_ENABLED: bool = Field(default=True, env="CACHE_ENABLED")
    CACHE_TTL: int = Field(default=300, env="CACHE_TTL")
    CACHE_MAX_SIZE: int = Field(default=1000, env="CACHE_MAX_SIZE")

    # Observability
    METRICS_ENABLED: bool = Field(default=True, env="METRICS_ENABLED")
    TRACING_ENABLED: bool = Field(default=True, env="TRACING_ENABLED")
    LOG_REQUESTS: bool = Field(default=True, env="LOG_REQUESTS")

    # Security Headers
    SECURITY_HEADERS_ENABLED: bool = Field(default=True, env="SECURITY_HEADERS_ENABLED")
    CORS_ENABLED: bool = Field(default=True, env="CORS_ENABLED")

    # Service Mesh
    SERVICE_MESH_ENABLED: bool = Field(default=False, env="SERVICE_MESH_ENABLED")
    ISTIO_ENABLED: bool = Field(default=False, env="ISTIO_ENABLED")

    @field_validator("GATEWAY_URL")
    @classmethod
    def validate_gateway_url(cls, v: str) -> str:
        """Validate gateway URL format."""
        if not v.startswith(("http://", "https://")):
            raise ValueError("GATEWAY_URL must start with http:// or https://")
        return v.rstrip("/")

    @field_validator("SERVICE_PORT")
    @classmethod
    def validate_service_port(cls, v: int) -> int:
        """Validate service port."""
        if not 1 <= v <= 65535:
            raise ValueError("SERVICE_PORT must be between 1 and 65535")
        return v

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True


class ServiceRoute:
    """Service route configuration for API Gateway."""

    def __init__(
        self,
        path: str,
        service_name: str,
        methods: List[str],
        auth_required: bool = True,
        permissions: Optional[List[str]] = None,
        rate_limit: Optional[int] = None,
        cache_ttl: Optional[int] = None
    ):
        self.path = path
        self.service_name = service_name
        self.methods = methods
        self.auth_required = auth_required
        self.permissions = permissions or []
        self.rate_limit = rate_limit
        self.cache_ttl = cache_ttl

    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary representation."""
        return {
            "path": self.path,
            "service": self.service_name,
            "methods": self.methods,
            "auth_required": self.auth_required,
            "permissions": self.permissions,
            "rate_limit": self.rate_limit,
            "cache_ttl": self.cache_ttl
        }


class ServiceDiscoveryConfig:
    """Service discovery configuration."""

    def __init__(self):
        self.services: Dict[str, Dict[str, Any]] = {}
        self.routes: List[ServiceRoute] = []
        self.load_balancer = "round_robin"  # round_robin, least_connections, random

    def register_service(
        self,
        name: str,
        url: str,
        health_check: str,
        weight: int = 1,
        tags: Optional[List[str]] = None
    ):
        """Register a service."""
        self.services[name] = {
            "url": url,
            "health_check": health_check,
            "weight": weight,
            "tags": tags or [],
            "instances": [],
            "last_health_check": None
        }

    def register_route(
        self,
        path: str,
        service_name: str,
        methods: List[str],
        **kwargs
    ):
        """Register a route."""
        route = ServiceRoute(
            path=path,
            service_name=service_name,
            methods=methods,
            **kwargs
        )
        self.routes.append(route)

    def get_service_url(self, service_name: str) -> Optional[str]:
        """Get service URL based on load balancing."""
        if service_name not in self.services:
            return None

        service = self.services[service_name]
        if not service["instances"]:
            return service["url"]

        # Simple round-robin load balancing
        instance = service["instances"][0]
        service["instances"] = service["instances"][1:] + [instance]

        return instance["url"]


class GatewayConfig:
    """Main gateway configuration manager."""

    def __init__(self):
        self.settings = GatewaySettings()
        self.service_discovery = ServiceDiscoveryConfig()
        self._configure_default_services()

    def _configure_default_services(self):
        """Configure default AI services."""
        # Property Intelligence Service
        self.service_discovery.register_service(
            name="property-intelligence",
            url="http://property-intelligence:8000",
            health_check="/health",
            tags=["ai", "property", "ml"]
        )

        # Conversational AI Service
        self.service_discovery.register_service(
            name="conversational-ai",
            url="http://conversational-ai:8000",
            health_check="/health",
            tags=["ai", "nlp", "chatbot"]
        )

        # Analytics Service
        self.service_discovery.register_service(
            name="analytics",
            url="http://analytics:8000",
            health_check="/health",
            tags=["analytics", "reporting"]
        )

        # ML Platform Service
        self.service_discovery.register_service(
            name="ml-platform",
            url="http://ml-platform:8000",
            health_check="/health",
            tags=["ml", "training", "deployment"]
        )

        # Ethical AI Service
        self.service_discovery.register_service(
            name="ethical-ai",
            url="http://ethical-ai:8000",
            health_check="/health",
            tags=["ai", "ethics", "compliance"]
        )

        # Register routes
        self._register_default_routes()

    def _register_default_routes(self):
        """Register default API routes."""
        # Property Intelligence routes
        self.service_discovery.register_route(
            path="/api/v1/property-valuation",
            service_name="property-intelligence",
            methods=["POST"],
            auth_required=True,
            permissions=["property:valuation"],
            rate_limit=100
        )

        self.service_discovery.register_route(
            path="/api/v1/property-analysis",
            service_name="property-intelligence",
            methods=["POST"],
            auth_required=True,
            permissions=["property:analyze"],
            rate_limit=50
        )

        # Conversational AI routes
        self.service_discovery.register_route(
            path="/api/v1/chat",
            service_name="conversational-ai",
            methods=["POST"],
            auth_required=True,
            rate_limit=1000
        )

        self.service_discovery.register_route(
            path="/api/v1/chat/stream",
            service_name="conversational-ai",
            methods=["POST"],
            auth_required=True,
            rate_limit=100
        )

        # Analytics routes
        self.service_discovery.register_route(
            path="/api/v1/analytics",
            service_name="analytics",
            methods=["GET", "POST"],
            auth_required=True,
            permissions=["analytics:read", "analytics:write"],
            cache_ttl=300
        )

        # ML Platform routes
        self.service_discovery.register_route(
            path="/api/v1/ml/train",
            service_name="ml-platform",
            methods=["POST"],
            auth_required=True,
            permissions=["ml:train"]
        )

        self.service_discovery.register_route(
            path="/api/v1/ml/deploy",
            service_name="ml-platform",
            methods=["POST"],
            auth_required=True,
            permissions=["ml:deploy"]
        )

        # Ethical AI routes
        self.service_discovery.register_route(
            path="/api/v1/ethical-ai/assessment",
            service_name="ethical-ai",
            methods=["POST"],
            auth_required=True,
            permissions=["ethical:assess"]
        )

        self.service_discovery.register_route(
            path="/api/v1/ethical-ai/bias-detection",
            service_name="ethical-ai",
            methods=["POST"],
            auth_required=True,
            permissions=["ethical:bias"]
        )

    def get_cors_config(self) -> Dict[str, Any]:
        """Get CORS configuration."""
        return {
            "allow_origins": [
                "https://app.gogidix.com",
                "https://admin.gogidix.com",
                "https://api.gogidix.com"
            ],
            "allow_credentials": True,
            "allow_methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
            "allow_headers": [
                "Authorization",
                "Content-Type",
                "X-Request-ID",
                "X-Tenant-ID",
                "X-API-Version"
            ]
        }

    def get_security_headers(self) -> Dict[str, str]:
        """Get security headers configuration."""
        return {
            "X-Content-Type-Options": "nosniff",
            "X-Frame-Options": "DENY",
            "X-XSS-Protection": "1; mode=block",
            "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
            "Content-Security-Policy": (
                "default-src 'self'; "
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; "
                "style-src 'self' 'unsafe-inline'; "
                "img-src 'self' data: https:; "
                "connect-src 'self' https://api.gogidix.com"
            ),
            "Referrer-Policy": "strict-origin-when-cross-origin",
            "Permissions-Policy": (
                "geolocation=(), microphone=(), camera=(), "
                "payment=(), usb=(), magnetometer=(), gyroscope=()"
            )
        }

    def get_rate_limit_config(self) -> Dict[str, Any]:
        """Get rate limiting configuration."""
        return {
            "enabled": self.settings.RATE_LIMIT_ENABLED,
            "default_limit": self.settings.RATE_LIMIT_DEFAULT,
            "window_seconds": self.settings.RATE_LIMIT_WINDOW,
            "burst": self.settings.RATE_LIMIT_BURST,
            "per_user": True,
            "per_ip": True
        }

    def get_circuit_breaker_config(self) -> Dict[str, Any]:
        """Get circuit breaker configuration."""
        return {
            "enabled": self.settings.CIRCUIT_BREAKER_ENABLED,
            "failure_threshold": self.settings.CIRCUIT_BREAKER_THRESHOLD,
            "recovery_timeout": self.settings.CIRCUIT_BREAKER_TIMEOUT,
            "monitoring_period": 60
        }

    def get_cache_config(self) -> Dict[str, Any]:
        """Get caching configuration."""
        return {
            "enabled": self.settings.CACHE_ENABLED,
            "default_ttl": self.settings.CACHE_TTL,
            "max_size": self.settings.CACHE_MAX_SIZE,
            "cacheable_methods": ["GET"],
            "cacheable_status_codes": [200, 301, 302]
        }

    def get_metrics_config(self) -> Dict[str, Any]:
        """Get metrics configuration."""
        return {
            "enabled": self.settings.METRICS_ENABLED,
            "endpoint": "/metrics",
            "include_labels": True,
            "track_request_size": True,
            "track_response_size": True
        }


# Global configuration instance
gateway_config = GatewayConfig()


@lru_cache()
def get_gateway_config() -> GatewayConfig:
    """Get gateway configuration instance."""
    return gateway_config


def get_gateway_settings() -> GatewaySettings:
    """Get gateway settings."""
    return gateway_config.settings