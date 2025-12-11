"""
Enterprise Logging Configuration

Structured logging with correlation IDs, performance metrics,
and production-ready log management.
"""

import logging
import sys
import time
import uuid
from contextlib import asynccontextmanager
from contextvars import ContextVar
from typing import Any, Dict, Optional

import structlog
from pythonjsonlogger import jsonlogger

# Context variables for tracing
correlation_id: ContextVar[Optional[str]] = ContextVar(
    "correlation_id", default=None
)
request_id: ContextVar[Optional[str]] = ContextVar(
    "request_id", default=None
)
user_id: ContextVar[Optional[str]] = ContextVar(
    "user_id", default=None
)


class CorrelationIdFilter(logging.Filter):
    """Add correlation ID to log records."""

    def filter(self, record: logging.LogRecord) -> bool:
        """Add correlation ID to log record."""
        record.correlation_id = correlation_id.get()
        record.request_id = request_id.get()
        record.user_id = user_id.get()
        return True


class JSONFormatter(jsonlogger.JsonFormatter):
    """Custom JSON formatter with enhanced fields."""

    def add_fields(
        self,
        log_record: Dict[str, Any],
        record: logging.LogRecord,
        message_dict: Dict[str, Any],
    ) -> None:
        """Add custom fields to log record."""
        super().add_fields(log_record, record, message_dict)

        # Add timestamp if not present
        if "timestamp" not in log_record:
            log_record["timestamp"] = time.time()

        # Add level
        if "level" not in log_record:
            log_record["level"] = record.levelname

        # Add correlation IDs
        if hasattr(record, "correlation_id") and record.correlation_id:
            log_record["correlation_id"] = record.correlation_id
        if hasattr(record, "request_id") and record.request_id:
            log_record["request_id"] = record.request_id
        if hasattr(record, "user_id") and record.user_id:
            log_record["user_id"] = record.user_id

        # Add service information
        log_record["service"] = "gogidix-ai"
        log_record["version"] = "1.0.0"

        # Add module information
        if "module" not in log_record:
            log_record["module"] = record.module
        if "function" not in log_record:
            log_record["function"] = record.funcName
        if "line_number" not in log_record:
            log_record["line_number"] = record.lineno


def setup_logging(
    log_level: str = "INFO",
    log_format: str = "json",
    enable_console: bool = True,
    enable_file: bool = False,
    log_file: Optional[str] = None,
) -> None:
    """Setup structured logging for the application."""

    # Set log level
    numeric_level = getattr(logging, log_level.upper(), logging.INFO)

    # Create root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(numeric_level)

    # Remove default handlers
    root_logger.handlers.clear()

    # Add correlation ID filter
    correlation_filter = CorrelationIdFilter()

    # Setup console handler
    if enable_console:
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(numeric_level)
        console_handler.addFilter(correlation_filter)

        if log_format.lower() == "json":
            console_formatter = JSONFormatter(
                "%(timestamp)s %(level)s %(name)s %(message)s"
            )
        else:
            console_formatter = logging.Formatter(
                "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
            )

        console_handler.setFormatter(console_formatter)
        root_logger.addHandler(console_handler)

    # Setup file handler
    if enable_file and log_file:
        file_handler = logging.FileHandler(log_file)
        file_handler.setLevel(numeric_level)
        file_handler.addFilter(correlation_filter)

        file_formatter = JSONFormatter(
            "%(timestamp)s %(level)s %(name)s %(message)s"
        )
        file_handler.setFormatter(file_formatter)
        root_logger.addHandler(file_handler)

    # Configure structlog
    processors = [
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
    ]

    if log_format.lower() == "json":
        processors.append(structlog.processors.JSONRenderer())
    else:
        processors.append(structlog.dev.ConsoleRenderer())

    structlog.configure(
        processors=processors,
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        wrapper_class=structlog.stdlib.BoundLogger,
        cache_logger_on_first_use=True,
    )

    # Reduce noise from third-party libraries
    logging.getLogger("uvicorn").setLevel(logging.WARNING)
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("kafka").setLevel(logging.WARNING)
    logging.getLogger("elasticsearch").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)


def get_logger(name: str) -> structlog.BoundLogger:
    """Get a structured logger with the given name."""
    return structlog.get_logger(name)


@asynccontextmanager
async def log_context(
    correlation_id_value: Optional[str] = None,
    request_id_value: Optional[str] = None,
    user_id_value: Optional[str] = None,
):
    """Context manager for log correlation IDs."""
    # Set correlation ID
    cid = correlation_id_value or str(uuid.uuid4())
    correlation_token = correlation_id.set(cid)

    # Set request ID
    request_token = request_id.set(request_id_value)

    # Set user ID
    user_token = user_id.set(user_id_value)

    try:
        yield cid
    finally:
        # Reset context variables
        correlation_id.reset(correlation_token)
        request_id.reset(request_token)
        user_id.reset(user_token)


class LoggerMixin:
    """Mixin class to add logging capabilities."""

    @property
    def logger(self) -> structlog.BoundLogger:
        """Get logger for this class."""
        return get_logger(self.__class__.__name__)

    def log_info(
        self,
        message: str,
        **kwargs: Any,
    ) -> None:
        """Log info message with context."""
        self.logger.info(message, **kwargs)

    def log_error(
        self,
        message: str,
        error: Optional[Exception] = None,
        **kwargs: Any,
    ) -> None:
        """Log error message with context."""
        if error:
            kwargs["error"] = str(error)
            kwargs["error_type"] = type(error).__name__
        self.logger.error(message, **kwargs)

    def log_warning(
        self,
        message: str,
        **kwargs: Any,
    ) -> None:
        """Log warning message with context."""
        self.logger.warning(message, **kwargs)

    def log_debug(
        self,
        message: str,
        **kwargs: Any,
    ) -> None:
        """Log debug message with context."""
        self.logger.debug(message, **kwargs)


class PerformanceLogger:
    """Logger for performance metrics."""

    def __init__(self, logger: structlog.BoundLogger):
        self.logger = logger
        self.start_time: Optional[float] = None

    def start(self, operation: str) -> None:
        """Start performance measurement."""
        self.start_time = time.time()
        self.logger.debug(
            "Operation started",
            operation=operation,
            start_time=self.start_time,
        )

    def end(self, operation: str, **kwargs: Any) -> None:
        """End performance measurement and log duration."""
        if self.start_time:
            duration = time.time() - self.start_time
            self.logger.info(
                "Operation completed",
                operation=operation,
                duration_seconds=duration,
                **kwargs,
            )
            self.start_time = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.start_time and exc_type:
            duration = time.time() - self.start_time
            self.logger.error(
                "Operation failed",
                duration_seconds=duration,
                error_type=exc_type.__name__ if exc_type else None,
                error=str(exc_val) if exc_val else None,
            )


def log_function_call(func):
    """Decorator to log function calls with arguments and return values."""
    def wrapper(*args, **kwargs):
        logger = get_logger(func.__module__)

        # Log function start
        logger.debug(
            "Function called",
            function=func.__name__,
            args_count=len(args),
            kwargs=list(kwargs.keys()),
        )

        try:
            result = func(*args, **kwargs)

            # Log successful completion
            logger.debug(
                "Function completed",
                function=func.__name__,
                result_type=type(result).__name__,
            )

            return result

        except Exception as e:
            # Log error
            logger.error(
                "Function failed",
                function=func.__name__,
                error_type=type(e).__name__,
                error=str(e),
            )
            raise

    return wrapper


# Initialize logging with default configuration
setup_logging()