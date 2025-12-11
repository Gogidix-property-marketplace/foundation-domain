package com.gogidix.platform.common.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception for validation errors
 * Used when input validation fails
 */
public class ValidationException extends GogidixException {

    private final Map<String, String> validationErrors;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message, 400);
        this.validationErrors = null;
    }

    public ValidationException(String message, Map<String, String> validationErrors) {
        super("VALIDATION_ERROR", message, 400, validationErrors);
        this.validationErrors = validationErrors;
    }

    public ValidationException(String message, List<ApiErrorDetail> errorDetails) {
        super("VALIDATION_ERROR", message, 400, errorDetails);
        this.validationErrors = null;
    }

    public ValidationException(String message, Throwable cause) {
        super("VALIDATION_ERROR", message, 400, cause);
        this.validationErrors = null;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Nested class for detailed error information
     */
    public static class ApiErrorDetail {
        private final String field;
        private final String message;
        private final Object rejectedValue;

        public ApiErrorDetail(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }
    }
}