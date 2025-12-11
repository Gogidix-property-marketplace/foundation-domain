package com.gogidix.platform.api.client;

import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.feign.CircuitBreakerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.time.Duration;

/**
 * Configuration for Feign API clients with circuit breaker and retry logic
 *
 * @author Agent A - Platform Architect
 * @version 1.0.0
 */
@Configuration
@EnableFeignClients(basePackages = "com.gogidix.platform.api.client")
@EnableRetry
public class ApiClientConfiguration {

    /**
     * Circuit breaker configuration for API clients
     */
    @Bean
    public CircuitBreakerFactory<?> circuitBreakerFactory() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return new CustomCircuitBreakerFactory(config);
    }

    /**
     * Feign retryer configuration
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(1000, 3000, 3);
    }

    /**
     * Feign request options
     */
    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
                5000, // Connect timeout
                10000, // Read timeout
                true, // Follow redirects
                0 // Per request retry
        );
    }

    /**
     * Custom error decoder
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ApiClientErrorDecoder();
    }

    /**
     * API client properties
     */
    @Bean
    @ConfigurationProperties(prefix = "gogidix.api.client")
    public ApiClientProperties apiClientProperties() {
        return new ApiClientProperties();
    }

    /**
     * Custom circuit breaker factory implementation
     */
    private static class CustomCircuitBreakerFactory implements CircuitBreakerFactory {
        private final CircuitBreakerConfig config;

        public CustomCircuitBreakerFactory(CircuitBreakerConfig config) {
            this.config = config;
        }

        @Override
        public CircuitBreaker create(String id) {
            return CircuitBreaker.of(id + "-cb", config);
        }
    }
}