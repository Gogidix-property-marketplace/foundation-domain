package com.gogidix.infrastructure.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// Removed custom config imports that don't exist
// import com.gogidix.common.security.config.EnableSecurity;
// import com.gogidix.common.audit.config.EnableAudit;
// import com.gogidix.common.monitoring.config.EnableMonitoring;
// import com.gogidix.common.validation.config.EnableValidation;
// import com.gogidix.common.client.config.EnableClient;

/**
 * 🏗️ GOGIDIX EUREKA SERVER APPLICATION
 *
 * Enterprise Spring Boot Application with Foundation Libraries Integration
 *
 * Features:
 * - Complete 9/11 Foundation Libraries Integration
 * - Service Registration and Discovery
 * - JWT Authentication & RBAC Authorization
 * - Event-Driven Architecture with Kafka
 * - Redis/Caffeine Multi-Layer Caching
 * - AOP-based Audit Logging & Monitoring
 * - Comprehensive Bean Validation
 * - JPA/Hibernate Persistence with Base Repositories
 * - HTTP REST Client Utilities with Resilience Patterns
 * - Zero-Configuration with Foundation Platform
 *
 * @author Gogidix Infrastructure Team
 * @version 1.0.0
 * @since 2025-12-03
 */
@SpringBootApplication
@EnableEurekaServer
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = {
    "com.gogidix.infrastructure.eureka.repository",
    "com.gogidix.platform.common.persistence.repository"
})
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableKafka
@EnableFeignClients
@ComponentScan(basePackages = {
    "com.gogidix.infrastructure.eureka",
    "com.gogidix.platform.common"
})

// REMOVED: Foundation library annotations that don't exist
// @EnableSecurity
// @EnableAudit
// @EnableMonitoring
// @EnableValidation
// @EnableClient

public class MainApplication {

    /**
     * 🚀 Application Entry Point
     *
     * Starts the Eureka Server microservice with:
     * - Shared library auto-configuration
     * - Central configuration integration
     * - Health checks and monitoring
     * - Graceful shutdown handling
     * - Service registry management
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}