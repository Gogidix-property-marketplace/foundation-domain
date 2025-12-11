package com.gogidix.platform.common.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for updating cache entries
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CachePut {

    /**
     * Cache name to put value in
     */
    String cacheName() default "";

    /**
     * Cache key expression (SpEL)
     */
    String key() default "";

    /**
     * Condition to cache (SpEL)
     */
    String condition() default "";

    /**
     * Condition to NOT cache (SpEL)
     */
    String unless() default "";
}