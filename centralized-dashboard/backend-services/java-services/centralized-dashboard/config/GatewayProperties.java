package com.gogidix.infrastructure.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration properties for API Gateway.
 * Contains all gateway-specific configuration parameters.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    /**
     * Circuit breaker configuration
     */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    /**
     * Rate limiting configuration
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * Security configuration
     */
    private Security security = new Security();

    /**
     * Service routes configuration
     */
    private Routes routes = new Routes();

    /**
     * Retry configuration
     */
    private Retry retry = new Retry();

    @Data
    public static class CircuitBreaker {
        private int failureRateThreshold = 50;
        private long waitDurationInOpenState = 30000; // 30 seconds
        private int slidingWindowSize = 10;
        private int permittedNumberOfCallsInHalfOpenState = 3;
        private boolean automaticTransitionFromOpenToHalfOpenEnabled = true;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int defaultLimit = 100;
        private long windowSeconds = 60;
        private Map<String, TierConfig> tiers = Map.of(
                "BASIC", new TierConfig(50, 60),
                "PREMIUM", new TierConfig(500, 60),
                "ENTERPRISE", new TierConfig(5000, 60)
        );
    }

    @Data
    public static class TierConfig {
        private int limit;
        private long windowSeconds;

        public TierConfig(int limit, long windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }
    }

    @Data
    public static class Security {
        private boolean enabled = true;
        private String jwtSecret = "${JWT_SECRET:defaultSecretKey}";
        private long jwtExpirationMs = 86400000; // 24 hours
        private String[] publicPaths = {
                "/api/v1/auth/login",
                "/api/v1/auth/register",
                "/health",
                "/actuator/health",
                "/actuator/info",
                "/favicon.ico",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        };
    }

    @Data
    public static class Routes {
        private boolean discoveryEnabled = true;
        private int timeoutMs = 5000;
        private int connectionTimeoutMs = 3000;
        private boolean loadBalancingEnabled = true;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long backoffMs = 1000;
        private double multiplier = 2.0;
        private String[] retryableStatusCodes = {"502", "503", "504"};
        private String[] retryableExceptions = {
                "java.net.ConnectException",
                "java.net.SocketTimeoutException",
                "org.springframework.cloud.gateway.support.TimeoutException"
        };
    }
}