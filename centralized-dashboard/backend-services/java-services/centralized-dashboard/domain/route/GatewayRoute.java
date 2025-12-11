package com.gogidix.infrastructure.gateway.domain.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing a Gateway Route configuration.
 * Implements DDD patterns with business logic encapsulation.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("gateway_routes")
public class GatewayRoute extends AbstractAggregateRoot<GatewayRoute> {

    @Id
    @Column("id")
    private UUID id;

    @Column("route_id")
    private String routeId;

    @Column("path_pattern")
    private String pathPattern;

    @Column("target_uri")
    private String targetUri;

    @Column("http_method")
    private String httpMethod;

    @Column("enabled")
    private boolean enabled;

    @Column("priority")
    private int priority;

    @Column("description")
    private String description;

    @Column("allowed_roles")
    private List<String> allowedRoles;

    @Column("blocked_roles")
    private List<String> blockedRoles;

    @Column("metadata")
    private Map<String, Object> metadata;

    // JSON columns for configuration
    @Column("rate_limit_config")
    private RateLimitConfig rateLimitConfig;

    @Column("circuit_breaker_config")
    private CircuitBreakerConfig circuitBreakerConfig;

    @Column("retry_config")
    private RetryConfig retryConfig;

    @Column("status")
    private RouteStatus status;

    // Audit fields
    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("updated_by")
    private String updatedBy;

    @Version
    @Column("version")
    private Long version;

    /**
     * Creates a new gateway route with validation.
     */
    public static GatewayRoute create(String routeId, String pathPattern, String targetUri, String createdBy) {
        GatewayRoute route = GatewayRoute.builder()
                .id(UUID.randomUUID())
                .routeId(routeId)
                .pathPattern(pathPattern)
                .targetUri(targetUri)
                .enabled(true)
                .priority(0)
                .status(RouteStatus.ACTIVE)
                .createdBy(createdBy)
                .build();

        route.validateNew();
        route.registerEvent(new RouteCreatedEvent(route.getId(), route.getRouteId()));
        return route;
    }

    /**
     * Updates the route configuration.
     */
    public void update(String pathPattern, String targetUri, String httpMethod, String description, String updatedBy) {
        validateUpdate(pathPattern, targetUri);

        this.pathPattern = pathPattern;
        this.targetUri = targetUri;
        this.httpMethod = httpMethod;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new RouteUpdatedEvent(this.getId(), this.getRouteId()));
    }

    /**
     * Enables the route.
     */
    public void enable(String updatedBy) {
        if (this.enabled) {
            throw new RouteStateException("Route is already enabled");
        }

        this.enabled = true;
        this.status = RouteStatus.ACTIVE;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new RouteEnabledEvent(this.getId(), this.getRouteId()));
    }

    /**
     * Disables the route.
     */
    public void disable(String updatedBy) {
        if (!this.enabled) {
            throw new RouteStateException("Route is already disabled");
        }

        this.enabled = false;
        this.status = RouteStatus.INACTIVE;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new RouteDisabledEvent(this.getId(), this.getRouteId()));
    }

    /**
     * Archives the route.
     */
    public void archive(String archivedBy) {
        if (this.status == RouteStatus.ARCHIVED) {
            throw new RouteStateException("Route is already archived");
        }

        this.status = RouteStatus.ARCHIVED;
        this.enabled = false;
        this.updatedBy = archivedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new RouteArchivedEvent(this.getId(), this.getRouteId()));
    }

    /**
     * Validates if the given user roles are allowed to access this route.
     */
    public boolean isAccessAllowed(List<String> userRoles) {
        if (!enabled || status != RouteStatus.ACTIVE) {
            return false;
        }

        if (userRoles == null || userRoles.isEmpty()) {
            return blockedRoles == null || blockedRoles.isEmpty();
        }

        // Check if user has any blocked role
        if (blockedRoles != null) {
            for (String blockedRole : blockedRoles) {
                if (userRoles.contains(blockedRole)) {
                    return false;
                }
            }
        }

        // Check if user has any allowed role (if specified)
        if (allowedRoles != null && !allowedRoles.isEmpty()) {
            return userRoles.stream().anyMatch(allowedRoles::contains);
        }

        return true;
    }

    /**
     * Updates rate limiting configuration.
     */
    public void updateRateLimitConfig(RateLimitConfig config, String updatedBy) {
        this.rateLimitConfig = config;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new RateLimitConfigUpdatedEvent(this.getId(), this.getRouteId()));
    }

    /**
     * Updates circuit breaker configuration.
     */
    public void updateCircuitBreakerConfig(CircuitBreakerConfig config, String updatedBy) {
        this.circuitBreakerConfig = config;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new CircuitBreakerConfigUpdatedEvent(this.getId(), this.getRouteId()));
    }

    /**
     * Updates retry configuration.
     */
    public void updateRetryConfig(RetryConfig config, String updatedBy) {
        this.retryConfig = config;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new RetryConfigUpdatedEvent(this.getId(), this.getRouteId()));
    }

    private void validateNew() {
        if (routeId == null || routeId.trim().isEmpty()) {
            throw new RouteValidationException("Route ID cannot be null or empty");
        }
        if (pathPattern == null || pathPattern.trim().isEmpty()) {
            throw new RouteValidationException("Path pattern cannot be null or empty");
        }
        if (targetUri == null || targetUri.trim().isEmpty()) {
            throw new RouteValidationException("Target URI cannot be null or empty");
        }
        if (!routeId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new RouteValidationException("Route ID can only contain alphanumeric characters, hyphens, and underscores");
        }
    }

    private void validateUpdate(String pathPattern, String targetUri) {
        if (pathPattern == null || pathPattern.trim().isEmpty()) {
            throw new RouteValidationException("Path pattern cannot be null or empty");
        }
        if (targetUri == null || targetUri.trim().isEmpty()) {
            throw new RouteValidationException("Target URI cannot be null or empty");
        }
    }

    // Domain events
    public static class RouteCreatedEvent {
        private final UUID routeId;
        private final String routeIdentifier;

        public RouteCreatedEvent(UUID routeId, String routeIdentifier) {
            this.routeId = routeId;
            this.routeIdentifier = routeIdentifier;
        }

        public UUID getRouteId() { return routeId; }
        public String getRouteIdentifier() { return routeIdentifier; }
    }

    public static class RouteUpdatedEvent {
        private final UUID routeId;
        private final String routeIdentifier;

        public RouteUpdatedEvent(UUID routeId, String routeIdentifier) {
            this.routeId = routeId;
            this.routeIdentifier = routeIdentifier;
        }

        public UUID getRouteId() { return routeId; }
        public String getRouteIdentifier() { return routeIdentifier; }
    }

    public static class RouteEnabledEvent {
        private final UUID routeId;
        private final String routeIdentifier;

        public RouteEnabledEvent(UUID routeId, String routeIdentifier) {
            this.routeId = routeId;
            this.routeIdentifier = routeIdentifier;
        }

        public UUID getRouteId() { return routeId; }
        public String getRouteIdentifier() { return routeIdentifier; }
    }

    public static class RouteDisabledEvent {
        private final UUID routeId;
        private final String routeIdentifier;

        public RouteDisabledEvent(UUID routeId, String routeIdentifier) {
            this.routeId = routeId;
            this.routeIdentifier = routeIdentifier;
        }

        public UUID getRouteId() { return routeId; }
        public String getRouteIdentifier() { return routeIdentifier; }
    }

    public static class RouteArchivedEvent {
        private final UUID routeId;
        private final String routeIdentifier;

        public RouteArchivedEvent(UUID routeId, String routeIdentifier) {
            this.routeId = routeId;
            this.routeIdentifier = routeIdentifier;
        }

        public UUID getRouteId() { return routeId; }
        public String getRouteIdentifier() { return routeIdentifier; }
    }

    public static class RateLimitConfigUpdatedEvent {
        private final UUID routeId;
        private final String routeIdentifier;

        public RateLimitConfigUpdatedEvent(UUID routeId, String routeIdentifier) {
            this.routeId = routeId;
            this.routeIdentifier = routeIdentifier;
        }

        public UUID getRouteId() { return routeId; }
        public String getRouteIdentifier() { return routeIdentifier; }
    }

    public static class CircuitBreakerConfigUpdatedEvent {
        private final UUID routeId;
        private final String routeIdentifier;

        public CircuitBreakerConfigUpdatedEvent(UUID routeId, String routeIdentifier) {
            this.routeId = routeId;
            this.routeIdentifier = routeIdentifier;
        }

        public UUID getRouteId() { return routeId; }
        public String getRouteIdentifier() { return routeIdentifier; }
    }

    public static class RetryConfigUpdatedEvent {
        private final UUID routeId;
        private final String routeIdentifier;

        public RetryConfigUpdatedEvent(UUID routeId, String routeIdentifier) {
            this.routeId = routeId;
            this.routeIdentifier = routeIdentifier;
        }

        public UUID getRouteId() { return routeId; }
        public String getRouteIdentifier() { return routeIdentifier; }
    }

    // Configuration classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RateLimitConfig {
        private boolean enabled;
        private int requestsPerMinute;
        private String burstCapacity;
        private String algorithm; // TOKEN_BUCKET, SLIDING_WINDOW, FIXED_WINDOW
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CircuitBreakerConfig {
        private boolean enabled;
        private int failureThreshold;
        private int timeoutSeconds;
        private String fallbackUri;
        private String halfOpenMaxCalls;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetryConfig {
        private boolean enabled;
        private int maxAttempts;
        private long backoffMs;
        private double multiplier;
        private List<String> retryableStatusCodes;
        private List<String> retryableExceptions;
    }

    public enum RouteStatus {
        ACTIVE,
        INACTIVE,
        ARCHIVED,
        DRAFT
    }

    // Domain exceptions
    public static class RouteValidationException extends RuntimeException {
        public RouteValidationException(String message) {
            super(message);
        }
    }

    public static class RouteStateException extends RuntimeException {
        public RouteStateException(String message) {
            super(message);
        }
    }
}