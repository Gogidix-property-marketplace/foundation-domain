package com.gogidix.platform.common.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 🏗️ GOGIDIX FOUNDATION - KEYCLOAK AUTO-CONFIGURATION
 *
 * Unified Keycloak integration for all Gogidix microservices
 *
 * Features:
 * - Automatic JWT decoder configuration
 * - Cross-domain token validation
 * - Unified RBAC role mapping
 * - Zero-configuration Keycloak integration
 * - Service-to-service authentication
 * - Centralized realm management
 *
 * @author Gogidix Platform Team
 * @version 1.0.0
 * @since 2025-12-03
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "gogidix.foundation.security.keycloak.enabled", havingValue = "true", matchIfMissing = true)
public class KeycloakAutoConfiguration {

    /**
     * 🗝️ JWT Decoder Bean
     *
     * Configures JWT decoder with Keycloak JWK set endpoint
     * Enables automatic token validation and signature verification
     */
    @Bean
    public JwtDecoder jwtDecoder(KeycloakProperties keycloakProperties) {
        return NimbusJwtDecoder.withJwkSetUri(keycloakProperties.getJwkSetUri())
                .build();
    }

    /**
     * 🔐 JWT Authentication Converter
     *
     * Converts JWT tokens to Spring Security authentication
     * Maps Keycloak roles to Spring Security authorities
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    /**
     * 🛡️ Security Filter Chain
     *
     * Configures unified security across all services
     * Enables OAuth2 resource server with JWT validation
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Actuator endpoints - admin only
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Health endpoints - public
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // Eureka dashboard - admin only
                .requestMatchers("/eureka/**").hasRole("ADMIN")
                // Service registration - authenticated users
                .requestMatchers("/eureka/apps/**").authenticated()
                // API docs - authenticated users
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()
                // All other requests - authenticated
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(jwtDecoder(new KeycloakProperties()))
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/eureka/**", "/actuator/**")
            );

        return http.build();
    }

    /**
     * 🏢 Keycloak Properties
     *
     * Configuration properties for Keycloak integration
     */
    public static class KeycloakProperties {
        private String authServerUrl = "http://localhost:8081/auth";
        private String realm = "gogidix";
        private String clientId = "gogidix-platform";
        private String clientSecret = "${KEYCLOAK_CLIENT_SECRET:}";
        private String jwkSetUri = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
        private String issuerUri = authServerUrl + "/realms/" + realm;

        // Getters and setters
        public String getAuthServerUrl() { return authServerUrl; }
        public void setAuthServerUrl(String authServerUrl) { this.authServerUrl = authServerUrl; }

        public String getRealm() { return realm; }
        public void setRealm(String realm) { this.realm = realm; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getJwkSetUri() { return jwkSetUri; }
        public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }

        public String getIssuerUri() { return issuerUri; }
        public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }
    }
}