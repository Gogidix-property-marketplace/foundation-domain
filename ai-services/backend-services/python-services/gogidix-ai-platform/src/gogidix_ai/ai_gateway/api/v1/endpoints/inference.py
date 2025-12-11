"""
AI Inference Endpoints

REST API endpoints for model inference, batch processing,
and real-time predictions.
"""

import uuid
from typing import List, Optional, Dict, Any

from fastapi import APIRouter, HTTPException, Depends, BackgroundTasks, Request
from fastapi.responses import JSONResponse

from gogidix_ai.core.logging import get_logger, correlation_id, request_id
from gogidix_ai.core.exceptions import AIServiceError
from gogidix_ai.ai_gateway.models import (
    AIRequest,
    AIResponse,
    BatchRequest,
    BatchResponse,
    Priority,
)
from gogidix_ai.ai_gateway.dependencies import get_ai_gateway, get_current_user

logger = get_logger(__name__)
router = APIRouter()


@router.post("/predict", response_model=AIResponse)
async def predict(
    request: AIRequest,
    background_tasks: BackgroundTasks,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Make a single prediction request.

    Args:
        request: AI prediction request
        background_tasks: FastAPI background tasks
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        AI prediction response

    Raises:
        HTTPException: For validation or processing errors
    """
    try:
        # Add user context if authenticated
        if current_user and not request.user_id:
            request.user_id = current_user.get("sub")

        # Add correlation context
        request.correlation_id = correlation_id.get()
        request.request_id = request.request_id or str(uuid.uuid4())

        logger.info(
            "Prediction request received",
            request_id=request.request_id,
            model_name=request.model_name,
            model_version=request.model_version,
            user_id=request.user_id,
        )

        # Process prediction
        response = await ai_gateway.predict(request)

        # Log successful prediction
        logger.info(
            "Prediction completed",
            request_id=request.request_id,
            model_name=request.model_name,
            model_version=response.model_version,
            latency_ms=response.latency_ms,
        )

        # Add background task for metrics recording
        background_tasks.add_task(
            log_prediction_metrics,
            request,
            response,
        )

        return response

    except AIServiceError as e:
        logger.error(
            "Prediction failed",
            request_id=request.request_id,
            error=str(e),
            error_code=e.error_code,
        )
        raise HTTPException(
            status_code=e.status_code,
            detail=e.to_response().dict(),
        )
    except Exception as e:
        logger.error(
            "Unexpected prediction error",
            request_id=request.request_id,
            error=str(e),
            error_type=type(e).__name__,
        )
        raise HTTPException(
            status_code=500,
            detail={
                "error": "Internal server error",
                "message": "An unexpected error occurred while processing your request",
                "request_id": request.request_id,
            },
        )


@router.post("/predict-batch", response_model=BatchResponse)
async def predict_batch(
    batch_request: BatchRequest,
    background_tasks: BackgroundTasks,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Make batch prediction requests.

    Args:
        batch_request: Batch prediction request
        background_tasks: FastAPI background tasks
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Returns:
        Batch prediction response

    Raises:
        HTTPException: For validation or processing errors
    """
    try:
        # Validate batch request
        if not batch_request.requests:
            raise HTTPException(
                status_code=400,
                detail="Batch cannot be empty",
            )

        if len(batch_request.requests) > ai_gateway.settings.MAX_BATCH_SIZE:
            raise HTTPException(
                status_code=400,
                detail=f"Batch size exceeds maximum of {ai_gateway.settings.MAX_BATCH_SIZE}",
            )

        # Ensure all requests are for the same model
        model_names = {req.model_name for req in batch_request.requests}
        if len(model_names) > 1:
            raise HTTPException(
                status_code=400,
                detail="All requests in batch must be for the same model",
            )

        logger.info(
            "Batch prediction request received",
            batch_id=batch_request.batch_id,
            model_name=batch_request.model_name,
            request_count=len(batch_request.requests),
        )

        # Process batch prediction
        response = await ai_gateway.predict_batch(batch_request)

        # Log successful batch
        logger.info(
            "Batch prediction completed",
            batch_id=batch_request.batch_id,
            successful=response.successful_requests,
            failed=response.failed_requests,
            total_latency_ms=response.total_latency_ms,
        )

        # Add background task for metrics
        background_tasks.add_task(
            log_batch_metrics,
            batch_request,
            response,
        )

        return response

    except AIServiceError as e:
        logger.error(
            "Batch prediction failed",
            batch_id=batch_request.batch_id,
            error=str(e),
            error_code=e.error_code,
        )
        raise HTTPException(
            status_code=e.status_code,
            detail=e.to_response().dict(),
        )
    except Exception as e:
        logger.error(
            "Unexpected batch prediction error",
            batch_id=batch_request.batch_id,
            error=str(e),
            error_type=type(e).__name__,
        )
        raise HTTPException(
            status_code=500,
            detail={
                "error": "Internal server error",
                "message": "An unexpected error occurred while processing your batch request",
                "batch_id": batch_request.batch_id,
            },
        )


@router.post("/predict-stream")
async def predict_stream(
    request: AIRequest,
    ai_gateway = Depends(get_ai_gateway),
    current_user: Optional[Dict] = Depends(get_current_user),
):
    """
    Stream prediction responses for real-time applications.

    Args:
        request: AI prediction request
        ai_gateway: AI Gateway service instance
        current_user: Authenticated user (optional)

    Yields:
        JSONResponse: Streaming prediction results
    """
    try:
        # Add user context
        if current_user and not request.user_id:
            request.user_id = current_user.get("sub")

        request.correlation_id = correlation_id.get()
        request.request_id = request.request_id or str(uuid.uuid4())

        logger.info(
            "Streaming prediction request",
            request_id=request.request_id,
            model_name=request.model_name,
        )

        # For streaming, we'd implement Server-Sent Events (SSE)
        # This is a simplified example
        response = await ai_gateway.predict(request)

        yield {
            "type": "prediction",
            "data": response.dict(),
            "request_id": request.request_id,
        }

    except Exception as e:
        logger.error(
            "Streaming prediction error",
            request_id=request.request_id,
            error=str(e),
        )
        yield {
            "type": "error",
            "error": str(e),
            "request_id": request.request_id,
        }


@router.get("/models/{model_name}/info")
async def get_model_info(
    model_name: str,
    version: Optional[str] = None,
    ai_gateway = Depends(get_ai_gateway),
):
    """
    Get information about a model.

    Args:
        model_name: Name of the model
        version: Optional version of the model
        ai_gateway: AI Gateway service instance

    Returns:
        Model information
    """
    try:
        # Get model metrics
        metrics = await ai_gateway.monitor.get_model_metrics(
            model_name=model_name,
            hours=24,
        )

        # Get model from router
        if model_name in ai_gateway.router.models:
            versions = ai_gateway.router.models[model_name]
            if version and version in versions:
                instance = versions[version]
                return {
                    "model_name": model_name,
                    "version": version,
                    "status": instance.version.status.value,
                    "endpoint": instance.endpoint,
                    "metrics": {
                        "total_requests": instance.total_requests,
                        "success_rate": instance.success_rate,
                        "average_response_time": instance.average_response_time,
                        "current_connections": instance.connections,
                    },
                    "health": {
                        "healthy": instance.healthy,
                        "last_health_check": instance.last_health_check,
                    },
                }
            else:
                return {
                    "model_name": model_name,
                    "versions": list(versions.keys()),
                    "metrics": metrics,
                }
        else:
            raise HTTPException(
                status_code=404,
                detail=f"Model {model_name} not found",
            )

    except Exception as e:
        logger.error(
            "Get model info error",
            model_name=model_name,
            version=version,
            error=str(e),
        )
        raise HTTPException(
            status_code=500,
            detail="Failed to get model information",
        )


async def log_prediction_metrics(request: AIRequest, response: AIResponse):
    """Background task to log prediction metrics."""
    try:
        logger.info(
            "Prediction metrics recorded",
            request_id=request.request_id,
            model_name=request.model_name,
            model_version=response.model_version,
            latency_ms=response.latency_ms,
            confidence=response.confidence,
        )
    except Exception as e:
        logger.warning(
            "Failed to log prediction metrics",
            error=str(e),
        )


async def log_batch_metrics(batch_request: BatchRequest, batch_response: BatchResponse):
    """Background task to log batch metrics."""
    try:
        logger.info(
            "Batch metrics recorded",
            batch_id=batch_request.batch_id,
            model_name=batch_request.model_name,
            total_requests=batch_response.total_requests,
            successful=batch_response.successful_requests,
            failed=batch_response.failed_requests,
            avg_latency_ms=batch_response.average_latency_ms,
        )
    except Exception as e:
        logger.warning(
            "Failed to log batch metrics",
            error=str(e),
        )