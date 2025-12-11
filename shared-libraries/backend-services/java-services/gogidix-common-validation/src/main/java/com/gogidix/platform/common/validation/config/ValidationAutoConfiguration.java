package com.gogidix.platform.common.validation.config;

import com.gogidix.platform.common.validation.aspect.ValidationAspect;
import com.gogidix.platform.common.validation.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import jakarta.validation.Validator;

/**
 * Auto-configuration for gogidix-common-validation
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ValidationProperties.class)
@EnableAspectJAutoProxy
@ConditionalOnProperty(name = "gogidix.validation.enabled", havingValue = "true", matchIfMissing = true)
public class ValidationAutoConfiguration {

    /**
     * Create validation service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ValidationService validationService(Validator validator) {
        log.info("Creating ValidationService bean");
        return new ValidationService(validator);
    }

    /**
     * Create primary validator bean
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(Validator.class)
    public LocalValidatorFactoryBean validator() {
        log.info("Creating LocalValidatorFactoryBean bean");
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setValidationMessageSource(validationMessageSource());
        validatorFactoryBean.afterPropertiesSet();
        return validatorFactoryBean;
    }

    /**
     * Create message source for validation messages
     */
    @Bean
    @ConditionalOnMissingBean(MessageSource.class)
    public MessageSource validationMessageSource() {
        log.info("Creating validation message source");
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:validation-messages");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    /**
     * Create method validation post processor for AOP-based validation
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "gogidix.validation.enableMethodValidation", havingValue = "true", matchIfMissing = true)
    public MethodValidationPostProcessor methodValidationPostProcessor(Validator validator) {
        log.info("Creating MethodValidationPostProcessor bean");
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator);
        return processor;
    }

    /**
     * Create validation aspect for automatic parameter validation
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "gogidix.validation.enableAopValidation", havingValue = "true", matchIfMissing = true)
    public ValidationAspect validationAspect(ValidationService validationService) {
        log.info("Creating ValidationAspect bean");
        return new ValidationAspect(validationService);
    }
}