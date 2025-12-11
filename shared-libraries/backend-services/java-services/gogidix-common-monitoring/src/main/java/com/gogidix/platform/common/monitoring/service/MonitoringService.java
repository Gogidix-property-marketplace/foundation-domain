package com.gogidix.platform.common.monitoring.service;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Health indicator imports removed - will be available only when actuator is on classpath
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

/**
 * Service for managing application metrics and monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final MeterRegistry meterRegistry;

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DistributionSummary> summaries = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeMetrics() {
        log.info("Initializing monitoring service with registry: {}", meterRegistry.getClass().getSimpleName());

        // Register JVM metrics
        registerJvmMetrics();

        // Register system metrics
        registerSystemMetrics();

        // Register application metrics
        registerApplicationMetrics();
    }

    /**
     * Increment a counter metric
     */
    public void incrementCounter(String name, String... tags) {
        Counter counter = counters.computeIfAbsent(name,
            key -> Counter.builder(name)
                .tags(tags)
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * Record a timer metric
     */
    public void recordTimer(String name, Duration duration, String... tags) {
        Timer timer = timers.computeIfAbsent(name,
            key -> Timer.builder(name)
                .tags(tags)
                .register(meterRegistry));
        timer.record(duration);
    }

    /**
     * Record a timer metric with runnable task
     */
    public void recordTimer(String name, Runnable task, String... tags) {
        Timer timer = timers.computeIfAbsent(name,
            key -> Timer.builder(name)
                .tags(tags)
                .register(meterRegistry));
        timer.record(task);
    }

    /**
     * Record a gauge metric
     */
    public <T> void registerGauge(String name, T obj, ToDoubleFunction<T> valueFunction, String... tags) {
        Gauge.builder(name, obj, valueFunction)
            .tags(tags)
            .register(meterRegistry);
    }

    /**
     * Record a distribution summary
     */
    public void recordSummary(String name, double amount, String... tags) {
        DistributionSummary summary = summaries.computeIfAbsent(name,
            key -> DistributionSummary.builder(name)
                .tags(tags)
                .register(meterRegistry));
        summary.record(amount);
    }

    /**
     * Create a custom counter builder
     */
    public Counter.Builder counterBuilder(String name) {
        return Counter.builder(name);
    }

    /**
     * Create a custom timer builder
     */
    public Timer.Builder timerBuilder(String name) {
        return Timer.builder(name);
    }

    /**
     * Create a custom gauge builder
     */
    public <T> Gauge.Builder<T> gaugeBuilder(String name, T obj, ToDoubleFunction<T> valueFunction) {
        return Gauge.builder(name, obj, valueFunction);
    }

    /**
     * Create a custom distribution summary builder
     */
    public DistributionSummary.Builder summaryBuilder(String name) {
        return DistributionSummary.builder(name);
    }

    /**
     * Get current value of a gauge
     */
    public double getGaugeValue(String name) {
        Gauge gauge = gauges.get(name);
        return gauge != null ? gauge.value() : 0.0;
    }

    /**
     * Get total count of a counter
     */
    public double getCounterCount(String name) {
        Counter counter = counters.get(name);
        return counter != null ? counter.count() : 0.0;
    }

    /**
     * Record business metric
     */
    public void recordBusinessMetric(String eventType, String serviceName, Duration duration, boolean success) {
        // Record processing time
        recordTimer("business.processing.time", duration,
            "event_type", eventType,
            "service", serviceName,
            "success", String.valueOf(success));

        // Record event count
        incrementCounter("business.events.count",
            "event_type", eventType,
            "service", serviceName,
            "success", String.valueOf(success));

        // Record success rate
        incrementCounter("business.success.rate",
            "service", serviceName,
            "success", String.valueOf(success));
    }

    /**
     * Record API call metrics
     */
    public void recordApiCall(String method, String endpoint, int statusCode, Duration duration) {
        // Record response time
        recordTimer("api.response.time", duration,
            "method", method,
            "endpoint", endpoint,
            "status", String.valueOf(statusCode));

        // Record request count
        incrementCounter("api.requests.count",
            "method", method,
            "endpoint", endpoint,
            "status", String.valueOf(statusCode));

        // Record error rate if status is error
        if (statusCode >= 400) {
            incrementCounter("api.errors.count",
                "method", method,
                "endpoint", endpoint,
                "status", String.valueOf(statusCode));
        }
    }

    /**
     * Record database operation metrics
     */
    public void recordDatabaseOperation(String operation, String table, Duration duration, boolean success) {
        // Record query time
        recordTimer("database.query.time", duration,
            "operation", operation,
            "table", table,
            "success", String.valueOf(success));

        // Record query count
        incrementCounter("database.queries.count",
            "operation", operation,
            "table", table,
            "success", String.valueOf(success));

        // Record slow queries (> 1 second)
        if (duration.toMillis() > 1000) {
            incrementCounter("database.slow.queries.count",
                "operation", operation,
                "table", table);
        }
    }

    /**
     * Record cache operation metrics
     */
    public void recordCacheOperation(String operation, String cacheName, boolean hit) {
        // Record cache operation count
        incrementCounter("cache.operations.count",
            "operation", operation,
            "cache", cacheName);

        // Record cache hit rate
        incrementCounter("cache.hits.count",
            "cache", cacheName,
            "hit", String.valueOf(hit));
    }

    /**
     * Record external service call metrics
     */
    public void recordExternalServiceCall(String serviceName, String operation, Duration duration, boolean success) {
        // Record response time
        recordTimer("external.service.response.time", duration,
            "service", serviceName,
            "operation", operation,
            "success", String.valueOf(success));

        // Record call count
        incrementCounter("external.service.calls.count",
            "service", serviceName,
            "operation", operation,
            "success", String.valueOf(success));

        // Record error count
        if (!success) {
            incrementCounter("external.service.errors.count",
                "service", serviceName,
                "operation", operation);
        }
    }

    private void registerJvmMetrics() {
        // Memory metrics
        registerGauge("jvm.memory.used", Runtime.getRuntime(),
            runtime -> (double) (runtime.totalMemory() - runtime.freeMemory()));

        registerGauge("jvm.memory.max", Runtime.getRuntime(),
            runtime -> (double) runtime.maxMemory());

        registerGauge("jvm.memory.total", Runtime.getRuntime(),
            runtime -> (double) runtime.totalMemory());

        // GC metrics (available with Micrometer JVM metrics)
        new io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics().bindTo(meterRegistry);
        new io.micrometer.core.instrument.binder.jvm.JvmGcMetrics().bindTo(meterRegistry);
        new io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics().bindTo(meterRegistry);
        new io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics().bindTo(meterRegistry);
    }

    private void registerSystemMetrics() {
        // System metrics (available with Micrometer system metrics)
        new io.micrometer.core.instrument.binder.system.ProcessorMetrics().bindTo(meterRegistry);
        // SystemMetrics class doesn't exist - removed
        new io.micrometer.core.instrument.binder.system.UptimeMetrics().bindTo(meterRegistry);
    }

    private void registerApplicationMetrics() {
        // Application-specific metrics
        AtomicLong activeUsers = new AtomicLong(0);
        registerGauge("application.active.users", activeUsers, AtomicLong::get);

        AtomicLong processedEvents = new AtomicLong(0);
        registerGauge("application.processed.events", processedEvents, AtomicLong::get);
    }

    /**
     * Custom health indicator for application-specific health checks
     * This class will only be used when Spring Boot Actuator is on the classpath
     */
    public static class ApplicationHealthIndicator {
        // Note: This class should implement HealthIndicator only when actuator is available
        // For now, it's a placeholder for health management functionality

        private volatile boolean isHealthy = true;
        private volatile String message = "Application is healthy";

        public void setHealthy(boolean healthy, String message) {
            this.isHealthy = healthy;
            this.message = message;
        }

        public boolean isHealthy() {
            return isHealthy;
        }

        public String getMessage() {
            return message;
        }

        // Health health() method would be implemented when actuator is available
        // public Health health() { ... }
    }

    /**
     * Metrics holder for custom business metrics
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class BusinessMetrics {
        private String eventType;
        private String serviceName;
        private Duration processingTime;
        private boolean success;
        private String userId;
        private String sessionId;
    }

    /**
     * Metrics holder for API metrics
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ApiMetrics {
        private String method;
        private String endpoint;
        private int statusCode;
        private Duration responseTime;
        private String userAgent;
        private String clientIp;
    }

    /**
     * Metrics holder for database metrics
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DatabaseMetrics {
        private String operation;
        private String table;
        private Duration queryTime;
        private boolean success;
        private int resultCount;
        private String queryType;
    }

    /**
     * Metrics holder for cache metrics
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CacheMetrics {
        private String operation;
        private String cacheName;
        private boolean hit;
        private String key;
        private Duration accessTime;
    }

    /**
     * Metrics holder for external service metrics
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ExternalServiceMetrics {
        private String serviceName;
        private String operation;
        private Duration responseTime;
        private boolean success;
        private String errorMessage;
        private int retryCount;
    }
}