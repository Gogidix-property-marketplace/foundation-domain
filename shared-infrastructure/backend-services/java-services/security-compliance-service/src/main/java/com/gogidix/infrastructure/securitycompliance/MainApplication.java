package com.gogidix.infrastructure.securitycompliance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Security Compliance Service.
 *
 * This service provides comprehensive security compliance management,
 * policy enforcement, and audit capabilities to ensure adherence to
 * security standards and regulatory requirements across the platform.
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