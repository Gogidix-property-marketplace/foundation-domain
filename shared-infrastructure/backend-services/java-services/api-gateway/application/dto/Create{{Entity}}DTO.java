package com.gogidix.infrastructure.gateway.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for creating Route Configuration.
 * Contains validation rules for API Gateway route creation.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRouteDTO {

    @NotBlank(message = "Route ID is required")
    @Size(min = 1, max = 100, message = "Route ID must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Route ID can only contain alphanumeric characters, hyphens, and underscores")
    private String routeId;

    @NotBlank(message = "Path pattern is required")
    @Size(min = 1, max = 500, message = "Path pattern must be between 1 and 500 characters")
    private String pathPattern;

    @NotBlank(message = "Target service URI is required")
    @Size(min = 1, max = 500, message = "Target URI must be between 1 and 500 characters")
    private String targetUri;

    private String httpMethod;
    private boolean enabled = true;
    private int priority = 0;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private List<String> allowedRoles;
    private List<String> blockedRoles;
    private Map<String, Object> metadata;

    // Rate limiting configuration
    private RateLimitConfig rateLimit;

    // Circuit breaker configuration
    private CircuitBreakerConfig circuitBreaker;

    // Retry configuration
    private RetryConfig retry;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitConfig {
        private boolean enabled = false;
        private int requestsPerMinute = 100;
        private String burstCapacity = "100";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircuitBreakerConfig {
        private boolean enabled = true;
        private int failureThreshold = 5;
        private int timeoutSeconds = 30;
        private String fallbackUri;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryConfig {
        private boolean enabled = false;
        private int maxAttempts = 3;
        private long backoffMs = 1000;
        private List<String> retryableStatusCodes;
    }
}