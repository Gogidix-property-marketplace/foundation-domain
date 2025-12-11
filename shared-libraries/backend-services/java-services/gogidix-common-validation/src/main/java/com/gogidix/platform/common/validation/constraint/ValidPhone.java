package com.gogidix.platform.common.validation.constraint;

import com.gogidix.platform.common.validation.validator.ValidPhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom phone number validation constraint
 */
@Documented
@Constraint(validatedBy = ValidPhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {

    String message() default "Invalid phone number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow blank values
     */
    boolean allowBlank() default true;

    /**
     * Default country code for validation
     */
    String defaultCountry() default "US";
}