package com.gogidix.dashboard.centralized.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Health check controller for the Centralized Dashboard service.
 * Provides comprehensive health monitoring for all system components.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health Monitoring", description = "System health monitoring and status checks")
public class HealthController {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, HealthIndicator> healthIndicators;

    @Autowired
    public HealthController(DataSource dataSource, RedisTemplate<String, Object> redisTemplate, Map<String, HealthIndicator> healthIndicators) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.healthIndicators = healthIndicators;
    }

    @GetMapping
    @Operation(summary = "Basic Health Check", description = "Returns basic health status")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "centralized-dashboard");
        return ResponseEntity.ok(health);
    }

    @GetMapping("/detailed")
    @Operation(summary = "Detailed Health Check", description = "Returns comprehensive health status of all components")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        boolean overallHealthy = true;

        Health dbHealth = checkDatabaseHealth();
        health.put("database", dbHealth);
        if (dbHealth.getStatus() != Status.UP) {
            overallHealthy = false;
        }

        Health redisHealth = checkRedisHealth();
        health.put("redis", redisHealth);
        if (redisHealth.getStatus() != Status.UP) {
            overallHealthy = false;
        }

        Health memoryHealth = checkMemoryHealth();
        health.put("memory", memoryHealth);

        health.put("status", overallHealthy ? "UP" : "DOWN");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "centralized-dashboard");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness Probe", description = "Kubernetes liveness probe endpoint")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("probe", "liveness");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness Probe", description = "Kubernetes readiness probe endpoint")
    public ResponseEntity<Map<String, Object>> readiness() {
        boolean databaseReady = checkDatabaseHealth().getStatus() == Status.UP;
        boolean redisReady = checkRedisHealth().getStatus() == Status.UP;
        boolean ready = databaseReady && redisReady;

        Map<String, Object> response = new HashMap<>();
        response.put("status", ready ? "UP" : "DOWN");
        response.put("timestamp", LocalDateTime.now());
        response.put("probe", "readiness");
        response.put("database", databaseReady ? "UP" : "DOWN");
        response.put("redis", redisReady ? "UP" : "DOWN");

        return ResponseEntity.status(ready ? 200 : 503).body(response);
    }

    private Health checkDatabaseHealth() {
        try {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connected")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connection invalid")
                        .build();
                }
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private Health checkRedisHealth() {
        try {
            redisTemplate.opsForValue().set("health-check", "ok", 10, java.util.concurrent.TimeUnit.SECONDS);
            String response = (String) redisTemplate.opsForValue().get("health-check");

            if ("ok".equals(response)) {
                return Health.up()
                    .withDetail("redis", "Connected")
                    .withDetail("status", "Operational")
                    .build();
            } else {
                return Health.down()
                    .withDetail("redis", "Connection failed")
                    .build();
            }
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                .withDetail("redis", "Disconnected")
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private Health checkMemoryHealth() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

        Health.Builder builder = memoryUsagePercent < 80 ? Health.up() : Health.down();

        return builder
            .withDetail("maxMemory", maxMemory / 1024 / 1024 + " MB")
            .withDetail("usedMemory", usedMemory / 1024 / 1024 + " MB")
            .withDetail("memoryUsagePercent", String.format("%.2f%%", memoryUsagePercent))
            .build();
    }
}
