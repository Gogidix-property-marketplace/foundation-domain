package com.gogidix.platform.common.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for configuring cache behavior
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheConfig {

    /**
     * Cache name to use
     */
    String cacheName() default "";

    /**
     * Cache key expression (SpEL)
     */
    String key() default "";

    /**
     * Cache key generator bean name
     */
    String keyGenerator() default "";

    /**
     * Cache manager bean name
     */
    String cacheManager() default "";

    /**
     * Cache resolver bean name
     */
    String cacheResolver() default "";

    /**
     * Condition to cache (SpEL)
     */
    String condition() default "";

    /**
     * Condition to NOT cache (SpEL)
     */
    String unless() default "";

    /**
     * Time to live (TTL) in seconds
     */
    long ttl() default 0;

    /**
     * Maximum size of cache (for local caches)
     */
    long maxSize() default 0;

    /**
     * Whether to cache null values
     */
    boolean cacheNull() default false;

    /**
     * Whether to sync cache operations
     */
    boolean sync() default false;

    /**
     * Time unit for TTL
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}