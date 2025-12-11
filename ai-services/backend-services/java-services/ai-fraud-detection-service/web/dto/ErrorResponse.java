package com.gogidix.ai.fraud.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response DTO for REST APIs.
 * Provides consistent error response format across all endpoints.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private boolean success = false;
    private String error;
    private String message;
    private String errorCode;
    private String path;
    private LocalDateTime timestamp;
    private List<FieldError> fieldErrors;

    /**
     * Field error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Creates a simple error response.
     *
     * @param message the error message
     * @return error response
     */
    public static ErrorResponse of(String message) {
        return ErrorResponse.builder()
                .error("Error")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with code.
     *
     * @param message the error message
     * @param errorCode the error code
     * @return error response
     */
    public static ErrorResponse of(String message, String errorCode) {
        return ErrorResponse.builder()
                .error("Error")
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a validation error response.
     *
     * @param message the error message
     * @param fieldErrors the field errors
     * @return error response
     */
    public static ErrorResponse of(String message, List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .error("Validation Error")
                .message(message)
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}