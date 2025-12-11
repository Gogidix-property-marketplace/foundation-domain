package com.gogidix.microservices.advanced;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AI-Powered Legal System and Compliance Adaptation Service
 *
 * Features:
 * - Legal System Analysis
 * - Compliance Requirements Adaptation
 * - Cross-Border Transaction Legal Support
 * - Regulatory Compliance Checking
 * - International Law Framework Integration
 * - Risk Assessment and Mitigation
 */
@SpringBootApplication(scanBasePackages = {
    "com.gogidix.microservices.advanced",
    "com.gogidix.foundation"
})
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class LegalComplianceAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalComplianceAIApplication.class, args);
    }
}