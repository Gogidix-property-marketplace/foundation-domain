package com.gogidix.platform.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Security configuration properties for Gogidix services
 */
@Data
@ConfigurationProperties(prefix = "gogidix.security")
public class SecurityProperties {

    /**
     * Enable/disable security features
     */
    private boolean enabled = true;

    /**
     * JWT configuration
     */
    @NestedConfigurationProperty
    private Jwt jwt = new Jwt();

    /**
     * CORS configuration
     */
    @NestedConfigurationProperty
    private Cors cors = new Cors();

    /**
     * API Key configuration
     */
    @NestedConfigurationProperty
    private ApiKey apiKey = new ApiKey();

    /**
     * OAuth2 configuration
     */
    @NestedConfigurationProperty
    private OAuth2 oauth2 = new OAuth2();

    /**
     * Public paths that don't require authentication
     */
    private List<String> publicPaths = new ArrayList<>(List.of(
        "/api/auth/**",
        "/api/public/**",
        "/error"
    ));

    /**
     * Paths protected by API key
     */
    private List<String> apiKeyProtectedPaths = new ArrayList<>(List.of(
        "/api/v1/**"
    ));

    /**
     * Allowed origins for CORS
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of(
        "http://localhost:3000",
        "http://localhost:8080",
        "https://gogidix.com"
    ));

    @Data
    public static class Jwt {
        /**
         * JWT secret key
         */
        private String secret;

        /**
         * JWT expiration time
         */
        private Duration expiration = Duration.ofHours(24);

        /**
         * JWT refresh token expiration time
         */
        private Duration refreshExpiration = Duration.ofDays(7);

        /**
         * JWT issuer
         */
        private String issuer = "gogidix-platform";

        /**
         * Clock skew to handle clock differences
         */
        private Duration clockSkew = Duration.ofSeconds(30);
    }

    @Data
    public static class Cors {
        /**
         * Enable CORS
         */
        private boolean enabled = true;

        /**
         * Allowed origins
         */
        private List<String> allowedOrigins = new ArrayList<>();

        /**
         * Allowed methods
         */
        private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        /**
         * Allowed headers
         */
        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

        /**
         * Exposed headers
         */
        private List<String> exposedHeaders = new ArrayList<>();

        /**
         * Allow credentials
         */
        private boolean allowCredentials = true;

        /**
         * Max age of pre-flight requests
         */
        private Duration maxAge = Duration.ofHours(1);
    }

    @Data
    public static class ApiKey {
        /**
         * Enable API key authentication
         */
        private boolean enabled = false;

        /**
         * API key header name
         */
        private String headerName = "X-API-Key";

        /**
         * Valid API keys
         */
        private List<String> validKeys = new ArrayList<>();
    }

    @Data
    public static class OAuth2 {
        /**
         * Enable OAuth2
         */
        private boolean enabled = false;

        /**
         * OAuth2 issuer URI
         */
        private String issuerUri;

        /**
         * OAuth2 client ID
         */
        private String clientId;

        /**
         * OAuth2 client secret
         */
        private String clientSecret;

        /**
         * OAuth2 redirect URI
         */
        private String redirectUri;
    }
}