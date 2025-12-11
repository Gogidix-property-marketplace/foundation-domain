package com.gogidix.infrastructure.gateway.infrastructure.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter filter that controls request frequency per client.
 * Implements token bucket algorithm using Redis for distributed rate limiting.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
public class RateLimiterFilter implements GatewayFilter, Ordered {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    // Configuration parameters
    private static final int DEFAULT_REQUEST_LIMIT = 100;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String clientId = getClientIdentifier(request);

        // Get rate limit configuration based on user tier
        RateLimitConfig config = getRateLimitConfig(request);

        // Redis key for rate limiting
        String redisKey = RATE_LIMIT_PREFIX + clientId;

        // Implement token bucket algorithm
        return redisTemplate.opsForValue()
                .increment(redisKey, 1)
                .flatMap(requestCount -> {
                    if (requestCount == 1) {
                        // First request in window, set expiration
                        return redisTemplate.expire(redisKey, config.getWindowSize())
                                .thenReturn(requestCount);
                    }
                    return Mono.just(requestCount);
                })
                .flatMap(requestCount -> {
                    if (requestCount > config.getRequestLimit()) {
                        log.warn("Rate limit exceeded for client: {} with {} requests", clientId, requestCount);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getRequestLimit()));
                        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
                        exchange.getResponse().getHeaders().add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + config.getWindowSize().toMillis()));
                        return exchange.getResponse().setComplete();
                    }

                    // Add rate limit headers
                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getRequestLimit()));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(Math.max(0, config.getRequestLimit() - requestCount.intValue())));

                    return chain.filter(exchange);
                })
                .onErrorResume(throwable -> {
                    log.error("Error in rate limiting for client: {}", clientId, throwable);
                    // Allow request if Redis is down
                    return chain.filter(exchange);
                });
    }

    private String getClientIdentifier(ServerHttpRequest request) {
        // Try to get user ID from headers (set by SecurityFilter)
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.trim().isEmpty()) {
            return "user:" + userId;
        }

        // Fallback to IP address
        String clientIp = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return "ip:" + clientIp;
    }

    private RateLimitConfig getRateLimitConfig(ServerHttpRequest request) {
        // Get user tier from headers or role
        String userRoles = request.getHeaders().getFirst("X-User-Roles");
        String userTier = request.getHeaders().getFirst("X-User-Tier");

        if (userTier != null) {
            return getTierBasedConfig(userTier);
        }

        if (userRoles != null && userRoles.contains("ADMIN")) {
            return new RateLimitConfig(1000, Duration.ofMinutes(1)); // Higher limit for admins
        }

        if (userRoles != null && userRoles.contains("PREMIUM")) {
            return new RateLimitConfig(500, Duration.ofMinutes(1));
        }

        return new RateLimitConfig(DEFAULT_REQUEST_LIMIT, DEFAULT_WINDOW);
    }

    private RateLimitConfig getTierBasedConfig(String tier) {
        switch (tier.toUpperCase()) {
            case "ENTERPRISE":
                return new RateLimitConfig(5000, Duration.ofMinutes(1));
            case "PREMIUM":
                return new RateLimitConfig(1000, Duration.ofMinutes(1));
            case "STANDARD":
                return new RateLimitConfig(200, Duration.ofMinutes(1));
            default:
                return new RateLimitConfig(DEFAULT_REQUEST_LIMIT, DEFAULT_WINDOW);
        }
    }

    @Override
    public int getOrder() {
        return -80; // Execute after security filters
    }

    @Data
    private static class RateLimitConfig {
        private final int requestLimit;
        private final Duration windowSize;

        public RateLimitConfig(int requestLimit, Duration windowSize) {
            this.requestLimit = requestLimit;
            this.windowSize = windowSize;
        }
    }
}