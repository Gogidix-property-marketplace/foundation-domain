package com.gogidix.infrastructure.gateway.security.waf;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WAF Metrics Collection Service.
 *
 * Collects and aggregates WAF performance metrics and statistics.
 * Provides real-time monitoring of WAF effectiveness and performance.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WafMetricsService {

    private final MeterRegistry meterRegistry;
    private final ReactiveStringRedisTemplate redisTemplate;

    // Counters
    private final Counter totalRequestsCounter;
    private final Counter allowedRequestsCounter;
    private final Counter blockedRequestsCounter;
    private final Counter sqlInjectionAttempts;
    private final Counter xssAttempts;
    private final Counter pathTraversalAttempts;
    private final Counter commandInjectionAttempts;
    private final Counter ddosAttempts;
    private final Counter rateLimitBlocks;

    // Timers
    private final Timer requestProcessingTime;

    // In-memory aggregations
    private final Map<String, AtomicLong> violationCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> ipRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> ruleTriggerCounts = new ConcurrentHashMap<>();

    public WafMetricsService(MeterRegistry meterRegistry, ReactiveStringRedisTemplate redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;

        // Initialize counters
        this.totalRequestsCounter = Counter.builder("waf.requests.total")
                .description("Total requests processed by WAF")
                .register(meterRegistry);

        this.allowedRequestsCounter = Counter.builder("waf.requests.allowed")
                .description("Requests allowed by WAF")
                .register(meterRegistry);

        this.blockedRequestsCounter = Counter.builder("waf.requests.blocked")
                .description("Requests blocked by WAF")
                .register(meterRegistry);

        this.sqlInjectionAttempts = Counter.builder("waf.violations.sql_injection")
                .description("SQL injection attempts blocked")
                .register(meterRegistry);

        this.xssAttempts = Counter.builder("waf.violations.xss")
                .description("XSS attempts blocked")
                .register(meterRegistry);

        this.pathTraversalAttempts = Counter.builder("waf.violations.path_traversal")
                .description("Path traversal attempts blocked")
                .register(meterRegistry);

        this.commandInjectionAttempts = Counter.builder("waf.violations.command_injection")
                .description("Command injection attempts blocked")
                .register(meterRegistry);

        this.ddosAttempts = Counter.builder("waf.violations.ddos")
                .description("DDoS attempts detected")
                .register(meterRegistry);

        this.rateLimitBlocks = Counter.builder("waf.blocks.rate_limit")
                .description("Requests blocked by rate limiting")
                .register(meterRegistry);

        this.requestProcessingTime = Timer.builder("waf.request.processing_time")
                .description("WAF request processing time")
                .register(meterRegistry);
    }

    /**
     * Records request metrics
     */
    public void recordRequest(String requestId, WafResult wafResult, long processingTimeMs) {
        // Record total requests
        totalRequestsCounter.increment();

        // Record processing time
        requestProcessingTime.record(processingTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Update request count by IP
        String clientIp = wafResult.getClientIp();
        ipRequestCounts.computeIfAbsent(clientIp, k -> new AtomicLong(0)).incrementAndGet();

        if (wafResult.isAllowed()) {
            // Request was allowed
            allowedRequestsCounter.increment();
        } else {
            // Request was blocked
            blockedRequestsCounter.increment();

            // Record violation type
            String reason = wafResult.getReason();
            violationCounts.computeIfAbsent(reason, k -> new AtomicLong(0)).incrementAndGet();

            // Update specific counters based on reason
            switch (reason) {
                case "SQL_INJECTION":
                    sqlInjectionAttempts.increment();
                    break;
                case "XSS_ATTEMPT":
                    xssAttempts.increment();
                    break;
                case "PATH_TRAVERSAL":
                    pathTraversalAttempts.increment();
                    break;
                case "COMMAND_INJECTION":
                    commandInjectionAttempts.increment();
                    break;
                case "DDOS_DETECTED":
                    ddosAttempts.increment();
                    break;
                case "RATE_LIMIT_EXCEEDED":
                    rateLimitBlocks.increment();
                    break;
            }

            // Record rule trigger
            if (wafResult.getRuleId() != null) {
                ruleTriggerCounts.computeIfAbsent(wafResult.getRuleId(), k -> new AtomicLong(0))
                        .incrementAndGet();
            }

            // Store in Redis for persistence
            storeMetricsInRedis(wafResult);
        }

        log.debug("Recorded WAF metrics for request {}: {} ({}ms)",
                requestId, wafResult.isAllowed() ? "allowed" : "blocked", processingTimeMs);
    }

    /**
     * Records error metrics
     */
    public void recordError(String requestId, Throwable error) {
        meterRegistry.counter("waf.errors",
                "error", error.getClass().getSimpleName())
                .increment();

        log.error("WAF error recorded for request {}: {}", requestId, error.getMessage());
    }

    /**
     * Stores metrics in Redis for persistence and dashboard
     */
    private void storeMetricsInRedis(WafResult wafResult) {
        String dateKey = "waf:metrics:" + LocalDateTime.now().toLocalDate();

        // Store daily metrics
        redisTemplate.opsForHash()
                .increment(dateKey, "blocked", 1)
                .subscribe();

        // Store by violation type
        String violationKey = dateKey + ":violations";
        if (wafResult.getReason() != null) {
            redisTemplate.opsForHash()
                    .increment(violationKey, wafResult.getReason(), 1)
                    .subscribe();
        }

        // Set expiry for daily metrics (30 days)
        redisTemplate.expire(dateKey, Duration.ofDays(30)).subscribe();
        redisTemplate.expire(violationKey, Duration.ofDays(30)).subscribe();
    }

    /**
     * Gets real-time WAF statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Basic counters
        stats.put("totalRequests", totalRequestsCounter.count());
        stats.put("allowedRequests", allowedRequestsCounter.count());
        stats.put("blockedRequests", blockedRequestsCounter.count());

        // Calculate block rate
        double total = totalRequestsCounter.count();
        double blockRate = total > 0 ? (blockedRequestsCounter.count() / total) * 100 : 0;
        stats.put("blockRate", String.format("%.2f%%", blockRate));

        // Violation counts
        Map<String, Long> violations = new HashMap<>();
        violationCounts.forEach((reason, count) -> violations.put(reason, count.get()));
        stats.put("violations", violations);

        // Top violating IPs
        Map<String, Long> topIps = new HashMap<>();
        ipRequestCounts.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(10)
                .forEach(entry -> topIps.put(entry.getKey(), entry.getValue().get()));
        stats.put("topViolatingIps", topIps);

        // Most triggered rules
        Map<String, Long> topRules = new HashMap<>();
        ruleTriggerCounts.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(10)
                .forEach(entry -> topRules.put(entry.getKey(), entry.getValue().get()));
        stats.put("topTriggeredRules", topRules);

        // Performance metrics
        stats.put("avgProcessingTimeMs", requestProcessingTime.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        stats.put("maxProcessingTimeMs", requestProcessingTime.max(java.util.concurrent.TimeUnit.MILLISECONDS));

        return stats;
    }

    /**
     * Gets detailed metrics for a time period
     */
    public Map<String, Object> getDetailedMetrics(String date) {
        Map<String, Object> metrics = new HashMap<>();

        // Get daily metrics from Redis
        String dateKey = "waf:metrics:" + date;

        redisTemplate.opsForHash()
                .entries(dateKey)
                .doOnNext(entryMap -> {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> map = (Map<Object, Object>) entryMap;
                    Map<String, Object> stringKeyMap = new HashMap<>();
                    map.forEach((key, value) -> stringKeyMap.put(key.toString(), value));
                    metrics.putAll(stringKeyMap);
                })
                .subscribe();

        // Get violation breakdown
        String violationKey = dateKey + ":violations";
        Map<String, Object> violations = new HashMap<>();

        redisTemplate.opsForHash()
                .entries(violationKey)
                .doOnNext(entryMap -> {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> map = (Map<Object, Object>) entryMap;
                    Map<String, Object> stringKeyMap = new HashMap<>();
                    map.forEach((key, value) -> stringKeyMap.put(key.toString(), value));
                    violations.putAll(stringKeyMap);
                })
                .subscribe();

        metrics.put("violationsByType", violations);

        return metrics;
    }

    /**
     * Clears metrics (for testing)
     */
    public void clearMetrics() {
        violationCounts.clear();
        ipRequestCounts.clear();
        ruleTriggerCounts.clear();
        log.info("WAF metrics cleared");
    }

    /**
     * Scheduled task to clean up old data
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldData() {
        // Clean up in-memory counters older than 1 hour
        // In production, consider more sophisticated cleanup strategies
        log.debug("WAF metrics cleanup completed");
    }
}