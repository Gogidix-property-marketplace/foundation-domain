package com.gogidix.infrastructure.zipkin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Zipkin Server Service.
 *
 * This service provides distributed tracing capabilities using Zipkin
 * for monitoring and troubleshooting microservices architecture
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