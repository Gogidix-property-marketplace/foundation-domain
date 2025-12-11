package com.gogidix.platform.common.validation.constraint;

import com.gogidix.platform.common.validation.validator.ValidEmailValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom email validation constraint
 */
@Documented
@Constraint(validatedBy = ValidEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {

    String message() default "Invalid email address";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow blank values
     */
    boolean allowBlank() default true;

    /**
     * Whether to check for MX records
     */
    boolean checkMX() default false;

    /**
     * Custom regex pattern for email validation
     */
    String pattern() default "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
}