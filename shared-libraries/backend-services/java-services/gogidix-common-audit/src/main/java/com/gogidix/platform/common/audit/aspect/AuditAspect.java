package com.gogidix.platform.common.audit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gogidix.platform.common.audit.annotation.Audit;
import com.gogidix.platform.common.audit.entity.AuditLog;
import com.gogidix.platform.common.audit.service.AuditService;
import com.gogidix.platform.common.security.model.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aspect for handling audit logging
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audit)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        try {
            // Proceed with method execution
            Object result = joinPoint.proceed();

            // Create successful audit log
            createAuditLog(joinPoint, audit, requestId, "SUCCESS", null, result, startTime);

            return result;
        } catch (Exception ex) {
            // Create failure audit log
            createAuditLog(joinPoint, audit, requestId, "FAILURE", ex.getMessage(), null, startTime);
            throw ex;
        }
    }

    private void createAuditLog(ProceedingJoinPoint joinPoint, Audit audit, String requestId,
                                String result, String errorMessage, Object response, long startTime) {
        try {
            AuditLog.AuditLogBuilder builder = AuditLog.builder()
                    .eventType(audit.eventType().isEmpty() ? joinPoint.getSignature().getName() : audit.eventType())
                    .eventCategory(audit.eventCategory().isEmpty() ? null : audit.eventCategory())
                    .action(audit.action().isEmpty() ? joinPoint.getSignature().getName() : audit.action())
                    .resourceType(audit.resourceType().isEmpty() ? null : audit.resourceType())
                    .result(result)
                    .resultMessage(errorMessage != null ? errorMessage : audit.resultMessage())
                    .severity(audit.severity().name())
                    .requestId(requestId)
                    .serviceName(getServiceName(joinPoint))
                    .operationName(joinPoint.getSignature().getName())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .traceId(getTraceId())
                    .spanId(getSpanId());

            // Set user information
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                builder.userId(userPrincipal.getUserId())
                       .username(userPrincipal.getUsername())
                       .userEmail(userPrincipal.getEmail())
                       .tenantId(userPrincipal.getTenantId());
            }

            // Extract resource ID from method parameters if specified
            if (!audit.resourceIdParam().isEmpty()) {
                String resourceId = extractResourceId(joinPoint, audit.resourceIdParam());
                if (resourceId != null) {
                    builder.resourceId(resourceId);
                }
            }

            // Include request parameters if specified
            if (audit.includeRequestParams()) {
                Object[] args = joinPoint.getArgs();
                Map<String, Object> requestData = new HashMap<>();
                for (int i = 0; i < args.length; i++) {
                    requestData.put("arg" + i, args[i]);
                }
                try {
                    builder.requestData(objectMapper.writeValueAsString(requestData));
                } catch (Exception e) {
                    builder.requestData(requestData.toString());
                }
            }

            // Include response data if specified
            if (audit.includeResponseData()) {
                try {
                    builder.responseData(objectMapper.writeValueAsString(response));
                } catch (Exception e) {
                    builder.responseData(response != null ? response.toString() : null);
                }
            }

            // Add additional information
            if (audit.additionalInfo().length > 0) {
                Map<String, Object> additionalInfo = new HashMap<>();
                for (String info : audit.additionalInfo()) {
                    additionalInfo.put(info, true);
                }
                try {
                    builder.additionalInfo(objectMapper.writeValueAsString(additionalInfo));
                } catch (Exception e) {
                    builder.additionalInfo(additionalInfo.toString());
                }
            }

            // Set HTTP request information if available
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                if (request != null) {
                    builder.sourceIp(getClientIpAddress(request))
                           .userAgent(request.getHeader("User-Agent"))
                           .sessionId(request.getSession().getId());
                }
            }

            // Create and save audit log asynchronously
            auditService.createAuditLogAsync(builder.build());

        } catch (Exception ex) {
            log.error("Error creating audit log", ex);
            // Don't re-throw as it shouldn't affect the business logic
        }
    }

    private String getServiceName(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getName();
        // Extract simple class name
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private String extractResourceId(ProceedingJoinPoint joinPoint, String paramName) {
        try {
            String[] paramNames = getParameterNames(joinPoint);
            Object[] args = joinPoint.getArgs();

            if (paramNames != null && args != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    if (paramNames[i].equals(paramName) && i < args.length) {
                        return args[i] != null ? args[i].toString() : null;
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Error extracting resource ID", ex);
        }
        return null;
    }

    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        // This is a simplified version
        // In a real implementation, you might want to use a library like Paranamer
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = new String[args.length];

        // Generate generic parameter names
        for (int i = 0; i < args.length; i++) {
            paramNames[i] = "param" + i;
        }

        return paramNames;
    }

    private String getTraceId() {
        // Try to get trace ID from MDC or other tracing context
        return null; // Implement based on your tracing solution
    }

    private String getSpanId() {
        // Try to get span ID from MDC or other tracing context
        return null; // Implement based on your tracing solution
    }

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
}