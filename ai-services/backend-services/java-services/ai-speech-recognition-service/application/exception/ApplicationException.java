package com.gogidix.ai.speech.application.exception;

/**
 * Base exception for application layer errors.
 * This exception represents errors that occur in the application layer.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public class ApplicationException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructs a new ApplicationException with the specified message.
     *
     * @param message the error message
     */
    public ApplicationException(String message) {
        super(message);
        this.errorCode = "APPLICATION_ERROR";
    }

    /**
     * Constructs a new ApplicationException with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the cause of this exception
     */
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "APPLICATION_ERROR";
    }

    /**
     * Constructs a new ApplicationException with the specified message and error code.
     *
     * @param message   the error message
     * @param errorCode the error code
     */
    public ApplicationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new ApplicationException with the specified message, cause, and error code.
     *
     * @param message   the error message
     * @param cause     the cause of this exception
     * @param errorCode the error code
     */
    public ApplicationException(String message, Throwable cause, String errorCode) {
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