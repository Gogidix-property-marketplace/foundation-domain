package com.gogidix.ai.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Gateway Service - Main Spring Boot Application
 *
 * This service acts as the central gateway for all AI services in the Gogidix platform.
 * It handles:
 * - Request routing to appropriate AI services
 * - API versioning
 * - Rate limiting
 * - Authentication and authorization
 * - Request/response transformation
 * - Service discovery integration
 *
 * @author Gogidix AI Team
 * @since 1.0.0
 */
@SpringBootApplication
public class AiGatewayServiceApplication {

    /**
     * Main method that starts the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AiGatewayServiceApplication.class, args);
    }
}