"""
AI Model Router

Enterprise-grade model routing with load balancing,
version management, and intelligent traffic distribution.
"""

import asyncio
import random
import time
from typing import Dict, List, Optional, Tuple, Any
from enum import Enum

from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger
from gogidix_ai.core.exceptions import (
    ModelNotFoundError,
    ServiceUnavailableError,
    TimeoutError,
)
from gogidix_ai.ai_gateway.models import (
    ModelInfo,
    ModelVersion,
    ModelStatus,
    RoutingConfig,
    AIRequest,
    AIResponse,
    TrafficSplitConfig,
)

logger = get_logger(__name__)


class LoadBalancingStrategy(str, Enum):
    """Load balancing strategies."""

    ROUND_ROBIN = "round_robin"
    LEAST_CONNECTIONS = "least_connections"
    RANDOM = "random"
    WEIGHTED_ROUND_ROBIN = "weighted_round_robin"
    RESPONSE_TIME = "response_time"


class ModelInstance:
    """Represents a single model instance/endpoint."""

    def __init__(
        self,
        version: ModelVersion,
        endpoint: str,
        weight: float = 1.0,
    ):
        self.version = version
        self.endpoint = endpoint
        self.weight = weight
        self.connections = 0
        self.total_requests = 0
        self.successful_requests = 0
        self.total_response_time = 0.0
        self.last_response_time = 0.0
        self.healthy = True
        self.last_health_check = 0.0
        self._lock = asyncio.Lock()

    @property
    def success_rate(self) -> float:
        """Calculate success rate."""
        if self.total_requests == 0:
            return 100.0
        return (self.successful_requests / self.total_requests) * 100

    @property
    def average_response_time(self) -> float:
        """Calculate average response time."""
        if self.successful_requests == 0:
            return 0.0
        return self.total_response_time / self.successful_requests

    async def record_request(self, response_time: float, success: bool):
        """Record request metrics."""
        async with self._lock:
            self.total_requests += 1
            self.last_response_time = response_time

            if success:
                self.successful_requests += 1
                self.total_response_time += response_time


class ModelRouter:
    """Enterprise model router with advanced routing capabilities."""

    def __init__(self):
        self.settings = get_settings()
        self.models: Dict[str, Dict[str, ModelInstance]] = {}
        self.model_configs: Dict[str, RoutingConfig] = {}
        self.traffic_splits: Dict[str, TrafficSplitConfig] = {}
        self._round_robin_counters: Dict[str, int] = {}
        self._locks: Dict[str, asyncio.Lock] = {}

    async def register_model(
        self,
        model_info: ModelInfo,
        versions: List[ModelVersion],
        routing_config: Optional[RoutingConfig] = None,
    ) -> None:
        """Register a model with its versions."""
        model_name = model_info.name

        # Initialize model registry
        if model_name not in self.models:
            self.models[model_name] = {}
            self._locks[model_name] = asyncio.Lock()
            self._round_robin_counters[model_name] = 0

        async with self._locks[model_name]:
            # Register versions
            for version_info in versions:
                instance = ModelInstance(
                    version=version_info,
                    endpoint=version_info.endpoint,
                    weight=version_info.traffic_percentage / 100.0,
                )
                self.models[model_name][version_info.version] = instance

                logger.info(
                    "Model version registered",
                    model_name=model_name,
                    version=version_info.version,
                    endpoint=version_info.endpoint,
                    traffic_percentage=version_info.traffic_percentage,
                )

            # Store routing config
            if routing_config:
                self.model_configs[model_name] = routing_config

            logger.info(
                "Model registration complete",
                model_name=model_name,
                versions=len(versions),
            )

    async def deregister_model(
        self,
        model_name: str,
        version: Optional[str] = None,
    ) -> None:
        """Deregister a model or specific version."""
        if model_name not in self.models:
            raise ModelNotFoundError(model_name, version)

        async with self._locks[model_name]:
            if version:
                # Remove specific version
                if version in self.models[model_name]:
                    del self.models[model_name][version]
                    logger.info(
                        "Model version deregistered",
                        model_name=model_name,
                        version=version,
                    )
                else:
                    raise ModelNotFoundError(model_name, version)
            else:
                # Remove all versions
                del self.models[model_name]
                del self._locks[model_name]
                del self._round_robin_counters[model_name]
                logger.info(
                    "Model deregistered",
                    model_name=model_name,
                )

    async def get_model_instance(
        self,
        request: AIRequest,
    ) -> Tuple[ModelInstance, Optional[str]]:
        """Get the best model instance for the request."""
        model_name = request.model_name
        requested_version = request.model_version

        if model_name not in self.models:
            raise ModelNotFoundError(model_name)

        versions = self.models[model_name]
        if not versions:
            raise ServiceUnavailableError(model_name)

        # Get routing config
        config = self.model_configs.get(model_name)
        strategy = (
            config.load_balancing_strategy
            if config
            else LoadBalancingStrategy.ROUND_ROBIN
        )

        # Handle specific version request
        if requested_version and requested_version in versions:
            return versions[requested_version], None

        # Check for traffic split configuration
        if model_name in self.traffic_splits:
            instance = await self._select_by_traffic_split(model_name)
            if instance:
                return instance, None

        # Select instance based on load balancing strategy
        return await self._select_by_strategy(model_name, strategy, versions)

    async def _select_by_traffic_split(self, model_name: str) -> Optional[ModelInstance]:
        """Select instance based on traffic split configuration."""
        split_config = self.traffic_splits[model_name]
        total = sum(split_config.versions.values())
        if total != 100.0:
            logger.warning(
                "Traffic split percentages don't sum to 100",
                model_name=model_name,
                total=total,
            )

        # Generate random number for selection
        rand = random.uniform(0, 100)
        cumulative = 0.0

        for version, percentage in split_config.versions.items():
            cumulative += percentage
            if rand <= cumulative:
                versions = self.models[model_name]
                if version in versions:
                    return versions[version]

        # Fallback to default version
        versions = self.models[model_name]
        if split_config.default_version in versions:
            return versions[split_config.default_version]

        return None

    async def _select_by_strategy(
        self,
        model_name: str,
        strategy: LoadBalancingStrategy,
        versions: Dict[str, ModelInstance],
    ) -> Tuple[ModelInstance, Optional[str]]:
        """Select instance based on load balancing strategy."""
        healthy_instances = [
            (version, instance)
            for version, instance in versions.items()
            if instance.healthy and instance.version.status == ModelStatus.READY
        ]

        if not healthy_instances:
            # Try to use any instance as fallback
            healthy_instances = list(versions.items())
            if not healthy_instances:
                raise ServiceUnavailableError(model_name)

        if strategy == LoadBalancingStrategy.ROUND_ROBIN:
            return self._round_robin_select(model_name, healthy_instances)

        elif strategy == LoadBalancingStrategy.LEAST_CONNECTIONS:
            return self._least_connections_select(healthy_instances)

        elif strategy == LoadBalancingStrategy.RANDOM:
            return random.choice(healthy_instances)

        elif strategy == LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN:
            return self._weighted_round_robin_select(
                model_name, healthy_instances
            )

        elif strategy == LoadBalancingStrategy.RESPONSE_TIME:
            return self._response_time_select(healthy_instances)

        else:
            # Default to round robin
            return self._round_robin_select(model_name, healthy_instances)

    def _round_robin_select(
        self,
        model_name: str,
        instances: List[Tuple[str, ModelInstance]],
    ) -> Tuple[ModelInstance, Optional[str]]:
        """Select instance using round robin."""
        counter = self._round_robin_counters[model_name]
        selected_instance = instances[counter % len(instances)]
        self._round_robin_counters[model_name] = counter + 1
        return selected_instance

    def _least_connections_select(
        self,
        instances: List[Tuple[str, ModelInstance]],
    ) -> Tuple[ModelInstance, Optional[str]]:
        """Select instance with least connections."""
        return min(instances, key=lambda x: x[1].connections)

    def _weighted_round_robin_select(
        self,
        model_name: str,
        instances: List[Tuple[str, ModelInstance]],
    ) -> Tuple[ModelInstance, Optional[str]]:
        """Select instance using weighted round robin."""
        # Create weighted list
        weighted_instances = []
        for version, instance in instances:
            weight = int(instance.weight * 100) or 1
            weighted_instances.extend([(version, instance)] * weight)

        # Use round robin on weighted list
        counter = self._round_robin_counters[model_name]
        selected_instance = weighted_instances[
            counter % len(weighted_instances)
        ]
        self._round_robin_counters[model_name] = counter + 1
        return selected_instance

    def _response_time_select(
        self,
        instances: List[Tuple[str, ModelInstance]],
    ) -> Tuple[ModelInstance, Optional[str]]:
        """Select instance with best response time."""
        # Filter to instances with response data
        instances_with_time = [
            (version, instance)
            for version, instance in instances
            if instance.average_response_time > 0
        ]

        if instances_with_time:
            return min(
                instances_with_time,
                key=lambda x: x[1].average_response_time
            )
        else:
            # Fallback to random if no response time data
            return random.choice(instances)

    async def update_model_health(
        self,
        model_name: str,
        version: str,
        healthy: bool,
    ) -> None:
        """Update health status of a model instance."""
        if model_name in self.models and version in self.models[model_name]:
            instance = self.models[model_name][version]
            instance.healthy = healthy
            instance.last_health_check = time.time()

            status = "healthy" if healthy else "unhealthy"
            logger.info(
                "Model health status updated",
                model_name=model_name,
                version=version,
                status=status,
            )

    async def get_model_metrics(
        self,
        model_name: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Get metrics for models."""
        metrics = {}

        if model_name:
            models_to_check = {model_name: self.models.get(model_name, {})}
        else:
            models_to_check = self.models

        for name, versions in models_to_check.items():
            model_metrics = {
                "total_requests": 0,
                "successful_requests": 0,
                "error_rate": 0.0,
                "average_response_time": 0.0,
                "versions": {},
            }

            total_response_time = 0.0
            total_requests = 0

            for version, instance in versions.items():
                version_metrics = {
                    "requests": instance.total_requests,
                    "success_rate": instance.success_rate,
                    "average_response_time": instance.average_response_time,
                    "connections": instance.connections,
                    "healthy": instance.healthy,
                }

                model_metrics["versions"][version] = version_metrics
                model_metrics["total_requests"] += instance.total_requests
                model_metrics["successful_requests"] += instance.successful_requests
                total_response_time += instance.total_response_time
                total_requests += instance.successful_requests

            # Calculate aggregate metrics
            if total_requests > 0:
                model_metrics["error_rate"] = (
                    (total_requests - model_metrics["successful_requests"])
                    / total_requests
                ) * 100
                model_metrics["average_response_time"] = total_response_time / total_requests

            metrics[name] = model_metrics

        return metrics

    def set_traffic_split(self, model_name: str, config: TrafficSplitConfig) -> None:
        """Set traffic split configuration for A/B testing."""
        self.traffic_splits[model_name] = config
        logger.info(
            "Traffic split configuration set",
            model_name=model_name,
            versions=list(config.versions.keys()),
        )

    def remove_traffic_split(self, model_name: str) -> None:
        """Remove traffic split configuration."""
        if model_name in self.traffic_splits:
            del self.traffic_splits[model_name]
            logger.info(
                "Traffic split configuration removed",
                model_name=model_name,
            )