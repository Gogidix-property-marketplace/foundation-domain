package com.gogidix.infrastructure.ratelimit.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rate Limit Policy entity for defining rate limiting rules.
 *
 * Supports multiple algorithms:
 * - Token Bucket: Allows burst traffic with sustained rate
 * - Sliding Window: Fixed window with time-based reset
 * - Fixed Window: Simple counter-based limiting
 * - Leaky Bucket: Smooths traffic at constant rate
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Entity
@Table(name = "rate_limit_policies", indexes = {
    @Index(name = "idx_client_id", columnList = "clientId"),
    @Index(name = "idx_api_key", columnList = "apiKey"),
    @Index(name = "idx_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_is_active", columnList = "isActive")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique client identifier
     */
    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    /**
     * API key for authentication
     */
    @Column(name = "api_key", nullable = false, length = 255)
    private String apiKey;

    /**
     * Tenant/organization ID for multi-tenancy
     */
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * API endpoint or resource identifier
     */
    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;

    /**
     * Rate limiting algorithm
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm", nullable = false)
    private RateLimitAlgorithm algorithm;

    /**
     * Maximum requests allowed
     */
    @Column(name = "max_requests", nullable = false)
    private Integer maxRequests;

    /**
     * Time window in seconds
     */
    @Column(name = "time_window_seconds", nullable = false)
    private Integer timeWindowSeconds;

    /**
     * Burst capacity (for token bucket)
     */
    @Column(name = "burst_capacity")
    private Integer burstCapacity;

    /**
     * Refill rate per second (for token bucket)
     */
    @Column(name = "refill_rate_per_second", precision = 10, scale = 4)
    private BigDecimal refillRatePerSecond;

    /**
     * Whether this policy is currently active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Priority of this policy (higher = more priority)
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 100;

    /**
     * Description of the policy
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Metadata as JSON
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Created timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last updated timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Created by
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Last updated by
     */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}