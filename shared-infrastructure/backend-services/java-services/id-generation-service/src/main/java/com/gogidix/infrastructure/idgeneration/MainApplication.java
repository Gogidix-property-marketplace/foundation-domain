package com.gogidix.infrastructure.idgeneration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for ID Generation Service.
 *
 * This service provides distributed unique identifier generation capabilities
 * including UUIDs, snowflake IDs, and custom ID formats with guaranteed
 * uniqueness across the platform.
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