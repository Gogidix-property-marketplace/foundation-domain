package com.gogidix.dashboard.centralized.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gogidix.dashboard.centralized.config.DashboardProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Security filter for dashboard authentication and authorization.
 * Validates JWT tokens and manages user sessions with Redis caching.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
public class DashboardSecurityFilter extends OncePerRequestFilter {

    private final DashboardProperties dashboardProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecretKey signingKey;

    @Autowired
    public DashboardSecurityFilter(DashboardProperties dashboardProperties,
                                 RedisTemplate<String, Object> redisTemplate,
                                 ObjectMapper objectMapper) {
        this.dashboardProperties = dashboardProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        // In production, this should come from a secure configuration
        this.signingKey = Keys.hmacShaKeyFor(dashboardProperties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Filters incoming requests for authentication.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if servlet exception occurs
     * @throws IOException if I/O exception occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        // Skip authentication for public endpoints
        if (isPublicEndpoint(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract and validate JWT token
        String token = extractToken(request);
        if (token == null) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing authentication token");
            return;
        }

        try {
            Claims claims = validateToken(token);

            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token has been revoked");
                return;
            }

            // Update user session in Redis
            updateUserSession(claims);

            // Set authentication context
            setAuthenticationContext(claims);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }
    }

    /**
     * Determines if the endpoint is public and doesn't require authentication.
     *
     * @param requestURI the request URI
     * @return true if public endpoint, false otherwise
     */
    private boolean isPublicEndpoint(String requestURI) {
        List<String> publicEndpoints = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/health",
            "/api/v1/metrics",
            "/ws/dashboard",
            "/v3/api-docs",
            "/swagger-ui"
        );

        return publicEndpoints.stream().anyMatch(requestURI::startsWith);
    }

    /**
     * Extracts JWT token from the request.
     *
     * @param request the HTTP request
     * @return JWT token or null if not found
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Also check for token in query parameters (for WebSocket connections)
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        return null;
    }

    /**
     * Validates JWT token and returns claims.
     *
     * @param token the JWT token
     * @return claims from the token
     */
    private Claims validateToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Check expiration
        if (claims.getExpiration().before(Date.from(Instant.now()))) {
            throw new RuntimeException("Token has expired");
        }

        // Check issuer
        if (!dashboardProperties.getSecurity().getIssuer().equals(claims.getIssuer())) {
            throw new RuntimeException("Invalid token issuer");
        }

        return claims;
    }

    /**
     * Checks if the token is blacklisted in Redis.
     *
     * @param token the JWT token
     * @return true if blacklisted, false otherwise
     */
    private boolean isTokenBlacklisted(String token) {
        String blacklistKey = "blacklist:token:" + token.hashCode();
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    /**
     * Updates user session in Redis.
     *
     * @param claims the user claims from JWT
     */
    private void updateUserSession(Claims claims) {
        String userId = claims.getSubject();
        String sessionKey = "session:user:" + userId;

        Map<String, Object> sessionData = Map.of(
            "userId", userId,
            "username", claims.get("username"),
            "email", claims.get("email"),
            "roles", claims.get("roles", List.of()),
            "lastAccess", Instant.now().toString(),
            "ipAddress", getClientIpAddress()
        );

        redisTemplate.opsForHash().putAll(sessionKey, sessionData);
        redisTemplate.expire(sessionKey, dashboardProperties.getBusiness().getSessionTimeoutMinutes(), TimeUnit.MINUTES);
    }

    /**
     * Sets the Spring Security authentication context.
     *
     * @param claims the user claims from JWT
     */
    @SuppressWarnings("unchecked")
    private void setAuthenticationContext(Claims claims) {
        String userId = claims.getSubject();
        List<String> roles = (List<String>) claims.get("roles", List.of());

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Extracts client IP address from the request.
     *
     * @return client IP address
     */
    private String getClientIpAddress() {
        return ""; // Implement IP extraction logic
    }

    /**
     * Sends error response.
     *
     * @param response the HTTP response
     * @param status the HTTP status
     * @param message the error message
     * @throws IOException if I/O exception occurs
     */
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
}