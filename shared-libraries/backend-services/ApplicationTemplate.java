package com.gogidix.platform.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gogidix.platform.common.core.annotation.EnableGogidixCore;
import com.gogidix.platform.common.security.annotation.EnableGogidixSecurity;
import com.gogidix.platform.common.messaging.annotation.EnableGogidixMessaging;
import com.gogidix.platform.common.persistence.annotation.EnableGogidixPersistence;
import com.gogidix.platform.common.observability.annotation.EnableGogidixObservability;

/**
 * 🏗️ GOGIDIX BILLING INVOICING SERVICE APPLICATION
 *
 * Enterprise Spring Boot Application with Shared Libraries Integration
 *
 * Features:
 * - DDD Architecture with Hexagonal Pattern
 * - JWT Authentication & RBAC Authorization
 * - Event-Driven Architecture with Kafka
 * - Redis Caching & Performance Optimization
 * - Comprehensive Monitoring & Observability
 * - Zero-Configuration with Shared Libraries
 *
 * @author Gogidix Platform Team
 * @version 1.0.0
 * @since 2025-11-29
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = {
    "com.gogidix.platform.billing.domain.repository",
    "com.gogidix.platform.common.persistence.repository"
})
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableKafka

// GOGIDIX SHARED LIBRARIES ENABLEMENT
@EnableGogidixCore
@EnableGogidixSecurity
@EnableGogidixMessaging
@EnableGogidixPersistence
@EnableGogidixObservability

public class BillingInvoicingApplication {

    /**
     * 🚀 Application Entry Point
     *
     * Starts the billing and invoicing microservice with:
     * - Shared library auto-configuration
     * - Central configuration integration
     * - Health checks and monitoring
     * - Graceful shutdown handling
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Set system properties for consistent startup
        System.setProperty("spring.application.name", "billing-invoicing-service");
        System.setProperty("spring.profiles.active",
            System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));

        // Run the application with shared libraries
        SpringApplication.run(BillingInvoicingApplication.class, args);
    }
}