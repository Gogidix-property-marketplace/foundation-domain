"""
Administration Endpoints

Administrative API endpoints for system management,
configuration, and maintenance tasks.
"""

import asyncio
from typing import List, Optional, Dict, Any
from datetime import datetime

from fastapi import APIRouter, HTTPException, Depends, BackgroundTasks
from fastapi.responses import JSONResponse

from gogidix_ai.core.logging import get_logger
from gogidix_ai.core.exceptions import AIServiceError
from gogidix_ai.ai_gateway.dependencies import get_ai_gateway, get_current_user

logger = get_logger(__name__)
router = APIRouter()


@router.post("/system/reload")
async def reload_system(
    background_tasks: BackgroundTasks,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Reload system configuration and models.

    Args:
        background_tasks: FastAPI background tasks
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Reload status
    """
    try:
        # Validate user permissions (admin required)
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Add background task for reload
        background_tasks.add_task(
            perform_system_reload,
            ai_gateway,
            current_user.get("sub"),
        )

        return {
            "status": "initiated",
            "message": "System reload initiated",
            "initiated_by": current_user.get("sub"),
        }

    except Exception as e:
        logger.error(
            "System reload initiation error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to initiate system reload",
        )


@router.post("/cache/warm")
async def warm_cache(
    model_name: str,
    background_tasks: BackgroundTasks,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Warm cache for a model with common inputs.

    Args:
        model_name: Model to warm cache for
        background_tasks: FastAPI background tasks
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Cache warming status
    """
    try:
        # Validate user permissions
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Check if model exists
        if model_name not in ai_gateway.router.models:
            raise HTTPException(
                status_code=404,
                detail=f"Model {model_name} not found",
            )

        # Add background task for cache warming
        background_tasks.add_task(
            perform_cache_warming,
            ai_gateway,
            model_name,
            current_user.get("sub"),
        )

        return {
            "status": "initiated",
            "message": f"Cache warming initiated for model {model_name}",
            "initiated_by": current_user.get("sub"),
        }

    except Exception as e:
        logger.error(
            "Cache warming initiation error",
            model_name=model_name,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to initiate cache warming",
        )


@router.post("/cache/cleanup")
async def cleanup_cache(
    background_tasks: BackgroundTasks,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Clean up expired cache entries.

    Args:
        background_tasks: FastAPI background tasks
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Cleanup status
    """
    try:
        # Validate user permissions
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Perform cleanup immediately (it's fast)
        cleaned_keys = await ai_gateway.cache.cleanup_expired()

        logger.info(
            "Cache cleanup completed",
            cleaned_keys=cleaned_keys,
            cleaned_by=current_user.get("sub"),
        )

        return {
            "status": "completed",
            "message": "Cache cleanup completed",
            "cleaned_keys": cleaned_keys,
        }

    except Exception as e:
        logger.error(
            "Cache cleanup error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to cleanup cache",
        )


@router.post("/models/{model_name}/retrain")
async def trigger_retraining(
    model_name: str,
    background_tasks: BackgroundTasks,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Trigger model retraining pipeline.

    Args:
        model_name: Model to retrain
        background_tasks: FastAPI background tasks
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Retraining status
    """
    try:
        # Validate user permissions
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Check if model exists
        if model_name not in ai_gateway.router.models:
            raise HTTPException(
                status_code=404,
                detail=f"Model {model_name} not found",
            )

        # Add background task for retraining
        background_tasks.add_task(
            perform_model_retraining,
            ai_gateway,
            model_name,
            current_user.get("sub"),
        )

        return {
            "status": "initiated",
            "message": f"Retraining initiated for model {model_name}",
            "initiated_by": current_user.get("sub"),
        }

    except Exception as e:
        logger.error(
            "Model retraining initiation error",
            model_name=model_name,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to initiate model retraining",
        )


@router.get("/config")
async def get_system_config(
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Get system configuration.

    Args:
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        System configuration
    """
    try:
        # Validate user permissions
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Get configuration (excluding sensitive data)
        config = {
            "app_name": ai_gateway.settings.APP_NAME,
            "app_version": ai_gateway.settings.APP_VERSION,
            "environment": ai_gateway.settings.ENVIRONMENT,
            "api_timeout": ai_gateway.settings.API_TIMEOUT,
            "default_model_timeout": ai_gateway.settings.DEFAULT_MODEL_TIMEOUT,
            "max_batch_size": ai_gateway.settings.MAX_BATCH_SIZE,
            "max_concurrent_requests": ai_gateway.settings.MAX_CONCURRENT_REQUESTS,
            "cache_ttl_seconds": ai_gateway.settings.CACHE_TTL_SECONDS,
            "rate_limit_enabled": ai_gateway.settings.RATE_LIMIT_ENABLED,
            "rate_limit_requests_per_minute": ai_gateway.settings.RATE_LIMIT_REQUESTS_PER_MINUTE,
            "metrics_enabled": ai_gateway.settings.METRICS_ENABLED,
            "gpu_enabled": ai_gateway.settings.GPU_ENABLED,
            "model_base_path": ai_gateway.settings.MODEL_BASE_PATH,
        }

        return {
            "timestamp": datetime.utcnow().isoformat(),
            "configuration": config,
        }

    except Exception as e:
        logger.error(
            "Get system config error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to retrieve system configuration",
        )


@router.get("/status")
async def get_system_status(
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Get comprehensive system status.

    Args:
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional for basic status)

    Returns:
        System status
    """
    try:
        # Get health check
        health = await ai_gateway.health_check()

        # Get model count
        model_count = len(ai_gateway.router.models)

        # Get active requests (approximate)
        active_requests = sum(
            instance.connections
            for versions in ai_gateway.router.models.values()
            for instance in versions.values()
        )

        status = {
            "timestamp": datetime.utcnow().isoformat(),
            "health": health.dict(),
            "models": {
                "total_count": model_count,
                "active_count": model_count,
            },
            "requests": {
                "active_estimate": active_requests,
                "total_processed": health.requests_processed,
            },
            "cache": await ai_gateway.cache.get_stats(),
        }

        # Add admin-only information
        if current_user and current_user.get("is_admin", False):
            import psutil

            # Add system resource usage
            status["system"] = {
                "cpu_percent": psutil.cpu_percent(),
                "memory_percent": psutil.virtual_memory().percent,
                "disk_percent": psutil.disk_usage('/').percent,
            }

        return status

    except Exception as e:
        logger.error(
            "Get system status error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to retrieve system status",
        )


@router.post("/maintenance/enable")
async def enable_maintenance_mode(
    message: Optional[str] = None,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Enable maintenance mode.

    Args:
        message: Optional maintenance message
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Maintenance mode status
    """
    try:
        # Validate user permissions
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Set maintenance mode (this would typically be stored in Redis or database)
        maintenance_data = {
            "enabled": True,
            "message": message or "System undergoing maintenance",
            "enabled_at": datetime.utcnow().isoformat(),
            "enabled_by": current_user.get("sub"),
        }

        logger.info(
            "Maintenance mode enabled",
            message=maintenance_data["message"],
            enabled_by=current_user.get("sub"),
        )

        return {
            "status": "enabled",
            "maintenance": maintenance_data,
        }

    except Exception as e:
        logger.error(
            "Enable maintenance mode error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to enable maintenance mode",
        )


@router.post("/maintenance/disable")
async def disable_maintenance_mode(
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Disable maintenance mode.

    Args:
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Maintenance mode status
    """
    try:
        # Validate user permissions
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Disable maintenance mode
        maintenance_data = {
            "enabled": False,
            "disabled_at": datetime.utcnow().isoformat(),
            "disabled_by": current_user.get("sub"),
        }

        logger.info(
            "Maintenance mode disabled",
            disabled_by=current_user.get("sub"),
        )

        return {
            "status": "disabled",
            "maintenance": maintenance_data,
        }

    except Exception as e:
        logger.error(
            "Disable maintenance mode error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to disable maintenance mode",
        )


# Background task functions
async def perform_system_reload(ai_gateway, user_id: str):
    """Background task to reload system."""
    try:
        logger.info(
            "Performing system reload",
            initiated_by=user_id,
        )

        # Reload router metrics
        await ai_gateway.router.get_model_metrics()

        # Reload monitoring
        await ai_gateway.monitor.get_model_metrics()

        logger.info(
            "System reload completed",
            initiated_by=user_id,
        )

    except Exception as e:
        logger.error(
            "System reload failed",
            initiated_by=user_id,
            error=str(e),
        )


async def perform_cache_warming(ai_gateway, model_name: str, user_id: str):
    """Background task to warm cache."""
    try:
        logger.info(
            "Performing cache warming",
            model_name=model_name,
            initiated_by=user_id,
        )

        # Generate common inputs (this would be more sophisticated in practice)
        common_inputs = [
            {"example": "input1"},
            {"example": "input2"},
        ]

        warmed = await ai_gateway.cache.warm_cache(
            model_name=model_name,
            common_inputs=common_inputs,
        )

        logger.info(
            "Cache warming completed",
            model_name=model_name,
            items_warmed=warmed,
            initiated_by=user_id,
        )

    except Exception as e:
        logger.error(
            "Cache warming failed",
            model_name=model_name,
            initiated_by=user_id,
            error=str(e),
        )


async def perform_model_retraining(ai_gateway, model_name: str, user_id: str):
    """Background task to trigger model retraining."""
    try:
        logger.info(
            "Triggering model retraining",
            model_name=model_name,
            initiated_by=user_id,
        )

        # This would typically call MLflow, Kubeflow, or similar
        # For now, we'll just log the intent
        logger.info(
            "Model retraining pipeline would be triggered here",
            model_name=model_name,
            initiated_by=user_id,
        )

        # Update model status to indicate retraining
        if model_name in ai_gateway.router.models:
            for instance in ai_gateway.router.models[model_name].values():
                instance.version.status = "updating"

    except Exception as e:
        logger.error(
            "Model retraining failed",
            model_name=model_name,
            initiated_by=user_id,
            error=str(e),
        )