package com.gogidix.platform.common.validation.constraint;

import com.gogidix.platform.common.validation.validator.StrongPasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Strong password validation constraint
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password does not meet security requirements";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Minimum password length
     */
    int minLength() default 8;

    /**
     * Require uppercase letters
     */
    boolean requireUppercase() default true;

    /**
     * Require lowercase letters
     */
    boolean requireLowercase() default true;

    /**
     * Require digits
     */
    boolean requireDigits() default true;

    /**
     * Require special characters
     */
    boolean requireSpecialChars() default true;

    /**
     * Allowed special characters
     */
    String specialChars() default "!@#$%^&*()_+-=[]{}|;:,.<>?";

    /**
     * Minimum number of special characters
     */
    int minSpecialChars() default 1;
}