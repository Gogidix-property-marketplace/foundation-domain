package com.gogidix.platform.common.security.rbac;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.gogidix.platform.common.security.rbac.properties.RBACProperties;
import com.gogidix.platform.common.security.rbac.service.RBACUserDetailsService;
import com.gogidix.platform.common.security.rbac.filter.RBACFilter;
import com.gogidix.platform.common.security.rbac.expression.RBACMethodSecurityExpressionHandler;

import java.util.Arrays;
import java.util.List;

/**
 * 🏗️ GOGIDIX FOUNDATION - UNIFIED RBAC CONFIGURATION
 *
 * Comprehensive Role-Based Access Control for all Gogidix services
 *
 * Features:
 * - Unified role hierarchy across all domains
 * - Method-level security with custom expressions
 * - Cross-domain permission inheritance
 * - Dynamic role assignment
 * - Service-to-service authorization
 * - Centralized permission management
 *
 * @author Gogidix Platform Team
 * @version 1.0.0
 * @since 2025-12-03
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@EnableConfigurationProperties(RBACProperties.class)
public class RBACConfiguration {

    /**
     * 🔐 Password Encoder
     *
     * BCrypt encoder for secure password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 👤 User Details Service
     *
     * Custom user service with RBAC support
     */
    @Bean
    public UserDetailsService userDetailsService(RBACProperties rbacProperties) {
        return new RBACUserDetailsService(rbacProperties);
    }

    /**
     * 🔑 JWT Authorities Converter
     *
     * Converts JWT claims to Spring Security authorities with RBAC mapping
     */
    @Bean
    public JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter(RBACProperties rbacProperties) {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("ROLE_");
        converter.setAuthoritiesClaimName(rbacProperties.getJwt().getRolesClaim());
        return converter;
    }

    /**
     * 🎫 JWT Authentication Converter
     *
     * Converts JWT tokens to authentication objects with RBAC support
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(RBACProperties rbacProperties) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter(rbacProperties));
        return converter;
    }

    /**
     * 🛡️ Security Filter Chain
     *
     * Configures HTTP security with RBAC rules
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RBACProperties rbacProperties, RBACFilter rbacFilter) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource(rbacProperties))
            )
            .csrf(csrf -> csrf
                .disable()
            )
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/eureka/**").permitAll()

                // Service-to-service communication
                .requestMatchers("/services/**").hasRole("SERVICE")

                // Admin endpoints
                .requestMatchers("/actuator/**", "/admin/**", "/management/**")
                    .hasAnyRole(rbacProperties.getRoles().getAdmin().toArray(new String[0]))

                // Domain-specific endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "USER_MANAGER", "USER")
                .requestMatchers("/api/v1/properties/**").hasAnyRole("ADMIN", "PROPERTY_MANAGER", "AGENT")
                .requestMatchers("/api/v1/bookings/**").hasAnyRole("ADMIN", "PROPERTY_MANAGER", "TENANT")
                .requestMatchers("/api/v1/billing/**").hasAnyRole("ADMIN", "FINANCE_MANAGER", "ACCOUNTANT")
                .requestMatchers("/api/v1/notifications/**").hasAnyRole("ADMIN", "NOTIFICATION_MANAGER")
                .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "REPORT_MANAGER", "ANALYST")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter(rbacProperties))
                )
            )
            .addFilterBefore(rbacFilter, AuthorizationFilter.class);

        return http.build();
    }

    /**
     * 🌐 CORS Configuration Source
     *
     * Configures CORS for cross-domain requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(RBACProperties rbacProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(rbacProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(rbacProperties.getCors().getAllowedMethods());
        configuration.setAllowedHeaders(rbacProperties.getCors().getAllowedHeaders());
        configuration.setAllowCredentials(rbacProperties.getCors().getAllowCredentials());
        configuration.setMaxAge(rbacProperties.getCors().getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 🎯 Method Security Expression Handler
     *
     * Custom expression handler with RBAC support
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RBACProperties rbacProperties) {
        DefaultMethodSecurityExpressionHandler handler = new RBACMethodSecurityExpressionHandler(rbacProperties);
        return handler;
    }

    /**
     * 🔍 RBAC Filter
     *
     * Custom filter for RBAC enforcement
     */
    @Bean
    public RBACFilter rbacFilter(RBACProperties rbacProperties) {
        return new RBACFilter(rbacProperties);
    }

    /**
     * 🏢 RBAC Properties Configuration
     */
    @Configuration
    @EnableConfigurationProperties(RBACProperties.class)
    public static class RBACPropertiesConfiguration {
        // Configuration properties are handled by RBACProperties class
    }
}