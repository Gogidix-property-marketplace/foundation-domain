"""
AI Gateway Middleware

Enterprise middleware for authentication, rate limiting,
correlation tracking, and request processing.
"""

import time
import uuid
from typing import Callable, Dict, Optional, Set

import aioredis
import jwt
from fastapi import FastAPI, Request, Response, HTTPException, status
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.types import ASGIApp

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger, correlation_id, request_id, user_id
from gogidix_ai.core.exceptions import (
    AuthenticationError,
    AuthorizationError,
    RateLimitError,
)

# Global logger
logger = get_logger(__name__)


class CorrelationMiddleware(BaseHTTPMiddleware):
    """Middleware for adding correlation IDs to requests."""

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """Add correlation ID and request ID to request context."""
        # Generate or get correlation ID
        correlation_id_value = request.headers.get("X-Correlation-ID") or str(uuid.uuid4())

        # Generate request ID
        request_id_value = str(uuid.uuid4())

        # Set context variables
        correlation_id.set(correlation_id_value)
        request_id.set(request_id_value)

        # Process request
        response = await call_next(request)

        # Add IDs to response headers
        response.headers["X-Correlation-ID"] = correlation_id_value
        response.headers["X-Request-ID"] = request_id_value

        return response


class AuthenticationMiddleware(BaseHTTPMiddleware):
    """Middleware for JWT authentication and authorization."""

    def __init__(self, app: ASGIApp):
        super().__init__(app)
        self.settings = get_settings()
        self.public_paths: Set[str] = {
            "/health",
            "/metrics",
            "/docs",
            "/openapi.json",
            "/redoc",
        }

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """Authenticate and authorize requests."""
        # Skip auth for public paths
        if request.url.path in self.public_paths:
            return await call_next(request)

        # Extract token
        auth_header = request.headers.get("Authorization")
        if not auth_header or not auth_header.startswith("Bearer "):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Missing or invalid authentication token",
                headers={"WWW-Authenticate": "Bearer"},
            )

        token = auth_header.split(" ")[1]

        try:
            # Decode JWT
            payload = jwt.decode(
                token,
                self.settings.SECRET_KEY,
                algorithms=[self.settings.ALGORITHM],
            )

            # Check expiration
            exp = payload.get("exp")
            if exp and time.time() > exp:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Token has expired",
                    headers={"WWW-Authenticate": "Bearer"},
                )

            # Extract user info
            user_id_value = payload.get("sub")
            if not user_id_value:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid token payload",
                    headers={"WWW-Authenticate": "Bearer"},
                )

            # Set user context
            user_id.set(user_id_value)
            request.state.user_id = user_id_value
            request.state.user_payload = payload

            logger.info(
                "User authenticated",
                user_id=user_id_value,
                path=request.url.path,
            )

        except jwt.InvalidTokenError as e:
            logger.warning(
                "Invalid token",
                error=str(e),
                path=request.url.path,
            )
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid authentication token",
                headers={"WWW-Authenticate": "Bearer"},
            )

        return await call_next(request)


class RateLimitMiddleware(BaseHTTPMiddleware):
    """Middleware for rate limiting using Redis."""

    def __init__(self, app: ASGIApp):
        super().__init__(app)
        self.settings = get_settings()
        self.redis: Optional[aioredis.Redis] = None
        self._initialized = False

    async def _ensure_redis(self):
        """Ensure Redis connection is initialized."""
        if not self._initialized:
            self.redis = aioredis.from_url(
                self.settings.REDIS_URL,
                max_connections=self.settings.REDIS_MAX_CONNECTIONS,
                retry_on_timeout=True,
            )
            self._initialized = True

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """Apply rate limiting to requests."""
        if not self.settings.RATE_LIMIT_ENABLED:
            return await call_next(request)

        await self._ensure_redis()

        # Get identifier for rate limiting
        identifier = getattr(request.state, "user_id", None)
        if not identifier:
            identifier = request.client.host

        # Create Redis keys
        window_key = f"rate_limit:{identifier}"
        request_key = f"rate_limit_requests:{identifier}"

        try:
            current_time = int(time.time())
            window_size = 60  # 1 minute window

            # Use Redis pipeline for atomic operations
            pipe = self.redis.pipeline()

            # Clean old requests
            pipe.zremrangebyscore(request_key, 0, current_time - window_size)

            # Count current requests
            pipe.zcard(request_key)

            # Add current request
            pipe.zadd(request_key, {str(uuid.uuid4()): current_time})

            # Set expiration
            pipe.expire(request_key, window_size)

            # Execute pipeline
            results = await pipe.execute()

            request_count = results[1]

            # Check rate limit
            if request_count > self.settings.RATE_LIMIT_REQUESTS_PER_MINUTE:
                logger.warning(
                    "Rate limit exceeded",
                    identifier=identifier,
                    request_count=request_count,
                    limit=self.settings.RATE_LIMIT_REQUESTS_PER_MINUTE,
                )

                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail="Rate limit exceeded",
                    headers={
                        "X-RateLimit-Limit": str(self.settings.RATE_LIMIT_REQUESTS_PER_MINUTE),
                        "X-RateLimit-Remaining": "0",
                        "X-RateLimit-Reset": str(current_time + window_size),
                    },
                )

            # Process request
            response = await call_next(request)

            # Add rate limit headers
            remaining = max(
                0,
                self.settings.RATE_LIMIT_REQUESTS_PER_MINUTE - request_count
            )

            response.headers["X-RateLimit-Limit"] = str(
                self.settings.RATE_LIMIT_REQUESTS_PER_MINUTE
            )
            response.headers["X-RateLimit-Remaining"] = str(remaining)
            response.headers["X-RateLimit-Reset"] = str(current_time + window_size)

            return response

        except aioredis.RedisError as e:
            logger.error(
                "Redis error in rate limiting",
                error=str(e),
                identifier=identifier,
            )
            # Fail open - allow request if Redis is down
            return await call_next(request)


class AIMiddleware(BaseHTTPMiddleware):
    """Middleware for AI request processing and monitoring."""

    def __init__(self, app: ASGIApp):
        super().__init__(app)
        self.start_time = time.time()
        self.request_count = 0
        self.error_count = 0

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """Process AI request with monitoring."""
        self.request_count += 1

        # Start timing
        start_time = time.time()

        # Log request start
        logger.info(
            "Request started",
            method=request.method,
            path=request.url.path,
            query_params=str(request.query_params),
            user_id=getattr(request.state, "user_id", None),
            correlation_id=correlation_id.get(),
            request_id=request_id.get(),
        )

        try:
            # Process request
            response = await call_next(request)

            # Calculate duration
            duration = time.time() - start_time

            # Log successful response
            logger.info(
                "Request completed",
                status_code=response.status_code,
                duration_seconds=duration,
                response_size=len(response.body) if hasattr(response, "body") else 0,
            )

            # Add performance headers
            response.headers["X-Response-Time"] = f"{duration:.3f}s"
            response.headers["X-Request-Count"] = str(self.request_count)

            return response

        except Exception as e:
            self.error_count += 1
            duration = time.time() - start_time

            # Log error
            logger.error(
                "Request failed",
                error=str(e),
                error_type=type(e).__name__,
                duration_seconds=duration,
                path=request.url.path,
                method=request.method,
            )

            # Re-raise exception
            raise

    @property
    def uptime(self) -> float:
        """Get service uptime in seconds."""
        return time.time() - self.start_time

    @property
    def error_rate(self) -> float:
        """Calculate error rate."""
        if self.request_count == 0:
            return 0.0
        return (self.error_count / self.request_count) * 100