package com.gogidix.infrastructure.ratelimit.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for rate limit checks.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitCheckResponse {

    /**
     * Whether the request is allowed
     */
    @JsonProperty("allowed")
    private Boolean allowed;

    /**
     * Remaining requests allowed in the current window
     */
    @JsonProperty("remaining")
    private Long remaining;

    /**
     * Total requests allowed in the window
     */
    @JsonProperty("limit")
    private Long limit;

    /**
     * Seconds until the window resets
     */
    @JsonProperty("reset_time_seconds")
    private Long resetTimeSeconds;

    /**
     * Timestamp when the window resets
     */
    @JsonProperty("reset_timestamp")
    private Instant resetTimestamp;

    /**
     * Rate limiting algorithm used
     */
    @JsonProperty("algorithm")
    private String algorithm;

    /**
     * Current request count in the window
     */
    @JsonProperty("current_count")
    private Long currentCount;

    /**
     * Reason for denial (if not allowed)
     */
    @JsonProperty("reason")
    private String reason;

    /**
     * Policy ID that was applied
     */
    @JsonProperty("policy_id")
    private String policyId;

    /**
     * Response time in milliseconds
     */
    @JsonProperty("response_time_ms")
    private Long responseTimeMs;

    /**
     * Additional headers to be added to the response
     */
    @JsonProperty("headers")
    private Map<String, String> headers;

    /**
     * Whether this is a partial deny (throttling)
     */
    @JsonProperty("throttled")
    private Boolean throttled;

    /**
     * Retry after seconds (if throttled)
     */
    @JsonProperty("retry_after_seconds")
    private Long retryAfterSeconds;

    /**
     * Cache hit/miss information
     */
    @JsonProperty("cache_info")
    private Map<String, Object> cacheInfo;

    /**
     * Timestamp of the check
     */
    @JsonProperty("checked_at")
    private Instant checkedAt;

    /**
     * Request ID from the original request
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * Tenant ID
     */
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * Warning level (low, medium, high, critical)
     */
    @JsonProperty("warning_level")
    private String warningLevel;
}