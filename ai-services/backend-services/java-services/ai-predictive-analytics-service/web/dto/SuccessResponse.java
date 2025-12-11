package com.gogidix.ai.predictive.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard success response DTO for REST APIs.
 * Provides consistent success response format across all endpoints.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse<T> {

    private boolean success = true;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    /**
     * Creates a success response with data.
     *
     * @param data the response data
     * @param <T> the data type
     * @return success response
     */
    public static <T> SuccessResponse<T> of(T data) {
        return SuccessResponse.<T>builder()
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a success response with message.
     *
     * @param message the success message
     * @param <T> the data type
     * @return success response
     */
    public static <T> SuccessResponse<T> of(String message) {
        return SuccessResponse.<T>builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a success response with message and data.
     *
     * @param message the success message
     * @param data the response data
     * @param <T> the data type
     * @return success response
     */
    public static <T> SuccessResponse<T> of(String message, T data) {
        return SuccessResponse.<T>builder()
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}