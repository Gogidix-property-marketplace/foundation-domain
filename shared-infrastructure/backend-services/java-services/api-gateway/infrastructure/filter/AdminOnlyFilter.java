package com.gogidix.infrastructure.gateway.infrastructure.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Admin-only filter that restricts access to administrator users only.
 * This filter checks if the user has admin privileges before allowing access.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
public class AdminOnlyFilter implements GatewayFilter, Ordered {

    private static final List<String> ADMIN_ROLES = Arrays.asList("ADMIN", "SUPER_ADMIN", "SYSTEM_ADMIN");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Extract user roles from headers (set by SecurityFilter)
        String userRoles = request.getHeaders().getFirst("X-User-Roles");

        if (userRoles == null || userRoles.trim().isEmpty()) {
            log.warn("No user roles found in request headers for path: {}", request.getPath());
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // Check if user has admin role
        boolean hasAdminRole = Arrays.stream(userRoles.split(","))
                .anyMatch(role -> ADMIN_ROLES.contains(role.trim().toUpperCase()));

        if (!hasAdminRole) {
            log.warn("User without admin privileges attempted to access admin endpoint: {}", request.getPath());
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        log.debug("Admin access granted for user with roles: {} to path: {}", userRoles, request.getPath());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -90; // Execute after SecurityFilter but before other filters
    }
}