package com.gogidix.infrastructure.ratelimit.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gogidix.infrastructure.ratelimit.domain.RateLimitAlgorithm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for Rate Limit Policy operations.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitPolicyDto {

    /**
     * Policy ID (null for new policies)
     */
    @JsonProperty("id")
    private String id;

    /**
     * Client identifier
     */
    @NotBlank(message = "Client ID is required")
    @JsonProperty("client_id")
    private String clientId;

    /**
     * API key for authentication
     */
    @JsonProperty("api_key")
    private String apiKey;

    /**
     * Tenant/organization ID
     */
    @NotBlank(message = "Tenant ID is required")
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * API endpoint or resource identifier
     */
    @NotBlank(message = "Endpoint is required")
    @JsonProperty("endpoint")
    private String endpoint;

    /**
     * Rate limiting algorithm
     */
    @NotNull(message = "Algorithm is required")
    @JsonProperty("algorithm")
    private RateLimitAlgorithm algorithm;

    /**
     * Maximum requests allowed
     */
    @Positive(message = "Max requests must be positive")
    @JsonProperty("max_requests")
    private Integer maxRequests;

    /**
     * Time window in seconds
     */
    @Positive(message = "Time window must be positive")
    @JsonProperty("time_window_seconds")
    private Integer timeWindowSeconds;

    /**
     * Burst capacity (for token bucket algorithm)
     */
    @JsonProperty("burst_capacity")
    private Integer burstCapacity;

    /**
     * Refill rate per second (for token bucket algorithm)
     */
    @JsonProperty("refill_rate_per_second")
    private BigDecimal refillRatePerSecond;

    /**
     * Whether this policy is active
     */
    @Builder.Default
    @JsonProperty("is_active")
    private Boolean isActive = true;

    /**
     * Priority of this policy
     */
    @Builder.Default
    @JsonProperty("priority")
    private Integer priority = 100;

    /**
     * Description of the policy
     */
    @JsonProperty("description")
    private String description;

    /**
     * Metadata as JSON
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Created timestamp
     */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    /**
     * Last updated timestamp
     */
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Created by
     */
    @JsonProperty("created_by")
    private String createdBy;

    /**
     * Last updated by
     */
    @JsonProperty("updated_by")
    private String updatedBy;

    /**
     * Current usage statistics
     */
    @JsonProperty("usage_stats")
    private UsageStats usageStats;

    /**
     * Nested class for usage statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageStats {
        @JsonProperty("current_usage")
        private Long currentUsage;

        @JsonProperty("remaining")
        private Long remaining;

        @JsonProperty("reset_time")
        private LocalDateTime resetTime;

        @JsonProperty("last_request_time")
        private LocalDateTime lastRequestTime;

        @JsonProperty("total_requests_today")
        private Long totalRequestsToday;

        @JsonProperty("peak_usage")
        private Long peakUsage;
    }
}