"""
AI Gateway Monitoring

Real-time monitoring, model performance tracking,
and drift detection for AI models.
"""

import asyncio
import time
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime, timedelta
from collections import defaultdict, deque

import numpy as np
from prometheus_client import Gauge, Histogram

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger
from gogidix_ai.core.exceptions import ModelDriftError, BiasDetectionError
from gogidix_ai.ai_gateway.models import (
    AIRequest,
    AIResponse,
    ModelInfo,
    ModelVersion,
    ModelStatus,
)

logger = get_logger(__name__)


class ModelMonitor:
    """Real-time model monitoring and drift detection."""

    def __init__(self):
        self.settings = get_settings()
        self.models: Dict[str, Dict[str, Any]] = {}
        self.prediction_history: Dict[str, deque] = defaultdict(
            lambda: deque(maxlen=1000)
        )
        self.performance_metrics: Dict[str, Dict[str, deque]] = defaultdict(
            lambda: defaultdict(lambda: deque(maxlen=100))
        )
        self._lock = asyncio.Lock()
        self._monitoring_task: Optional[asyncio.Task] = None

        # Prometheus metrics
        self._init_metrics()

    def _init_metrics(self):
        """Initialize monitoring metrics."""
        # Model accuracy gauges
        self.model_accuracy_gauge = Gauge(
            'ai_model_accuracy',
            'Model accuracy score',
            ['model_name', 'model_version']
        )

        # Model drift score
        self.model_drift_gauge = Gauge(
            'ai_model_drift_score',
            'Model drift detection score',
            ['model_name', 'model_version', 'metric']
        )

        # Bias metrics
        self.model_bias_gauge = Gauge(
            'ai_model_bias_score',
            'Model bias detection score',
            ['model_name', 'model_version', 'attribute']
        )

        # Request rate
        self.request_rate_gauge = Gauge(
            'ai_model_request_rate',
            'Request rate per second',
            ['model_name', 'model_version']
        )

    async def initialize(self) -> None:
        """Initialize monitoring service."""
        logger.info("Initializing Model Monitor")

        # Start background monitoring task
        self._monitoring_task = asyncio.create_task(self._monitoring_loop())

        logger.info("Model Monitor initialized")

    async def close(self) -> None:
        """Close monitoring service."""
        if self._monitoring_task:
            self._monitoring_task.cancel()
            try:
                await self._monitoring_task
            except asyncio.CancelledError:
                pass

        logger.info("Model Monitor closed")

    async def register_model(
        self,
        model_info: ModelInfo,
        versions: List[ModelVersion],
    ) -> None:
        """Register a model for monitoring."""
        async with self._lock:
            self.models[model_info.name] = {
                "info": model_info,
                "versions": {v.version: v for v in versions},
                "registered_at": datetime.utcnow(),
                "baseline_metrics": {},
                "drift_threshold": 0.1,  # 10% drift threshold
                "bias_threshold": 0.05,  # 5% bias threshold
            }

            logger.info(
                "Model registered for monitoring",
                model_name=model_info.name,
                versions=len(versions),
            )

    async def record_prediction(
        self,
        request: AIRequest,
        response: AIResponse,
    ) -> None:
        """Record prediction for monitoring."""
        model_key = f"{request.model_name}:{response.model_version}"

        # Store prediction
        prediction_record = {
            "timestamp": datetime.utcnow(),
            "request_id": request.request_id,
            "input_data": request.input_data,
            "output_data": response.output_data,
            "confidence": response.confidence,
            "latency_ms": response.latency_ms,
        }

        self.prediction_history[model_key].append(prediction_record)

        # Update performance metrics
        await self._update_performance_metrics(model_key, prediction_record)

        # Check for drift periodically
        if len(self.prediction_history[model_key]) % 100 == 0:
            await self._check_drift(model_key)

        # Check for bias periodically
        if len(self.prediction_history[model_key]) % 500 == 0:
            await self._check_bias(model_key)

    async def _update_performance_metrics(
        self,
        model_key: str,
        prediction_record: Dict[str, Any],
    ) -> None:
        """Update performance metrics for model."""
        metrics = self.performance_metrics[model_key]

        # Latency metrics
        metrics["latency"].append(prediction_record["latency_ms"])
        metrics["confidence"].append(prediction_record["confidence"] or 0)

        # Calculate rolling averages
        if len(metrics["latency"]) >= 10:
            avg_latency = np.mean(metrics["latency"])
            avg_confidence = np.mean(metrics["confidence"])

            # Update Prometheus metrics
            model_name, model_version = model_key.split(":", 1)
            self.request_rate_gauge.labels(
                model_name=model_name,
                model_version=model_version,
            ).set(len(self.prediction_history[model_key]) / 60)  # requests per minute

    async def _check_drift(self, model_key: str) -> None:
        """Check for model drift."""
        if model_key not in self.models:
            return

        predictions = self.prediction_history[model_key]
        if len(predictions) < 100:  # Need minimum samples
            return

        model_name, model_version = model_key.split(":", 1)

        # Calculate drift metrics
        recent_predictions = list(predictions)[-50:]  # Last 50 predictions
        baseline_predictions = list(predictions)[-100:-50]  # Previous 50

        # Check confidence drift
        recent_confidence = [p["confidence"] or 0 for p in recent_predictions]
        baseline_confidence = [p["confidence"] or 0 for p in baseline_predictions]

        if baseline_confidence and recent_confidence:
            confidence_drift = abs(
                np.mean(recent_confidence) - np.mean(baseline_confidence)
            ) / np.mean(baseline_confidence)

            # Update Prometheus
            self.model_drift_gauge.labels(
                model_name=model_name,
                model_version=model_version,
                metric="confidence",
            ).set(confidence_drift)

            # Check threshold
            if confidence_drift > self.models[model_name]["drift_threshold"]:
                logger.warning(
                    "Model confidence drift detected",
                    model_key=model_key,
                    drift_score=confidence_drift,
                    threshold=self.models[model_key]["drift_threshold"],
                )

                # Could trigger retraining or alert here
                # raise ModelDriftError(model_name, confidence_drift, threshold)

        # Check latency drift
        recent_latency = [p["latency_ms"] for p in recent_predictions]
        baseline_latency = [p["latency_ms"] for p in baseline_predictions]

        if baseline_latency and recent_latency:
            latency_drift = abs(
                np.mean(recent_latency) - np.mean(baseline_latency)
            ) / np.mean(baseline_latency)

            # Update Prometheus
            self.model_drift_gauge.labels(
                model_name=model_name,
                model_version=model_version,
                metric="latency",
            ).set(latency_drift)

    async def _check_bias(self, model_key: str) -> None:
        """Check for model bias in predictions."""
        if model_key not in self.models:
            return

        predictions = self.prediction_history[model_key]
        if len(predictions) < 100:
            return

        model_name, model_version = model_key.split(":", 1)

        # Extract protected attributes from input data
        # This is a simplified example - in practice, you'd have
        # more sophisticated bias detection
        bias_metrics = {}

        # Example: Check if confidence varies by input size
        predictions_by_size = defaultdict(list)
        for pred in predictions:
            input_size = len(str(pred["input_data"]))
            size_bucket = "small" if input_size < 100 else "large"
            predictions_by_size[size_bucket].append(pred["confidence"] or 0)

        # Calculate bias score
        if len(predictions_by_size) > 1:
            confidences_by_size = [
                np.mean(confs) for confs in predictions_by_size.values()
            ]
            bias_score = np.std(confidences_by_size) / np.mean(confidences_by_size)

            # Update Prometheus
            self.model_bias_gauge.labels(
                model_name=model_name,
                model_version=model_version,
                attribute="input_size",
            ).set(bias_score)

            bias_metrics["input_size"] = bias_score

            # Check threshold
            threshold = self.models[model_name]["bias_threshold"]
            if bias_score > threshold:
                logger.warning(
                    "Model bias detected",
                    model_key=model_key,
                    bias_score=bias_score,
                    threshold=threshold,
                    attribute="input_size",
                )

                # Could trigger investigation here
                # raise BiasDetectionError(model_name, bias_metrics, threshold)

    async def _monitoring_loop(self) -> None:
        """Background monitoring loop."""
        while True:
            try:
                await asyncio.sleep(60)  # Check every minute

                # Update metrics for all models
                for model_key in list(self.models.keys()):
                    await self._update_model_metrics(model_key)

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(
                    "Monitoring loop error",
                    error=str(e),
                    error_type=type(e).__name__,
                )

    async def _update_model_metrics(self, model_key: str) -> None:
        """Update metrics for a specific model."""
        if model_key not in self.prediction_history:
            return

        predictions = self.prediction_history[model_key]
        if not predictions:
            return

        model_name, model_version = model_key.split(":", 1)

        # Calculate metrics over last hour
        now = datetime.utcnow()
        hour_ago = now - timedelta(hours=1)
        recent_predictions = [
            p for p in predictions
            if p["timestamp"] > hour_ago
        ]

        if recent_predictions:
            # Calculate average accuracy (if available)
            accuracies = [
                p.get("accuracy", 0)
                for p in recent_predictions
                if "accuracy" in p
            ]
            if accuracies:
                avg_accuracy = np.mean(accuracies)
                self.model_accuracy_gauge.labels(
                    model_name=model_name,
                    model_version=model_version,
                ).set(avg_accuracy)

    async def get_model_metrics(
        self,
        model_name: Optional[str] = None,
        hours: int = 24,
    ) -> Dict[str, Any]:
        """Get metrics for models."""
        cutoff_time = datetime.utcnow() - timedelta(hours=hours)
        metrics = {}

        for model_key, predictions in self.prediction_history.items():
            if model_name and not model_key.startswith(model_name):
                continue

            # Filter by time
            recent_predictions = [
                p for p in predictions
                if p["timestamp"] > cutoff_time
            ]

            if not recent_predictions:
                continue

            # Calculate metrics
            latencies = [p["latency_ms"] for p in recent_predictions]
            confidences = [p["confidence"] or 0 for p in recent_predictions]

            metrics[model_key] = {
                "total_predictions": len(recent_predictions),
                "average_latency_ms": np.mean(latencies),
                "p95_latency_ms": np.percentile(latencies, 95),
                "p99_latency_ms": np.percentile(latencies, 99),
                "average_confidence": np.mean(confidences),
                "min_confidence": np.min(confidences),
                "max_confidence": np.max(confidences),
                "requests_per_hour": len(recent_predictions) / hours,
            }

        return metrics

    async def get_drift_report(
        self,
        model_name: str,
        hours: int = 24,
    ) -> Dict[str, Any]:
        """Get drift analysis for model."""
        model_keys = [
            k for k in self.prediction_history.keys()
            if k.startswith(model_name)
        ]

        if not model_keys:
            return {"error": "Model not found"}

        drift_report = {
            "model_name": model_name,
            "analysis_period_hours": hours,
            "versions": {},
        }

        for model_key in model_keys:
            version = model_key.split(":", 1)[1]
            predictions = self.prediction_history[model_key]

            if len(predictions) < 200:
                drift_report["versions"][version] = {
                    "status": "insufficient_data",
                    "message": "Need at least 200 predictions",
                }
                continue

            # Calculate drift metrics
            recent = list(predictions)[-100:]
            baseline = list(predictions)[-200:-100]

            confidence_drift = self._calculate_distribution_drift(
                [p["confidence"] or 0 for p in baseline],
                [p["confidence"] or 0 for p in recent],
            )

            latency_drift = self._calculate_distribution_drift(
                [p["latency_ms"] for p in baseline],
                [p["latency_ms"] for p in recent],
            )

            drift_status = "stable"
            if confidence_drift > 0.1 or latency_drift > 0.1:
                drift_status = "drift_detected"

            drift_report["versions"][version] = {
                "status": drift_status,
                "confidence_drift": confidence_drift,
                "latency_drift": latency_drift,
                "total_predictions": len(predictions),
            }

        return drift_report

    def _calculate_distribution_drift(
        self,
        baseline: List[float],
        recent: List[float],
    ) -> float:
        """Calculate distribution drift using KL divergence."""
        if not baseline or not recent:
            return 0.0

        # Create histograms
        hist_bins = 10
        baseline_hist, _ = np.histogram(baseline, bins=hist_bins, density=True)
        recent_hist, _ = np.histogram(recent, bins=hist_bins, density=True)

        # Add small epsilon to avoid division by zero
        eps = 1e-10
        baseline_hist = baseline_hist + eps
        recent_hist = recent_hist + eps

        # Normalize
        baseline_hist = baseline_hist / np.sum(baseline_hist)
        recent_hist = recent_hist / np.sum(recent_hist)

        # Calculate KL divergence
        kl_div = np.sum(recent_hist * np.log(recent_hist / baseline_hist))

        return float(kl_div)