package com.gogidix.infrastructure.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 🌐 GOGIDIX API GATEWAY MAIN APPLICATION
 *
 * Enterprise Spring Cloud Gateway with Foundation Libraries Integration
 *
 * Features:
 * - Spring Cloud Gateway for API routing and load balancing
 * - Service discovery with Eureka integration
 * - Circuit breaker patterns with Resilience4j
 * - Request/response transformation and filtering
 * - Rate limiting and security filters
 * - Comprehensive monitoring and tracing
 * - JWT authentication and authorization
 * - Cross-origin resource sharing (CORS)
 * - Request/response logging and audit
 *
 * @author Gogidix Infrastructure Team
 * @version 1.0.0
 * @since 2025-12-03
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}