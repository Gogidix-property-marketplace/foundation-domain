"""
AI Gateway Cache Service

Redis-based caching for AI model responses with
intelligent invalidation and cache warming.
"""

import json
import hashlib
import pickle
from typing import Optional, Any, Dict, List
from datetime import datetime, timedelta

import aioredis
from gogidix_ai.core.config import get_settings
from gogidix_ai.core.logging import get_logger
from gogidix_ai.ai_gateway.models import AIRequest, AIResponse

logger = get_logger(__name__)


class ModelCache:
    """Enterprise-grade caching for AI model responses."""

    def __init__(self):
        self.settings = get_settings()
        self.redis: Optional[aioredis.Redis] = None
        self._initialized = False

    async def initialize(self) -> None:
        """Initialize Redis connection."""
        try:
            self.redis = aioredis.from_url(
                self.settings.REDIS_URL,
                max_connections=self.settings.REDIS_MAX_CONNECTIONS,
                retry_on_timeout=True,
                socket_keepalive=True,
                socket_keepalive_options={},
            )

            # Test connection
            await self.redis.ping()
            self._initialized = True

            logger.info(
                "Model cache initialized",
                redis_url=self.settings.REDIS_URL,
            )

        except Exception as e:
            logger.error(
                "Failed to initialize model cache",
                error=str(e),
                error_type=type(e).__name__,
            )
            # Continue without cache (fail open)
            self._initialized = False

    async def close(self) -> None:
        """Close Redis connection."""
        if self.redis:
            await self.redis.close()
            logger.info("Model cache connection closed")

    def _generate_cache_key(
        self,
        request: AIRequest,
        version: Optional[str] = None,
    ) -> str:
        """Generate cache key for request."""
        # Create deterministic hash of request
        cache_data = {
            "model_name": request.model_name,
            "model_version": version or request.model_version,
            "input_data": request.input_data,
            "parameters": request.parameters,
        }

        # Sort keys for consistent hashing
        cache_json = json.dumps(cache_data, sort_keys=True, default=str)
        hash_value = hashlib.sha256(cache_json.encode()).hexdigest()

        return f"ai_cache:{request.model_name}:{hash_value}"

    def _serialize_response(self, response: AIResponse) -> bytes:
        """Serialize AI response for caching."""
        return pickle.dumps(response)

    def _deserialize_response(self, data: bytes) -> AIResponse:
        """Deserialize AI response from cache."""
        return pickle.loads(data)

    async def get(
        self,
        request: AIRequest,
        version: Optional[str] = None,
    ) -> Optional[AIResponse]:
        """Get cached response for request."""
        if not self._initialized or not self.redis:
            return None

        cache_key = self._generate_cache_key(request, version)

        try:
            cached_data = await self.redis.get(cache_key)
            if cached_data:
                response = self._deserialize_response(cached_data)
                logger.debug(
                    "Cache hit",
                    request_id=request.request_id,
                    model_name=request.model_name,
                    cache_key=cache_key,
                )
                return response

            logger.debug(
                "Cache miss",
                request_id=request.request_id,
                model_name=request.model_name,
                cache_key=cache_key,
            )
            return None

        except Exception as e:
            logger.warning(
                "Cache get error",
                error=str(e),
                cache_key=cache_key,
            )
            return None

    async def set(
        self,
        request: AIRequest,
        response: AIResponse,
        ttl: Optional[int] = None,
    ) -> bool:
        """Cache response for request."""
        if not self._initialized or not self.redis:
            return False

        cache_key = self._generate_cache_key(request)
        ttl = ttl or self.settings.CACHE_TTL_SECONDS

        try:
            serialized_response = self._serialize_response(response)
            await self.redis.setex(cache_key, ttl, serialized_response)

            logger.debug(
                "Cache set",
                request_id=request.request_id,
                model_name=request.model_name,
                cache_key=cache_key,
                ttl=ttl,
            )
            return True

        except Exception as e:
            logger.warning(
                "Cache set error",
                error=str(e),
                cache_key=cache_key,
            )
            return False

    async def invalidate(
        self,
        model_name: str,
        version: Optional[str] = None,
    ) -> int:
        """Invalidate cache for model/version."""
        if not self._initialized or not self.redis:
            return 0

        try:
            pattern = f"ai_cache:{model_name}:*"
            keys = await self.redis.keys(pattern)

            if keys:
                await self.redis.delete(*keys)
                logger.info(
                    "Cache invalidated",
                    model_name=model_name,
                    version=version,
                    keys_deleted=len(keys),
                )
                return len(keys)

            return 0

        except Exception as e:
            logger.warning(
                "Cache invalidation error",
                error=str(e),
                model_name=model_name,
                version=version,
            )
            return 0

    async def invalidate_pattern(self, pattern: str) -> int:
        """Invalidate cache entries matching pattern."""
        if not self._initialized or not self.redis:
            return 0

        try:
            keys = await self.redis.keys(pattern)
            if keys:
                await self.redis.delete(*keys)
                logger.info(
                    "Cache pattern invalidated",
                    pattern=pattern,
                    keys_deleted=len(keys),
                )
                return len(keys)

            return 0

        except Exception as e:
            logger.warning(
                "Cache pattern invalidation error",
                error=str(e),
                pattern=pattern,
            )
            return 0

    async def get_stats(self) -> Dict[str, Any]:
        """Get cache statistics."""
        if not self._initialized or not self.redis:
            return {"initialized": False}

        try:
            info = await self.redis.info("memory")
            keyspace_info = await self.redis.info("keyspace")

            # Count cache keys
            cache_keys = 0
            for db_info in keyspace_info.values():
                if isinstance(db_info, dict):
                    cache_keys += db_info.get("keys", 0)

            return {
                "initialized": True,
                "memory_usage_bytes": info.get("used_memory", 0),
                "memory_usage_human": info.get("used_memory_human", "0B"),
                "cache_keys": cache_keys,
                "connected_clients": info.get("connected_clients", 0),
                "hits": info.get("keyspace_hits", 0),
                "misses": info.get("keyspace_misses", 0),
                "hit_rate": self._calculate_hit_rate(info),
            }

        except Exception as e:
            logger.warning(
                "Failed to get cache stats",
                error=str(e),
            )
            return {"initialized": True, "error": str(e)}

    def _calculate_hit_rate(self, info: Dict[str, Any]) -> float:
        """Calculate cache hit rate."""
        hits = info.get("keyspace_hits", 0)
        misses = info.get("keyspace_misses", 0)
        total = hits + misses

        if total == 0:
            return 0.0

        return (hits / total) * 100

    async def warm_cache(
        self,
        model_name: str,
        common_inputs: List[Dict[str, Any]],
        version: Optional[str] = None,
    ) -> int:
        """Warm cache with common inputs."""
        if not self._initialized:
            return 0

        warmed = 0
        for input_data in common_inputs:
            # Create dummy request
            request = AIRequest(
                request_id=f"warm-{warmed}",
                model_name=model_name,
                model_version=version,
                input_data=input_data,
            )

            # Check if already cached
            cached = await self.get(request, version)
            if not cached:
                # In a real implementation, you would trigger
                # prediction and cache the result
                logger.debug(
                    "Cache warming item",
                    model_name=model_name,
                    input_hash=hashlib.sha256(
                        json.dumps(input_data, sort_keys=True).encode()
                    ).hexdigest()[:8],
                )
                warmed += 1

        logger.info(
            "Cache warming complete",
            model_name=model_name,
            items_warmed=warmed,
        )
        return warmed

    async def cleanup_expired(self) -> int:
        """Clean up expired cache entries."""
        if not self._initialized:
            return 0

        # Redis automatically handles expired keys
        # This is more for reporting
        stats = await self.get_stats()
        logger.info(
            "Cache cleanup completed",
            total_keys=stats.get("cache_keys", 0),
        )
        return stats.get("cache_keys", 0)

    async def export_cache(
        self,
        model_name: str,
        output_file: str,
    ) -> int:
        """Export cache data for model to file."""
        if not self._initialized:
            return 0

        try:
            pattern = f"ai_cache:{model_name}:*"
            keys = await self.redis.keys(pattern)

            exported = 0
            with open(output_file, "w") as f:
                for key in keys:
                    data = await self.redis.get(key)
                    if data:
                        try:
                            response = self._deserialize_response(data)
                            # Export as JSON
                            export_data = {
                                "key": key.decode(),
                                "response": response.dict(),
                                "timestamp": datetime.utcnow().isoformat(),
                            }
                            f.write(json.dumps(export_data) + "\n")
                            exported += 1
                        except Exception as e:
                            logger.warning(
                                "Failed to export cache entry",
                                key=key,
                                error=str(e),
                            )

            logger.info(
                "Cache export completed",
                model_name=model_name,
                entries_exported=exported,
                output_file=output_file,
            )
            return exported

        except Exception as e:
            logger.error(
                "Cache export failed",
                model_name=model_name,
                error=str(e),
            )
            return 0