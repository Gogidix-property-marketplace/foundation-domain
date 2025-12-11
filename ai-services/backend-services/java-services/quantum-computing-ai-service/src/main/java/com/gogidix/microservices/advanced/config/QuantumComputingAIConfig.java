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
 * Configuration for Quantum Computing Analysis AI Service
 * Includes CORS, Swagger, and quantum-specific configurations
 */
@Configuration
public class QuantumComputingAIConfig {

    @Bean
    public OpenAPI quantumComputingAIOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Quantum Computing Analysis AI Service API")
                        .description("Advanced quantum computing algorithms for real estate portfolio optimization and market analysis")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Gogidix Quantum AI Team")
                                .email("quantum-ai@gogidix.com")
                                .url("https://gogidix.com")))
                .servers(Arrays.asList(
                        new Server().url("http://localhost:8504").description("Development Server"),
                        new Server().url("https://api.gogidix.com/quantum-computing-ai").description("Production Server")
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