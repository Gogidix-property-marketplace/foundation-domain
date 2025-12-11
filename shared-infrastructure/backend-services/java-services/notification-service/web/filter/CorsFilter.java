package com.gogidix.infrastructure.notification.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CORS filter for cross-origin requests.
 * Handles CORS headers and pre-flight requests.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String origin = request.getHeader("Origin");

        // Set CORS headers
        response.setHeader("Access-Control-Allow-Origin", getAllowedOrigin(origin));
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, X-Total-Count, X-Page-Count");
        response.setHeader("Access-Control-Expose-Headers", "X-Total-Count, X-Page-Count");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");

        // Handle pre-flight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("Handling CORS pre-flight request from origin: {}", origin);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        log.trace("CORS filter processed request from origin: {}", origin);
        filterChain.doFilter(request, response);
    }

    /**
     * Determines allowed origin based on request origin.
     *
     * @param requestOrigin the request origin
     * @return allowed origin
     */
    private String getAllowedOrigin(String requestOrigin) {
        if (requestOrigin == null) {
            return "*";
        }

        // In production, implement proper origin validation
        if (isDevelopmentEnvironment() || isTrustedOrigin(requestOrigin)) {
            return requestOrigin;
        }

        return "";
    }

    /**
     * Checks if the environment is development.
     *
     * @return true if development environment
     */
    private boolean isDevelopmentEnvironment() {
        String env = System.getProperty("spring.profiles.active", "dev");
        return env.contains("dev") || env.contains("local");
    }

    /**
     * Checks if the origin is trusted.
     *
     * @param origin the origin to check
     * @return true if trusted
     */
    private boolean isTrustedOrigin(String origin) {
        // Add your trusted origins here
        return origin.endsWith("example.com") ||
               origin.endsWith("localhost") ||
               origin.endsWith("127.0.0.1");
    }
}