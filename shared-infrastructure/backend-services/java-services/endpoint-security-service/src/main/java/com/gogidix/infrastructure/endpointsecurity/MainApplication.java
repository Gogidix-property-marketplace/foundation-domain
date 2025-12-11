package com.gogidix.infrastructure.endpointsecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Endpoint Security Service.
 *
 * This service provides comprehensive security protection for API endpoints,
 * including authentication, authorization, rate limiting, and threat detection
 * to ensure secure access to platform services.
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