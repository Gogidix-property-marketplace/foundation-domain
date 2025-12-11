package com.gogidix.infrastructure.allocation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.gogidix.infrastructure.allocation.config.properties.AppProperties;
import com.gogidix.infrastructure.allocation.config.KafkaProducerConfig;
import com.gogidix.infrastructure.allocation.config.KafkaConsumerConfig;

/**
 * Main Application Entry Point
 *
 * Enterprise Spring Boot 3.2.2 application following:
 * - Hexagonal Architecture (Ports & Adapters)
 * - Domain-Driven Design (DDD)
 * - Clean Architecture Principles
 * - Microservices Best Practices
 *
 * @author Gogidix Enterprise
 * @version 1.0.0
 * @since 2025-11-28
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableJpaRepositories(basePackages = "com.gogidix.UserManagement.EnterpriseTestService.infrastructure.persistence")
@EnableCaching
@EnableFeignClients(basePackages = "com.gogidix.UserManagement.EnterpriseTestService.infrastructure.external")
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {
    "com.gogidix.UserManagement.EnterpriseTestService.domain",
    "com.gogidix.UserManagement.EnterpriseTestService.application",
    "com.gogidix.UserManagement.EnterpriseTestService.infrastructure",
    "com.gogidix.UserManagement.EnterpriseTestService.web"
})
public class EnterpriseTestServiceApplication {

    /**
     * Main method to start the Spring Boot application
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Configure system properties for production
        System.setProperty("spring.backgroundpreinitializer.ignore", "true");

        SpringApplication app = new SpringApplication(EnterpriseTestServiceApplication.class);

        // Production-ready configuration
        app.setAdditionalProfiles("default");

        // Start the application
        app.run(args);
    }
}