package com.gogidix.microservices.advanced;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AI-Powered Cross-Border Transaction Management Service
 *
 * Features:
 * - International Transaction Processing
 * - Multi-Currency Exchange Management
 * - Regulatory Approval Management
 * - International Escrow Services
 * - Tax Optimization Strategies
 * - Cross-Border Compliance Management
 */
@SpringBootApplication(scanBasePackages = {
    "com.gogidix.microservices.advanced",
    "com.gogidix.foundation"
})
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class CrossBorderTransactionAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrossBorderTransactionAIApplication.class, args);
    }
}