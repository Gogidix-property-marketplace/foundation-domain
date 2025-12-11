package com.gogidix.ai.analytics.infrastructure.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Audit logger for EnterpriseTestService-EnterpriseTestService.
 * Provides comprehensive audit logging for all system operations.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private final ExecutorService auditExecutor = Executors.newFixedThreadPool(2);

    /**
     * Logs user authentication event.
     *
     * @param userId   the user ID
     * @param username the username
     * @param success  whether authentication was successful
     * @param request  the HTTP request
     */
    public void logAuthentication(String userId, String username, boolean success, HttpServletRequest request) {
        logAuditEvent("AUTHENTICATION", Map.of(
                "userId", userId,
                "username", username,
                "success", success,
                "ipAddress", getClientIpAddress(request),
                "userAgent", request.getHeader("User-Agent"),
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Logs data access event.
     *
     * @param userId     the user ID
     * @param resource   the resource being accessed
     * @param action     the action performed
     * @param resourceId the resource ID
     * @param request    the HTTP request
     */
    public void logDataAccess(String userId, String resource, String action, String resourceId, HttpServletRequest request) {
        logAuditEvent("DATA_ACCESS", Map.of(
                "userId", userId,
                "resource", resource,
                "action", action,
                "resourceId", resourceId,
                "ipAddress", getClientIpAddress(request),
                "method", request.getMethod(),
                "uri", request.getRequestURI(),
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Logs data modification event.
     *
     * @param userId     the user ID
     * @param resource   the resource being modified
     * @param action     the action performed
     * @param resourceId the resource ID
     * @param before     the state before modification
     * @param after      the state after modification
     * @param request    the HTTP request
     */
    public void logDataModification(String userId, String resource, String action, String resourceId,
                                   Object before, Object after, HttpServletRequest request) {
        logAuditEvent("DATA_MODIFICATION", Map.of(
                "userId", userId,
                "resource", resource,
                "action", action,
                "resourceId", resourceId,
                "before", before != null ? before.toString() : null,
                "after", after != null ? after.toString() : null,
                "ipAddress", getClientIpAddress(request),
                "method", request.getMethod(),
                "uri", request.getRequestURI(),
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Logs system event.
     *
     * @param eventType the event type
     * @param details   the event details
     */
    public void logSystemEvent(String eventType, Map<String, Object> details) {
        logAuditEvent("SYSTEM_EVENT", Map.of(
                "eventType", eventType,
                "details", details,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Logs security event.
     *
     * @param eventType the security event type
     * @param userId    the user ID (if available)
     * @param details   the event details
     * @param request   the HTTP request
     */
    public void logSecurityEvent(String eventType, String userId, Map<String, Object> details, HttpServletRequest request) {
        Map<String, Object> auditData = new ConcurrentHashMap<>();
        auditData.put("eventType", eventType);
        if (userId != null) {
            auditData.put("userId", userId);
        }
        auditData.put("details", details);
        auditData.put("ipAddress", getClientIpAddress(request));
        auditData.put("userAgent", request.getHeader("User-Agent"));
        auditData.put("timestamp", LocalDateTime.now());

        logAuditEvent("SECURITY_EVENT", auditData);
    }

    /**
     * Logs business event.
     *
     * @param userId     the user ID
     * @param businessProcess the business process name
     * @param action     the business action
     * @param details    the event details
     * @param request    the HTTP request
     */
    public void logBusinessEvent(String userId, String businessProcess, String action,
                                 Map<String, Object> details, HttpServletRequest request) {
        logAuditEvent("BUSINESS_EVENT", Map.of(
                "userId", userId,
                "businessProcess", businessProcess,
                "action", action,
                "details", details,
                "ipAddress", getClientIpAddress(request),
                "method", request.getMethod(),
                "uri", request.getRequestURI(),
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Logs error event.
     *
     * @param userId   the user ID (if available)
     * @param error    the error
     * @param request  the HTTP request (if available)
     */
    public void logError(String userId, Exception error, HttpServletRequest request) {
        Map<String, Object> errorDetails = new ConcurrentHashMap<>();
        errorDetails.put("errorType", error.getClass().getSimpleName());
        errorDetails.put("errorMessage", error.getMessage());
        errorDetails.put("stackTrace", getStackTraceAsString(error));

        if (request != null) {
            logAuditEvent("ERROR_EVENT", Map.of(
                    "userId", userId,
                    "error", errorDetails,
                    "ipAddress", getClientIpAddress(request),
                    "method", request.getMethod(),
                    "uri", request.getRequestURI(),
                    "timestamp", LocalDateTime.now()
            ));
        } else {
            logAuditEvent("ERROR_EVENT", Map.of(
                    "userId", userId,
                    "error", errorDetails,
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * Logs audit event asynchronously.
     *
     * @param eventType the event type
     * @param data      the audit data
     */
    private void logAuditEvent(String eventType, Map<String, Object> data) {
        auditExecutor.submit(() -> {
            try {
                // Log to application log
                log.info("AUDIT_EVENT [{}]: {}", eventType, data);

                // Here you could also:
                // - Write to a dedicated audit database table
                // - Send to a SIEM system
                // - Write to a log aggregation service
                // - Send to a security monitoring service

            } catch (Exception e) {
                log.error("Failed to log audit event", e);
            }
        });
    }

    /**
     * Gets client IP address from request.
     *
     * @param request the HTTP request
     * @return client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Converts stack trace to string.
     *
     * @param exception the exception
     * @return stack trace as string
     */
    private String getStackTraceAsString(Exception exception) {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            exception.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return "Failed to get stack trace: " + e.getMessage();
        }
    }

    /**
     * Shuts down the audit executor service.
     */
    public void shutdown() {
        auditExecutor.shutdown();
        try {
            if (!auditExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                auditExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            auditExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}