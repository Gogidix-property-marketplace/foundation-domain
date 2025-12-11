"""
AI Gateway Service

Core service for AI model orchestration, request routing,
and enterprise-scale AI operations.
"""

import asyncio
import time
from typing import Dict, List, Optional, Any, Union
from datetime import datetime

import httpx
import prometheus_client
from prometheus_client import Counter, Histogram, Gauge

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger
from gogidix_ai.core.exceptions import (
    AIServiceError,
    ModelNotFoundError,
    ModelPredictionError,
    TimeoutError,
    ValidationError,
)

from .models import (
    AIRequest,
    AIResponse,
    BatchRequest,
    BatchResponse,
    HealthCheck,
    Metrics,
    ModelInfo,
    ModelVersion,
    RoutingConfig,
    TrafficSplitConfig,
)
from .router import ModelRouter, LoadBalancingStrategy
from .cache import ModelCache
from .monitoring import ModelMonitor

logger = get_logger(__name__)


class AIGatewayService:
    """Enterprise AI Gateway Service."""

    def __init__(self):
        self.settings = get_settings()
        self.router = ModelRouter()
        self.cache = ModelCache()
        self.monitor = ModelMonitor()
        self.http_client: Optional[httpx.AsyncClient] = None
        self.start_time = time.time()

        # Prometheus metrics
        self._init_metrics()

    def _init_metrics(self):
        """Initialize Prometheus metrics."""
        # Request metrics
        self.requests_total = Counter(
            'ai_gateway_requests_total',
            'Total number of AI requests',
            ['model_name', 'model_version', 'status']
        )

        self.request_duration = Histogram(
            'ai_gateway_request_duration_seconds',
            'Request processing duration',
            ['model_name', 'model_version'],
            buckets=[0.01, 0.05, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0]
        )

        self.batch_requests_total = Counter(
            'ai_gateway_batch_requests_total',
            'Total number of batch requests',
            ['model_name', 'batch_size']
        )

        # Model metrics
        self.model_predictions_total = Counter(
            'ai_model_predictions_total',
            'Total model predictions',
            ['model_name', 'model_version']
        )

        self.model_prediction_errors = Counter(
            'ai_model_prediction_errors_total',
            'Total model prediction errors',
            ['model_name', 'model_version', 'error_type']
        )

        self.model_response_time = Histogram(
            'ai_model_response_time_seconds',
            'Model inference response time',
            ['model_name', 'model_version'],
            buckets=[0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0]
        )

        # Cache metrics
        self.cache_hits_total = Counter(
            'ai_gateway_cache_hits_total',
            'Total cache hits',
            ['model_name']
        )

        self.cache_misses_total = Counter(
            'ai_gateway_cache_misses_total',
            'Total cache misses',
            ['model_name']
        )

        # System metrics
        self.active_models_gauge = Gauge(
            'ai_gateway_active_models',
            'Number of active models'
        )

    async def initialize(self) -> None:
        """Initialize the AI Gateway service."""
        logger.info("Initializing AI Gateway Service")

        # Initialize HTTP client
        timeout = httpx.Timeout(
            connect=5.0,
            read=self.settings.DEFAULT_MODEL_TIMEOUT / 1000.0,
            write=5.0,
            pool=30.0,
        )
        limits = httpx.Limits(
            max_keepalive_connections=100,
            max_connections=1000,
        )

        self.http_client = httpx.AsyncClient(
            timeout=timeout,
            limits=limits,
            http2=True,
        )

        # Initialize cache
        await self.cache.initialize()

        # Initialize monitoring
        await self.monitor.initialize()

        logger.info("AI Gateway Service initialized successfully")

    async def shutdown(self) -> None:
        """Shutdown the AI Gateway service."""
        logger.info("Shutting down AI Gateway Service")

        if self.http_client:
            await self.http_client.aclose()

        await self.cache.close()
        await self.monitor.close()

        logger.info("AI Gateway Service shutdown complete")

    async def health_check(self) -> HealthCheck:
        """Perform health check."""
        uptime = time.time() - self.start_time
        metrics = await self.get_metrics()

        # Check system health
        is_healthy = (
            metrics.error_rate < 5.0  # Error rate below 5%
            and metrics.average_latency_ms < 5000  # Latency below 5 seconds
        )

        return HealthCheck(
            status="healthy" if is_healthy else "unhealthy",
            version=self.settings.APP_VERSION,
            uptime_seconds=uptime,
            models_loaded=metrics.active_models,
            requests_processed=metrics.total_requests,
            error_rate=metrics.error_rate,
            details={
                "cache": await self.cache.get_stats(),
                "router": await self.router.get_model_metrics(),
            },
        )

    async def get_metrics(self) -> Metrics:
        """Get service metrics."""
        router_metrics = await self.router.get_model_metrics()
        active_models = len(router_metrics)

        total_requests = sum(
            model["total_requests"]
            for model in router_metrics.values()
        )
        successful_requests = sum(
            model["successful_requests"]
            for model in router_metrics.values()
        )
        failed_requests = total_requests - successful_requests

        return Metrics(
            total_requests=total_requests,
            successful_requests=successful_requests,
            failed_requests=failed_requests,
            active_models=active_models,
        )

    async def register_model(
        self,
        model_info: ModelInfo,
        versions: List[ModelVersion],
        routing_config: Optional[RoutingConfig] = None,
    ) -> None:
        """Register a new model."""
        await self.router.register_model(model_info, versions, routing_config)
        await self.monitor.register_model(model_info, versions)
        self.active_models_gauge.set(len(self.router.models))

    async def predict(self, request: AIRequest) -> AIResponse:
        """Process single prediction request."""
        start_time = time.time()

        try:
            # Validate request
            self._validate_request(request)

            # Check cache
            if self.settings.CACHE_TTL_SECONDS > 0:
                cached_response = await self.cache.get(request)
                if cached_response:
                    self.cache_hits_total.labels(model_name=request.model_name).inc()

                    # Update metrics
                    self.requests_total.labels(
                        model_name=request.model_name,
                        model_version=cached_response.model_version,
                        status="cached"
                    ).inc()

                    return cached_response
                else:
                    self.cache_misses_total.labels(model_name=request.model_name).inc()

            # Get model instance
            instance, fallback_version = await self.router.get_model_instance(request)

            # Make prediction
            response = await self._make_prediction(
                request=request,
                instance=instance,
            )

            # Cache response
            if self.settings.CACHE_TTL_SECONDS > 0:
                await self.cache.set(request, response, self.settings.CACHE_TTL_SECONDS)

            # Update metrics
            duration = time.time() - start_time
            self.request_duration.labels(
                model_name=request.model_name,
                model_version=response.model_version,
            ).observe(duration)

            self.requests_total.labels(
                model_name=request.model_name,
                model_version=response.model_version,
                status="success"
            ).inc()

            self.model_predictions_total.labels(
                model_name=request.model_name,
                model_version=response.model_version,
            ).inc()

            # Record metrics with router
            await instance.record_request(
                response_time=response.latency_ms / 1000.0,
                success=True,
            )

            # Update monitoring
            await self.monitor.record_prediction(request, response)

            return response

        except Exception as e:
            # Record error metrics
            self.requests_total.labels(
                model_name=request.model_name,
                model_version="unknown",
                status="error"
            ).inc()

            self.model_prediction_errors.labels(
                model_name=request.model_name,
                model_version="unknown",
                error_type=type(e).__name__,
            ).inc()

            logger.error(
                "Prediction failed",
                request_id=request.request_id,
                model_name=request.model_name,
                error=str(e),
                error_type=type(e).__name__,
            )

            # Try fallback if available
            if fallback_version and fallback_version != request.model_version:
                logger.info(
                    "Attempting fallback prediction",
                    fallback_version=fallback_version,
                )
                request_fallback = AIRequest(
                    **request.dict(),
                    model_version=fallback_version,
                )
                return await self.predict(request_fallback)

            raise ModelPredictionError(
                model_name=request.model_name,
                error_message=str(e),
                request_id=request.request_id,
            )

    async def predict_batch(self, batch_request: BatchRequest) -> BatchResponse:
        """Process batch prediction request."""
        start_time = time.time()
        responses = []

        # Get all instances for the model
        instances = list(self.router.models.get(batch_request.model_name, {}).values())
        if not instances:
            raise ModelNotFoundError(batch_request.model_name)

        # Distribute requests across instances
        batch_size = self.settings.MAX_BATCH_SIZE
        request_chunks = [
            batch_request.requests[i:i + batch_size]
            for i in range(0, len(batch_request.requests), batch_size)
        ]

        successful = 0
        failed = 0

        for chunk in request_chunks:
            try:
                # Create batch payload
                batch_input = {
                    "instances": [req.input_data for req in chunk],
                    "parameters": chunk[0].parameters if chunk else {},
                }

                # Make batch request
                response = await self._make_batch_prediction(
                    model_name=batch_request.model_name,
                    batch_input=batch_input,
                    instances=instances,
                )

                # Create individual responses
                for i, req in enumerate(chunk):
                    try:
                        output = response["predictions"][i]
                        ai_response = AIResponse(
                            request_id=req.request_id,
                            model_name=batch_request.model_name,
                            model_version="batch",
                            output_data=output,
                            latency_ms=0,  # Will be calculated later
                        )
                        responses.append(ai_response)
                        successful += 1
                    except Exception as e:
                        # Create error response
                        ai_response = AIResponse(
                            request_id=req.request_id,
                            model_name=batch_request.model_name,
                            model_version="batch",
                            output_data={"error": str(e)},
                            latency_ms=0,
                        )
                        responses.append(ai_response)
                        failed += 1

            except Exception as e:
                logger.error(
                    "Batch prediction chunk failed",
                    chunk_size=len(chunk),
                    error=str(e),
                )
                # Create error responses for all requests in chunk
                for req in chunk:
                    ai_response = AIResponse(
                        request_id=req.request_id,
                        model_name=batch_request.model_name,
                        model_version="batch",
                        output_data={"error": str(e)},
                        latency_ms=0,
                    )
                    responses.append(ai_response)
                    failed += 1

        # Calculate metrics
        total_latency = (time.time() - start_time) * 1000
        avg_latency = total_latency / len(batch_request.requests)

        # Update response latencies
        for response in responses:
            response.latency_ms = avg_latency

        # Update metrics
        self.batch_requests_total.labels(
            model_name=batch_request.model_name,
            batch_size=len(batch_request.requests),
        ).inc()

        return BatchResponse(
            batch_id=batch_request.batch_id,
            responses=responses,
            total_requests=len(batch_request.requests),
            successful_requests=successful,
            failed_requests=failed,
            total_latency_ms=total_latency,
            average_latency_ms=avg_latency,
        )

    async def _make_prediction(
        self,
        request: AIRequest,
        instance,
    ) -> AIResponse:
        """Make prediction to model instance."""
        if not self.http_client:
            raise AIServiceError("HTTP client not initialized")

        headers = {
            "Content-Type": "application/json",
            "X-Request-ID": request.request_id,
        }

        payload = {
            "instances": [request.input_data],
            "parameters": request.parameters or {},
        }

        # Add authentication if needed
        if hasattr(request, "user_id") and request.user_id:
            headers["X-User-ID"] = request.user_id

        start_time = time.time()

        try:
            response = await self.http_client.post(
                instance.endpoint + ":predict",
                json=payload,
                headers=headers,
            )
            response.raise_for_status()

            result = response.json()
            predictions = result.get("predictions", [])
            if not predictions:
                raise ModelPredictionError(
                    model_name=request.model_name,
                    error_message="No predictions returned",
                    request_id=request.request_id,
                )

            inference_time = (time.time() - start_time) * 1000

            return AIResponse(
                request_id=request.request_id,
                model_name=request.model_name,
                model_version=instance.version.version,
                output_data=predictions[0],
                confidence=result.get("confidence"),
                explanation=result.get("explanation"),
                latency_ms=inference_time,
                inference_time_ms=inference_time,
            )

        except httpx.HTTPStatusError as e:
            raise ModelPredictionError(
                model_name=request.model_name,
                error_message=f"HTTP error: {e.response.status_code} {e.response.text}",
                request_id=request.request_id,
            )
        except httpx.RequestError as e:
            raise ModelPredictionError(
                model_name=request.model_name,
                error_message=f"Request error: {str(e)}",
                request_id=request.request_id,
            )
        except Exception as e:
            raise ModelPredictionError(
                model_name=request.model_name,
                error_message=f"Unexpected error: {str(e)}",
                request_id=request.request_id,
            )

    async def _make_batch_prediction(
        self,
        model_name: str,
        batch_input: Dict[str, Any],
        instances: List,
    ) -> Dict[str, Any]:
        """Make batch prediction request."""
        if not self.http_client:
            raise AIServiceError("HTTP client not initialized")

        # Select instance for batch processing
        instance = instances[0]  # Use first instance

        headers = {
            "Content-Type": "application/json",
        }

        payload = batch_input

        try:
            response = await self.http_client.post(
                instance.endpoint + ":predict",
                json=payload,
                headers=headers,
            )
            response.raise_for_status()

            return response.json()

        except httpx.HTTPStatusError as e:
            raise ModelPredictionError(
                model_name=model_name,
                error_message=f"Batch HTTP error: {e.response.status_code} {e.response.text}",
                request_id="batch",
            )
        except Exception as e:
            raise ModelPredictionError(
                model_name=model_name,
                error_message=f"Batch error: {str(e)}",
                request_id="batch",
            )

    def _validate_request(self, request: AIRequest) -> None:
        """Validate AI request."""
        if not request.request_id:
            raise pydantic.ValidationError("Request ID is required")

        if not request.model_name:
            raise pydantic.ValidationError("Model name is required")

        if not request.input_data:
            raise pydantic.ValidationError("Input data is required")

        # Validate timeout
        timeout = request.timeout_ms or self.settings.DEFAULT_MODEL_TIMEOUT
        if timeout <= 0:
            raise pydantic.ValidationError("Timeout must be positive")

    def format_prometheus_metrics(self, metrics: Metrics) -> str:
        """Format metrics for Prometheus."""
        # Generate Prometheus metrics text
        output = []

        # Custom metrics
        output.append(f"# HELP ai_gateway_uptime_seconds Service uptime")
        output.append(f"# TYPE ai_gateway_uptime_seconds gauge")
        output.append(f"ai_gateway_uptime_seconds {metrics.timestamp.timestamp()}")

        output.append(f"# HELP ai_gateway_total_requests Total requests processed")
        output.append(f"# TYPE ai_gateway_total_requests counter")
        output.append(f"ai_gateway_total_requests {metrics.total_requests}")

        output.append(f"# HELP ai_gateway_error_rate Error rate percentage")
        output.append(f"# TYPE ai_gateway_error_rate gauge")
        output.append(f"ai_gateway_error_rate {metrics.error_rate}")

        return "\n".join(output)