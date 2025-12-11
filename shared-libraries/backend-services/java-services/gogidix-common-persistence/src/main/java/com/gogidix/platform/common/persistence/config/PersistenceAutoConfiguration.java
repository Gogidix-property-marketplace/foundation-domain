package com.gogidix.platform.common.persistence.config;

import com.gogidix.platform.common.persistence.auditing.AuditAwareImpl;
import com.gogidix.platform.common.persistence.service.PersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for gogidix-common-persistence
 */
@Slf4j
@AutoConfiguration
@EnableJpaRepositories(basePackages = "com.gogidix.platform")
@EnableJpaAuditing
@EnableConfigurationProperties({
    PersistenceProperties.class
})
@ConditionalOnProperty(name = "gogidix.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class PersistenceAutoConfiguration {

    /**
     * Create persistence service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public PersistenceService persistenceService() {
        log.info("Creating PersistenceService bean");
        return new PersistenceService();
    }

    /**
     * Create auditor aware bean for JPA auditing
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "gogidix.persistence.auditing.enabled", havingValue = "true", matchIfMissing = true)
    public AuditorAware<String> auditorAware() {
        log.info("Creating AuditorAware bean");
        return new AuditAwareImpl();
    }
}