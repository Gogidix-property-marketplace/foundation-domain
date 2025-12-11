package com.gogidix.platform.common.monitoring.config;

import com.gogidix.platform.common.monitoring.aspect.MonitoredAspect;
import com.gogidix.platform.common.monitoring.service.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for gogidix-common-monitoring
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(name = "gogidix.monitoring.enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy
public class MonitoringAutoConfiguration {

    /**
     * Create MetricsService bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MetricsService metricsService(MeterRegistry meterRegistry) {
        log.info("Creating MetricsService bean");
        return new MetricsService(meterRegistry);
    }

    /**
     * Create MonitoredAspect bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MonitoredAspect monitoredAspect(MetricsService metricsService) {
        log.info("Creating MonitoredAspect bean");
        return new MonitoredAspect(metricsService);
    }

    /**
     * ApplicationHealthIndicator is only available when Spring Boot Actuator is on classpath
     * Health indicators will be auto-configured by Spring Boot Actuator if available
     */

    /**
     * Customize MeterRegistry with common tags
     */
    @Bean
    @ConditionalOnMissingBean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            String appName = System.getProperty("spring.application.name", "gogidix-app");
            String instanceId = System.getProperty("instance.id", "local");
            String profile = System.getProperty("spring.profiles.active", "default");

            registry.config().commonTags(
                    "application", appName,
                    "instance", instanceId,
                    "profile", profile
            );

            log.info("Configured common tags for meter registry: application={}, instance={}, profile={}",
                    appName, instanceId, profile);
        };
    }
}