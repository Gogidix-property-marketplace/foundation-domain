package com.gogidix.infrastructure.validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Validation Service.
 *
 * This service provides comprehensive data validation capabilities
 * including schema validation, business rule validation, and
 * data quality checks across the platform.
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