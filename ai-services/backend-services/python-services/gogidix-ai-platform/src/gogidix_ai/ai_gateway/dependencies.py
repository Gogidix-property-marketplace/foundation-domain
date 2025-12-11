"""
FastAPI Dependencies

Common dependencies for API endpoints including
authentication, service injection, and validation.
"""

from functools import lru_cache
from typing import Optional, Dict, Any

import jwt
from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger

logger = get_logger(__name__)

# HTTP Bearer for authentication
security = HTTPBearer()


@lru_cache()
def get_ai_gateway():
    """Get AI Gateway service instance."""
    # This would typically be initialized at app startup
    # and stored in app.state for dependency injection
    # For now, we'll create a mock or import from app
    return None


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
) -> Optional[Dict[str, Any]]:
    """
    Get current authenticated user from JWT token.

    Args:
        credentials: HTTP Bearer credentials

    Returns:
        User information from JWT payload

    Raises:
        HTTPException: For invalid or expired tokens
    """
    settings = get_settings()

    try:
        # Decode JWT
        payload = jwt.decode(
            credentials.credentials,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM],
        )

        # Check expiration
        exp = payload.get("exp")
        if exp and payload["exp"] < jwt.decode(credentials.credentials, options={"verify_signature": False})["iat"]:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Token has expired",
                headers={"WWW-Authenticate": "Bearer"},
            )

        # Extract user information
        user_info = {
            "sub": payload.get("sub"),
            "email": payload.get("email"),
            "name": payload.get("name"),
            "is_admin": payload.get("is_admin", False),
            "permissions": payload.get("permissions", []),
            "iat": payload.get("iat"),
            "exp": payload.get("exp"),
        }

        return user_info

    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has expired",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.InvalidTokenError as e:
        logger.warning(
            "Invalid token",
            error=str(e),
        )
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except Exception as e:
        logger.error(
            "Authentication error",
            error=str(e),
        )
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication failed",
            headers={"WWW-Authenticate": "Bearer"},
        )


async def get_optional_current_user(
    request: Request,
) -> Optional[Dict[str, Any]]:
    """
    Get current user if authenticated, but don't require auth.

    Args:
        request: FastAPI request object

    Returns:
        User information if authenticated, None otherwise
    """
    try:
        # Check for Authorization header
        auth_header = request.headers.get("Authorization")
        if not auth_header or not auth_header.startswith("Bearer "):
            return None

        # Create credentials object
        credentials = HTTPAuthorizationCredentials(
            scheme="Bearer",
            credentials=auth_header.split(" ")[1],
        )

        # Get user
        return await get_current_user(credentials)

    except Exception:
        # Return None for any authentication errors
        return None


def require_permission(permission: str):
    """
    Decorator to require specific permission.

    Args:
        permission: Required permission string

    Returns:
        Dependency function
    """
    async def permission_dependency(
        current_user: Dict[str, Any] = Depends(get_current_user),
    ) -> Dict[str, Any]:
        """Check if user has required permission."""
        permissions = current_user.get("permissions", [])

        if permission not in permissions and not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Permission '{permission}' required",
            )

        return current_user

    return permission_dependency


def require_admin():
    """Dependency to require administrator privileges."""
    return require_permission("admin")


async def validate_model_access(
    model_name: str,
    current_user: Dict[str, Any] = Depends(get_current_user),
) -> Dict[str, Any]:
    """
    Validate user has access to specified model.

    Args:
        model_name: Name of the model
        current_user: Authenticated user

    Returns:
        User information if access is granted

    Raises:
        HTTPException: If access is denied
    """
    # Check if user is admin (full access)
    if current_user.get("is_admin", False):
        return current_user

    # Check specific model permissions
    permissions = current_user.get("permissions", [])
    model_permission = f"model:{model_name}:access"

    if model_permission not in permissions:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Access denied for model {model_name}",
        )

    return current_user


async def rate_limit_check(
    request: Request,
    current_user: Optional[Dict[str, Any]] = Depends(get_optional_current_user),
):
    """
    Check rate limits for the request.

    Args:
        request: FastAPI request object
        current_user: Authenticated user (optional)

    Returns:
        None if rate limit is OK

    Raises:
        HTTPException: If rate limit is exceeded
    """
    settings = get_settings()

    if not settings.RATE_LIMIT_ENABLED:
        return

    # Get identifier for rate limiting
    if current_user:
        identifier = current_user.get("sub")
    else:
        identifier = request.client.host

    # This would typically use Redis for distributed rate limiting
    # For now, we'll implement a simple in-memory check
    import time
    from collections import defaultdict

    # Store rate limit data (this should be in Redis in production)
    if not hasattr(rate_limit_check, "rate_data"):
        rate_limit_check.rate_data = defaultdict(list)

    now = time.time()
    minute_ago = now - 60

    # Clean old entries
    rate_limit_check.rate_data[identifier] = [
        timestamp for timestamp in rate_limit_check.rate_data[identifier]
        if timestamp > minute_ago
    ]

    # Check rate limit
    request_count = len(rate_limit_check.rate_data[identifier])
    if request_count >= settings.RATE_LIMIT_REQUESTS_PER_MINUTE:
        logger.warning(
            "Rate limit exceeded",
            identifier=identifier,
            request_count=request_count,
            limit=settings.RATE_LIMIT_REQUESTS_PER_MINUTE,
        )

        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Rate limit exceeded",
            headers={
                "X-RateLimit-Limit": str(settings.RATE_LIMIT_REQUESTS_PER_MINUTE),
                "X-RateLimit-Remaining": "0",
                "X-RateLimit-Reset": str(int(now + 60)),
            },
        )

    # Record this request
    rate_limit_check.rate_data[identifier].append(now)


async def validate_content_type(
    request: Request,
    allowed_types: list = ["application/json"],
) -> None:
    """
    Validate request content type.

    Args:
        request: FastAPI request object
        allowed_types: List of allowed content types

    Raises:
        HTTPException: If content type is not allowed
    """
    content_type = request.headers.get("content-type", "").split(";")[0]

    if content_type not in allowed_types:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail=f"Content type {content_type} not supported",
        )


async def get_request_context(
    request: Request,
    current_user: Optional[Dict[str, Any]] = Depends(get_optional_current_user),
) -> Dict[str, Any]:
    """
    Get request context for logging and tracking.

    Args:
        request: FastAPI request object
        current_user: Authenticated user (optional)

    Returns:
        Request context dictionary
    """
    from gogidix_ai.core.logging import correlation_id, request_id

    return {
        "request_id": request_id.get(),
        "correlation_id": correlation_id.get(),
        "user_id": current_user.get("sub") if current_user else None,
        "method": request.method,
        "path": request.url.path,
        "query_params": str(request.query_params),
        "client_ip": request.client.host,
        "user_agent": request.headers.get("user-agent"),
    }