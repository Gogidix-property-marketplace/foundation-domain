package com.gogidix.platform.common.validation.constraint;

import com.gogidix.platform.common.validation.validator.ValidEnumValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation constraint for enum values
 */
@Documented
@Constraint(validatedBy = ValidEnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {

    String message() default "Invalid enum value";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Enum class to validate against
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * Whether to ignore case during validation
     */
    boolean ignoreCase() default true;

    /**
     * Whether to allow null values
     */
    boolean allowNull() default true;
}