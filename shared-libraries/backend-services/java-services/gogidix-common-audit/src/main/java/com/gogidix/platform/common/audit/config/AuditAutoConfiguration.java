package com.gogidix.platform.common.audit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gogidix.platform.common.audit.aspect.AuditAspect;
import com.gogidix.platform.common.audit.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Auto-configuration for gogidix-common-audit
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "gogidix.audit.enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableAspectJAutoProxy
@EnableJpaRepositories(basePackages = "com.gogidix.platform.common.audit.repository")
@EnableAsync
public class AuditAutoConfiguration {

    /**
     * Create AuditAspect bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({AuditService.class, ObjectMapper.class})
    public AuditAspect auditAspect(AuditService auditService, ObjectMapper objectMapper) {
        log.info("Creating AuditAspect bean");
        return new AuditAspect(auditService, objectMapper);
    }
}