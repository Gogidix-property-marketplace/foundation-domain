package com.gogidix.infrastructure.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Audit Service.
 *
 * This service provides comprehensive auditing and logging capabilities for
 * tracking system events, user actions, and compliance requirements across
 * the infrastructure platform.
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