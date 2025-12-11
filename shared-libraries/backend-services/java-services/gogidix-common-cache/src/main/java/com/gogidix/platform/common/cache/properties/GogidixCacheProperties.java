package com.gogidix.platform.common.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Gogidix caching
 */
@Data
@ConfigurationProperties(prefix = "gogidix.cache")
public class GogidixCacheProperties {

    /**
     * Whether caching is enabled
     */
    private boolean enabled = true;

    /**
     * Default cache type (redis, caffeine, composite, none)
     */
    private CacheType type = CacheType.CAFFEINE;

    /**
     * Default TTL in seconds
     */
    private long defaultTtl = 3600;

    /**
     * Whether to cache null values by default
     */
    private boolean cacheNullValues = false;

    /**
     * Cache key prefix
     */
    private String keyPrefix = "gogidix:";

    /**
     * Redis configuration
     */
    private Redis redis = new Redis();

    /**
     * Caffeine configuration
     */
    private Caffeine caffeine = new Caffeine();

    /**
     * Per-cache configuration
     */
    private Map<String, CacheSpec> caches = new HashMap<>();

    @Data
    public static class Redis {
        /**
         * Redis namespace for keys
         */
        private String namespace = "gogidix";

        /**
         * Key separator
         */
        private String keySeparator = ":";

        /**
         * Time to live for empty values to prevent cache stampede
         */
        private long cacheNullTtl = 60;

        /**
         * Whether to use Redis as a secondary cache (cache-aside pattern)
         */
        private boolean useAsSecondary = false;

        /**
         * Redis key expiration policy
         */
        private ExpirationPolicy expirationPolicy = ExpirationPolicy.WRITE;

        public enum ExpirationPolicy {
            WRITE, ACCESS, NONE
        }
    }

    @Data
    public static class Caffeine {
        /**
         * Maximum cache size
         */
        private long maximumSize = 10000;

        /**
         * Time to live after write
         */
        private long expireAfterWrite = 0;

        /**
         * Time to live after access
         */
        private long expireAfterAccess = 0;

        /**
         * Refresh after write
         */
        private long refreshAfterWrite = 0;

        /**
         * Initial capacity
         */
        private int initialCapacity = 100;

        /**
         * Weak keys
         */
        private boolean weakKeys = false;

        /**
         * Weak values
         */
        private boolean weakValues = false;

        /**
         * Soft values
         */
        private boolean softValues = false;

        /**
         * Record statistics
         */
        private boolean recordStats = false;

        /**
         * Executor for refresh operations
         */
        private String executor;
    }

    @Data
    public static class CacheSpec {
        /**
         * Cache name
         */
        private String name;

        /**
         * Cache type override
         */
        private CacheType type;

        /**
         * Time to live in seconds
         */
        private long ttl;

        /**
         * Maximum size (for local caches)
         */
        private long maxSize;

        /**
         * Whether to cache null values
         */
        private boolean cacheNull;

        /**
         * Cache key prefix
         */
        private String keyPrefix;

        /**
         * Additional configuration
         */
        private Map<String, Object> config = new HashMap<>();
    }

    public enum CacheType {
        REDIS, CAFFEINE, COMPOSITE, NONE
    }
}