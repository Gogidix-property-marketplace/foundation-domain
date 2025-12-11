package com.gogidix.platform.common.validation.validator;

import com.gogidix.platform.common.validation.constraint.ValidEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validator for enum values
 */
@Slf4j
public class ValidEnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Class<? extends Enum<?>> enumClass;
    private boolean ignoreCase;
    private boolean allowNull;
    private List<String> validValues;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.ignoreCase = constraintAnnotation.ignoreCase();
        this.allowNull = constraintAnnotation.allowNull();

        // Pre-compute valid enum values for performance
        this.validValues = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());

        log.debug("Initialized ValidEnumValidator for enum class: {} with valid values: {}",
                enumClass.getSimpleName(), validValues);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }

        boolean isValid = validValues.stream()
                .anyMatch(validValue -> ignoreCase ?
                    validValue.equalsIgnoreCase(value) :
                    validValue.equals(value));

        if (!isValid) {
            String validValuesStr = String.join(", ", validValues);
            String errorMessage = String.format("Value '%s' is not valid. Valid values are: %s",
                    value, validValuesStr);

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(errorMessage)
                    .addConstraintViolation();
        }

        return isValid;
    }
}