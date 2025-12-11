package com.gogidix.microservices.operational;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Application for Process Mining and Optimization AI Service
 * Part of the AI Services ecosystem for operational excellence
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableJpaRepositories
public class ProcessMiningAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessMiningAIApplication.class, args);
    }
}