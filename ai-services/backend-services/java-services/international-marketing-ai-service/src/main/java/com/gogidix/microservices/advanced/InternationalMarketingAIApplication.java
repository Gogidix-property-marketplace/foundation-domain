package com.gogidix.microservices.advanced;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AI-Powered International Marketing and Cultural Adaptation Service
 *
 * Features:
 * - Cultural Market Analysis
 * - Marketing Content Localization
 * - Cross-Cultural Communication Strategies
 * - Global Market Entry Strategy Development
 * - Cultural Sensitivity Assessment
 * - International Marketing Campaign Optimization
 */
@SpringBootApplication(scanBasePackages = {
    "com.gogidix.microservices.advanced",
    "com.gogidix.foundation"
})
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class InternationalMarketingAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternationalMarketingAIApplication.class, args);
    }
}