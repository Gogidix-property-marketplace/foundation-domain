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
 * Enhanced Gateway Route entity with advanced routing capabilities.
 *
 * Supports:
 * - Header-based routing
 * - Query parameter-based routing
 * - Body content-based routing
 * - Weighted routing for A/B testing
 * - Canary deployments
 * - Blue-green deployments
 * - Geo-based routing
 * - User segmentation routing
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("advanced_gateway_routes")
public class AdvancedGatewayRoute extends AbstractAggregateRoot<AdvancedGatewayRoute> {

    @Id
    @Column("id")
    private UUID id;

    @Column("route_id")
    private String routeId;

    @Column("path_pattern")
    private String pathPattern;

    @Column("http_method")
    private String httpMethod;

    @Column("enabled")
    private boolean enabled;

    @Column("priority")
    private int priority;

    @Column("description")
    private String description;

    // Advanced routing configurations
    @Column("header_conditions")
    private List<HeaderCondition> headerConditions;

    @Column("query_conditions")
    private List<QueryCondition> queryConditions;

    @Column("body_conditions")
    private List<BodyCondition> bodyConditions;

    @Column("target_services")
    private List<TargetService> targetServices;

    @Column("load_balancing_strategy")
    private LoadBalancingStrategy loadBalancingStrategy;

    @Column("routing_strategy")
    private RoutingStrategy routingStrategy;

    @Column("canary_config")
    private CanaryConfig canaryConfig;

    @Column("geo_config")
    private GeoConfig geoConfig;

    @Column("segment_config")
    private SegmentConfig segmentConfig;

    @Column("transformation_config")
    private TransformationConfig transformationConfig;

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

    @Column("timeout_config")
    private TimeoutConfig timeoutConfig;

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
     * Header-based routing condition
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeaderCondition {
        private String headerName;
        private String value;
        private String operator; // EQUALS, NOT_EQUALS, CONTAINS, REGEX
        private boolean caseSensitive;
    }

    /**
     * Query parameter-based routing condition
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueryCondition {
        private String parameterName;
        private String value;
        private String operator; // EQUALS, NOT_EQUALS, CONTAINS, REGEX, EXISTS, NOT_EXISTS
    }

    /**
     * Body content-based routing condition
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BodyCondition {
        private String jsonPath; // JSONPath expression
        private String value;
        private String operator; // EQUALS, NOT_EQUALS, CONTAINS, REGEX
        private String dataType; // STRING, NUMBER, BOOLEAN, ARRAY, OBJECT
    }

    /**
     * Target service configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TargetService {
        private String serviceId;
        private String targetUri;
        private int weight; // For weighted routing
        private List<String> regions; // Geo-based routing
        private Map<String, String> headers; // Additional headers to add
        private Map<String, String> queryParams; // Additional query params to add
        private boolean primary; // For primary/backup routing
    }

    /**
     * Load balancing strategies
     */
    public enum LoadBalancingStrategy {
        ROUND_ROBIN,
        WEIGHTED_ROUND_ROBIN,
        LEAST_CONNECTIONS,
        RANDOM,
        IP_HASH,
        CONSISTENT_HASH,
        RESPONSE_TIME_BASED,
        CUSTOM
    }

    /**
     * Routing strategies
     */
    public enum RoutingStrategy {
        SIMPLE, // Single target
        WEIGHTED, // Weighted distribution
        CANARY, // Canary deployment
        BLUE_GREEN, // Blue-green deployment
        GEOGRAPHIC, // Geo-based routing
        SEGMENT_BASED, // User segment-based
        HEADER_BASED, // Header-based routing
        QUERY_BASED, // Query-based routing
        BODY_BASED, // Body-content based
        FAILOVER, // Primary/backup failover
        MIRROR // Request mirroring
    }

    /**
     * Canary deployment configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CanaryConfig {
        private double canaryPercentage; // Percentage of traffic to canary
        private boolean gradualRampUp; // Gradual increase of canary traffic
        private int rampUpDurationMinutes; // Duration for ramp-up
        private String canaryTrafficHeader; // Header to force canary routing
        private List<String> canaryUserIds; // Specific users for canary
        private List<String> canaryIpRanges; // IP ranges for canary
    }

    /**
     * Geographic routing configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeoConfig {
        private List<String> allowedCountries; // ISO country codes
        private List<String> blockedCountries;
        private List<String> allowedRegions;
        private List<String> blockedRegions;
        private boolean useIpGeoLocation;
        private String geoLocationHeader; // Header containing geo info
    }

    /**
     * User segment routing configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SegmentConfig {
        private List<String> userSegments; // VIP, PREMIUM, FREE, etc.
        private String segmentHeader; // Header containing segment info
        private String segmentCookie; // Cookie containing segment info
        private String segmentQueryParam; // Query param containing segment
        private boolean allowAnonymous;
    }

    /**
     * Request/response transformation configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransformationConfig {
        private boolean addCorrelationId;
        private boolean addTraceId;
        private boolean sanitizeHeaders;
        private List<String> headersToRemove;
        private List<String> headersToAdd;
        private Map<String, String> headerMappings;
        private boolean transformRequestBody;
        private boolean transformResponseBody;
        private String requestTransformationScript;
        private String responseTransformationScript;
    }

    /**
     * Timeout configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeoutConfig {
        private long connectTimeoutMs;
        private long readTimeoutMs;
        private long writeTimeoutMs;
        private long overallTimeoutMs;
        private boolean enableSlowRequestLogging;
        private long slowRequestThresholdMs;
    }

    /**
     * Creates a new advanced gateway route
     */
    public static AdvancedGatewayRoute create(String routeId, String pathPattern, String createdBy) {
        AdvancedGatewayRoute route = AdvancedGatewayRoute.builder()
                .id(UUID.randomUUID())
                .routeId(routeId)
                .pathPattern(pathPattern)
                .enabled(true)
                .priority(0)
                .status(RouteStatus.ACTIVE)
                .routingStrategy(RoutingStrategy.SIMPLE)
                .loadBalancingStrategy(LoadBalancingStrategy.ROUND_ROBIN)
                .createdBy(createdBy)
                .build();

        route.validateNew();
        route.registerEvent(new RouteCreatedEvent(route.getId(), route.getRouteId()));
        return route;
    }

    /**
     * Checks if the request matches this route based on all conditions
     */
    public boolean matches(RequestContext context) {
        if (!enabled || status != RouteStatus.ACTIVE) {
            return false;
        }

        // Check HTTP method
        if (httpMethod != null && !httpMethod.equals(context.getHttpMethod())) {
            return false;
        }

        // Check path pattern
        if (!context.getPath().matches(pathPattern)) {
            return false;
        }

        // Check header conditions
        if (headerConditions != null && !matchesHeaderConditions(context)) {
            return false;
        }

        // Check query conditions
        if (queryConditions != null && !matchesQueryConditions(context)) {
            return false;
        }

        // Check body conditions
        if (bodyConditions != null && !matchesBodyConditions(context)) {
            return false;
        }

        // Check role-based access
        if (!isAccessAllowed(context.getUserRoles())) {
            return false;
        }

        return true;
    }

    /**
     * Selects the appropriate target service based on routing strategy
     */
    public TargetService selectTarget(RequestContext context) {
        if (targetServices == null || targetServices.isEmpty()) {
            throw new RouteStateException("No target services configured");
        }

        return switch (routingStrategy) {
            case SIMPLE -> targetServices.get(0);
            case WEIGHTED -> selectWeightedTarget();
            case CANARY -> selectCanaryTarget(context);
            case BLUE_GREEN -> selectBlueGreenTarget(context);
            case GEOGRAPHIC -> selectGeoTarget(context);
            case SEGMENT_BASED -> selectSegmentTarget(context);
            case FAILOVER -> selectFailoverTarget();
            case MIRROR -> selectMirrorTarget();
            default -> targetServices.get(0);
        };
    }

    private boolean matchesHeaderConditions(RequestContext context) {
        Map<String, String> headers = context.getHeaders();

        for (HeaderCondition condition : headerConditions) {
            String headerValue = headers.get(condition.getHeaderName());
            if (headerValue == null) {
                return false;
            }

            if (!matchesCondition(headerValue, condition.getValue(), condition.getOperator())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesQueryConditions(RequestContext context) {
        Map<String, String> queryParams = context.getQueryParams();

        for (QueryCondition condition : queryConditions) {
            String paramValue = queryParams.get(condition.getParameterName());

            if ("EXISTS".equals(condition.getOperator())) {
                if (paramValue == null) return false;
            } else if ("NOT_EXISTS".equals(condition.getOperator())) {
                if (paramValue != null) return false;
            } else {
                if (paramValue == null) return false;
                if (!matchesCondition(paramValue, condition.getValue(), condition.getOperator())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchesBodyConditions(RequestContext context) {
        String body = context.getBody();

        for (BodyCondition condition : bodyConditions) {
            Object extractedValue = extractFromJson(body, condition.getJsonPath());
            if (extractedValue == null) {
                return false;
            }

            String stringValue = String.valueOf(extractedValue);
            if (!matchesCondition(stringValue, condition.getValue(), condition.getOperator())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesCondition(String actual, String expected, String operator) {
        switch (operator) {
            case "EQUALS":
                return actual.equals(expected);
            case "NOT_EQUALS":
                return !actual.equals(expected);
            case "CONTAINS":
                return actual.contains(expected);
            case "REGEX":
                return actual.matches(expected);
            default:
                return false;
        }
    }

    private TargetService selectWeightedTarget() {
        int totalWeight = targetServices.stream()
                .mapToInt(TargetService::getWeight)
                .sum();

        int random = (int) (Math.random() * totalWeight);
        int currentWeight = 0;

        for (TargetService service : targetServices) {
            currentWeight += service.getWeight();
            if (random < currentWeight) {
                return service;
            }
        }

        return targetServices.get(targetServices.size() - 1);
    }

    private TargetService selectCanaryTarget(RequestContext context) {
        if (canaryConfig == null || targetServices.size() < 2) {
            return targetServices.get(0);
        }

        // Check for forced canary routing
        String canaryHeader = context.getHeaders().get(canaryConfig.getCanaryTrafficHeader());
        if ("true".equals(canaryHeader)) {
            return targetServices.get(1); // Assume second service is canary
        }

        // Check for specific user IDs
        if (canaryConfig.getCanaryUserIds() != null &&
            canaryConfig.getCanaryUserIds().contains(context.getUserId())) {
            return targetServices.get(1);
        }

        // Check IP ranges
        String clientIp = context.getClientIp();
        for (String ipRange : canaryConfig.getCanaryIpRanges()) {
            if (isIpInRange(clientIp, ipRange)) {
                return targetServices.get(1);
            }
        }

        // Use percentage-based routing
        double random = Math.random() * 100;
        if (random < canaryConfig.getCanaryPercentage()) {
            return targetServices.get(1);
        }

        return targetServices.get(0);
    }

    private TargetService selectBlueGreenTarget(RequestContext context) {
        String blueGreenHeader = context.getHeaders().get("X-Blue-Green");
        if ("blue".equals(blueGreenHeader)) {
            return targetServices.get(0);
        } else if ("green".equals(blueGreenHeader)) {
            return targetServices.get(1);
        }

        // Default to primary (blue)
        return targetServices.get(0);
    }

    private TargetService selectGeoTarget(RequestContext context) {
        if (geoConfig == null) {
            return targetServices.get(0);
        }

        String country = context.getCountry();

        for (TargetService service : targetServices) {
            if (service.getRegions() != null && service.getRegions().contains(country)) {
                return service;
            }
        }

        // Check if country is blocked
        if (geoConfig.getBlockedCountries() != null &&
            geoConfig.getBlockedCountries().contains(country)) {
            throw new RouteStateException("Access blocked for country: " + country);
        }

        return targetServices.get(0);
    }

    private TargetService selectSegmentTarget(RequestContext context) {
        if (segmentConfig == null) {
            return targetServices.get(0);
        }

        String segment = context.getUserSegment();

        for (TargetService service : targetServices) {
            if (service.getRegions() != null && service.getRegions().contains(segment)) {
                return service;
            }
        }

        return targetServices.get(0);
    }

    private TargetService selectFailoverTarget() {
        // Return primary if available and healthy
        for (TargetService service : targetServices) {
            if (service.isPrimary() && isHealthy(service)) {
                return service;
            }
        }

        // Return first healthy backup
        for (TargetService service : targetServices) {
            if (!service.isPrimary() && isHealthy(service)) {
                return service;
            }
        }

        // All services down, return primary anyway
        return targetServices.stream()
                .filter(TargetService::isPrimary)
                .findFirst()
                .orElse(targetServices.get(0));
    }

    private TargetService selectMirrorTarget() {
        // For mirroring, we return primary but will mirror to all
        return targetServices.stream()
                .filter(TargetService::isPrimary)
                .findFirst()
                .orElse(targetServices.get(0));
    }

    private boolean isAccessAllowed(List<String> userRoles) {
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

    // Helper methods
    private Object extractFromJson(String json, String jsonPath) {
        // Implementation using Jackson or similar JSON library
        // For brevity, simplified implementation
        return null;
    }

    private boolean isIpInRange(String ip, String range) {
        // Implementation of IP range checking
        // For brevity, simplified implementation
        return false;
    }

    private boolean isHealthy(TargetService service) {
        // Implementation of health check
        // For brevity, always return true
        return true;
    }

    private void validateNew() {
        if (routeId == null || routeId.trim().isEmpty()) {
            throw new RouteValidationException("Route ID cannot be null or empty");
        }
        if (pathPattern == null || pathPattern.trim().isEmpty()) {
            throw new RouteValidationException("Path pattern cannot be null or empty");
        }
        if (!routeId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new RouteValidationException("Route ID can only contain alphanumeric characters, hyphens, and underscores");
        }
    }

    // Domain events (same as original)
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

    // ... other events can be added here

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

    // Configuration classes (same as original)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RateLimitConfig {
        private boolean enabled;
        private int requestsPerMinute;
        private String burstCapacity;
        private String algorithm;
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
}