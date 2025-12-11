package com.gogidix.platform.common.monitoring.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;

import com.gogidix.platform.common.monitoring.binder.BusinessMetricsBinder;
import com.gogidix.platform.common.monitoring.binder.PerformanceMetricsBinder;
import com.gogidix.platform.common.monitoring.binder.SystemMetricsBinder;
import com.gogidix.platform.common.monitoring.service.MetricsCollectionService;
import com.gogidix.platform.common.monitoring.service.DashboardConfigurationService;
import com.gogidix.platform.common.monitoring.config.properties.MonitoringProperties;

/**
 * 🏗️ GOGIDIX FOUNDATION - MONITORING AUTO-CONFIGURATION
 *
 * Comprehensive monitoring setup for all Gogidix microservices
 *
 * Features:
 * - Automatic Prometheus metrics setup
 * - Custom business metrics binding
 * - Performance and system metrics collection
 * - Dynamic dashboard configuration
 * - Cross-service metrics aggregation
 * - Service Level Objective (SLO) monitoring
 *
 * @author Gogidix Platform Team
 * @version 1.0.0
 * @since 2025-12-03
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(name = "gogidix.foundation.monitoring.enabled", havingValue = "true", matchIfMissing = true)
@Import({
    BusinessMetricsBinder.class,
    PerformanceMetricsBinder.class,
    SystemMetricsBinder.class,
    MetricsCollectionService.class,
    DashboardConfigurationService.class
})
public class MonitoringAutoConfiguration {

    /**
     * 📊 Prometheus Meter Registry
     *
     * Configures Prometheus meter registry with custom settings
     */
    @Bean
    @ConditionalOnProperty(name = "gogidix.foundation.monitoring.export.prometheus.enabled", havingValue = "true", matchIfMissing = true)
    public PrometheusMeterRegistry prometheusMeterRegistry(MonitoringProperties properties) {
        PrometheusConfig config = new PrometheusConfig() {
            @Override
            public String get(String key) {
                return null; // Use defaults
            }

            @Override
            public boolean enabled() {
                return properties.getExport().getPrometheus().isEnabled();
            }

            @Override
            public String prefix() {
                return "prometheus";
            }
        };

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(config);
        registry.config().commonTags(
            "application", properties.getApplication(),
            "service", properties.getService(),
            "environment", properties.getEnvironment(),
            "zone", properties.getZone(),
            "version", properties.getVersion(),
            "instance", properties.getInstance()
        );

        // Add custom meter filters
        registry.config().meterFilter(
            MeterFilter.denyNameStartingWith("jvm.memory.nonheap.")
        );

        return registry;
    }

    /**
     * 🎛️ Meter Registry Customizer
     *
     * Applies common configuration to all meter registries
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer(MonitoringProperties properties) {
        return registry -> {
            // Add common tags
            registry.config().commonTags(
                "application", properties.getApplication(),
                "service", properties.getService(),
                "environment", properties.getEnvironment(),
                "zone", properties.getZone(),
                "version", properties.getVersion(),
                "instance", properties.getInstance()
            );

            // Add service-specific tags if available
            if (properties.getDomain() != null) {
                registry.config().commonTag("domain", properties.getDomain());
            }

            // Configure filters
            registry.config().meterFilter(
                MeterFilter.denyNameStartingWith("jvm.memory.nonheap.")
            );

            registry.config().meterFilter(
                MeterFilter.denyNameStartingWith("hikaricp.connections.")
            );
        };
    }

    /**
     * 🔢 Business Metrics Counter
     *
     * Generic business metrics counter for common operations
     */
    @Bean
    public Counter businessEventsCounter(MeterRegistry meterRegistry, MonitoringProperties properties) {
        return Counter.builder("gogidix_business_events_total")
                .description("Total number of business events")
                .tag("service", properties.getService())
                .register(meterRegistry);
    }

    /**
     * ⏱️ Request Timer
     *
     * Tracks request processing time with custom tags
     */
    @Bean
    public Timer requestTimer(MeterRegistry meterRegistry, MonitoringProperties properties) {
        return Timer.builder("gogidix_request_duration")
                .description("Request processing time")
                .tag("service", properties.getService())
                .percentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry);
    }

    /**
     * 🎯 SLO Monitoring Timer
     *
     * Service Level Objective monitoring
     */
    @Bean
    public Timer sloResponseTimeTimer(MeterRegistry meterRegistry, MonitoringProperties properties) {
        return Timer.builder("gogidix_slo_response_time")
                .description("SLO response time monitoring")
                .tag("service", properties.getService())
                .tag("slo", "p95_response_time")
                .distributionStatisticExpiry(properties.getSlo().getDistributionStatisticExpiry())
                .percentiles(0.95)
                .publishPercentilesForHistogram(true)
                .register(meterRegistry);
    }

    /**
     * 💰 Revenue Gauge
     *
     * Tracks business revenue metrics
     */
    @Bean
    public Gauge revenueGauge(MeterRegistry meterRegistry, MonitoringProperties properties) {
        return Gauge.builder("gogidix_business_revenue_current")
                .description("Current revenue tracking")
                .tag("service", properties.getService())
                .register(meterRegistry, this, obj -> getCurrentRevenue());
    }

    /**
     * 📈 Custom Metrics Collection Service
     *
     * Service for collecting and aggregating custom metrics
     */
    @Bean
    public MetricsCollectionService metricsCollectionService(
            MeterRegistry meterRegistry,
            MonitoringProperties properties,
            BusinessMetricsBinder businessMetrics,
            PerformanceMetricsBinder performanceMetrics,
            SystemMetricsBinder systemMetrics) {
        return new MetricsCollectionService(
                meterRegistry,
                properties,
                businessMetrics,
                performanceMetrics,
                systemMetrics
        );
    }

    /**
     * 📊 Dashboard Configuration Service
     *
     * Service for configuring Grafana dashboards
     */
    @Bean
    @ConditionalOnProperty(name = "gogidix.foundation.monitoring.dashboards.enabled", havingValue = "true", matchIfMissing = true)
    public DashboardConfigurationService dashboardConfigurationService(MonitoringProperties properties) {
        return new DashboardConfigurationService(properties);
    }

    /**
     * 🔍 Health Check Metrics
     *
     * Custom health check metrics
     */
    @Bean
    public MeterBinder healthCheckMetricsBinder(MonitoringProperties properties) {
        return registry -> {
            Gauge.builder("gogidix_health_check_status")
                    .description("Health check status")
                    .tag("service", properties.getService())
                    .register(registry, this, obj -> getHealthCheckStatus());
        };
    }

    /**
     * 🔄 Cache Metrics
     *
     * Cache performance metrics
     */
    @Bean
    public MeterBinder cacheMetricsBinder(MonitoringProperties properties) {
        return registry -> {
            Timer.builder("gogidix_cache_operation_duration")
                    .description("Cache operation duration")
                    .tag("service", properties.getService())
                    .register(registry);

            Counter.builder("gogidix_cache_operations_total")
                    .description("Total cache operations")
                    .tag("service", properties.getService())
                    .register(registry);
        };
    }

    /**
     * 🗄️ Database Metrics
     *
     * Database performance metrics
     */
    @Bean
    public MeterBinder databaseMetricsBinder(MonitoringProperties properties) {
        return registry -> {
            Timer.builder("gogidix_database_query_duration")
                    .description("Database query duration")
                    .tag("service", properties.getService())
                    .percentiles(0.5, 0.9, 0.95, 0.99)
                    .register(registry);

            Counter.builder("gogidix_database_queries_total")
                    .description("Total database queries")
                    .tag("service", properties.getService())
                    .register(registry);

            Gauge.builder("gogidix_database_connections_active")
                    .description("Active database connections")
                    .tag("service", properties.getService())
                    .register(registry, this, obj -> getActiveDatabaseConnections());
        };
    }

    /**
     * 📨 Messaging Metrics
     *
     * Kafka messaging metrics
     */
    @Bean
    public MeterBinder messagingMetricsBinder(MonitoringProperties properties) {
        return registry -> {
            Counter.builder("gogidix_messaging_messages_total")
                    .description("Total messages processed")
                    .tag("service", properties.getService())
                    .register(registry);

            Timer.builder("gogidix_messaging_processing_duration")
                    .description("Message processing duration")
                    .tag("service", properties.getService())
                    .register(registry);
        };
    }

    // Helper methods for metrics (these would be implemented with actual logic)

    private double getCurrentRevenue() {
        // Implementation would track actual revenue
        return 0.0;
    }

    private double getHealthCheckStatus() {
        // Implementation would check actual health status
        return 1.0; // 1.0 for healthy, 0.0 for unhealthy
    }

    private double getActiveDatabaseConnections() {
        // Implementation would track actual database connections
        return 0.0;
    }
}