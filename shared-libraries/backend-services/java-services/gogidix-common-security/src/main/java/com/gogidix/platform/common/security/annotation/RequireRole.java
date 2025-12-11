package com.gogidix.platform.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce role-based access control
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * Required roles to access the resource
     */
    String[] value();

    /**
     * Logical operator for role checking
     * Default: ANY (user needs any of the specified roles)
     */
    LogicalOperator operator() default LogicalOperator.ANY;

    enum LogicalOperator {
        ANY,
        ALL
    }
}