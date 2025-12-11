"""
API Gateway Authentication Integration

Handles authentication, authorization, and secure communication
between AI services and the API Gateway following enterprise patterns.
"""

import asyncio
import json
import time
from datetime import datetime, timedelta
from typing import Any, Dict, List, Optional, Union
from urllib.parse import urljoin

import jwt
import httpx
from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field, field_validator

from ..core.config import get_settings
from ..core.logging import get_logger
from ..core.exceptions import AuthenticationError, AuthorizationError

logger = get_logger(__name__)
settings = get_settings()

# Security scheme
security = HTTPBearer(auto_error=False)


class ServiceAccount(BaseModel):
    """Service account credentials for service-to-service communication."""

    service_name: str = Field(..., description="Name of the service")
    service_id: str = Field(..., description="Unique service identifier")
    api_key: str = Field(..., description="API key for authentication")
    scopes: List[str] = Field(default=[], description="Allowed scopes")
    permissions: List[str] = Field(default=[], description="Granted permissions")
    expires_at: Optional[datetime] = Field(None, description="API key expiration")
    is_active: bool = Field(default=True, description="Whether service account is active")

    @field_validator('api_key')
    @classmethod
    def validate_api_key(cls, v):
        if len(v) < 32:
            raise ValueError("API key must be at least 32 characters")
        return v


class JWKSResponse(BaseModel):
    """JWKS (JSON Web Key Set) response."""

    keys: List[Dict[str, Any]]


class TokenInfo(BaseModel):
    """Decoded JWT token information."""

    sub: str = Field(..., description="Subject (user/service ID)")
    iss: str = Field(..., description="Issuer")
    aud: List[str] = Field(..., description="Audience")
    exp: int = Field(..., description="Expiration time")
    iat: int = Field(..., description="Issued at time")
    jti: str = Field(..., description="JWT ID")
    scope: Optional[str] = Field(None, description="Access scope")
    permissions: List[str] = Field(default=[], description="User permissions")
    tenant_id: Optional[str] = Field(None, description="Tenant identifier")
    user_type: str = Field(default="user", description="User type (user/service)")


class APIGatewayClient:
    """Client for API Gateway communication and authentication."""

    def __init__(self):
        gateway_config = settings.api_gateway
        self.gateway_url = gateway_config["url"]
        self.service_name = gateway_config["service_name"]
        self.service_id = gateway_config["service_id"]
        self.api_key = gateway_config["api_key"].get_secret_value()
        self.jwks_cache: Dict[str, Any] = {}
        self.jwks_cache_time: float = 0
        self.jwks_cache_ttl: int = 3600  # 1 hour
        self._http_client: Optional[httpx.AsyncClient] = None

    @property
    def http_client(self) -> httpx.AsyncClient:
        """Lazy initialization of HTTP client."""
        if self._http_client is None or self._http_client.is_closed:
            self._http_client = httpx.AsyncClient(
                base_url=self.gateway_url,
                timeout=30.0,
                headers={
                    "X-Service-Name": self.service_name,
                    "X-Service-ID": self.service_id,
                    "User-Agent": f"{self.service_name}/1.0"
                }
            )
        return self._http_client

    async def get_jwks(self) -> JWKSResponse:
        """Get JSON Web Key Set from API Gateway for token validation."""
        now = time.time()

        # Check cache
        if self.jwks_cache and (now - self.jwks_cache_time) < self.jwks_cache_ttl:
            return JWKSResponse(keys=self.jwks_cache["keys"])

        try:
            response = await self.http_client.get("/auth/.well-known/jwks.json")
            response.raise_for_status()

            jwks_data = response.json()
            self.jwks_cache = jwks_data
            self.jwks_cache_time = now

            return JWKSResponse(**jwks_data)

        except httpx.HTTPError as e:
            logger.error(f"Failed to fetch JWKS from API Gateway: {e}")
            raise AuthenticationError("Unable to fetch authentication keys")

    async def validate_token(self, token: str) -> TokenInfo:
        """Validate JWT token with API Gateway public keys."""
        try:
            # Get JWKS
            jwks = await self.get_jwks()

            # Decode token without verification first to get key ID
            unverified_header = jwt.get_unverified_header(token)

            # Find matching key
            key = None
            for jwk in jwks.keys:
                if jwk["kid"] == unverified_header["kid"]:
                    key = jwt.algorithms.RSAAlgorithm.from_jwk(json.dumps(jwk))
                    break

            if not key:
                raise AuthenticationError("Invalid token signature")

            # Decode and verify token
            payload = jwt.decode(
                token,
                key,
                algorithms=["RS256"],
                audience=settings.api_gateway["token_audience"],
                issuer=settings.api_gateway["token_issuer"]
            )

            # Check token expiration
            if payload.get("exp", 0) < time.time():
                raise AuthenticationError("Token has expired")

            return TokenInfo(**payload)

        except jwt.PyJWTError as e:
            logger.error(f"Token validation failed: {e}")
            raise AuthenticationError("Invalid authentication token")

    async def introspect_token(self, token: str) -> Dict[str, Any]:
        """Introspect token with API Gateway (for additional validation)."""
        try:
            response = await self.http_client.post(
                "/auth/introspect",
                data={"token": token}
            )
            response.raise_for_status()
            return response.json()

        except httpx.HTTPError as e:
            logger.error(f"Token introspection failed: {e}")
            return {"active": False}

    async def exchange_service_token(self) -> str:
        """Exchange service credentials for access token."""
        try:
            response = await self.http_client.post(
                "/auth/token/exchange",
                data={
                    "grant_type": "client_credentials",
                    "client_id": self.service_id,
                    "client_secret": self.api_key,
                    "scope": "api:access"
                }
            )
            response.raise_for_status()

            token_data = response.json()
            return token_data["access_token"]

        except httpx.HTTPError as e:
            logger.error(f"Service token exchange failed: {e}")
            raise AuthenticationError("Unable to obtain service access token")

    async def register_service_health(self, status: str = "healthy"):
        """Register service health with API Gateway."""
        try:
            await self.http_client.post(
                "/services/health",
                json={
                    "service_id": self.service_id,
                    "service_name": self.service_name,
                    "status": status,
                    "timestamp": datetime.utcnow().isoformat()
                }
            )
        except httpx.HTTPError as e:
            logger.warning(f"Failed to register health status: {e}")

    async def close(self):
        """Close HTTP client."""
        if self._http_client and not self._http_client.is_closed:
            await self._http_client.aclose()


class APIGatewayAuthenticator:
    """Authentication middleware for API Gateway integration."""

    def __init__(self):
        self.client = APIGatewayClient()
        self.service_cache: Dict[str, ServiceAccount] = {}
        self._init_time = time.time()

    async def authenticate_request(
        self,
        request: Request,
        credentials: Optional[HTTPAuthorizationCredentials] = None
    ) -> TokenInfo:
        """Authenticate incoming request via API Gateway."""

        # Check for service-to-service authentication
        service_token = request.headers.get("X-Service-Token")
        if service_token:
            return await self._authenticate_service(service_token)

        # Check for user JWT token
        if credentials and credentials.scheme == "Bearer":
            return await self.client.validate_token(credentials.credentials)

        # Check API key authentication
        api_key = request.headers.get("X-API-Key")
        if api_key:
            return await self._authenticate_api_key(api_key)

        raise AuthenticationError("Missing authentication credentials")

    async def _authenticate_service(self, token: str) -> TokenInfo:
        """Authenticate service-to-service request."""
        try:
            # Validate service token
            payload = jwt.decode(
                token,
                self.client.api_key,
                algorithms=["HS256"]
            )

            # Check if service is active
            service_name = payload.get("service_name")
            if not service_name:
                raise AuthenticationError("Invalid service token")

            # Verify service is registered
            if not await self._is_service_authorized(service_name):
                raise AuthorizationError(f"Service {service_name} is not authorized")

            return TokenInfo(
                sub=payload["service_id"],
                iss=self.client.gateway_url,
                aud=["api-gateway"],
                exp=payload["exp"],
                iat=payload["iat"],
                jti=payload.get("jti", ""),
                scope="service:access",
                permissions=payload.get("permissions", []),
                user_type="service"
            )

        except jwt.PyJWTError as e:
            logger.error(f"Service token validation failed: {e}")
            raise AuthenticationError("Invalid service token")

    async def _authenticate_api_key(self, api_key: str) -> TokenInfo:
        """Authenticate request using API key."""
        # In production, validate against database or API Gateway
        if api_key == self.client.api_key:
            return TokenInfo(
                sub=self.client.service_id,
                iss=self.client.gateway_url,
                aud=["api-gateway"],
                exp=int(time.time()) + 3600,
                iat=int(time.time()),
                jti="api-key-" + str(int(time.time())),
                scope="api:read api:write",
                permissions=["read", "write"],
                user_type="service"
            )

        raise AuthenticationError("Invalid API key")

    async def _is_service_authorized(self, service_name: str) -> bool:
        """Check if service is authorized to communicate."""
        # List of authorized services
        authorized_services = [
            "ai-gateway",
            "property-intelligence",
            "conversational-ai",
            "analytics",
            "ml-platform",
            "ethical-ai",
            "api-gateway"
        ]
        return service_name in authorized_services

    def require_permissions(self, required_permissions: List[str]):
        """Create dependency that requires specific permissions."""
        async def permission_checker(
            request: Request,
            credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)
        ) -> TokenInfo:
            token_info = await self.authenticate_request(request, credentials)

            # Check if user has all required permissions
            user_permissions = set(token_info.permissions)
            required = set(required_permissions)

            if not required.issubset(user_permissions):
                missing = required - user_permissions
                raise AuthorizationError(
                    f"Missing required permissions: {', '.join(missing)}"
                )

            return token_info

        return permission_checker

    def require_scope(self, required_scope: str):
        """Create dependency that requires specific scope."""
        async def scope_checker(
            request: Request,
            credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)
        ) -> TokenInfo:
            token_info = await self.authenticate_request(request, credentials)

            if not token_info.scope or required_scope not in token_info.scope:
                raise AuthorizationError(f"Required scope '{required_scope}' not found")

            return token_info

        return scope_checker

    def require_tenant(self, tenant_id: Optional[str] = None):
        """Create dependency that requires tenant access."""
        async def tenant_checker(
            request: Request,
            credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)
        ) -> TokenInfo:
            token_info = await self.authenticate_request(request, credentials)

            # If tenant_id is specified, check if token has access
            if tenant_id:
                if not token_info.tenant_id or token_info.tenant_id != tenant_id:
                    raise AuthorizationError(
                        f"Access denied to tenant {tenant_id}"
                    )

            return token_info

        return tenant_checker

    async def __call__(
        self,
        request: Request,
        credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)
    ) -> TokenInfo:
        """Main authentication callable for FastAPI dependency injection."""
        return await self.authenticate_request(request, credentials)


# Global authenticator instance
authenticator = APIGatewayAuthenticator()


# Common authentication dependencies
async def get_current_user(
    request: Request,
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)
) -> TokenInfo:
    """Get current authenticated user/service."""
    return await authenticator.authenticate_request(request, credentials)


async def get_current_user_info(
    token_info: TokenInfo = Depends(get_current_user)
) -> Dict[str, Any]:
    """Get current user information in dictionary format."""
    return {
        "id": token_info.sub,
        "type": token_info.user_type,
        "tenant_id": token_info.tenant_id,
        "permissions": token_info.permissions,
        "scope": token_info.scope
    }


# Specific permission dependencies
require_read_permission = authenticator.require_permissions(["read"])
require_write_permission = authenticator.require_permissions(["write"])
require_admin_permission = authenticator.require_permissions(["admin"])
require_ml_permission = authenticator.require_permissions(["ml:train", "ml:deploy"])
require_api_permission = authenticator.require_scope("api:access")


# Rate limiting dependencies
class RateLimiter:
    """Rate limiting for API requests."""

    def __init__(self, redis_client):
        self.redis = redis_client

    async def check_rate_limit(
        self,
        key: str,
        limit: int,
        window: int
    ) -> bool:
        """Check if request is within rate limit."""
        current_time = int(time.time())
        window_start = current_time - window

        # Remove old entries
        await self.redis.zremrangebyscore(
            key,
            0,
            window_start
        )

        # Count current requests
        count = await self.redis.zcard(key)

        if count >= limit:
            return False

        # Add current request
        await self.redis.zadd(key, {str(current_time): current_time})
        await self.redis.expire(key, window)

        return True


# Service token generation
def generate_service_token(
    service_name: str,
    service_id: str,
    permissions: List[str],
    expires_in: int = 3600
) -> str:
    """Generate service-to-service authentication token."""
    now = int(time.time())
    payload = {
        "service_name": service_name,
        "service_id": service_id,
        "permissions": permissions,
        "iat": now,
        "exp": now + expires_in,
        "jti": f"svc-{now}-{service_id}"
    }

    return jwt.encode(
        payload,
        settings.api_gateway["api_key"].get_secret_value(),
        algorithm="HS256"
    )


# Decorator for endpoint protection
def require_auth(
    permissions: Optional[List[str]] = None,
    scope: Optional[str] = None,
    tenant_id: Optional[str] = None
):
    """Decorator to protect endpoints with authentication."""
    def decorator(func):
        dependency = Depends(get_current_user)

        if permissions:
            dependency = Depends(authenticator.require_permissions(permissions))
        elif scope:
            dependency = Depends(authenticator.require_scope(scope))
        elif tenant_id:
            dependency = Depends(authenticator.require_tenant(tenant_id))

        func.__requires_auth__ = dependency
        return func

    return decorator


# Request tracing
async def trace_request(request: Request, token_info: TokenInfo) -> Dict[str, Any]:
    """Add tracing information to request context."""
    trace_data = {
        "request_id": request.headers.get("X-Request-ID", ""),
        "user_id": token_info.sub,
        "user_type": token_info.user_type,
        "tenant_id": token_info.tenant_id,
        "service": token_info.iss,
        "timestamp": datetime.utcnow().isoformat(),
        "path": request.url.path,
        "method": request.method
    }

    # Log request for audit trail
    logger.info(
        "Request traced",
        extra={
            "trace": trace_data,
            "user_agent": request.headers.get("User-Agent"),
            "ip": request.client.host if request.client else None
        }
    )

    return trace_data


# Cleanup on shutdown
async def cleanup_auth():
    """Cleanup authentication resources."""
    await authenticator.client.close()