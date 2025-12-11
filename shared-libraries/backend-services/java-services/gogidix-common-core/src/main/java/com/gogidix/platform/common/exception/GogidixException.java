package com.gogidix.platform.common.exception;

import com.gogidix.platform.common.dto.ApiResponse;
import lombok.Getter;

/**
 * Base exception class for all Gogidix platform exceptions
 * Provides consistent error handling across all services
 */
@Getter
public class GogidixException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;
    private final Object details;

    public GogidixException(String message) {
        super(message);
        this.errorCode = "GENERIC_ERROR";
        this.httpStatus = 500;
        this.details = null;
    }

    public GogidixException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERIC_ERROR";
        this.httpStatus = 500;
        this.details = null;
    }

    public GogidixException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = null;
    }

    public GogidixException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = null;
    }

    public GogidixException(String errorCode, String message, int httpStatus, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details;
    }

    public GogidixException(String errorCode, String message, int httpStatus, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details;
    }

    /**
     * Convert exception to ApiResponse
     */
    public <T> ApiResponse<T> toApiResponse() {
        @SuppressWarnings("unchecked")
        ApiResponse<T> response = (ApiResponse<T>) ApiResponse.error(this.httpStatus, this.getMessage());
        return response;
    }

    /**
     * Convert exception to ApiResponse with path
     */
    public <T> ApiResponse<T> toApiResponse(String path) {
        @SuppressWarnings("unchecked")
        ApiResponse<T> response = (ApiResponse<T>) ApiResponse.error(this.httpStatus, this.getMessage())
                .withPath(path);
        return response;
    }
}