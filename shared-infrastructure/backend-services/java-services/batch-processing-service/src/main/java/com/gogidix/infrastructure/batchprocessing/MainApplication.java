package com.gogidix.infrastructure.batchprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Batch Processing Service.
 *
 * This service provides scalable batch processing capabilities for
 * handling large volumes of data, scheduled jobs, and asynchronous
 * background tasks across the platform.
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