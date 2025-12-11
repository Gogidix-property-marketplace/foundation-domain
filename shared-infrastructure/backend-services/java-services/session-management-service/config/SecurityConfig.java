package com.gogidix.infrastructure.session.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for EnterpriseTestService application.
 * Configures JWT/OAuth2 authentication and authorization.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.security.jwt.secret:EnterpriseTestService-default-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration:86400}")
    private Long jwtExpiration;

    @Value("${app.security.oauth2.enabled:false}")
    private Boolean oauth2Enabled;

    @Value("${app.security.oauth2.issuer-uri:}")
    private String oauth2IssuerUri;

    @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:4200}")
    private List<String> allowedOrigins;

    @Value("${app.security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${app.security.cors.allowed-headers:*}")
    private List<String> allowedHeaders;

    /**
     * Main security filter chain configuration.
     *
     * @param http HttpSecurity configuration
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");

        // Disable CSRF for REST APIs
        http.csrf(AbstractHttpConfigurer::disable);

        // Configure CORS
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // Configure session management
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Configure request authorization
        http.authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/api/actuator/health").permitAll()
                .requestMatchers("/api/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Webhook endpoints (if any)
                .requestMatchers("/api/webhooks/**").permitAll()

                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/actuator/**").hasRole("ADMIN")

                // Protected endpoints
                .requestMatchers("/api/**").authenticated()

                // Catch all
                .anyRequest().authenticated()
        );

        // Configure OAuth2 resource server if enabled
        if (oauth2Enabled && !oauth2IssuerUri.isEmpty()) {
            log.info("Enabling OAuth2 resource server with issuer: {}", oauth2IssuerUri);
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .decoder(jwtDecoder())
                            .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    )
            );
        } else {
            log.info("Using custom JWT authentication");
            // Configure JWT authentication
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            http.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint));
        }

        // Configure headers
        http.headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAge(Duration.ofSeconds(31536000))
                        .preload(true)
                )
        );

        return http.build();
    }

    /**
     * Password encoder bean.
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication manager bean.
     *
     * @param config AuthenticationConfiguration
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * JWT decoder for OAuth2 resource server.
     *
     * @return JwtDecoder
     */
    @Bean
    @Profile("oauth2")
    public JwtDecoder jwtDecoder() {
        if (!oauth2IssuerUri.isEmpty()) {
            return JwtDecoders.fromIssuerLocation(oauth2IssuerUri);
        }

        // Fallback to symmetric key decoder
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    /**
     * JWT authentication converter.
     *
     * @return JwtAuthenticationConverter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }

    /**
     * CORS configuration source.
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS with origins: {}", allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * JWT secret key bean.
     *
     * @return SecretKey
     */
    @Bean
    @Profile("!oauth2")
    public SecretKey jwtSecretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
    }

    /**
     * RSA public key bean for JWT verification (if using RSA).
     *
     * @return RSAPublicKey (dummy implementation)
     */
    @Bean
    @Profile("rsa-jwt")
    public RSAPublicKey rsaPublicKey() {
        // In production, this should load the actual public key from a file or keystore
        log.warn("Using dummy RSA public key. Implement proper RSA key management in production.");
        return null;
    }

    /**
     * Security properties bean for custom security configuration.
     *
     * @return SecurityProperties
     */
    @Bean
    public SecurityProperties securityProperties() {
        SecurityProperties properties = new SecurityProperties();
        properties.setJwtSecret(jwtSecret);
        properties.setJwtExpiration(jwtExpiration);
        properties.setOauth2Enabled(oauth2Enabled);
        properties.setOauth2IssuerUri(oauth2IssuerUri);
        return properties;
    }

    /**
     * Inner class for security properties.
     */
    public static class SecurityProperties {
        private String jwtSecret;
        private Long jwtExpiration;
        private Boolean oauth2Enabled;
        private String oauth2IssuerUri;

        // Getters and setters
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

        public Long getJwtExpiration() { return jwtExpiration; }
        public void setJwtExpiration(Long jwtExpiration) { this.jwtExpiration = jwtExpiration; }

        public Boolean getOauth2Enabled() { return oauth2Enabled; }
        public void setOauth2Enabled(Boolean oauth2Enabled) { this.oauth2Enabled = oauth2Enabled; }

        public String getOauth2IssuerUri() { return oauth2IssuerUri; }
        public void setOauth2IssuerUri(String oauth2IssuerUri) { this.oauth2IssuerUri = oauth2IssuerUri; }
    }
}