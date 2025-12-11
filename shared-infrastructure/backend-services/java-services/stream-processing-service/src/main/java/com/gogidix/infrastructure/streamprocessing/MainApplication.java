package com.gogidix.infrastructure.streamprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Stream Processing Service.
 *
 * This service provides real-time stream processing capabilities for
 * handling continuous data flows, event streams, and real-time analytics
 * across the platform.
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