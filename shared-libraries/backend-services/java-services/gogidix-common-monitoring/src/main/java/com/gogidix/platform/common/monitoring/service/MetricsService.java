package com.gogidix.platform.common.monitoring.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToDoubleFunction;

/**
 * Service for managing application metrics with Micrometer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    /**
     * Record a counter metric
     */
    public void incrementCounter(String metricName, String... tags) {
        Counter counter = Counter.builder(metricName)
                .tags(tags)
                .description("Counter metric for " + metricName)
                .register(meterRegistry);
        counter.increment();
        log.debug("Incremented counter: {}", metricName);
    }

    /**
     * Record a counter metric with custom amount
     */
    public void incrementCounter(String metricName, double amount, String... tags) {
        Counter counter = Counter.builder(metricName)
                .tags(tags)
                .description("Counter metric for " + metricName)
                .register(meterRegistry);
        counter.increment(amount);
        log.debug("Incremented counter: {} by {}", metricName, amount);
    }

    /**
     * Record a timer metric for execution time
     */
    public Timer startTimer(String metricName, String... tags) {
        Timer timer = Timer.builder(metricName)
                .tags(tags)
                .description("Timer metric for " + metricName)
                .register(meterRegistry);
        log.debug("Started timer: {}", metricName);
        return timer;
    }

    /**
     * Record a timer sample
     */
    public void recordTimer(Timer.Sample sample, String metricName, Duration duration, String... tags) {
        Timer timer = Timer.builder(metricName)
                .tags(tags)
                .description("Timer metric for " + metricName)
                .register(meterRegistry);
        sample.stop(timer);
        log.debug("Recorded timer: {} with duration: {}", metricName, duration);
    }

    /**
     * Record execution time directly
     */
    public void recordExecutionTime(String metricName, Duration duration, String... tags) {
        Timer timer = Timer.builder(metricName)
                .tags(tags)
                .description("Timer metric for " + metricName)
                .register(meterRegistry);
        timer.record(duration);
        log.debug("Recorded execution time: {} with duration: {}", metricName, duration);
    }

    /**
     * Create a gauge metric
     */
    public <T> void registerGauge(String metricName, T obj, ToDoubleFunction<T> valueFunction, String... tags) {
        Gauge.builder(metricName, obj, valueFunction)
                .tags(tags)
                .description("Gauge metric for " + metricName)
                .register(meterRegistry);
        log.debug("Registered gauge: {}", metricName);
    }

    /**
     * Create a gauge metric with atomic long
     */
    public AtomicLong registerGauge(String metricName, String... tags) {
        AtomicLong gauge = new AtomicLong(0);
        Gauge.builder(metricName, gauge, AtomicLong::get)
                .tags(tags)
                .description("Gauge metric for " + metricName)
                .register(meterRegistry);
        log.debug("Registered gauge: {}", metricName);
        return gauge;
    }

    /**
     * Record a distribution summary
     */
    public void recordDistribution(String metricName, double amount, String... tags) {
        meterRegistry.summary(metricName, tags)
                .record(amount);
        log.debug("Recorded distribution: {} with amount: {}", metricName, amount);
    }

    /**
     * Get current value of a counter
     */
    public double getCounterValue(String metricName, String... tags) {
        Counter counter = meterRegistry.find(metricName)
                .tags(tags)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }

    /**
     * Get current value of a gauge
     */
    public double getGaugeValue(String metricName, String... tags) {
        Gauge gauge = meterRegistry.find(metricName)
                .tags(tags)
                .gauge();
        return gauge != null ? gauge.value() : 0.0;
    }

    /**
     * Check if a metric exists
     */
    public boolean hasMetric(String metricName) {
        return meterRegistry.find(metricName).meter() != null;
    }

    /**
     * Remove a metric
     */
    public void removeMetric(String metricName) {
        meterRegistry.remove(meterRegistry.find(metricName).meter());
        log.debug("Removed metric: {}", metricName);
    }

    /**
     * Create a builder for custom metrics
     */
    public Timer.Builder timerBuilder(String metricName) {
        return Timer.builder(metricName)
                .description("Timer metric for " + metricName);
    }

    /**
     * Create a builder for custom counters
     */
    public Counter.Builder counterBuilder(String metricName) {
        return Counter.builder(metricName)
                .description("Counter metric for " + metricName);
    }

    /**
     * Create a builder for custom gauges
     */
    public <T> Gauge.Builder<T> gaugeBuilder(String metricName, T obj, ToDoubleFunction<T> valueFunction) {
        return Gauge.builder(metricName, obj, valueFunction)
                .description("Gauge metric for " + metricName);
    }

    /**
     * Record custom business metrics
     */
    public void recordBusinessMetric(String eventName, String category, Object value, String... additionalTags) {
        String[] allTags = Arrays.copyOf(additionalTags, additionalTags.length + 2);
        allTags[additionalTags.length] = "category";
        allTags[additionalTags.length + 1] = category;

        if (value instanceof Number) {
            recordDistribution("business.metric." + eventName, ((Number) value).doubleValue(), allTags);
        } else {
            incrementCounter("business.metric." + eventName, allTags);
        }

        log.debug("Recorded business metric: {} with category: {} and value: {}", eventName, category, value);
    }

    /**
     * Get the meter registry for advanced usage
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}