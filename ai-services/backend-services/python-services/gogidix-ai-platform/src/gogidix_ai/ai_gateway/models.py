"""
AI Gateway Data Models

Pydantic models for AI request/response, model metadata,
and routing configuration with enterprise-grade validation.
"""

from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional, Union

from pydantic import BaseModel, Field, validator


class ModelStatus(str, Enum):
    """Model deployment status."""

    LOADING = "loading"
    READY = "ready"
    ERROR = "error"
    UPDATING = "updating"
    DRAINING = "draining"
    DISABLED = "disabled"


class Priority(str, Enum):
    """Request priority levels."""

    LOW = "low"
    NORMAL = "normal"
    HIGH = "high"
    CRITICAL = "critical"


class ModelInfo(BaseModel):
    """Information about an AI model."""

    name: str = Field(..., description="Model name")
    version: str = Field(..., description="Model version")
    description: Optional[str] = Field(None, description="Model description")
    framework: str = Field(..., description="ML framework used")
    model_type: str = Field(..., description="Type of model (classification, regression, etc.)")
    input_schema: Dict[str, Any] = Field(..., description="Input schema")
    output_schema: Dict[str, Any] = Field(..., description="Output schema")
    tags: List[str] = Field(default_factory=list, description="Model tags")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    status: ModelStatus = Field(default=ModelStatus.LOADING)

    # Performance metrics
    accuracy: Optional[float] = Field(None, description="Model accuracy")
    latency_p50: Optional[float] = Field(None, description="50th percentile latency (ms)")
    latency_p95: Optional[float] = Field(None, description="95th percentile latency (ms)")
    throughput: Optional[float] = Field(None, description="Requests per second")

    # Resource requirements
    cpu_request: Optional[str] = Field(None, description="CPU request")
    cpu_limit: Optional[str] = Field(None, description="CPU limit")
    memory_request: Optional[str] = Field(None, description="Memory request")
    memory_limit: Optional[str] = Field(None, description="Memory limit")
    gpu_request: Optional[str] = Field(None, description="GPU request")
    gpu_limit: Optional[str] = Field(None, description="GPU limit")

    class Config:
        use_enum_values = True


class ModelVersion(BaseModel):
    """Model version configuration."""

    version: str = Field(..., description="Version string")
    model_id: str = Field(..., description="Unique model identifier")
    endpoint: str = Field(..., description="Model serving endpoint")
    status: ModelStatus = Field(default=ModelStatus.LOADING)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    is_default: bool = Field(default=False, description="Whether this is the default version")

    # Traffic splitting
    traffic_percentage: float = Field(default=100.0, description="Traffic percentage for A/B testing")

    @validator("traffic_percentage")
    def validate_traffic_percentage(cls, v: float) -> float:
        """Validate traffic percentage is between 0 and 100."""
        if not 0 <= v <= 100:
            raise ValueError("Traffic percentage must be between 0 and 100")
        return v


class RoutingConfig(BaseModel):
    """Routing configuration for model requests."""

    model_name: str = Field(..., description="Target model name")
    version: Optional[str] = Field(None, description="Specific model version")
    fallback_versions: List[str] = Field(
        default_factory=list,
        description="Fallback versions in order of preference"
    )
    load_balancing_strategy: str = Field(
        default="round_robin",
        description="Load balancing strategy"
    )
    timeout_ms: int = Field(default=5000, description="Request timeout in milliseconds")
    retry_attempts: int = Field(default=2, description="Number of retry attempts")
    retry_delay_ms: int = Field(default=100, description="Delay between retries in milliseconds")

    # Batch processing
    enable_batching: bool = Field(default=False, description="Enable request batching")
    max_batch_size: int = Field(default=32, description="Maximum batch size")
    batch_timeout_ms: int = Field(default=10, description="Maximum wait time for batch")

    # Caching
    enable_cache: bool = Field(default=False, description="Enable response caching")
    cache_ttl_seconds: int = Field(default=300, description="Cache TTL in seconds")

    class Config:
        use_enum_values = True


class AIRequest(BaseModel):
    """AI inference request."""

    request_id: str = Field(..., description="Unique request identifier")
    model_name: str = Field(..., description="Target model name")
    model_version: Optional[str] = Field(None, description="Specific model version")
    input_data: Dict[str, Any] = Field(..., description="Input data for model")
    parameters: Optional[Dict[str, Any]] = Field(
        default_factory=dict,
        description="Additional model parameters"
    )
    priority: Priority = Field(default=Priority.NORMAL, description="Request priority")
    timeout_ms: Optional[int] = Field(None, description="Request timeout override")
    correlation_id: Optional[str] = Field(None, description="Correlation ID for tracing")
    user_id: Optional[str] = Field(None, description="User identifier")
    session_id: Optional[str] = Field(None, description="Session identifier")

    # Metadata
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    source: Optional[str] = Field(None, description="Request source")
    tags: List[str] = Field(default_factory=list, description="Request tags")

    class Config:
        use_enum_values = True


class AIResponse(BaseModel):
    """AI inference response."""

    request_id: str = Field(..., description="Original request ID")
    model_name: str = Field(..., description="Model used for inference")
    model_version: str = Field(..., description="Model version used")
    output_data: Dict[str, Any] = Field(..., description="Model output")
    confidence: Optional[float] = Field(None, description="Prediction confidence score")
    explanation: Optional[Dict[str, Any]] = Field(None, description="Model explanation")

    # Performance metrics
    latency_ms: float = Field(..., description="Total latency in milliseconds")
    inference_time_ms: Optional[float] = Field(None, description="Model inference time")
    preprocessing_time_ms: Optional[float] = Field(None, description="Preprocessing time")
    postprocessing_time_ms: Optional[float] = Field(None, description="Postprocessing time")

    # Metadata
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    server_id: Optional[str] = Field(None, description="Server that processed the request")
    cached: bool = Field(default=False, description="Whether response was cached")
    batch_id: Optional[str] = Field(None, description="Batch identifier if batched")

    class Config:
        use_enum_values = True


class BatchRequest(BaseModel):
    """Batch AI inference request."""

    batch_id: str = Field(..., description="Unique batch identifier")
    requests: List[AIRequest] = Field(..., description="Requests in batch")
    model_name: str = Field(..., description="Target model name for all requests")
    model_version: Optional[str] = Field(None, description="Specific model version")
    max_wait_ms: int = Field(default=100, description="Maximum wait time for batch completion")

    @validator("requests")
    def validate_requests(cls, v: List[AIRequest]) -> List[AIRequest]:
        """Validate all requests are for the same model."""
        if not v:
            raise ValueError("Batch cannot be empty")
        return v


class BatchResponse(BaseModel):
    """Batch AI inference response."""

    batch_id: str = Field(..., description="Original batch ID")
    responses: List[AIResponse] = Field(..., description="Response for each request")
    total_requests: int = Field(..., description="Total number of requests")
    successful_requests: int = Field(..., description="Number of successful requests")
    failed_requests: int = Field(..., description="Number of failed requests")
    total_latency_ms: float = Field(..., description="Total batch processing time")
    average_latency_ms: float = Field(..., description="Average latency per request")

    @validator("responses")
    def validate_response_count(cls, v: List[AIResponse], values: Dict[str, Any]) -> List[AIResponse]:
        """Validate response count matches request count."""
        if "total_requests" in values and len(v) != values["total_requests"]:
            raise ValueError("Response count must match request count")
        return v


class HealthCheck(BaseModel):
    """Health check response."""

    status: str = Field(..., description="Service status")
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    version: str = Field(..., description="Service version")
    uptime_seconds: float = Field(..., description="Service uptime in seconds")
    models_loaded: int = Field(..., description="Number of loaded models")
    requests_processed: int = Field(..., description="Total requests processed")
    error_rate: float = Field(..., description="Current error rate")
    details: Optional[Dict[str, Any]] = Field(None, description="Additional health details")


class Metrics(BaseModel):
    """Service metrics."""

    timestamp: datetime = Field(default_factory=datetime.utcnow)
    total_requests: int = Field(default=0)
    successful_requests: int = Field(default=0)
    failed_requests: int = Field(default=0)
    average_latency_ms: float = Field(default=0.0)
    p95_latency_ms: float = Field(default=0.0)
    p99_latency_ms: float = Field(default=0.0)
    requests_per_second: float = Field(default=0.0)
    cpu_usage_percent: float = Field(default=0.0)
    memory_usage_percent: float = Field(default=0.0)
    gpu_usage_percent: Optional[float] = Field(None)
    active_models: int = Field(default=0)

    @property
    def error_rate(self) -> float:
        """Calculate error rate."""
        total = self.total_requests
        if total == 0:
            return 0.0
        return (self.failed_requests / total) * 100

    @property
    def success_rate(self) -> float:
        """Calculate success rate."""
        total = self.total_requests
        if total == 0:
            return 0.0
        return (self.successful_requests / total) * 100


class TrafficSplitConfig(BaseModel):
    """Traffic split configuration for A/B testing."""

    model_name: str = Field(..., description="Model name")
    versions: Dict[str, float] = Field(..., description="Version to traffic percentage mapping")
    default_version: str = Field(..., description="Default version if no match")

    @validator("versions")
    def validate_traffic_percentages(cls, v: Dict[str, float]) -> Dict[str, float]:
        """Validate traffic percentages sum to 100."""
        total = sum(v.values())
        if abs(total - 100.0) > 0.01:  # Allow small floating point errors
            raise ValueError(f"Traffic percentages must sum to 100, got {total}")
        return v

    @validator("default_version")
    def validate_default_version_exists(cls, v: str, values: Dict[str, Any]) -> str:
        """Validate default version exists in versions."""
        if "versions" in values and v not in values["versions"]:
            raise ValueError(f"Default version {v} not found in versions")
        return v