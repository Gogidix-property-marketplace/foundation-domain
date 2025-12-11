package com.gogidix.infrastructure.circuitbreaker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application Properties for Circuit Breaker Service
 * Configuration properties for circuit breaker behavior and settings
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name = "Circuit Breaker Service";
    private String version = "1.0.0";
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    private Retry retry = new Retry();
    private Bulkhead bulkhead = new Bulkhead();

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public Bulkhead getBulkhead() {
        return bulkhead;
    }

    public void setBulkhead(Bulkhead bulkhead) {
        this.bulkhead = bulkhead;
    }

    public static class CircuitBreaker {
        private int failureRateThreshold = 50;
        private int waitDurationInOpenState = 30000;
        private int ringBufferSizeInHalfOpenState = 10;
        private int ringBufferSizeInClosedState = 100;

        public int getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(int failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public int getWaitDurationInOpenState() {
            return waitDurationInOpenState;
        }

        public void setWaitDurationInOpenState(int waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public int getRingBufferSizeInHalfOpenState() {
            return ringBufferSizeInHalfOpenState;
        }

        public void setRingBufferSizeInHalfOpenState(int ringBufferSizeInHalfOpenState) {
            this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
        }

        public int getRingBufferSizeInClosedState() {
            return ringBufferSizeInClosedState;
        }

        public void setRingBufferSizeInClosedState(int ringBufferSizeInClosedState) {
            this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
        }
    }

    public static class Retry {
        private int maxAttempts = 3;
        private long waitDuration = 1000;
        private double backoffMultiplier = 2.0;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getWaitDuration() {
            return waitDuration;
        }

        public void setWaitDuration(long waitDuration) {
            this.waitDuration = waitDuration;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }
    }

    public static class Bulkhead {
        private int maxConcurrentCalls = 10;
        private int maxWaitDuration = 5000;

        public int getMaxConcurrentCalls() {
            return maxConcurrentCalls;
        }

        public void setMaxConcurrentCalls(int maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
        }

        public int getMaxWaitDuration() {
            return maxWaitDuration;
        }

        public void setMaxWaitDuration(int maxWaitDuration) {
            this.maxWaitDuration = maxWaitDuration;
        }
    }
}