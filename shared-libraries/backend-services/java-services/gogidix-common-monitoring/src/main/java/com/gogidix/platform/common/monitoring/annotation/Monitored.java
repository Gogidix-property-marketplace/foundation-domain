package com.gogidix.platform.common.monitoring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for monitoring method performance and metrics
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitored {

    /**
     * Name of the metric (optional)
     */
    String name() default "";

    /**
     * Description of what this method does
     */
    String description() default "";

    /**
     * Additional tags to add to the metric
     */
    String[] tags() default {};

    /**
     * Whether to record execution time
     */
    boolean recordExecutionTime() default true;

    /**
     * Whether to record success/failure count
     */
    boolean recordSuccessFailure() default true;

    /**
     * Whether to record concurrent executions
     */
    boolean recordConcurrentExecutions() default false;

    /**
     * Percentiles to record for execution time
     */
    double[] percentiles() default {0.5, 0.9, 0.95, 0.99};

    
    /**
     * Custom metric suffix
     */
    String suffix() default "";
}