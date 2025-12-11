package com.gogidix.platform.common.exception;

/**
 * Exception for business logic violations
 * Used when business rules are violated or constraints are not met
 */
public class BusinessException extends GogidixException {

    public BusinessException(String message) {
        super("BUSINESS_ERROR", message, 400);
    }

    public BusinessException(String message, Throwable cause) {
        super("BUSINESS_ERROR", message, 400, cause);
    }

    public BusinessException(String errorCode, String message) {
        super(errorCode, message, 400);
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, 400, cause);
    }

    public BusinessException(String errorCode, String message, Object details) {
        super(errorCode, message, 400, details);
    }

    public BusinessException(String errorCode, String message, Object details, Throwable cause) {
        super(errorCode, message, 400, details, cause);
    }
}