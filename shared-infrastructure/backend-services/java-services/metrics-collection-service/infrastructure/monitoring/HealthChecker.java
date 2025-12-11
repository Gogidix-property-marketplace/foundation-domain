package com.gogidix.infrastructure.metrics.infrastructure.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.time.LocalDateTime;

/**
 * Health checker for EnterpriseTestService-EnterpriseTestService components.
 * Provides comprehensive health monitoring for all system components.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthChecker {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Database health indicator.
     *
     * @return database health
     */
    public Health checkDatabaseHealth() {
        try {
            // Test database connection
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            return Health.up()
                    .withDetail("status", "UP")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("connection", "OK")
                    .build();
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Redis health indicator.
     *
     * @return Redis health
     */
    public Health checkRedisHealth() {
        try {
            // Test Redis connection
            String testKey = "health-check:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "ok", 10, java.util.concurrent.TimeUnit.SECONDS);
            String result = redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);

            if ("ok".equals(result)) {
                return Health.up()
                        .withDetail("status", "UP")
                        .withDetail("timestamp", LocalDateTime.now())
                        .withDetail("connection", "OK")
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "DOWN")
                        .withDetail("timestamp", LocalDateTime.now())
                        .withDetail("error", "Unexpected response from Redis")
                        .build();
            }
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Kafka health indicator.
     *
     * @return Kafka health
     */
    public Health checkKafkaHealth() {
        try {
            // Test Kafka connection by sending a health check message
            kafkaTemplate.send("health-check", "test-message");

            return Health.up()
                    .withDetail("status", "UP")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("connection", "OK")
                    .build();
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Application health indicator.
     *
     * @return application health
     */
    public Health checkApplicationHealth() {
        try {
            // Check JVM memory
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

            // Check thread count
            int activeThreads = java.lang.Thread.activeCount();

            Health.Builder builder = Health.up()
                    .withDetail("status", "UP")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("jvm", javaMap(
                            "usedMemory", usedMemory / 1024 / 1024 + " MB",
                            "freeMemory", freeMemory / 1024 / 1024 + " MB",
                            "maxMemory", maxMemory / 1024 / 1024 + " MB",
                            "memoryUsagePercent", String.format("%.2f%%", memoryUsagePercent),
                            "activeThreads", activeThreads
                    ));

            // Add warning if memory usage is high
            if (memoryUsagePercent > 80) {
                builder.status(Health.Status.WARNING);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Application health check failed", e);
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * External services health indicator.
     *
     * @return external services health
     */
    public Health checkExternalServicesHealth() {
        try {
            // Add checks for external services here
            // This is a placeholder for external service health checks

            return Health.up()
                    .withDetail("status", "UP")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("services", javaMap(
                            "paymentService", "UP",
                            "emailService", "UP",
                            "notificationService", "UP"
                    ))
                    .build();
        } catch (Exception e) {
            log.error("External services health check failed", e);
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Overall system health indicator.
     *
     * @return overall system health
     */
    public Health checkOverallHealth() {
        try {
            Health databaseHealth = checkDatabaseHealth();
            Health redisHealth = checkRedisHealth();
            Health kafkaHealth = checkKafkaHealth();
            Health appHealth = checkApplicationHealth();
            Health externalHealth = checkExternalServicesHealth();

            boolean allUp = databaseHealth.getStatus() == Health.Status.UP &&
                    redisHealth.getStatus() == Health.Status.UP &&
                    kafkaHealth.getStatus() == Health.Status.UP &&
                    appHealth.getStatus() == Health.Status.UP;

            Health.Builder builder = allUp ? Health.up() : Health.down();

            return builder
                    .withDetail("status", allUp ? "UP" : "DOWN")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("database", databaseHealth.getStatus().getCode())
                    .withDetail("redis", redisHealth.getStatus().getCode())
                    .withDetail("kafka", kafkaHealth.getStatus().getCode())
                    .withDetail("application", appHealth.getStatus().getCode())
                    .withDetail("externalServices", externalHealth.getStatus().getCode())
                    .build();

        } catch (Exception e) {
            log.error("Overall health check failed", e);
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Helper method to create a simple map.
     *
     * @param key   the key
     * @param value the value
     * @return map with single entry
     */
    private java.util.Map<String, Object> javaMap(String key, Object value) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put(key, value);
        return map;
    }

    /**
     * Helper method to create a map with multiple entries.
     *
     * @param entries the entries
     * @return map with all entries
     */
    private java.util.Map<String, Object> javaMap(Object... entries) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((String) entries[i], entries[i + 1]);
        }
        return map;
    }
}