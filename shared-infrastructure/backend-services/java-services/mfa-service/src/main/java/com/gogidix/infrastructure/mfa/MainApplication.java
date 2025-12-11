package com.gogidix.infrastructure.mfa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Multi-Factor Authentication Service.
 *
 * This service provides comprehensive multi-factor authentication capabilities
 * including TOTP, SMS, email, hardware tokens, and biometric authentication
 * to enhance security across the platform.
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