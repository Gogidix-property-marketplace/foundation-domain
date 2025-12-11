package com.gogidix.infrastructure.anomalydetection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Anomaly Detection Service.
 *
 * This service provides intelligent anomaly detection capabilities for monitoring
 * and identifying unusual patterns in system behavior, performance metrics,
 * and application logs.
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