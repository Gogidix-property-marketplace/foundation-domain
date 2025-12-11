"""
Model Management Endpoints

REST API endpoints for model registration, versioning,
configuration, and lifecycle management.
"""

import uuid
from typing import List, Optional, Dict, Any

from fastapi import APIRouter, HTTPException, Depends, Query
from fastapi.responses import JSONResponse

from gogidix_ai.core.logging import get_logger
from gogidix_ai.core.exceptions import AIServiceError, ModelNotFoundError
from gogidix_ai.ai_gateway.models import (
    ModelInfo,
    ModelVersion,
    ModelStatus,
    RoutingConfig,
    TrafficSplitConfig,
)
from gogidix_ai.ai_gateway.dependencies import get_ai_gateway, get_current_user

logger = get_logger(__name__)
router = APIRouter()


@router.post("/register", response_model=Dict[str, Any])
async def register_model(
    model_info: ModelInfo,
    versions: List[ModelVersion],
    routing_config: Optional[RoutingConfig] = None,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Register a new model with the AI Gateway.

    Args:
        model_info: Model information and metadata
        versions: List of model versions
        routing_config: Optional routing configuration
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Registration confirmation

    Raises:
        HTTPException: For validation or registration errors
    """
    try:
        # Validate user permissions (admin role required)
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Validate model registration
        if not versions:
            raise HTTPException(
                status_code=400,
                detail="At least one model version must be provided",
            )

        # Check if model already exists
        if model_info.name in ai_gateway.router.models:
            logger.warning(
                "Model already exists, updating",
                model_name=model_info.name,
            )

        # Register model
        await ai_gateway.register_model(
            model_info=model_info,
            versions=versions,
            routing_config=routing_config,
        )

        logger.info(
            "Model registered successfully",
            model_name=model_info.name,
            versions=len(versions),
            registered_by=current_user.get("sub"),
        )

        return {
            "status": "success",
            "message": f"Model {model_info.name} registered successfully",
            "model_name": model_info.name,
            "versions": [v.version for v in versions],
        }

    except AIServiceError as e:
        logger.error(
            "Model registration failed",
            model_name=model_info.name,
            error=str(e),
            error_code=e.error_code,
        )
        raise HTTPException(
            status_code=e.status_code,
            detail=e.to_response().dict(),
        )
    except Exception as e:
        logger.error(
            "Unexpected registration error",
            model_name=model_info.name,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to register model",
        )


@router.delete("/{model_name}")
async def deregister_model(
    model_name: str,
    version: Optional[str] = None,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Deregister a model or specific version.

    Args:
        model_name: Name of the model
        version: Optional version to deregister (if None, deregisters all)
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Deregistration confirmation
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

        # Deregister model
        await ai_gateway.router.deregister_model(
            model_name=model_name,
            version=version,
        )

        action = f"Model {model_name} version {version}" if version else f"Model {model_name}"
        logger.info(
            "Model deregistered successfully",
            model_name=model_name,
            version=version,
            deregistered_by=current_user.get("sub"),
        )

        return {
            "status": "success",
            "message": f"{action} deregistered successfully",
        }

    except ModelNotFoundError as e:
        raise HTTPException(
            status_code=404,
            detail=str(e),
        )
    except Exception as e:
        logger.error(
            "Model deregistration error",
            model_name=model_name,
            version=version,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to deregister model",
        )


@router.get("/")
async def list_models(
    include_metrics: bool = Query(False, description="Include model metrics"),
    status_filter: Optional[ModelStatus] = Query(None, description="Filter by status"),
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
) -> List[Dict[str, Any]]:
    """
    List all registered models.

    Args:
        include_metrics: Whether to include performance metrics
        status_filter: Optional status filter
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        List of model information
    """
    try:
        models_list = []
        router_metrics = await ai_gateway.router.get_model_metrics()

        for model_name, versions in ai_gateway.router.models.items():
            model_info = {}

            for version_str, instance in versions.items():
                # Apply status filter if provided
                if status_filter and instance.version.status != status_filter:
                    continue

                model_data = {
                    "model_name": model_name,
                    "version": version_str,
                    "status": instance.version.status.value,
                    "endpoint": instance.endpoint,
                    "is_default": instance.version.is_default,
                    "traffic_percentage": instance.version.traffic_percentage,
                    "healthy": instance.healthy,
                    "created_at": instance.version.created_at.isoformat(),
                }

                # Add metrics if requested
                if include_metrics and model_name in router_metrics:
                    metrics = router_metrics[model_name]["versions"].get(version_str, {})
                    model_data["metrics"] = metrics

                models_list.append(model_data)

        return models_list

    except Exception as e:
        logger.error(
            "List models error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to list models",
        )


@router.post("/{model_name}/traffic-split")
async def set_traffic_split(
    model_name: str,
    traffic_config: TrafficSplitConfig,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Configure traffic splitting for A/B testing.

    Args:
        model_name: Name of the model
        traffic_config: Traffic split configuration
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Configuration confirmation
    """
    try:
        # Validate user permissions
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Validate model exists
        if model_name not in ai_gateway.router.models:
            raise HTTPException(
                status_code=404,
                detail=f"Model {model_name} not found",
            )

        # Set traffic split configuration
        ai_gateway.router.set_traffic_split(model_name, traffic_config)

        logger.info(
            "Traffic split configured",
            model_name=model_name,
            versions=list(traffic_config.versions.keys()),
            configured_by=current_user.get("sub"),
        )

        return {
            "status": "success",
            "message": f"Traffic split configured for model {model_name}",
            "config": traffic_config.dict(),
        }

    except Exception as e:
        logger.error(
            "Traffic split configuration error",
            model_name=model_name,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to configure traffic split",
        )


@router.delete("/{model_name}/traffic-split")
async def remove_traffic_split(
    model_name: str,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Remove traffic splitting configuration.

    Args:
        model_name: Name of the model
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Removal confirmation
    """
    try:
        # Validate user permissions
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        # Remove traffic split
        ai_gateway.router.remove_traffic_split(model_name)

        logger.info(
            "Traffic split removed",
            model_name=model_name,
            removed_by=current_user.get("sub"),
        )

        return {
            "status": "success",
            "message": f"Traffic split removed for model {model_name}",
        }

    except Exception as e:
        logger.error(
            "Traffic split removal error",
            model_name=model_name,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to remove traffic split",
        )


@router.put("/{model_name}/health")
async def update_model_health(
    model_name: str,
    version: str,
    healthy: bool,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Update health status of a model version.

    Args:
        model_name: Name of the model
        version: Model version
        healthy: Health status
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        Update confirmation
    """
    try:
        # Update model health
        await ai_gateway.router.update_model_health(
            model_name=model_name,
            version=version,
            healthy=healthy,
        )

        logger.info(
            "Model health updated",
            model_name=model_name,
            version=version,
            healthy=healthy,
            updated_by=current_user.get("sub") if current_user else "system",
        )

        return {
            "status": "success",
            "message": f"Health status updated for {model_name}:{version}",
            "healthy": healthy,
        }

    except Exception as e:
        logger.error(
            "Model health update error",
            model_name=model_name,
            version=version,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to update model health",
        )


@router.get("/{model_name}/metrics")
async def get_model_metrics(
    model_name: str,
    hours: int = Query(24, description="Hours of metrics to retrieve"),
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Get detailed metrics for a model.

    Args:
        model_name: Name of the model
        hours: Number of hours of metrics to retrieve
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        Model metrics
    """
    try:
        # Get router metrics
        router_metrics = await ai_gateway.router.get_model_metrics(model_name)

        # Get monitoring metrics
        monitor_metrics = await ai_gateway.monitor.get_model_metrics(
            model_name=model_name,
            hours=hours,
        )

        # Get drift report
        drift_report = await ai_gateway.monitor.get_drift_report(
            model_name=model_name,
            hours=hours,
        )

        return {
            "model_name": model_name,
            "analysis_period_hours": hours,
            "router_metrics": router_metrics.get(model_name, {}),
            "monitoring_metrics": monitor_metrics,
            "drift_analysis": drift_report,
        }

    except Exception as e:
        logger.error(
            "Get model metrics error",
            model_name=model_name,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to get model metrics",
        )