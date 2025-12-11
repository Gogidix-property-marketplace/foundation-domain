package com.gogidix.platform.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require authentication for method access
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuthentication {

    /**
     * Custom message to be returned if authentication is missing
     */
    String message() default "Authentication is required to access this resource";

    /**
     * Whether to check for active authentication
     */
    boolean checkActive() default true;
}