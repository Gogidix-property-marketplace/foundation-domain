package com.gogidix.ai.dataquality.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Application properties configuration for EnterpriseTestService.
 * Centralized configuration management with validation.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    /**
     * Basic application information
     */
    @NotBlank
    private String name = "EnterpriseTestService-EnterpriseTestService";

    @NotBlank
    private String version = "1.0.0";

    @NotBlank
    private String description = "EnterpriseTestService EnterpriseTestService application";

    private String environment = "development";

    @NotNull
    private Boolean debug = false;

    /**
     * Server configuration
     */
    @NestedConfigurationProperty
    private ServerProperties server = new ServerProperties();

    /**
     * Database configuration
     */
    @NestedConfigurationProperty
    private DatabaseProperties database = new DatabaseProperties();

    /**
     * Security configuration
     */
    @NestedConfigurationProperty
    private SecurityProperties security = new SecurityProperties();

    /**
     * Kafka configuration
     */
    @NestedConfigurationProperty
    private KafkaProperties kafka = new KafkaProperties();

    /**
     * Cache configuration
     */
    @NestedConfigurationProperty
    private CacheProperties cache = new CacheProperties();

    /**
     * Logging configuration
     */
    @NestedConfigurationProperty
    private LoggingProperties logging = new LoggingProperties();

    /**
     * Monitoring configuration
     */
    @NestedConfigurationProperty
    private MonitoringProperties monitoring = new MonitoringProperties();

    /**
     * External services configuration
     */
    @NestedConfigurationProperty
    private ExternalServicesProperties externalServices = new ExternalServicesProperties();

    /**
     * Business configuration
     */
    @NestedConfigurationProperty
    private BusinessProperties business = new BusinessProperties();

    /**
     * Server properties
     */
    @Data
    public static class ServerProperties {
        private Integer port = 8080;
        private String host = "localhost";
        private String contextPath = "/";
        private Boolean compressionEnabled = true;
        private Integer compressionMimeTypesSize = 2048;
        private List<String> compressionMimeTypes = List.of(
                "application/json",
                "application/xml",
                "text/html",
                "text/xml",
                "text/plain"
        );
        private Boolean http2Enabled = true;
        private String servletEncoding = "UTF-8";
        private Integer maxHttpHeaderSize = 8192;
        private Integer maxHttpPostSize = 2097152; // 2MB
        private Integer connectionTimeout = 20000;
        private Boolean traceEnabled = false;
    }

    /**
     * Database properties
     */
    @Data
    public static class DatabaseProperties {
        @NestedConfigurationProperty
        private DatabaseConfigProperties primary = new DatabaseConfigProperties();

        @NestedConfigurationProperty
        private DatabaseConfigProperties readReplica = new DatabaseConfigProperties();

        private Integer connectionPoolSize = 20;
        private Integer connectionPoolMinimumIdle = 5;
        private Integer connectionPoolMaximum = 50;
        private Long connectionPoolIdleTimeout = 30000L;
        private Long connectionPoolMaxLifetime = 1800000L;
        private Boolean flywayEnabled = true;
        private Boolean liquibaseEnabled = false;

        @Data
        public static class DatabaseConfigProperties {
            private String type = "postgresql";
            private String host = "localhost";
            private Integer port = 5432;
            private String name;
            private String username;
            private String password;
            private String schema = "public";
            private Boolean sslEnabled = false;
            private String sslMode = "prefer";
            private Integer connectTimeout = 10;
            private Integer socketTimeout = 30;
            private Boolean showSql = false;
            private Boolean formatSql = true;
            private String ddlAuto = "validate";
        }
    }

    /**
     * Security properties
     */
    @Data
    public static class SecurityProperties {
        private String jwtSecret = "EnterpriseTestService-default-secret-key-change-in-production";
        private Long jwtExpiration = 86400L;
        private Long jwtRefreshExpiration = 604800L;
        private String issuer = "EnterpriseTestService";
        private String audience = "EnterpriseTestService-clients";
        private Boolean oauth2Enabled = false;
        private String oauth2IssuerUri = "";
        private String oauth2ClientId = "";
        private String oauth2ClientSecret = "";
        private Integer passwordMinLength = 8;
        private Boolean passwordRequireUppercase = true;
        private Boolean passwordRequireLowercase = true;
        private Boolean passwordRequireNumbers = true;
        private Boolean passwordRequireSpecialChars = true;
        private Integer maxLoginAttempts = 5;
        private Integer lockoutDurationMinutes = 30;
        private Boolean corsEnabled = true;
        private List<String> corsAllowedOrigins = List.of("http://localhost:3000", "http://localhost:4200");
        private List<String> corsAllowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> corsAllowedHeaders = List.of("*");
        private Boolean csrfEnabled = false;
    }

    /**
     * Kafka properties
     */
    @Data
    public static class KafkaProperties {
        private String bootstrapServers = "localhost:9092";
        private String consumerGroupId = "EnterpriseTestService-EnterpriseTestService";
        private String clientId = "EnterpriseTestService-EnterpriseTestService";
        private Boolean enableAutoCommit = false;
        private String autoOffsetReset = "earliest";
        private Integer maxPollRecords = 100;
        private Integer sessionTimeout = 30000;
        private Integer heartbeatInterval = 10000;
        private String acks = "all";
        private Integer retries = 3;
        private Integer batchSize = 16384;
        private Integer bufferMemory = 33554432;
        private Integer lingerMs = 1;
        private String compressionType = "snappy";
        private Boolean enableIdempotence = true;
        private Map<String, String> topics = Map.of(
                "events", "EnterpriseTestService-EnterpriseTestService-events",
                "commands", "EnterpriseTestService-EnterpriseTestService-commands",
                "notifications", "EnterpriseTestService-EnterpriseTestService-notifications"
        );
    }

    /**
     * Cache properties
     */
    @Data
    public static class CacheProperties {
        private String type = "redis";
        private String host = "localhost";
        private Integer port = 6379;
        private String password = "";
        private Integer database = 0;
        private Long timeout = 2000L;
        private Integer maxTotal = 200;
        private Integer maxIdle = 20;
        private Integer minIdle = 5;
        private Long ttl = 3600L;
        private Boolean cacheNullValues = false;
        private Boolean useKeyPrefix = true;
        private String keyPrefix = "EnterpriseTestService::";
    }

    /**
     * Logging properties
     */
    @Data
    public static class LoggingProperties {
        private String level = "INFO";
        private String pattern = "%d{yyyy-MM-dd HH:mm:ss} - %msg%n";
        private Boolean consoleEnabled = true;
        private Boolean fileEnabled = false;
        private String filePath = "logs/EnterpriseTestService.log";
        private Integer maxFileSize = 10; // MB
        private Integer maxHistory = 30;
        private Boolean asyncEnabled = true;
        private Integer queueCapacity = 1024;
        private Boolean includeMdc = true;
        private Boolean includeStackTrace = true;
    }

    /**
     * Monitoring properties
     */
    @Data
    public static class MonitoringProperties {
        private Boolean metricsEnabled = true;
        private Boolean tracingEnabled = true;
        private Boolean healthEnabled = true;
        private Boolean infoEnabled = true;
        private Boolean prometheusEnabled = false;
        private Boolean zipkinEnabled = false;
        private String zipkinUrl = "http://localhost:9411/api/v2/spans";
        private Double sampleProbability = 0.1;
        private String serviceName = "EnterpriseTestService-EnterpriseTestService";
        private String serviceVersion = "1.0.0";
        private String environment = "development";
    }

    /**
     * External services properties
     */
    @Data
    public static class ExternalServicesProperties {
        @NestedConfigurationProperty
        private ServiceProperties payment = new ServiceProperties();

        @NestedConfigurationProperty
        private ServiceProperties notification = new ServiceProperties();

        @NestedConfigurationProperty
        private ServiceProperties email = new ServiceProperties();

        @NestedConfigurationProperty
        private ServiceProperties sms = new ServiceProperties();

        @NestedConfigurationProperty
        private ServiceProperties storage = new ServiceProperties();

        @Data
        public static class ServiceProperties {
            private String url;
            private String apiKey;
            private String secretKey;
            private Integer timeout = 30000;
            private Integer retryAttempts = 3;
            private Boolean enabled = false;
            private Map<String, String> headers = Map.of();
        }
    }

    /**
     * Business properties
     */
    @Data
    public static class BusinessProperties {
        @Min(1)
        private Integer itemsPerPage = 20;

        @Min(1)
        private Integer maxItemsPerPage = 100;

        private String defaultLanguage = "en";
        private List<String> supportedLanguages = List.of("en", "es", "fr", "de", "it", "pt");
        private String defaultCurrency = "USD";
        private List<String> supportedCurrencies = List.of("USD", "EUR", "GBP", "JPY");
        private String defaultTimeZone = "UTC";
        private Integer sessionTimeoutMinutes = 30;
        private Integer fileMaxSize = 10485760; // 10MB
        private List<String> allowedFileTypes = List.of("jpg", "jpeg", "png", "gif", "pdf", "doc", "docx");
        private Boolean auditEnabled = true;
        private Boolean rateLimitingEnabled = true;
        private Integer rateLimitRequestsPerMinute = 100;
        private Boolean maintenanceMode = false;
    }
}