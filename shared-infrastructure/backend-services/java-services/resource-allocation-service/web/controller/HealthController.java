package com.gogidix.infrastructure.allocation.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for EnterpriseTestService-EnterpriseTestService.
 * Provides comprehensive health status information.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthIndicator databaseHealthIndicator;
    private final HealthIndicator redisHealthIndicator;
    private final HealthIndicator kafkaHealthIndicator;

    /**
     * Basic health check endpoint.
     *
     * @return health status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "EnterpriseTestService-EnterpriseTestService");
        health.put("version", "1.0.0");

        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with all components.
     *
     * @return detailed health status
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "EnterpriseTestService-EnterpriseTestService");
        health.put("version", "1.0.0");

        // Component health
        Map<String, Object> components = new HashMap<>();
        components.put("database", checkComponentHealth(databaseHealthIndicator));
        components.put("redis", checkComponentHealth(redisHealthIndicator));
        components.put("kafka", checkComponentHealth(kafkaHealthIndicator));
        health.put("components", components);

        return ResponseEntity.ok(health);
    }

    /**
     * Liveness probe endpoint.
     *
     * @return liveness status
     */
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "UP");
        liveness.put("timestamp", LocalDateTime.now());
        liveness.put("probe", "liveness");

        return ResponseEntity.ok(liveness);
    }

    /**
     * Readiness probe endpoint.
     *
     * @return readiness status
     */
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("status", "UP");
        readiness.put("timestamp", LocalDateTime.now());
        readiness.put("probe", "readiness");

        // Check critical dependencies
        boolean dbReady = databaseHealthIndicator.health().getStatus() == Health.Status.UP;
        boolean redisReady = redisHealthIndicator.health().getStatus() == Health.Status.UP;

        if (dbReady && redisReady) {
            readiness.put("status", "UP");
        } else {
            readiness.put("status", "DOWN");
        }

        return ResponseEntity.ok(readiness);
    }

    /**
     * Checks component health and returns status.
     *
     * @param indicator the health indicator
     * @return component health status
     */
    private Map<String, Object> checkComponentHealth(HealthIndicator indicator) {
        Health health = indicator.health();
        Map<String, Object> componentHealth = new HashMap<>();
        componentHealth.put("status", health.getStatus().getCode());
        componentHealth.put("details", health.getDetails());
        return componentHealth;
    }
}