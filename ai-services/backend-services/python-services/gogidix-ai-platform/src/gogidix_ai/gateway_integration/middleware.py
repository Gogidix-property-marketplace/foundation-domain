"""
API Gateway Middleware Integration

Middleware components for seamless integration with the API Gateway
including request routing, rate limiting, and service discovery.
"""

import asyncio
import json
import time
from typing import Any, Dict, List, Optional, Callable, Awaitable
from uuid import uuid4

import httpx
from fastapi import Request, Response, HTTPException, status
from fastapi.middleware import Middleware
from fastapi.middleware.base import BaseHTTPMiddleware
from starlette.middleware.base import RequestResponseEndpoint

from ..core.config import get_settings
from ..core.logging import get_logger
from ..core.exceptions import RateLimitError, ServiceUnavailableError
from .auth import authenticator, RateLimiter, trace_request

logger = get_logger(__name__)
settings = get_settings()


class RequestIDMiddleware(BaseHTTPMiddleware):
    """Add unique request ID to all incoming requests."""

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint
    ) -> Response:
        # Generate or extract request ID
        request_id = request.headers.get("X-Request-ID")
        if not request_id:
            request_id = str(uuid4())

        # Add request ID to response headers
        response = await call_next(request)
        response.headers["X-Request-ID"] = request_id

        # Store request ID in request state for later use
        request.state.request_id = request_id

        return response


class ServiceRegistryMiddleware(BaseHTTPMiddleware):
    """Service discovery and registration middleware."""

    def __init__(self, app):
        super().__init__(app)
        self.registry_url = settings.service_registry.url
        self.service_info = {
            "name": settings.api_gateway.service_name,
            "id": settings.api_gateway.service_id,
            "version": settings.APP_VERSION,
            "host": settings.api_gateway.host,
            "port": settings.api_gateway.port,
            "health_check": f"{settings.api_gateway.host}:{settings.api_gateway.port}/health"
        }
        self._registered = False
        self._heartbeat_task: Optional[asyncio.Task] = None

    async def startup(self):
        """Register service and start heartbeat."""
        await self._register_service()
        self._heartbeat_task = asyncio.create_task(self._heartbeat_loop())

    async def shutdown(self):
        """Unregister service and stop heartbeat."""
        if self._heartbeat_task:
            self._heartbeat_task.cancel()
            try:
                await self._heartbeat_task
            except asyncio.CancelledError:
                pass
        await self._unregister_service()

    async def _register_service(self):
        """Register service with service registry."""
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.registry_url}/services/register",
                    json=self.service_info,
                    timeout=10.0
                )
                response.raise_for_status()
                self._registered = True
                logger.info(f"Service {self.service_info['name']} registered successfully")
        except Exception as e:
            logger.error(f"Failed to register service: {e}")

    async def _unregister_service(self):
        """Unregister service from service registry."""
        if not self._registered:
            return

        try:
            async with httpx.AsyncClient() as client:
                response = await client.delete(
                    f"{self.registry_url}/services/{self.service_info['id']}",
                    timeout=10.0
                )
                response.raise_for_status()
                logger.info(f"Service {self.service_info['name']} unregistered")
        except Exception as e:
            logger.error(f"Failed to unregister service: {e}")

    async def _heartbeat(self):
        """Send heartbeat to registry."""
        if not self._registered:
            return

        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.registry_url}/services/{self.service_info['id']}/heartbeat",
                    json={"timestamp": time.time()},
                    timeout=5.0
                )
                response.raise_for_status()
        except Exception as e:
            logger.warning(f"Heartbeat failed: {e}")

    async def _heartbeat_loop(self):
        """Continuous heartbeat loop."""
        while True:
            try:
                await self._heartbeat()
                await asyncio.sleep(30)  # Heartbeat every 30 seconds
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Heartbeat loop error: {e}")
                await asyncio.sleep(5)

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint
    ) -> Response:
        # Add service information to request
        request.state.service = self.service_info
        return await call_next(request)


class CircuitBreakerMiddleware(BaseHTTPMiddleware):
    """Circuit breaker for protecting services from cascading failures."""

    def __init__(
        self,
        app,
        failure_threshold: int = 5,
        recovery_timeout: float = 60.0,
        expected_exception: type = Exception
    ):
        super().__init__(app)
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.expected_exception = expected_exception
        self.failure_count = 0
        self.last_failure_time = None
        self.state = "CLOSED"  # CLOSED, OPEN, HALF_OPEN

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint
    ) -> Response:
        # Check circuit breaker state
        if self.state == "OPEN":
            if time.time() - self.last_failure_time > self.recovery_timeout:
                self.state = "HALF_OPEN"
                logger.info("Circuit breaker entering HALF_OPEN state")
            else:
                raise ServiceUnavailableError(
                    "Service temporarily unavailable",
                    retry_after=int(self.recovery_timeout)
                )

        try:
            # Attempt to process request
            response = await call_next(request)

            # Reset failure count on success
            if self.state == "HALF_OPEN":
                self.state = "CLOSED"
                self.failure_count = 0
                logger.info("Circuit breaker reset to CLOSED state")

            return response

        except self.expected_exception as e:
            self.failure_count += 1
            self.last_failure_time = time.time()

            if self.failure_count >= self.failure_threshold:
                self.state = "OPEN"
                logger.error(
                    f"Circuit breaker opened after {self.failure_count} failures"
                )

            raise


class RateLimitMiddleware(BaseHTTPMiddleware):
    """Rate limiting middleware with Redis backend."""

    def __init__(self, app):
        super().__init__(app)
        self.rate_limiter = None
        self._init_rate_limiter()

    def _init_rate_limiter(self):
        """Initialize rate limiter with Redis client."""
        try:
            import redis.asyncio as redis
            self.redis_client = redis.Redis.from_url(
                settings.REDIS_URL,
                encoding="utf-8",
                decode_responses=True
            )
            self.rate_limiter = RateLimiter(self.redis_client)
        except ImportError:
            logger.warning("Redis not available, rate limiting disabled")
        except Exception as e:
            logger.error(f"Failed to initialize rate limiter: {e}")

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint
    ) -> Response:
        if not self.rate_limiter:
            return await call_next(request)

        # Get rate limit key from request
        if hasattr(request.state, "token_info"):
            # Authenticated user
            key = f"rate_limit:user:{request.state.token_info.sub}"
            limit = 1000  # 1000 requests per hour for users
        elif request.client:
            # Anonymous user by IP
            key = f"rate_limit:ip:{request.client.host}"
            limit = 100  # 100 requests per hour for anonymous
        else:
            key = f"rate_limit:unknown"
            limit = 10

        # Check rate limit
        if not await self.rate_limiter.check_rate_limit(key, limit, 3600):
            raise RateLimitError(
                "Rate limit exceeded",
                limit=limit,
                window=3600
            )

        # Add rate limit headers
        response = await call_next(request)
        response.headers["X-RateLimit-Limit"] = str(limit)
        response.headers["X-RateLimit-Remaining"] = str(
            max(0, limit - await self.redis_client.zcard(key))
        )
        response.headers["X-RateLimit-Reset"] = str(
            int(time.time()) + 3600
        )

        return response


class AuthenticationMiddleware(BaseHTTPMiddleware):
    """Authentication middleware for API Gateway integration."""

    def __init__(self, app, public_paths: Optional[List[str]] = None):
        super().__init__(app)
        self.public_paths = public_paths or [
            "/health",
            "/metrics",
            "/docs",
            "/redoc",
            "/openapi.json"
        ]

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint
    ) -> Response:
        # Skip authentication for public paths
        if request.url.path in self.public_paths:
            return await call_next(request)

        # Extract authentication credentials
        from fastapi.security import HTTPBearer
        security = HTTPBearer(auto_error=False)

        try:
            credentials = await security(request)
            # Authenticate and get token info
            token_info = await authenticator.authenticate_request(request, credentials)
            request.state.token_info = token_info

            # Trace request for audit
            await trace_request(request, token_info)

        except Exception as e:
            logger.warning(f"Authentication failed: {e}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication required",
                headers={"WWW-Authenticate": "Bearer"},
            )

        return await call_next(request)


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """Detailed request logging for monitoring and debugging."""

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint
    ) -> Response:
        start_time = time.time()

        # Log request
        logger.info(
            "Request started",
            extra={
                "method": request.method,
                "url": str(request.url),
                "user_agent": request.headers.get("User-Agent"),
                "x_forwarded_for": request.headers.get("X-Forwarded-For"),
                "request_id": getattr(request.state, "request_id", None)
            }
        )

        # Process request
        response = await call_next(request)

        # Calculate duration
        duration = time.time() - start_time

        # Log response
        logger.info(
            "Request completed",
            extra={
                "status_code": response.status_code,
                "duration": f"{duration:.3f}s",
                "request_id": getattr(request.state, "request_id", None)
            }
        )

        return response


class CacheMiddleware(BaseHTTPMiddleware):
    """Response caching middleware for GET requests."""

    def __init__(
        self,
        app,
        cache_ttl: int = 300,  # 5 minutes
        max_cache_size: int = 1000
    ):
        super().__init__(app)
        self.cache_ttl = cache_ttl
        self.max_cache_size = max_cache_size
        self._cache: Dict[str, Dict[str, Any]] = {}

    def _get_cache_key(self, request: Request) -> Optional[str]:
        """Generate cache key for request."""
        if request.method != "GET":
            return None

        # Include path and query parameters in key
        key = f"{request.url.path}?{request.url.query}"
        return key

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint
    ) -> Response:
        cache_key = self._get_cache_key(request)
        if not cache_key:
            return await call_next(request)

        # Check cache
        if cache_key in self._cache:
            cached = self._cache[cache_key]
            if time.time() - cached["timestamp"] < self.cache_ttl:
                logger.debug(f"Cache hit for {cache_key}")
                return Response(
                    content=cached["content"],
                    status_code=cached["status_code"],
                    headers=cached["headers"],
                    media_type=cached["media_type"]
                )
            else:
                # Cache expired
                del self._cache[cache_key]

        # Process request
        response = await call_next(request)

        # Cache response if successful
        if response.status_code == 200:
            # Evict old entries if cache is full
            if len(self._cache) >= self.max_cache_size:
                oldest_key = min(
                    self._cache.keys(),
                    key=lambda k: self._cache[k]["timestamp"]
                )
                del self._cache[oldest_key]

            # Store response
            self._cache[cache_key] = {
                "content": response.body,
                "status_code": response.status_code,
                "headers": dict(response.headers),
                "media_type": response.media_type,
                "timestamp": time.time()
            }

        return response


class MetricsMiddleware(BaseHTTPMiddleware):
    """Prometheus metrics collection middleware."""

    def __init__(self, app):
        super().__init__(app)
        self.metrics = {
            "requests_total": 0,
            "request_duration": [],
            "requests_by_status": {},
            "requests_by_path": {},
            "active_requests": 0
        }

    async def dispatch(
        self,
        request: Request,
        call_next: RequestResponseEndpoint
    ) -> Response:
        # Increment active requests
        self.metrics["active_requests"] += 1

        try:
            start_time = time.time()

            # Process request
            response = await call_next(request)

            # Update metrics
            duration = time.time() - start_time
            self.metrics["requests_total"] += 1
            self.metrics["request_duration"].append(duration)

            # Track by status
            status = str(response.status_code)
            self.metrics["requests_by_status"][status] = \
                self.metrics["requests_by_status"].get(status, 0) + 1

            # Track by path
            path = request.url.path
            self.metrics["requests_by_path"][path] = \
                self.metrics["requests_by_path"].get(path, 0) + 1

            # Add metrics to headers
            response.headers["X-Metrics-Request-Duration"] = f"{duration:.3f}"
            response.headers["X-Metrics-Active-Requests"] = \
                str(self.metrics["active_requests"])

            return response

        finally:
            # Decrement active requests
            self.metrics["active_requests"] -= 1

    def get_metrics(self) -> Dict[str, Any]:
        """Get current metrics."""
        avg_duration = (
            sum(self.metrics["request_duration"]) /
            len(self.metrics["request_duration"])
            if self.metrics["request_duration"]
            else 0
        )

        return {
            "requests_total": self.metrics["requests_total"],
            "average_request_duration": avg_duration,
            "requests_by_status": self.metrics["requests_by_status"],
            "requests_by_path": self.metrics["requests_by_path"],
            "active_requests": self.metrics["active_requests"]
        }


# Middleware factory functions
def create_gateway_middleware() -> List[Middleware]:
    """Create standard middleware stack for API Gateway integration."""
    return [
        Middleware(RequestIDMiddleware),
        Middleware(RequestLoggingMiddleware),
        Middleware(AuthenticationMiddleware),
        Middleware(RateLimitMiddleware),
        Middleware(CircuitBreakerMiddleware),
        Middleware(CacheMiddleware),
        Middleware(MetricsMiddleware),
    ]


def create_lightweight_middleware() -> List[Middleware]:
    """Create lightweight middleware stack for internal services."""
    return [
        Middleware(RequestIDMiddleware),
        Middleware(RequestLoggingMiddleware),
        Middleware(AuthenticationMiddleware, public_paths=["/health", "/metrics"]),
    ]


# Lifecycle management
async def startup_gateway_middleware(app):
    """Initialize gateway middleware components."""
    # Find and initialize ServiceRegistryMiddleware
    for middleware in app.user_middleware:
        if hasattr(middleware.cls, "__name__") and \
           "ServiceRegistryMiddleware" in middleware.cls.__name__:
            instance = middleware.cls(app)
            await instance.startup()
            app.state.service_registry = instance
            break


async def shutdown_gateway_middleware(app):
    """Cleanup gateway middleware components."""
    # Cleanup service registry
    if hasattr(app.state, "service_registry"):
        await app.state.service_registry.shutdown()

    # Cleanup authentication
    from .auth import cleanup_auth
    await cleanup_auth()