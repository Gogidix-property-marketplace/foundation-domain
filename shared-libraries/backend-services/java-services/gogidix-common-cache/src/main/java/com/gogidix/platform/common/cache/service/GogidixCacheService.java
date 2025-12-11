package com.gogidix.platform.common.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Generic cache service for Gogidix platform
 */
@Slf4j
@RequiredArgsConstructor
public class GogidixCacheService {

    private final CacheManager cacheManager;

    /**
     * Get value from cache
     */
    public <T> T get(String cacheName, String key, Class<T> type) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found", cacheName);
                return null;
            }

            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                T value = (T) wrapper.get();
                log.debug("Cache hit for key '{}' in cache '{}'", key, cacheName);
                return value;
            }

            log.debug("Cache miss for key '{}' in cache '{}'", key, cacheName);
            return null;
        } catch (Exception e) {
            log.error("Error getting value from cache '{}' with key '{}'", cacheName, key, e);
            return null;
        }
    }

    /**
     * Get value from cache with fallback loader
     */
    public <T> T get(String cacheName, String key, Class<T> type, Callable<T> loader) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found, calling loader directly", cacheName);
                return loader.call();
            }

            T value = cache.get(key, loader);
            log.debug("Retrieved value for key '{}' in cache '{}' (cache: {})",
                     key, cacheName, value != null ? "hit" : "miss");
            return value;
        } catch (Exception e) {
            log.error("Error getting value from cache '{}' with key '{}'", cacheName, key, e);
            try {
                return loader.call();
            } catch (Exception loaderException) {
                log.error("Error calling fallback loader", loaderException);
                return null;
            }
        }
    }

    /**
     * Put value in cache
     */
    public void put(String cacheName, String key, Object value) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found, cannot put value", cacheName);
                return;
            }

            cache.put(key, value);
            log.debug("Put value in cache '{}' with key '{}'", cacheName, key);
        } catch (Exception e) {
            log.error("Error putting value in cache '{}' with key '{}'", cacheName, key, e);
        }
    }

    /**
     * Put value in cache with TTL (if supported by cache manager)
     */
    public void put(String cacheName, String key, Object value, Duration ttl) {
        put(cacheName, key, value);
        log.debug("Put value in cache '{}' with key '{}' and TTL: {}", cacheName, key, ttl);
    }

    /**
     * Evict specific key from cache
     */
    public void evict(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found, cannot evict key", cacheName);
                return;
            }

            cache.evict(key);
            log.debug("Evicted key '{}' from cache '{}'", key, cacheName);
        } catch (Exception e) {
            log.error("Error evicting key '{}' from cache '{}'", key, cacheName, e);
        }
    }

    /**
     * Clear entire cache
     */
    public void clear(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found, cannot clear", cacheName);
                return;
            }

            cache.clear();
            log.info("Cleared cache '{}'", cacheName);
        } catch (Exception e) {
            log.error("Error clearing cache '{}'", cacheName, e);
        }
    }

    /**
     * Check if key exists in cache
     */
    public boolean contains(String cacheName, String key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                return false;
            }

            Cache.ValueWrapper wrapper = cache.get(key);
            return wrapper != null;
        } catch (Exception e) {
            log.error("Error checking if key '{}' exists in cache '{}'", key, cacheName, e);
            return false;
        }
    }

    /**
     * Get all cache names
     */
    public Collection<String> getCacheNames() {
        return cacheManager.getCacheNames();
    }

    /**
     * Check if cache exists
     */
    public boolean cacheExists(String cacheName) {
        return cacheManager.getCache(cacheName) != null;
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return Map.of("exists", false);
        }

        return Map.of(
            "exists", true,
            "name", cacheName,
            "cacheManager", cacheManager.getClass().getSimpleName()
        );
    }

    /**
     * Bulk operations - put multiple entries
     */
    public void putAll(String cacheName, Map<String, Object> entries) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' not found, cannot put bulk entries", cacheName);
            return;
        }

        entries.forEach((key, value) -> {
            try {
                cache.put(key, value);
            } catch (Exception e) {
                log.error("Error putting bulk entry for key '{}' in cache '{}'", key, cacheName, e);
            }
        });

        log.debug("Put {} entries in cache '{}'", entries.size(), cacheName);
    }

    /**
     * Bulk operations - evict multiple keys
     */
    public void evictAll(String cacheName, List<String> keys) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' not found, cannot evict bulk keys", cacheName);
            return;
        }

        keys.forEach(key -> {
            try {
                cache.evict(key);
            } catch (Exception e) {
                log.error("Error evicting bulk key '{}' from cache '{}'", key, cacheName, e);
            }
        });

        log.debug("Evicted {} keys from cache '{}'", keys.size(), cacheName);
    }
}