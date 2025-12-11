package com.gogidix.platform.common.validation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Gogidix validation
 */
@Data
@ConfigurationProperties(prefix = "gogidix.validation")
public class ValidationProperties {

    /**
     * Enable/disable validation
     */
    private boolean enabled = true;

    /**
     * Enable method parameter validation
     */
    private boolean enableMethodValidation = true;

    /**
     * Enable AOP-based validation
     */
    private boolean enableAopValidation = true;

    /**
     * Fail fast on first validation error
     */
    private boolean failFast = false;

    /**
     * Message source configuration
     */
    private MessageSource messageSource = new MessageSource();

    /**
     * Custom validator configuration
     */
    private CustomValidator customValidator = new CustomValidator();

    @Data
    public static class MessageSource {
        /**
         * Base name for validation messages
         */
        private String basename = "classpath:validation-messages";

        /**
         * Default encoding
         */
        private String encoding = "UTF-8";

        /**
         * Cache duration in seconds
         */
        private int cacheSeconds = -1;

        /**
         * Use code as default message
         */
        private boolean useCodeAsDefaultMessage = true;

        /**
         * Fallback to system locale
         */
        private boolean fallbackToSystemLocale = true;
    }

    @Data
    public static class CustomValidator {
        /**
         * Enable custom validators
         */
        private boolean enabled = true;

        /**
         * Scan base packages for validators
         */
        private String[] basePackages = {"com.gogidix.platform.common.validation.validator"};

        /**
         * Enable regex-based validation
         */
        private boolean enableRegexValidation = true;

        /**
         * Enable business rule validation
         */
        private boolean enableBusinessRuleValidation = true;
    }
}