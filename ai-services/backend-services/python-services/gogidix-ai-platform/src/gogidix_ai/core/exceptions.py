"""
Enterprise Exception Handling

Custom exceptions with structured error responses,
error codes, and proper error tracking.
"""

from typing import Any, Dict, List, Optional, Union

from pydantic import BaseModel


class ErrorDetail(BaseModel):
    """Detailed error information."""

    code: str
    message: str
    field: Optional[str] = None
    resource: Optional[str] = None
    details: Optional[Dict[str, Any]] = None


class ErrorResponse(BaseModel):
    """Standardized error response format."""

    error: str
    message: str
    error_code: str
    status_code: int
    details: Optional[List[ErrorDetail]] = None
    timestamp: str
    request_id: Optional[str] = None
    correlation_id: Optional[str] = None


class AIServiceError(Exception):
    """Base exception for all AI services."""

    def __init__(
        self,
        message: str,
        error_code: str = "AI_000",
        status_code: int = 500,
        details: Optional[Union[Dict[str, Any], List[ErrorDetail]]] = None,
        request_id: Optional[str] = None,
    ):
        self.message = message
        self.error_code = error_code
        self.status_code = status_code
        self.details = details
        self.request_id = request_id
        super().__init__(message)

    def to_response(self) -> ErrorResponse:
        """Convert to standardized error response."""
        from datetime import datetime

        # Convert details to proper format
        formatted_details = None
        if isinstance(self.details, dict):
            formatted_details = [
                ErrorDetail(
                    code=self.error_code,
                    message=self.message,
                    details=self.details,
                )
            ]
        elif isinstance(self.details, list):
            formatted_details = self.details

        return ErrorResponse(
            error=self.__class__.__name__,
            message=self.message,
            error_code=self.error_code,
            status_code=self.status_code,
            details=formatted_details,
            timestamp=datetime.utcnow().isoformat(),
            request_id=self.request_id,
        )


class ModelNotFoundError(AIServiceError):
    """Raised when requested model is not found."""

    def __init__(
        self,
        model_name: str,
        version: Optional[str] = None,
        request_id: Optional[str] = None,
    ):
        message = f"Model '{model_name}'"
        if version:
            message += f" version '{version}'"
        message += " not found"

        super().__init__(
            message=message,
            error_code="AI_001",
            status_code=404,
            details={
                "model_name": model_name,
                "version": version,
            },
            request_id=request_id,
        )


class ModelPredictionError(AIServiceError):
    """Raised when model prediction fails."""

    def __init__(
        self,
        model_name: str,
        error_message: str,
        request_id: Optional[str] = None,
    ):
        super().__init__(
            message=f"Model '{model_name}' prediction failed: {error_message}",
            error_code="AI_002",
            status_code=500,
            details={
                "model_name": model_name,
                "original_error": error_message,
            },
            request_id=request_id,
        )


class pydantic.ValidationError(AIServiceError):
    """Raised when input validation fails."""

    def __init__(
        self,
        message: str,
        field_errors: Optional[Dict[str, List[str]]] = None,
        request_id: Optional[str] = None,
    ):
        details = None
        if field_errors:
            details = [
                ErrorDetail(
                    code="VALIDATION_ERROR",
                    message=error_msg,
                    field=field,
                    details={"validation_errors": [error_msg]},
                )
                for field, errors in field_errors.items()
                for error_msg in errors
            ]

        super().__init__(
            message=message,
            error_code="AI_003",
            status_code=400,
            details=details,
            request_id=request_id,
        )


class AuthenticationError(AIServiceError):
    """Raised when authentication fails."""

    def __init__(
        self,
        message: str = "Authentication failed",
        request_id: Optional[str] = None,
    ):
        super().__init__(
            message=message,
            error_code="AI_004",
            status_code=401,
            request_id=request_id,
        )


class AuthorizationError(AIServiceError):
    """Raised when authorization fails."""

    def __init__(
        self,
        message: str = "Access denied",
        required_permission: Optional[str] = None,
        request_id: Optional[str] = None,
    ):
        details = None
        if required_permission:
            details = {"required_permission": required_permission}

        super().__init__(
            message=message,
            error_code="AI_005",
            status_code=403,
            details=details,
            request_id=request_id,
        )


class RateLimitError(AIServiceError):
    """Raised when rate limit is exceeded."""

    def __init__(
        self,
        message: str = "Rate limit exceeded",
        retry_after: Optional[int] = None,
        limit: Optional[int] = None,
        window: Optional[int] = None,
        request_id: Optional[str] = None,
    ):
        details = {}
        if retry_after:
            details["retry_after"] = retry_after
        if limit:
            details["limit"] = limit
        if window:
            details["window"] = window

        super().__init__(
            message=message,
            error_code="AI_006",
            status_code=429,
            details=details if details else None,
            request_id=request_id,
        )


class ResourceExhaustedError(AIServiceError):
    """Raised when system resources are exhausted."""

    def __init__(
        self,
        resource_type: str,
        message: Optional[str] = None,
        request_id: Optional[str] = None,
    ):
        if not message:
            message = f"Resource exhausted: {resource_type}"

        super().__init__(
            message=message,
            error_code="AI_007",
            status_code=503,
            details={"resource_type": resource_type},
            request_id=request_id,
        )


class ServiceUnavailableError(AIServiceError):
    """Raised when a service is temporarily unavailable."""

    def __init__(
        self,
        service_name: str,
        message: Optional[str] = None,
        retry_after: Optional[int] = None,
        request_id: Optional[str] = None,
    ):
        if not message:
            message = f"Service '{service_name}' is temporarily unavailable"

        details = {"service_name": service_name}
        if retry_after:
            details["retry_after"] = retry_after

        super().__init__(
            message=message,
            error_code="AI_008",
            status_code=503,
            details=details,
            request_id=request_id,
        )


class TimeoutError(AIServiceError):
    """Raised when operation times out."""

    def __init__(
        self,
        operation: str,
        timeout_seconds: float,
        request_id: Optional[str] = None,
    ):
        super().__init__(
            message=f"Operation '{operation}' timed out after {timeout_seconds} seconds",
            error_code="AI_009",
            status_code=408,
            details={
                "operation": operation,
                "timeout_seconds": timeout_seconds,
            },
            request_id=request_id,
        )


class ConfigurationError(AIServiceError):
    """Raised when there's a configuration error."""

    def __init__(
        self,
        message: str,
        config_key: Optional[str] = None,
        request_id: Optional[str] = None,
    ):
        details = None
        if config_key:
            details = {"config_key": config_key}

        super().__init__(
            message=message,
            error_code="AI_010",
            status_code=500,
            details=details,
            request_id=request_id,
        )


class DataQualityError(AIServiceError):
    """Raised when data quality issues are detected."""

    def __init__(
        self,
        message: str,
        data_issues: Optional[Dict[str, Any]] = None,
        request_id: Optional[str] = None,
    ):
        super().__init__(
            message=message,
            error_code="AI_011",
            status_code=422,
            details=data_issues,
            request_id=request_id,
        )


class ModelDriftError(AIServiceError):
    """Raised when model drift is detected."""

    def __init__(
        self,
        model_name: str,
        drift_score: float,
        threshold: float,
        request_id: Optional[str] = None,
    ):
        super().__init__(
            message=f"Model '{model_name}' drift detected: score {drift_score} exceeds threshold {threshold}",
            error_code="AI_012",
            status_code=500,
            details={
                "model_name": model_name,
                "drift_score": drift_score,
                "threshold": threshold,
            },
            request_id=request_id,
        )


class BiasDetectionError(AIServiceError):
    """Raised when bias is detected in model predictions."""

    def __init__(
        self,
        model_name: str,
        bias_metrics: Dict[str, float],
        threshold: float,
        request_id: Optional[str] = None,
    ):
        super().__init__(
            message=f"Bias detected in model '{model_name}': metrics exceed threshold {threshold}",
            error_code="AI_013",
            status_code=500,
            details={
                "model_name": model_name,
                "bias_metrics": bias_metrics,
                "threshold": threshold,
            },
            request_id=request_id,
        )


class EthicalAIError(AIServiceError):
    """Raised when ethical AI violations are detected."""

    def __init__(
        self,
        message: str,
        violation_type: str,
        severity: str = "medium",
        recommendations: Optional[List[str]] = None,
        request_id: Optional[str] = None,
    ):
        super().__init__(
            message=message,
            error_code="AI_014",
            status_code=422,
            details={
                "violation_type": violation_type,
                "severity": severity,
                "recommendations": recommendations or [],
            },
            request_id=request_id,
        )


class ComplianceError(AIServiceError):
    """Raised when compliance requirements are not met."""

    def __init__(
        self,
        message: str,
        standard: str,
        requirement: str,
        remediation: Optional[str] = None,
        request_id: Optional[str] = None,
    ):
        super().__init__(
            message=f"Compliance violation for {standard}: {message}",
            error_code="AI_015",
            status_code=422,
            details={
                "standard": standard,
                "requirement": requirement,
                "remediation": remediation,
            },
            request_id=request_id,
        )


# Error code mapping for quick reference
ERROR_CODE_MAP = {
    "AI_000": AIServiceError,
    "AI_001": ModelNotFoundError,
    "AI_002": ModelPredictionError,
    "AI_003": ValidationError,
    "AI_004": AuthenticationError,
    "AI_005": AuthorizationError,
    "AI_006": RateLimitError,
    "AI_007": ResourceExhaustedError,
    "AI_008": ServiceUnavailableError,
    "AI_009": TimeoutError,
    "AI_010": ConfigurationError,
    "AI_011": DataQualityError,
    "AI_012": ModelDriftError,
    "AI_013": BiasDetectionError,
    "AI_014": EthicalAIError,
    "AI_015": ComplianceError,
}


def create_error_response(
    error_code: str,
    message: str,
    details: Optional[Dict[str, Any]] = None,
    request_id: Optional[str] = None,
) -> ErrorResponse:
    """Create error response from error code."""
    from datetime import datetime

    error_class = ERROR_CODE_MAP.get(error_code, AIServiceError)
    error = error_class(message=message, details=details, request_id=request_id)
    return error.to_response()