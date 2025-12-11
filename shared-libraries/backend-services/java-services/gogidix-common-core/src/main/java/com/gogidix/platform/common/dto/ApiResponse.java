package com.gogidix.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standard API Response wrapper for all Gogidix platform services
 * Provides consistent response format across all APIs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Response timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Response message
     */
    private String message;

    /**
     * Response payload data
     */
    private T data;

    /**
     * Request path
     */
    private String path;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Pagination information for paginated responses
     */
    private PageInfo pageInfo;

    /**
     * Error details for error responses
     */
    private List<ErrorDetail> errors;

    /**
     * Request tracking ID
     */
    private String requestId;

    /**
     * Success response static factory method
     *
     * @param data the response data
     * @param <T> type of the data
     * @return success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Success response with custom message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Created response (201)
     */
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .status(201)
                .message("Resource created successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Created response with custom message
     */
    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .status(201)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * No content response (204)
     */
    public static <T> ApiResponse<T> noContent() {
        return ApiResponse.<T>builder()
                .status(204)
                .message("No Content")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * No content response with custom message
     */
    public static <T> ApiResponse<T> noContent(String message) {
        return ApiResponse.<T>builder()
                .status(204)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Bad request error response (400)
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return ApiResponse.<T>builder()
                .status(400)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Not found error response (404)
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return ApiResponse.<T>builder()
                .status(404)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Internal server error response (500)
     */
    public static <T> ApiResponse<T> internalServerError(String message) {
        return ApiResponse.<T>builder()
                .status(500)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Error response with custom status and message
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Error response with details
     */
    public static <T> ApiResponse<T> error(int status, String message, List<ErrorDetail> errors) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create paginated response
     */
    public static <T> ApiResponse<T> paginated(T data, PageInfo pageInfo) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("Success")
                .data(data)
                .pageInfo(pageInfo)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Add metadata to response
     */
    public ApiResponse<T> withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = Map.of();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Add metadata to response (Map)
     */
    public ApiResponse<T> withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Set request path
     */
    public ApiResponse<T> withPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Set request ID
     */
    public ApiResponse<T> withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Nested class for pagination information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * Nested class for error details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String field;
        private String message;
        private Object rejectedValue;
        private String code;
    }
}