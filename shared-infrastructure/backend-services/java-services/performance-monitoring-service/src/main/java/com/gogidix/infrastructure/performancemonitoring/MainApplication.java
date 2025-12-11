package com.gogidix.infrastructure.performancemonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Performance Monitoring Service.
 *
 * This service provides real-time performance monitoring, profiling,
 * and optimization recommendations for applications and infrastructure
 * components across the platform.
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