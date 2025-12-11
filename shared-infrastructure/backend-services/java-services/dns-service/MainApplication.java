package com.gogidix.infrastructure.dns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gogidix.infrastructure.dns.config.AppProperties;

/**
 * Main Spring Boot application class for EnterpriseTestService-EnterpriseTestService.
 *
 * This is the entry point of the enterprise Java application with comprehensive
 * enterprise-level features including:
 * - JPA with auditing support
 * - Caching with Redis
 * - Kafka messaging
 * - Async processing
 * - Scheduled tasks
 * - Configuration properties
 * - Transaction management
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties(AppProperties.class)
public class EnterpriseTestServiceApplication {

    /**
     * Main method that starts the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(EnterpriseTestServiceApplication.class, args);
    }
}