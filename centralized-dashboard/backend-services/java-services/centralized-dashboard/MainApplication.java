package com.gogidix.dashboard.centralized;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gogidix.dashboard.centralized.config.DashboardProperties;
import com.gogidix.dashboard.centralized.config.WebSocketConfig;
import com.gogidix.dashboard.centralized.infrastructure.filter.DashboardSecurityFilter;
import com.gogidix.dashboard.centralized.infrastructure.filter.AdminOnlyFilter;
import com.gogidix.dashboard.centralized.infrastructure.filter.RateLimiterFilter;

/**
 * Main Spring Boot application class for Centralized Dashboard Service.
 *
 * This is the entry point of the enterprise Centralized Dashboard with comprehensive
 * features including:
 * - Real-time dashboard visualization
 * - JPA with auditing support
 * - Caching with Redis
 * - WebSocket support for real-time updates
 * - Async processing
 * - Scheduled tasks for data aggregation
 * - Configuration properties
 * - Transaction management
 * - Multi-layer security filtering
 * - Analytics and reporting
 * - User administration
 * - Alert management
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties({DashboardProperties.class})
public class CentralizedDashboardApplication {

    /**
     * Main method that starts the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CentralizedDashboardApplication.class, args);
    }

    /**
     * Configures CORS settings for the dashboard application.
     * Supports cross-origin requests from the frontend application.
     *
     * @return WebMvcConfigurer with CORS configuration
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("${dashboard.frontend.url:http://localhost:3000}")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);

                registry.addMapping("/ws/**")
                        .allowedOrigins("${dashboard.frontend.url:http://localhost:3000}")
                        .allowedMethods("GET", "POST")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    /**
     * Dashboard security filter for authentication and authorization.
     *
     * @return configured security filter
     */
    @Bean
    public DashboardSecurityFilter dashboardSecurityFilter() {
        return new DashboardSecurityFilter();
    }

    /**
     * Admin-only access filter for administrative endpoints.
     *
     * @return configured admin filter
     */
    @Bean
    public AdminOnlyFilter adminOnlyFilter() {
        return new AdminOnlyFilter();
    }

    /**
     * Rate limiting filter for API protection.
     *
     * @return configured rate limiter
     */
    @Bean
    public RateLimiterFilter rateLimiter() {
        return new RateLimiterFilter();
    }
}