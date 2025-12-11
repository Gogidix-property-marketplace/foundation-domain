package com.gogidix.infrastructure.token;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Token Service.
 *
 * This service provides comprehensive token management capabilities
 * including JWT creation, validation, refresh, and revocation for
 * authentication and authorization across the platform.
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