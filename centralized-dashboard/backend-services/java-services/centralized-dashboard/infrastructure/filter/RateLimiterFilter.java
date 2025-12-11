package com.gogidix.dashboard.centralized.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gogidix.dashboard.centralized.config.DashboardProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Rate limiting filter for dashboard API protection.
 * Implements distributed rate limiting using Redis with tier-based access.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private final DashboardProperties dashboardProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public RateLimiterFilter(DashboardProperties dashboardProperties,
                           RedisTemplate<String, Object> redisTemplate,
                           ObjectMapper objectMapper) {
        this.dashboardProperties = dashboardProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        if (!dashboardProperties.getBusiness().getRateLimitingEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = getClientId(request);
        String endpoint = getEndpointCategory(request.getRequestURI());

        RateLimitConfig rateLimitConfig = getRateLimitConfig(clientId, endpoint);

        if (isRateLimited(clientId, endpoint, rateLimitConfig)) {
            log.warn("Rate limit exceeded for client: {} on endpoint: {}", clientId, endpoint);
            sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Please try again later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId != null) {
            return "user:" + userId;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        return "ip:" + ip;
    }

    private String getEndpointCategory(String requestURI) {
        if (requestURI.contains("/auth/")) {
            return "auth";
        } else if (requestURI.contains("/dashboard/")) {
            return "dashboard";
        } else if (requestURI.contains("/metrics/") || requestURI.contains("/analytics/")) {
            return "analytics";
        } else if (requestURI.contains("/admin/")) {
            return "admin";
        } else {
            return "general";
        }
    }

    private RateLimitConfig getRateLimitConfig(String clientId, String endpoint) {
        String tier = getUserTier(clientId);

        return switch (tier.toLowerCase()) {
            case "enterprise" -> getEnterpriseConfig(endpoint);
            case "premium" -> getPremiumConfig(endpoint);
            case "basic" -> getBasicConfig(endpoint);
            default -> getFreeConfig(endpoint);
        };
    }

    private boolean isRateLimited(String clientId, String endpoint, RateLimitConfig config) {
        String key = "ratelimit:" + clientId + ":" + endpoint;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key, 1);

            if (currentCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(config.getPeriodSeconds()));
            }

            if (currentCount > config.getMaxRequests()) {
                redisTemplate.expire(key, Duration.ofSeconds(config.getBlockTimeSeconds()));
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Error checking rate limit: {}", e.getMessage());
            return false;
        }
    }

    private String getUserTier(String clientId) {
        try {
            String sessionKey = "session:" + clientId;
            Object tier = redisTemplate.opsForHash().get(sessionKey, "tier");
            if (tier != null) {
                return tier.toString();
            }
        } catch (Exception e) {
            log.debug("Error retrieving user tier: {}", e.getMessage());
        }

        return "free";
    }

    private RateLimitConfig getEnterpriseConfig(String endpoint) {
        return switch (endpoint) {
            case "auth" -> new RateLimitConfig(100, 60, 300);
            case "dashboard" -> new RateLimitConfig(1000, 60, 60);
            case "analytics" -> new RateLimitConfig(500, 60, 120);
            case "admin" -> new RateLimitConfig(200, 60, 60);
            default -> new RateLimitConfig(2000, 60, 30);
        };
    }

    private RateLimitConfig getPremiumConfig(String endpoint) {
        return switch (endpoint) {
            case "auth" -> new RateLimitConfig(50, 60, 300);
            case "dashboard" -> new RateLimitConfig(500, 60, 60);
            case "analytics" -> new RateLimitConfig(250, 60, 120);
            case "admin" -> new RateLimitConfig(100, 60, 60);
            default -> new RateLimitConfig(1000, 60, 30);
        };
    }

    private RateLimitConfig getBasicConfig(String endpoint) {
        return switch (endpoint) {
            case "auth" -> new RateLimitConfig(20, 60, 300);
            case "dashboard" -> new RateLimitConfig(200, 60, 60);
            case "analytics" -> new RateLimitConfig(100, 60, 120);
            case "admin" -> new RateLimitConfig(50, 60, 60);
            default -> new RateLimitConfig(400, 60, 30);
        };
    }

    private RateLimitConfig getFreeConfig(String endpoint) {
        return switch (endpoint) {
            case "auth" -> new RateLimitConfig(10, 60, 300);
            case "dashboard" -> new RateLimitConfig(100, 60, 60);
            case "analytics" -> new RateLimitConfig(50, 60, 120);
            case "admin" -> new RateLimitConfig(20, 60, 60);
            default -> new RateLimitConfig(200, 60, 30);
        };
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");

        Map<String, Object> errorResponse = Map.of(
            "error", status.getReasonPhrase(),
            "message", message,
            "status", status.value(),
            "timestamp", Instant.now()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private record RateLimitConfig(int maxRequests, int periodSeconds, int blockTimeSeconds) {}
}
