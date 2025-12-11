package com.gogidix.dashboard.centralized.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Admin-only access filter for administrative dashboard endpoints.
 * Restricts access to users with ADMIN role.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
public class AdminOnlyFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public AdminOnlyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Filters requests to ensure only admin users can access administrative endpoints.
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

        // Check if this is an admin endpoint
        if (isAdminEndpoint(request.getRequestURI())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !hasAdminRole(authentication)) {
                log.warn("Unauthorized access attempt to admin endpoint: {} by user: {}",
                    request.getRequestURI(), authentication != null ? authentication.getName() : "anonymous");

                sendErrorResponse(response, HttpStatus.FORBIDDEN,
                    "Access denied. Administrator privileges required.");
                return;
            }

            log.debug("Admin access granted to: {} for endpoint: {}",
                authentication.getName(), request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determines if the endpoint is administrative and requires admin privileges.
     *
     * @param requestURI the request URI
     * @return true if admin endpoint, false otherwise
     */
    private boolean isAdminEndpoint(String requestURI) {
        return requestURI.startsWith("/api/v1/admin/") ||
               requestURI.startsWith("/api/v1/dashboard/admin/") ||
               requestURI.startsWith("/api/v1/users/") ||
               requestURI.startsWith("/api/v1/system/") ||
               requestURI.startsWith("/api/v1/config/") ||
               requestURI.startsWith("/api/v1/maintenance/");
    }

    /**
     * Checks if the authenticated user has admin role.
     *
     * @param authentication the Spring Security authentication
     * @return true if user has admin role, false otherwise
     */
    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Sends error response for unauthorized access.
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
            "timestamp", Instant.now(),
            "path", ""
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
