package com.gogidix.platform.common.validation.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validation operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final Validator validator;

    /**
     * Validate object and return validation result
     */
    public ValidationResult validate(Object object) {
        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        return new ValidationResult(violations);
    }

  
    /**
     * Validate object and throw exception if invalid
     */
    public void validateOrThrow(Object object) throws ValidationException {
        ValidationResult result = validate(object);
        if (!result.isValid()) {
            throw new ValidationException(result.getErrors());
        }
    }

    /**
     * Validate specific fields of an object
     */
    public ValidationResult validateFields(Object object, String... fieldNames) {
        Set<ConstraintViolation<Object>> violations = new HashSet<>();

        for (String fieldName : fieldNames) {
            Set<ConstraintViolation<Object>> fieldViolations = validator.validateProperty(object, fieldName);
            violations.addAll(fieldViolations);
        }

        return new ValidationResult(violations);
    }

    /**
     * Validate object with specific validation group
     */
    public ValidationResult validate(Object object, Class<?>... groups) {
        Set<ConstraintViolation<Object>> violations = validator.validate(object, groups);
        return new ValidationResult(violations);
    }

    /**
     * Check if object is valid
     */
    public boolean isValid(Object object) {
        return validate(object).isValid();
    }

    /**
     * Get validation errors as formatted string
     */
    public String getValidationErrorsAsString(Object object) {
        ValidationResult result = validate(object);
        return result.getErrors().stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getMessage()))
                .collect(Collectors.joining("; "));
    }

    /**
     * Check if string value is not null or empty
     */
    public boolean isNotEmpty(String value) {
        return StringUtils.isNotBlank(value);
    }

    /**
     * Check if string value matches pattern
     */
    public boolean matchesPattern(String value, String pattern) {
        return value != null && value.matches(pattern);
    }

    /**
     * Check if number is within range
     */
    public boolean isInRange(Number value, Number min, Number max) {
        if (value == null) return false;
        double doubleValue = value.doubleValue();
        return doubleValue >= min.doubleValue() && doubleValue <= max.doubleValue();
    }

    /**
     * Check if collection size is within range
     */
    public boolean isSizeInRange(Collection<?> collection, int min, int max) {
        if (collection == null) return false;
        int size = collection.size();
        return size >= min && size <= max;
    }

    /**
     * Check if string length is within range
     */
    public boolean isLengthInRange(String value, int min, int max) {
        if (value == null) return false;
        int length = value.length();
        return length >= min && length <= max;
    }

    /**
     * Validation result wrapper class
     */
    public static class ValidationResult {
        private final List<ValidationError> errors;

        public ValidationResult(Set<ConstraintViolation<Object>> violations) {
            this.errors = violations.stream()
                    .map(ValidationError::fromConstraintViolation)
                    .collect(Collectors.toList());
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<ValidationError> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public Map<String, String> getErrorMap() {
            return errors.stream()
                    .collect(Collectors.toMap(
                            ValidationError::getField,
                            ValidationError::getMessage,
                            (existing, replacement) -> existing + "; " + replacement
                    ));
        }
    }

    /**
     * Validation error details
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
        private String invalidValue;

        public static ValidationError fromConstraintViolation(ConstraintViolation<?> violation) {
            String fieldName = violation.getPropertyPath().toString();
            // Remove nested property path prefixes
            if (fieldName.contains(".")) {
                fieldName = fieldName.substring(fieldName.lastIndexOf('.') + 1);
            }

            return new ValidationError(
                    fieldName,
                    violation.getMessage(),
                    violation.getInvalidValue() != null ? violation.getInvalidValue().toString() : "null"
            );
        }
    }

    /**
     * Custom validation exception
     */
    public static class ValidationException extends RuntimeException {
        private final List<ValidationError> errors;

        public ValidationException(List<ValidationError> errors) {
            super("Validation failed: " + errors.stream()
                    .map(e -> e.getField() + ": " + e.getMessage())
                    .collect(Collectors.joining(", ")));
            this.errors = errors;
        }

        public List<ValidationError> getErrors() {
            return errors;
        }
    }
}