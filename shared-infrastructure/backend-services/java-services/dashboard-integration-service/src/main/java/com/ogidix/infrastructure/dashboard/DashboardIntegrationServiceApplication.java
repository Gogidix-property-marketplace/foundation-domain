package com.ogidix.infrastructure.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Dashboard Integration Service
 *
 * Service responsible for integrating the centralized dashboard with all infrastructure
 * monitoring systems including Prometheus, Grafana, ELK stack, and custom metrics.
 *
 * @author Agent B - Dashboard Integration Specialist
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class DashboardIntegrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardIntegrationServiceApplication.class, args);
    }
}