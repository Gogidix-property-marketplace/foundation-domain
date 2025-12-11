package com.gogidix.infrastructure.taskorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Task Orchestrator Service.
 *
 * This service provides intelligent task orchestration, workflow management,
 * and job scheduling capabilities for coordinating complex operations
 * and dependencies across the platform.
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