package com.gogidix.infrastructure.consent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Consent Service.
 *
 * This service manages user consent and privacy preferences, ensuring
 * compliance with data protection regulations and providing granular
 * control over personal data usage.
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