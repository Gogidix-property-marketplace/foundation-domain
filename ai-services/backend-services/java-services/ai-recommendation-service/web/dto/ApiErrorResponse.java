package com.gogidix.ai.recommendation.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard API error response DTO for REST APIs.
 * Provides detailed error information for API consumers.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private boolean success = false;
    private String error;
    private String message;
    private String errorCode;
    private String path;
    private LocalDateTime timestamp;
    private String requestId;
    private List<String> details;
    private Object metadata;

    /**
     * Creates a basic API error response.
     *
     * @param error   the error type
     * @param message the error message
     * @return API error response
     */
    public static ApiErrorResponse of(String error, String message) {
        return ApiErrorResponse.builder()
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an API error response with error code.
     *
     * @param error     the error type
     * @param message   the error message
     * @param errorCode the error code
     * @return API error response
     */
    public static ApiErrorResponse of(String error, String message, String errorCode) {
        return ApiErrorResponse.builder()
                .error(error)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an API error response with request context.
     *
     * @param error     the error type
     * @param message   the error message
     * @param errorCode the error code
     * @param path      the request path
     * @return API error response
     */
    public static ApiErrorResponse of(String error, String message, String errorCode, String path) {
        return ApiErrorResponse.builder()
                .error(error)
                .message(message)
                .errorCode(errorCode)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an API error response with details.
     *
     * @param error     the error type
     * @param message   the error message
     * @param errorCode the error code
     * @param details   the error details
     * @return API error response
     */
    public static ApiErrorResponse withDetails(String error, String message, String errorCode, List<String> details) {
        return ApiErrorResponse.builder()
                .error(error)
                .message(message)
                .errorCode(errorCode)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a validation error response.
     *
     * @param message the validation error message
     * @param details the validation error details
     * @return validation error response
     */
    public static ApiErrorResponse validationError(String message, List<String> details) {
        return ApiErrorResponse.builder()
                .error("Validation Error")
                .message(message)
                .errorCode("VALIDATION_FAILED")
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a not found error response.
     *
     * @param resourceType the type of resource
     * @param resourceId   the resource ID
     * @return not found error response
     */
    public static ApiErrorResponse notFound(String resourceType, String resourceId) {
        return ApiErrorResponse.builder()
                .error("Not Found")
                .message(String.format("%s with ID '%s' not found", resourceType, resourceId))
                .errorCode("RESOURCE_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a server error response.
     *
     * @param message the server error message
     * @return server error response
     */
    public static ApiErrorResponse serverError(String message) {
        return ApiErrorResponse.builder()
                .error("Internal Server Error")
                .message(message)
                .errorCode("INTERNAL_ERROR")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a bad request error response.
     *
     * @param message the bad request message
     * @return bad request error response
     */
    public static ApiErrorResponse badRequest(String message) {
        return ApiErrorResponse.builder()
                .error("Bad Request")
                .message(message)
                .errorCode("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an unauthorized error response.
     *
     * @param message the unauthorized message
     * @return unauthorized error response
     */
    public static ApiErrorResponse unauthorized(String message) {
        return ApiErrorResponse.builder()
                .error("Unauthorized")
                .message(message)
                .errorCode("UNAUTHORIZED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a forbidden error response.
     *
     * @param message the forbidden message
     * @return forbidden error response
     */
    public static ApiErrorResponse forbidden(String message) {
        return ApiErrorResponse.builder()
                .error("Forbidden")
                .message(message)
                .errorCode("FORBIDDEN")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a conflict error response.
     *
     * @param message the conflict message
     * @return conflict error response
     */
    public static ApiErrorResponse conflict(String message) {
        return ApiErrorResponse.builder()
                .error("Conflict")
                .message(message)
                .errorCode("CONFLICT")
                .timestamp(LocalDateTime.now())
                .build();
    }
}