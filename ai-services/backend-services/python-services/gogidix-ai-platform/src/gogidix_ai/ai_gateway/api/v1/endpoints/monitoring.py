"""
Monitoring Endpoints

REST API endpoints for system monitoring,
performance metrics, and health checks.
"""

from typing import List, Optional, Dict, Any
from datetime import datetime, timedelta

from fastapi import APIRouter, HTTPException, Depends, Query
from fastapi.responses import PlainTextResponse

from gogidix_ai.core.logging import get_logger
from gogidix_ai.ai_gateway.models import HealthCheck, Metrics
from gogidix_ai.ai_gateway.dependencies import get_ai_gateway, get_current_user

logger = get_logger(__name__)
router = APIRouter()


@router.get("/health", response_model=HealthCheck)
async def health_check(
    detailed: bool = Query(False, description="Include detailed health information"),
    ai_gateway = Depends(get_ai_gateway),
):
    """
    Get system health status.

    Args:
        detailed: Whether to include detailed health information
        ai_gateway: AI Gateway service instance

    Returns:
        Health check response
    """
    try:
        health = await ai_gateway.health_check()

        if not detailed:
            # Return simplified health check
            return HealthCheck(
                status=health.status,
                version=health.version,
                uptime_seconds=health.uptime_seconds,
                models_loaded=health.models_loaded,
                requests_processed=health.requests_processed,
                error_rate=health.error_rate,
            )

        return health

    except Exception as e:
        logger.error(
            "Health check error",
            error=str(e),
        )
        # Always return a health response, even if there's an error
        return HealthCheck(
            status="unhealthy",
            version="unknown",
            uptime_seconds=0,
            models_loaded=0,
            requests_processed=0,
            error_rate=100.0,
            details={"error": str(e)},
        )


@router.get("/metrics")
async def get_metrics(
    model_name: Optional[str] = Query(None, description="Filter by model name"),
    hours: int = Query(1, description="Hours of metrics to retrieve"),
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Get system and model metrics.

    Args:
        model_name: Optional model name filter
        hours: Number of hours of metrics to retrieve
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        Metrics data
    """
    try:
        # Get service metrics
        service_metrics = await ai_gateway.get_metrics()

        # Get model metrics
        model_metrics = await ai_gateway.monitor.get_model_metrics(
            model_name=model_name,
            hours=hours,
        )

        # Get router metrics
        router_metrics = await ai_gateway.router.get_model_metrics(
            model_name=model_name,
        )

        return {
            "timestamp": datetime.utcnow().isoformat(),
            "service_metrics": service_metrics.dict(),
            "model_metrics": model_metrics,
            "router_metrics": router_metrics,
            "analysis_period_hours": hours,
        }

    except Exception as e:
        logger.error(
            "Get metrics error",
            model_name=model_name,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to retrieve metrics",
        )


@router.get("/prometheus", response_class=PlainTextResponse)
async def prometheus_metrics(
    ai_gateway = Depends(get_ai_gateway),
):
    """
    Get metrics in Prometheus format.

    Args:
        ai_gateway: AI Gateway service instance

    Returns:
        Prometheus-formatted metrics
    """
    try:
        # Get service metrics
        metrics = await ai_gateway.get_metrics()

        # Format for Prometheus
        prometheus_text = ai_gateway.format_prometheus_metrics(metrics)

        return PlainTextResponse(
            content=prometheus_text,
            media_type="text/plain",
        )

    except Exception as e:
        logger.error(
            "Prometheus metrics error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to generate Prometheus metrics",
        )


@router.get("/drift-analysis")
async def get_drift_analysis(
    model_name: str = Query(..., description="Model name for drift analysis"),
    hours: int = Query(24, description="Hours of data to analyze"),
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Get drift analysis for a model.

    Args:
        model_name: Name of the model
        hours: Number of hours of data to analyze
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        Drift analysis report
    """
    try:
        drift_report = await ai_gateway.monitor.get_drift_report(
            model_name=model_name,
            hours=hours,
        )

        return drift_report

    except Exception as e:
        logger.error(
            "Drift analysis error",
            model_name=model_name,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to generate drift analysis",
        )


@router.get("/cache-stats")
async def get_cache_statistics(
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Get cache statistics.

    Args:
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        Cache statistics
    """
    try:
        cache_stats = await ai_gateway.cache.get_stats()

        return {
            "timestamp": datetime.utcnow().isoformat(),
            "cache_statistics": cache_stats,
        }

    except Exception as e:
        logger.error(
            "Cache statistics error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to retrieve cache statistics",
        )


@router.post("/cache/invalidate")
async def invalidate_cache(
    model_name: Optional[str] = Query(None, description="Model name to invalidate"),
    pattern: Optional[str] = Query(None, description="Cache pattern to invalidate"),
    ai_gateway = Depends(get_ai_gateway),
    current_user: Dict = Depends(get_current_user),
):
    """
    Invalidate cache entries.

    Args:
        model_name: Optional model name to invalidate
        pattern: Optional cache pattern to invalidate
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user

    Returns:
        Invalidation result
    """
    try:
        # Validate user permissions
        if not current_user.get("is_admin", False):
            raise HTTPException(
                status_code=403,
                detail="Administrator privileges required",
            )

        if model_name:
            # Invalidate specific model
            keys_invalidated = await ai_gateway.cache.invalidate(model_name)
            action = f"Model {model_name}"
        elif pattern:
            # Invalidate by pattern
            keys_invalidated = await ai_gateway.cache.invalidate_pattern(pattern)
            action = f"Pattern {pattern}"
        else:
            raise HTTPException(
                status_code=400,
                detail="Either model_name or pattern must be provided",
            )

        logger.info(
            "Cache invalidated",
            action=action,
            keys_invalidated=keys_invalidated,
            invalidated_by=current_user.get("sub"),
        )

        return {
            "status": "success",
            "action": action,
            "keys_invalidated": keys_invalidated,
        }

    except Exception as e:
        logger.error(
            "Cache invalidation error",
            model_name=model_name,
            pattern=pattern,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to invalidate cache",
        )


@router.get("/alerts")
async def get_alerts(
    severity: Optional[str] = Query(None, description="Filter by alert severity"),
    hours: int = Query(24, description="Hours of alerts to retrieve"),
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Get system alerts.

    Args:
        severity: Optional severity filter
        hours: Number of hours of alerts to retrieve
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        List of alerts
    """
    try:
        # This would typically integrate with an alerting system
        # For now, we'll return a mock structure
        alerts = []

        # Check for model drift alerts
        model_metrics = await ai_gateway.router.get_model_metrics()
        for model_name, model_data in model_metrics.items():
            if model_data.get("error_rate", 0) > 5.0:
                alerts.append({
                    "id": str(uuid.uuid4()),
                    "type": "model_error_rate",
                    "severity": "warning",
                    "model_name": model_name,
                    "message": f"High error rate: {model_data['error_rate']:.2f}%",
                    "timestamp": datetime.utcnow().isoformat(),
                    "resolved": False,
                })

        # Filter by severity if provided
        if severity:
            alerts = [a for a in alerts if a["severity"] == severity]

        return {
            "timestamp": datetime.utcnow().isoformat(),
            "alerts": alerts,
            "total_count": len(alerts),
            "filter": {"severity": severity, "hours": hours},
        }

    except Exception as e:
        logger.error(
            "Get alerts error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to retrieve alerts",
        )


@router.get("/system-usage")
async def get_system_usage(
    hours: int = Query(1, description="Hours of usage data"),
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Get system resource usage statistics.

    Args:
        hours: Number of hours of usage data
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        System usage statistics
    """
    try:
        import psutil
        import GPUtil  # if available

        # CPU usage
        cpu_percent = psutil.cpu_percent(interval=1)
        cpu_count = psutil.cpu_count()

        # Memory usage
        memory = psutil.virtual_memory()
        memory_percent = memory.percent

        # Disk usage
        disk = psutil.disk_usage('/')
        disk_percent = (disk.used / disk.total) * 100

        # Network I/O
        network = psutil.net_io_counters()

        # GPU usage (if available)
        gpu_info = []
        try:
            gpus = GPUtil.getGPUs()
            for gpu in gpus:
                gpu_info.append({
                    "id": gpu.id,
                    "name": gpu.name,
                    "load": gpu.load * 100,
                    "memory_used": gpu.memoryUsed,
                    "memory_total": gpu.memoryTotal,
                    "memory_percent": (gpu.memoryUsed / gpu.memoryTotal) * 100,
                    "temperature": gpu.temperature,
                })
        except ImportError:
            gpu_info = [{"message": "GPU monitoring not available"}]

        return {
            "timestamp": datetime.utcnow().isoformat(),
            "cpu": {
                "percent": cpu_percent,
                "count": cpu_count,
            },
            "memory": {
                "percent": memory_percent,
                "total_gb": memory.total / (1024**3),
                "available_gb": memory.available / (1024**3),
                "used_gb": memory.used / (1024**3),
            },
            "disk": {
                "percent": disk_percent,
                "total_gb": disk.total / (1024**3),
                "free_gb": disk.free / (1024**3),
                "used_gb": disk.used / (1024**3),
            },
            "network": {
                "bytes_sent": network.bytes_sent,
                "bytes_recv": network.bytes_recv,
                "packets_sent": network.packets_sent,
                "packets_recv": network.packets_recv,
            },
            "gpu": gpu_info,
        }

    except Exception as e:
        logger.error(
            "System usage error",
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to retrieve system usage",
        )