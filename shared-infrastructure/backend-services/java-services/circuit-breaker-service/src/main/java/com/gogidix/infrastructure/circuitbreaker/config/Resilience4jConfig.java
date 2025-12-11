package com.gogidix.infrastructure.circuitbreaker.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Resilience4j components.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "resilience4j")
@Validated
public class Resilience4jConfig {

    @Valid
    @NotNull
    private CircuitBreakerProperties circuitbreaker = new CircuitBreakerProperties();

    @Valid
    @NotNull
    private RetryProperties retry = new RetryProperties();

    @Valid
    @NotNull
    private BulkheadProperties bulkhead = new BulkheadProperties();

    @Valid
    @NotNull
    private RateLimiterProperties ratelimiter = new RateLimiterProperties();

    @Valid
    @NotNull
    private TimeLimiterProperties timelimiter = new TimeLimiterProperties();

    @Valid
    @NotNull
    private InstanceProperties instances = new InstanceProperties();

    /**
     * Circuit breaker configuration
     */
    @Data
    public static class CircuitBreakerProperties {
        private boolean enabled = true;
        private Config configs = new Config();

        @Data
        public static class Config {
            private CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)
                    .slowCallRateThreshold(100)
                    .slowCallDurationThreshold(Duration.ofSeconds(5))
                    .permittedNumberOfCallsInHalfOpenState(10)
                    .minimumNumberOfCalls(100)
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                    .slidingWindowSize(100)
                    .waitDurationInOpenState(Duration.ofSeconds(30))
                    .automaticTransitionFromOpenToHalfOpenEnabled(true)
                    .build();

            private Map<String, CircuitBreakerConfig> instances = new HashMap<>();

            public Map<String, CircuitBreakerConfig> getInstances() {
                Map<String, CircuitBreakerConfig> configs = new HashMap<>();
                configs.put("default", defaultConfig);

                // Add predefined configurations for common services
                configs.put("database", CircuitBreakerConfig.custom()
                        .failureRateThreshold(60)
                        .slowCallDurationThreshold(Duration.ofSeconds(3))
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .minimumNumberOfCalls(20)
                        .slidingWindowSize(50)
                        .build());

                configs.put("external-api", CircuitBreakerConfig.custom()
                        .failureRateThreshold(40)
                        .slowCallDurationThreshold(Duration.ofSeconds(10))
                        .waitDurationInOpenState(Duration.ofSeconds(120))
                        .minimumNumberOfCalls(10)
                        .slidingWindowSize(20)
                        .build());

                configs.put("cache-service", CircuitBreakerConfig.custom()
                        .failureRateThreshold(70)
                        .slowCallDurationThreshold(Duration.ofSeconds(1))
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .minimumNumberOfCalls(50)
                        .slidingWindowSize(100)
                        .build());

                // Add custom instances
                if (instances != null) {
                    configs.putAll(instances);
                }

                return configs;
            }
        }
    }

    /**
     * Retry configuration
     */
    @Data
    public static class RetryProperties {
        private boolean enabled = true;
        private Config configs = new Config();

        @Data
        public static class Config {
            private RetryConfig defaultConfig = RetryConfig.custom()
                    .maxAttempts(3)
                    .waitDuration(Duration.ofMillis(100))
                    .intervalFunction(attempt -> attempt * 100L)
                    .retryOnException(e -> {
                        if (e instanceof java.net.SocketTimeoutException) return true;
                        if (e instanceof java.net.ConnectException) return true;
                        if (e instanceof java.io.IOException) return true;
                        if (e instanceof org.springframework.web.client.HttpServerErrorException) return true;
                        return false;
                    })
                    .retryExceptions(
                            java.net.SocketTimeoutException.class,
                            java.net.ConnectException.class,
                            java.io.IOException.class,
                            org.springframework.web.client.HttpServerErrorException.class
                    )
                    .ignoreExceptions(
                            IllegalArgumentException.class,
                            NullPointerException.class
                    )
                    .build();

            private Map<String, RetryConfig> instances = new HashMap<>();

            public Map<String, RetryConfig> getInstances() {
                Map<String, RetryConfig> configs = new HashMap<>();
                configs.put("default", defaultConfig);

                // Add predefined configurations
                configs.put("critical", RetryConfig.custom()
                        .maxAttempts(5)
                        .waitDuration(Duration.ofMillis(50))
                        .exponentialBackoffMultiplier(2)
                        .maxWaitDuration(Duration.ofSeconds(30))
                        .build());

                configs.put("non-critical", RetryConfig.custom()
                        .maxAttempts(2)
                        .waitDuration(Duration.ofMillis(200))
                        .build());

                // Add custom instances
                if (instances != null) {
                    configs.putAll(instances);
                }

                return configs;
            }
        }
    }

    /**
     * Bulkhead configuration
     */
    @Data
    public static class BulkheadProperties {
        private boolean enabled = true;
        private Config configs = new Config();

        @Data
        public static class Config {
            private BulkheadConfig defaultConfig = BulkheadConfig.custom()
                    .maxConcurrentCalls(100)
                    .maxWaitDuration(Duration.ofSeconds(5))
                    .build();

            private Map<String, BulkheadConfig> instances = new HashMap<>();

            public Map<String, BulkheadConfig> getInstances() {
                Map<String, BulkheadConfig> configs = new HashMap<>();
                configs.put("default", defaultConfig);

                // Add predefined configurations
                configs.put("io-intensive", BulkheadConfig.custom()
                        .maxConcurrentCalls(10)
                        .maxWaitDuration(Duration.ofSeconds(10))
                        .build());

                configs.put("cpu-intensive", BulkheadConfig.custom()
                        .maxConcurrentCalls(50)
                        .maxWaitDuration(Duration.ofSeconds(1))
                        .build());

                configs.put("memory-intensive", BulkheadConfig.custom()
                        .maxConcurrentCalls(20)
                        .maxWaitDuration(Duration.ofSeconds(5))
                        .build());

                // Add custom instances
                if (instances != null) {
                    configs.putAll(instances);
                }

                return configs;
            }
        }
    }

    /**
     * Rate limiter configuration
     */
    @Data
    public static class RateLimiterProperties {
        private boolean enabled = true;
        private Config configs = new Config();

        @Data
        public static class Config {
            private RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                    .limitForPeriod(100)
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .timeoutDuration(Duration.ofSeconds(1))
                    .build();

            private Map<String, RateLimiterConfig> instances = new HashMap<>();

            public Map<String, RateLimiterConfig> getInstances() {
                Map<String, RateLimiterConfig> configs = new HashMap<>();
                configs.put("default", defaultConfig);

                // Add predefined configurations
                configs.put("strict", RateLimiterConfig.custom()
                        .limitForPeriod(10)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ofMillis(500))
                        .build());

                configs.put("lenient", RateLimiterConfig.custom()
                        .limitForPeriod(1000)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ofMillis(100))
                        .build());

                configs.put("user-specific", RateLimiterConfig.custom()
                        .limitForPeriod(20)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build());

                // Add custom instances
                if (instances != null) {
                    configs.putAll(instances);
                }

                return configs;
            }
        }
    }

    /**
     * Time limiter configuration
     */
    @Data
    public static class TimeLimiterProperties {
        private boolean enabled = true;
        private Config configs = new Config();

        @Data
        public static class Config {
            private TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(10))
                    .cancelRunningFuture(true)
                    .build();

            private Map<String, TimeLimiterConfig> instances = new HashMap<>();

            public Map<String, TimeLimiterConfig> getInstances() {
                Map<String, TimeLimiterConfig> configs = new HashMap<>();
                configs.put("default", defaultConfig);

                // Add predefined configurations
                configs.put("fast", TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build());

                configs.put("slow", TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(30))
                        .build());

                configs.put("real-time", TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofMillis(500))
                        .build());

                // Add custom instances
                if (instances != null) {
                    configs.putAll(instances);
                }

                return configs;
            }
        }
    }

    /**
     * Instance-specific configurations
     */
    @Data
    public static class InstanceProperties {
        private Map<String, InstanceConfig> configs = new HashMap<>();

        @Data
        public static class InstanceConfig {
            private String circuitbreaker = "default";
            private String retry = "default";
            private String bulkhead = "default";
            private String ratelimiter = "default";
            private String timelimiter = "default";

            private boolean enableCircuitbreaker = true;
            private boolean enableRetry = true;
            private boolean enableBulkhead = false;
            private boolean enableRatelimiter = false;
            private boolean enableTimelimiter = false;
        }

        /**
         * Gets configuration for a specific instance
         */
        public InstanceConfig get(String instanceName) {
            return configs.get(instanceName);
        }

        /**
         * Gets default configuration for instances not explicitly configured
         */
        public InstanceConfig getDefault() {
            InstanceConfig defaultConfig = new InstanceConfig();
            defaultConfig.setEnableCircuitbreaker(true);
            defaultConfig.setEnableRetry(true);
            defaultConfig.setEnableBulkhead(false);
            defaultConfig.setEnableRatelimiter(false);
            defaultConfig.setEnableTimelimiter(false);
            return defaultConfig;
        }
    }
}