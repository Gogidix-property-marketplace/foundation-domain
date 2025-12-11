package com.gogidix.microservices.advanced;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Application for Lead Conversion AI Service
 * Part of the AI Services ecosystem for lead conversion and client management
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableJpaRepositories
public class LeadConversionAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeadConversionAIApplication.class, args);
    }
}