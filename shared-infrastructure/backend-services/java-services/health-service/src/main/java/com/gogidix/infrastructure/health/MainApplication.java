package com.gogidix.infrastructure.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Health Service.
 *
 * This service provides comprehensive health monitoring and status reporting
 * for all platform services, including health checks, metrics collection,
 * and system diagnostics.
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