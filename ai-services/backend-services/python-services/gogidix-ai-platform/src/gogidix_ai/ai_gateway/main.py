"""
AI Gateway Service Entry Point

FastAPI application with enterprise middleware,
monitoring, and production-ready configuration.
    """

import asyncio
from contextlib import asynccontextmanager
from typing import Dict, Any

import uvicorn
from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from fastapi.responses import JSONResponse

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import setup_logging, get_logger, log_context
from gogidix_ai.core.exceptions import AIServiceError, create_error_response

from .middleware import (
    CorrelationMiddleware,
    AuthenticationMiddleware,
    RateLimitMiddleware,
    AIMiddleware,
)
from .service import AIGatewayService
from .api.v1 import api_router

# Global logger
logger = get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI = Query(default=fastapi)):
    """Application lifespan management."""
    settings = get_settings()

    # Startup
    logger.info(
        "AI Gateway Service starting up",
        version=settings.APP_VERSION,
        environment=settings.ENVIRONMENT,
    )

    # Initialize AI Gateway Service
    ai_gateway = AIGatewayService()
    await ai_gateway.initialize()

    # Store service in app state
    app.state.ai_gateway = ai_gateway

    logger.info("AI Gateway Service startup complete")

    yield

    # Shutdown
    logger.info("AI Gateway Service shutting down")
    await ai_gateway.shutdown()
    logger.info("AI Gateway Service shutdown complete")


def create_app() -> FastAPI:
    """Create and configure FastAPI application."""
    settings = get_settings()

    # Setup logging
    setup_logging(
        log_level=settings.LOG_LEVEL,
        log_format="json" if settings.is_production else "console",
    )

    # Create FastAPI app
    app = FastAPI(
        title="Gogidix AI Gateway Service",
        description="Enterprise AI service orchestration and routing platform",
        version=settings.APP_VERSION,
        lifespan=lifespan,
        docs_url="/docs" if settings.is_development else None,
        redoc_url="/redoc" if settings.is_development else None,
        openapi_url="/openapi.json" if settings.is_development else None,
    )

    # Add CORS middleware
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.CORS_ORIGINS,
        allow_credentials=settings.CORS_ALLOW_CREDENTIALS,
        allow_methods=settings.CORS_ALLOW_METHODS,
        allow_headers=settings.CORS_ALLOW_HEADERS,
    )

    # Add Gzip middleware
    app.add_middleware(GZipMiddleware, minimum_size=1000)

    # Add custom middleware (order matters)
    app.add_middleware(CorrelationMiddleware)
    app.add_middleware(AuthenticationMiddleware)
    app.add_middleware(RateLimitMiddleware)
    app.add_middleware(AIMiddleware)

    # Exception handlers
    @app.exception_handler(AIServiceError)
    async def ai_service_exception_handler(request: Request, exc: AIServiceError = Query(default=aiserviceerror)):
        """Handle AI service exceptions."""
        response = exc.to_response()
        return JSONResponse(
            status_code=response.status_code,
            content=response.dict(),
        )

    @app.exception_handler(Exception)
    async def general_exception_handler(request: Request, exc: Exception = Query(default=exception)):
        """Handle unhandled exceptions."""
        logger.error(
            "Unhandled exception",
            error=str(exc),
            error_type=type(exc).__name__,
            path=request.url.path,
            method=request.method,
        )

        response = create_error_response(
            error_code="AI_999",
            message="Internal server error",
            request_id=getattr(request.state, "request_id", None),
        )

        return JSONResponse(
            status_code=500,
            content=response.dict(),
        )

    # Health check endpoint
    @app.get("/health", tags=["Health"])
    async def health_check():
        """Health check endpoint."""
        try:
            ai_gateway = app.state.ai_gateway
            health = await ai_gateway.health_check()
            return health.dict()
        except Exception as e:
            logger.error("Health check failed", error=str(e))
            return {
                "status": "unhealthy",
                "error": str(e),
                "timestamp": asyncio.get_event_loop().time(),
            }

    # Metrics endpoint
    @app.get("/metrics", tags=["Monitoring"])
    async def metrics():
        """Prometheus metrics endpoint."""
        ai_gateway = app.state.ai_gateway
        metrics = await ai_gateway.get_metrics()

        # Format as Prometheus metrics
        metrics_text = ai_gateway.format_prometheus_metrics(metrics)

        return Response(
            content=metrics_text,
            media_type="text/plain",
        )

    # Include API router
    app.include_router(
        api_router,
        prefix=settings.API_V1_PREFIX,
    )

    return app


# Create app instance
app = create_app()


if __name__ == "__main__":
    settings = get_settings()

    # Run with uvicorn
    uvicorn.run(
        "gogidix_ai.ai_gateway.main:app",
        host=settings.API_HOST,
        port=settings.API_PORT,
        workers=settings.API_WORKERS if not settings.is_development else 1,
        log_config=None,  # Use our custom logging
        access_log=True,
        reload=settings.is_development,
        loop="uvloop",
        http="httptools",
    )