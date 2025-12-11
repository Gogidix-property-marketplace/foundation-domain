package com.gogidix.infrastructure.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Metrics Service.
 *
 * This service provides comprehensive metrics collection, aggregation, and
 * analysis capabilities for monitoring system performance, application
 * behavior, and business KPIs across the platform.
 *
 * @author Gogidix Infrastructure Team
 * @version 1.0.0
 */
@SpringBootApplication
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}