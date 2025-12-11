package com.gogidix.infrastructure.ratelimit.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for rate limit checks.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitCheckRequest {

    /**
     * Client identifier
     */
    @NotBlank(message = "Client ID is required")
    @JsonProperty("client_id")
    private String clientId;

    /**
     * API key for authentication
     */
    @NotBlank(message = "API key is required")
    @JsonProperty("api_key")
    private String apiKey;

    /**
     * API endpoint or resource being accessed
     */
    @NotBlank(message = "Endpoint is required")
    @JsonProperty("endpoint")
    private String endpoint;

    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    @NotNull(message = "HTTP method is required")
    @JsonProperty("http_method")
    private String httpMethod;

    /**
     * Request IP address
     */
    @NotBlank(message = "IP address is required")
    @JsonProperty("ip_address")
    private String ipAddress;

    /**
     * User agent
     */
    @JsonProperty("user_agent")
    private String userAgent;

    /**
     * Request timestamp (optional, defaults to current time)
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * Request weight or cost (for weighted rate limiting)
     */
    @Builder.Default
    @JsonProperty("weight")
    private Integer weight = 1;

    /**
     * Additional metadata for the request
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Tenant ID for multi-tenancy
     */
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * Request ID for tracing
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * Geographic region (for geo-based rate limiting)
     */
    @JsonProperty("region")
    private String region;
}