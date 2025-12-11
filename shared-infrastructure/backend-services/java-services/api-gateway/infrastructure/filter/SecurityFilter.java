package com.gogidix.infrastructure.gateway.infrastructure.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Security filter for API Gateway that validates JWT tokens.
 * This filter intercepts all incoming requests and validates authentication.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
public class SecurityFilter implements GatewayFilter, Ordered {

    private final WebClient.Builder webClientBuilder;

    public SecurityFilter(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Skip authentication for health checks and public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("Skipping authentication for public endpoint: {}", path);
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // Validate token with authentication service
        return validateToken(token)
                .flatMap(isValid -> {
                    if (isValid) {
                        // Add user context to headers
                        exchange.getRequest().mutate()
                                .header("X-User-Id", extractUserIdFromToken(token))
                                .header("X-User-Roles", extractUserRolesFromToken(token));
                        return chain.filter(exchange);
                    } else {
                        log.warn("Invalid token for path: {}", path);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("Error validating token for path: {}", path, throwable);
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/health") ||
               path.contains("/actuator") ||
               path.contains("/favicon.ico") ||
               path.contains("/swagger") ||
               path.contains("/api/v1/auth/login") ||
               path.contains("/api/v1/auth/register") ||
               path.contains("/fallback");
    }

    private Mono<Boolean> validateToken(String token) {
        return webClientBuilder.build()
                .post()
                .uri("lb://authentication-service/api/v1/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Boolean.class)
                .defaultIfEmpty(false);
    }

    private String extractUserIdFromToken(String token) {
        // In production, decode JWT token properly
        return "user-123"; // Placeholder
    }

    private String extractUserRolesFromToken(String token) {
        // In production, decode JWT token properly
        return "USER,ADMIN"; // Placeholder
    }

    @Override
    public int getOrder() {
        return -100; // High priority
    }
}