"""
API Gateway Integration

Components for seamless integration with the API Gateway including:
- Authentication and authorization
- Service discovery and registration
- Rate limiting and circuit breaking
- Request routing and middleware
"""

__version__ = "1.0.0"
__author__ = "AI Services Team"
__email__ = "ai-team@gogidix.com"

from .auth import (
    APIGatewayClient,
    APIGatewayAuthenticator,
    ServiceAccount,
    TokenInfo,
    authenticator,
    get_current_user,
    get_current_user_info,
    require_read_permission,
    require_write_permission,
    require_admin_permission,
    require_ml_permission,
    require_api_permission,
    generate_service_token,
    cleanup_auth
)

from .middleware import (
    RequestIDMiddleware,
    ServiceRegistryMiddleware,
    CircuitBreakerMiddleware,
    RateLimitMiddleware,
    AuthenticationMiddleware,
    RequestLoggingMiddleware,
    CacheMiddleware,
    MetricsMiddleware,
    create_gateway_middleware,
    create_lightweight_middleware,
    startup_gateway_middleware,
    shutdown_gateway_middleware
)

from .config import (
    GatewaySettings,
    GatewayConfig,
    ServiceRoute,
    ServiceDiscoveryConfig,
    get_gateway_config,
    get_gateway_settings
)

__all__ = [
    # Authentication
    "APIGatewayClient",
    "APIGatewayAuthenticator",
    "ServiceAccount",
    "TokenInfo",
    "authenticator",
    "get_current_user",
    "get_current_user_info",
    "require_read_permission",
    "require_write_permission",
    "require_admin_permission",
    "require_ml_permission",
    "require_api_permission",
    "generate_service_token",
    "cleanup_auth",

    # Middleware
    "RequestIDMiddleware",
    "ServiceRegistryMiddleware",
    "CircuitBreakerMiddleware",
    "RateLimitMiddleware",
    "AuthenticationMiddleware",
    "RequestLoggingMiddleware",
    "CacheMiddleware",
    "MetricsMiddleware",
    "create_gateway_middleware",
    "create_lightweight_middleware",
    "startup_gateway_middleware",
    "shutdown_gateway_middleware",

    # Configuration
    "GatewaySettings",
    "GatewayConfig",
    "ServiceRoute",
    "ServiceDiscoveryConfig",
    "get_gateway_config",
    "get_gateway_settings",
]