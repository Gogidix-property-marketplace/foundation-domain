package com.gogidix.infrastructure.cache.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter for API endpoints.
 * Implements rate limiting using Redis sliding window algorithm.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private final int requestsPerMinute;
    private final Duration windowSize;

    public RateLimitingFilter(RedisTemplate<String, String> redisTemplate,
                              @Value("${app.business.rate-limit-requests-per-minute:100}") int requestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.requestsPerMinute = requestsPerMinute;
        this.windowSize = Duration.ofMinutes(1);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        if (shouldApplyRateLimiting(request)) {
            String clientIdentifier = getClientIdentifier(request);
            String rateLimitKey = "rate_limit:" + clientIdentifier;

            if (isRateLimited(rateLimitKey)) {
                log.warn("Rate limit exceeded for client: {}", clientIdentifier);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
                response.setHeader("X-RateLimit-Remaining", "0");
                response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + windowSize.toMillis()));
                return;
            }

            recordRequest(rateLimitKey);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determines if rate limiting should be applied to this request.
     *
     * @param request the HTTP request
     * @return true if rate limiting should be applied
     */
    private boolean shouldApplyRateLimiting(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Apply rate limiting to POST, PUT, DELETE, PATCH endpoints
        return ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method)) &&
               uri.startsWith("/api/") &&
               !uri.contains("/actuator/") &&
               !uri.contains("/health");
    }

    /**
     * Gets client identifier for rate limiting.
     *
     * @param request the HTTP request
     * @return client identifier
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Use IP address for rate limiting
        String clientIp = getClientIpAddress(request);

        // If user is authenticated, use user ID instead of IP
        String userId = getAuthenticatedUserId(request);

        return userId != null ? "user:" + userId : "ip:" + clientIp;
    }

    /**
     * Gets client IP address.
     *
     * @param request the HTTP request
     * @return IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Gets authenticated user ID.
     *
     * @param request the HTTP request
     * @return user ID or null if not authenticated
     */
    private String getAuthenticatedUserId(HttpServletRequest request) {
        // This would typically be implemented using Spring Security context
        // For now, return null to use IP-based rate limiting
        return null;
    }

    /**
     * Checks if the client is rate limited.
     *
     * @param rateLimitKey the rate limit key
     * @return true if rate limited
     */
    private boolean isRateLimited(String rateLimitKey) {
        try {
            Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);

            if (currentCount == 1) {
                // First request in the window, set expiration
                redisTemplate.expire(rateLimitKey, windowSize.getSeconds(), TimeUnit.SECONDS);
            }

            return currentCount > requestsPerMinute;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", rateLimitKey, e);
            // If Redis is unavailable, allow the request (fail open)
            return false;
        }
    }

    /**
     * Records a request for rate limiting.
     *
     * @param rateLimitKey the rate limit key
     */
    private void recordRequest(String rateLimitKey) {
        try {
            redisTemplate.opsForValue().increment(rateLimitKey);
        } catch (Exception e) {
            log.error("Error recording rate limit request for key: {}", rateLimitKey, e);
        }
    }
}