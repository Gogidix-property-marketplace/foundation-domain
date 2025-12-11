package com.gogidix.microservices.advanced.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;

import java.util.Arrays;

/**
 * Configuration for Lead Conversion AI Service
 * Includes CORS, Swagger, and lead conversion-specific configurations
 */
@Configuration
public class LeadConversionAIConfig {

    @Bean
    public OpenAPI leadConversionAIOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lead Conversion AI Service API")
                        .description("AI-powered lead conversion optimization and client journey management")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Gogidix Lead Conversion Team")
                                .email("lead-conversion@gogidix.com")
                                .url("https://gogidix.com")))
                .servers(Arrays.asList(
                        new Server().url("http://localhost:8506").description("Development Server"),
                        new Server().url("https://api.gogidix.com/lead-conversion-ai").description("Production Server")
                ));
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}