"""
AI Gateway API v1

RESTful API endpoints for AI model inference,
management, and monitoring.
"""

from fastapi import APIRouter

# Import routers
from .endpoints import (
    inference,
    models,
    monitoring,
    admin,
)

# Create main API router
api_router = APIRouter()

# Include endpoint routers
api_router.include_router(
    inference.router,
    prefix="/inference",
    tags=["Inference"],
)

api_router.include_router(
    models.router,
    prefix="/models",
    tags=["Models"],
)

api_router.include_router(
    monitoring.router,
    prefix="/monitoring",
    tags=["Monitoring"],
)

api_router.include_router(
    admin.router,
    prefix="/admin",
    tags=["Administration"],
)