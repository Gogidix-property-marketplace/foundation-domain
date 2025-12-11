package com.gogidix.platform.common.validation.validator;

import com.gogidix.platform.common.validation.constraint.ValidUUID;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * UUID validator implementation
 */
@Slf4j
public class ValidUUIDValidator implements ConstraintValidator<ValidUUID, String> {

    private boolean allowNull;
    private boolean allowEmpty;

    @Override
    public void initialize(ValidUUID constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }

        if (value.trim().isEmpty()) {
            return allowEmpty;
        }

        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid UUID format: {}", value);
            return false;
        }
    }
}