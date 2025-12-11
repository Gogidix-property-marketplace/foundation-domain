package com.gogidix.infrastructure.archive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Archive Service.
 *
 * This service provides long-term storage and archiving capabilities for
 * historical data, logs, and other assets that need to be preserved
 * according to retention policies.
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