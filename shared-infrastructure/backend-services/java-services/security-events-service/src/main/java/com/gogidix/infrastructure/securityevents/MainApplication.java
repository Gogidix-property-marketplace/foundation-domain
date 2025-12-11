package com.gogidix.infrastructure.securityevents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Security Events Service.
 *
 * This service provides comprehensive security event collection, analysis,
 * and threat detection capabilities for monitoring and responding to
 * security incidents across the platform.
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