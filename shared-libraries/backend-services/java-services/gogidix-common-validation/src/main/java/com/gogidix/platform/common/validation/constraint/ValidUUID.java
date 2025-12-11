package com.gogidix.platform.common.validation.constraint;

import com.gogidix.platform.common.validation.validator.ValidUUIDValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * UUID validation constraint
 */
@Documented
@Constraint(validatedBy = ValidUUIDValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUUID {

    String message() default "Invalid UUID format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow null values
     */
    boolean allowNull() default true;

    /**
     * Whether to allow empty strings
     */
    boolean allowEmpty() default false;
}