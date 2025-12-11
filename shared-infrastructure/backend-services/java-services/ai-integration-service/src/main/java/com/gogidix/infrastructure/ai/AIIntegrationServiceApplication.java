package com.gogidix.infrastructure.ai;

import com.gogidix.platform.core.EnableSharedLibraries;
import com.gogidix.platform.security.EnableSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Integration Service
 *
 * Service responsible for integrating AI services with the infrastructure,
 * providing predictive analytics, anomaly detection, and intelligent recommendations.
 *
 * @author Agent C - AI Services Specialist
 * @version 1.0.0
 */
@SpringBootApplication
@EnableSharedLibraries
@EnableSecurity
@EnableFeignClients(basePackages = "com.gogidix.infrastructure.ai.service")
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableKafka
public class AIIntegrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIIntegrationServiceApplication.class, args);
    }
}