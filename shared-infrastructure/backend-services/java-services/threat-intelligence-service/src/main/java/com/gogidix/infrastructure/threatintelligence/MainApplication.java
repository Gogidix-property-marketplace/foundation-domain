package com.gogidix.infrastructure.threatintelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Threat Intelligence Service.
 *
 * This service provides comprehensive threat intelligence gathering,
 * analysis, and distribution capabilities for proactive security
 * monitoring and defense across the platform.
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