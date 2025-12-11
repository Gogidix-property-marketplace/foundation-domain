package com.gogidix.ai.inference.domain.UserManagement.exception;

/**
 * Base exception for Usermanagement domain-specific errors.
 * This exception represents violations of business rules in the UserManagement bounded context.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public class DomainException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructs a new DomainException with the specified message.
     *
     * @param message the error message
     */
    public DomainException(String message) {
        super(message);
        this.errorCode = "_DOMAIN_ERROR";
    }

    /**
     * Constructs a new DomainException with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the cause of this exception
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "_DOMAIN_ERROR";
    }

    /**
     * Constructs a new DomainException with the specified message and error code.
     *
     * @param message   the error message
     * @param errorCode the error code
     */
    public DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new DomainException with the specified message, cause, and error code.
     *
     * @param message   the error message
     * @param cause     the cause of this exception
     * @param errorCode the error code
     */
    public DomainException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code for this exception.
     *
     * @return error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}