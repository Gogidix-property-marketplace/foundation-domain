package com.gogidix.platform.common.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for auditing method calls (alias for @Audit)
 *
 * @deprecated Use @Audit instead
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Operation type for the audit log
     */
    String operation() default "";

    /**
     * Entity type being audited
     */
    Class<?> entityType() default Object.class;

    /**
     * Event type for the audit log
     */
    String eventType() default "";

    /**
     * Event category for grouping similar events
     */
    String eventCategory() default "";

    /**
     * Action being performed
     */
    String action() default "";

    /**
     * Type of resource being accessed
     */
    String resourceType() default "";

    /**
     * Name of parameter that contains the resource ID
     */
    String resourceIdParam() default "";

    /**
     * Severity level of the event
     */
    Severity severity() default Severity.LOW;

    /**
     * Whether to include request parameters in audit log
     */
    boolean includeRequestParams() default false;

    /**
     * Whether to include response data in audit log
     */
    boolean includeResponseData() default false;

    /**
     * Additional information to include
     */
    String[] additionalInfo() default {};

    /**
     * Custom result message
     */
    String resultMessage() default "";

    /**
     * Severity levels for audit events
     */
    enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}