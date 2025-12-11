package com.gogidix.platform.common.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for evicting entries from cache
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {

    /**
     * Cache name to evict from
     */
    String cacheName() default "";

    /**
     * Cache key expression (SpEL)
     */
    String key() default "";

    /**
     * Whether to evict all entries from cache
     */
    boolean allEntries() default false;

    /**
     * Whether eviction should occur before method execution
     */
    boolean beforeInvocation() default false;
}