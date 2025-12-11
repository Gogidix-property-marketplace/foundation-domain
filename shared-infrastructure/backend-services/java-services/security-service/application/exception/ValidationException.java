package com.gogidix.infrastructure.security.application.exception;

/**
 * Exception thrown when validation fails.
 * Represents validation errors in the application layer.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public class ValidationException extends ApplicationException {

    /**
     * Constructs a new ValidationException with the specified message.
     *
     * @param message the error message
     */
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }

    /**
     * Constructs a new ValidationException with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the cause of this exception
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause, "VALIDATION_ERROR");
    }
}