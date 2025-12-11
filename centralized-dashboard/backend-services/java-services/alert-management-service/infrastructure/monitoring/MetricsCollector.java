package com.gogidix.dashboard.alerts.infrastructure.monitoring;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collector for EnterpriseTestService-EnterpriseTestService.
 * Provides custom metrics collection and reporting.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsCollector {

    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> timers = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeMetrics() {
        // Initialize custom metrics
        initializeCustomCounters();
        initializeCustomTimers();
        log.info("Metrics collector initialized");
    }

    /**
     * Records a business metric count.
     *
     * @param metricName the metric name
     * @param delta the increment value
     */
    @Counted(value = "business.metrics.count", description = "Business metrics count")
    public void recordBusinessMetric(String metricName, long delta) {
        AtomicLong counter = counters.computeIfAbsent(metricName, k -> {
            io.micrometer.core.instrument.Counter.builder("business.custom.counter")
                .tag("metric", k)
                .description("Custom business counter: " + k)
                .register(meterRegistry);
            return new AtomicLong();
        });
        counter.addAndGet(delta);
    }

    /**
     * Records a business metric execution time.
     *
     * @param metricName the metric name
     * @param timeMs execution time in milliseconds
     */
    @Timed(value = "business.metrics.time", description = "Business metrics time")
    public void recordBusinessTime(String metricName, long timeMs) {
        AtomicLong timer = timers.computeIfAbsent(metricName, k -> {
            io.micrometer.core.instrument.Timer.builder("business.custom.timer")
                .tag("metric", k)
                .description("Custom business timer: " + k)
                .register(meterRegistry);
            return new AtomicLong();
        });
        timer.set(timeMs);
    }

    /**
     * Records API request count.
     *
     * @param endpoint the API endpoint
     * @param method the HTTP method
     * @param status the response status
     */
    public void recordApiRequest(String endpoint, String method, int status) {
        meterRegistry.counter("api.requests.count",
            "endpoint", endpoint,
            "method", method,
            "status", String.valueOf(status)
        ).increment();
    }

    /**
     * Records API response time.
     *
     * @param endpoint the API endpoint
     * @param method the HTTP method
     * @param timeMs response time in milliseconds
     */
    public void recordApiResponseTime(String endpoint, String method, long timeMs) {
        meterRegistry.timer("api.requests.time",
            "endpoint", endpoint,
            "method", method
        ).record(timeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Records database query count.
     *
     * @param queryType the query type
     * @param tableName the table name
     */
    public void recordDatabaseQuery(String queryType, String tableName) {
        meterRegistry.counter("database.queries.count",
            "query_type", queryType,
            "table", tableName
        ).increment();
    }

    /**
     * Records database query time.
     *
     * @param queryType the query type
     * @param tableName the table name
     * @param timeMs query time in milliseconds
     */
    public void recordDatabaseQueryTime(String queryType, String tableName, long timeMs) {
        meterRegistry.timer("database.queries.time",
            "query_type", queryType,
            "table", tableName
        ).record(timeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Records cache hit/miss.
     *
     * @param cacheName the cache name
     * @param hit true for hit, false for miss
     */
    public void recordCacheAccess(String cacheName, boolean hit) {
        meterRegistry.counter("cache.access.count",
            "cache", cacheName,
            "result", hit ? "hit" : "miss"
        ).increment();
    }

    /**
     * Records external service call.
     *
     * @param serviceName the service name
     * @param endpoint the endpoint
     * @param status the response status
     * @param timeMs response time in milliseconds
     */
    public void recordExternalServiceCall(String serviceName, String endpoint, String status, long timeMs) {
        // Count
        meterRegistry.counter("external.service.calls.count",
            "service", serviceName,
            "endpoint", endpoint,
            "status", status
        ).increment();

        // Time
        meterRegistry.timer("external.service.calls.time",
            "service", serviceName,
            "endpoint", endpoint
        ).record(timeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Records Kafka message production.
     *
     * @param topic the Kafka topic
     * @param status the production status
     */
    public void recordKafkaMessageProduced(String topic, String status) {
        meterRegistry.counter("kafka.messages.produced.count",
            "topic", topic,
            "status", status
        ).increment();
    }

    /**
     * Records Kafka message consumption.
     *
     * @param topic the Kafka topic
     * @param status the consumption status
     */
    public void recordKafkaMessageConsumed(String topic, String status) {
        meterRegistry.counter("kafka.messages.consumed.count",
            "topic", topic,
            "status", status
        ).increment();
    }

    /**
     * Records security events.
     *
     * @param eventType the event type
     * @param userId the user ID (if available)
     */
    public void recordSecurityEvent(String eventType, String userId) {
        io.micrometer.core.instrument.Counter counter = meterRegistry.counter(
            "security.events.count",
            "event_type", eventType,
            userId != null ? "user_id" : "anonymous", userId != null ? userId : "anonymous"
        );
        counter.increment();
    }

    /**
     * Records error events.
     *
     * @param errorType the error type
     * @param component the component where error occurred
     */
    public void recordError(String errorType, String component) {
        meterRegistry.counter("errors.count",
            "error_type", errorType,
            "component", component
        ).increment();
    }

    private void initializeCustomCounters() {
        // Business process counters
        meterRegistry.gauge("business.process.active", this, obj -> getActiveProcessCount());
        meterRegistry.gauge("business.process.completed", this, obj -> getCompletedProcessCount());
    }

    private void initializeCustomTimers() {
        // Business process timers
        meterRegistry.timer("business.process.average.time",
            "description", "Average business process execution time"
        );
    }

    private double getActiveProcessCount() {
        return counters.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }

    private double getCompletedProcessCount() {
        return timers.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }
}