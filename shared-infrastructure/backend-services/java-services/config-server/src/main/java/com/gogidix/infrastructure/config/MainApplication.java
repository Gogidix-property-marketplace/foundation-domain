package com.gogidix.infrastructure.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 🗄️ GOGIDIX CENTRAL CONFIGURATION SERVER
 *
 * Enterprise Spring Cloud Config Server for Centralized Configuration Management
 *
 * Features:
 * - Centralized configuration management for all services
 * - Multiple configuration sources (Git, Vault, Database)
 * - Environment-specific configuration profiles
 * - Configuration versioning and rollback
 * - Real-time configuration updates with Spring Cloud Bus
 * - Security and encryption of sensitive properties
 * - Configuration validation and monitoring
 * - Integration with Eureka service discovery
 *
 * @author Gogidix Infrastructure Team
 * @version 1.0.0
 * @since 2025-12-03
 */
@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}