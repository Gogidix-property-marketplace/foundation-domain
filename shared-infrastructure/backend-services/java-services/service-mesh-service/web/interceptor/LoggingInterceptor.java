package com.gogidix.infrastructure.mesh.web.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logging interceptor for HTTP requests.
 * Logs request and response information for debugging and monitoring.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = generateRequestId();
        request.setAttribute("requestId", requestId);
        request.setAttribute("startTime", System.currentTimeMillis());

        log.info("Incoming Request - ID: {}, Method: {}, URI: {}, RemoteAddr: {}, UserAgent: {}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                getRemoteAddr(request),
                request.getHeader("User-Agent"));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        String requestId = (String) request.getAttribute("requestId");
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        log.info("Request Completed - ID: {}, Method: {}, URI: {}, Status: {}, Duration: {}ms, Timestamp: {}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                LocalDateTime.now().format(formatter));

        if (ex != null) {
            log.error("Request Exception - ID: {}, Exception: {}", requestId, ex.getMessage(), ex);
        }
    }

    /**
     * Generates a unique request ID.
     *
     * @return request ID
     */
    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Gets client IP address with X-Forwarded-For header support.
     *
     * @param request the HTTP request
     * @return client IP address
     */
    private String getRemoteAddr(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}