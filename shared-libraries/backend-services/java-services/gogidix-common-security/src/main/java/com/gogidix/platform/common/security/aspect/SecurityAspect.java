package com.gogidix.platform.common.security.aspect;

import com.gogidix.platform.common.security.annotation.RequireRole;
import com.gogidix.platform.common.security.model.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security aspect for handling security annotations
 */
@Slf4j
@Aspect
@Component
public class SecurityAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        if (!(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("Invalid user principal type");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String[] requiredRoles = requireRole.value();
        RequireRole.LogicalOperator operator = requireRole.operator();

        boolean hasRequiredRole;

        if (operator == RequireRole.LogicalOperator.ALL) {
            // User must have ALL specified roles
            hasRequiredRole = true;
            for (String role : requiredRoles) {
                if (!userPrincipal.hasRole(role)) {
                    hasRequiredRole = false;
                    break;
                }
            }
        } else {
            // User must have ANY of the specified roles (default)
            hasRequiredRole = userPrincipal.hasAnyRole(requiredRoles);
        }

        if (!hasRequiredRole) {
            log.warn("User {} attempted to access {} without required role(s): {}",
                    userPrincipal.getUsername(),
                    joinPoint.getSignature().toShortString(),
                    String.join(", ", requiredRoles));

            throw new AccessDeniedException(
                String.format("Access denied. User requires %s role(s): %s",
                    operator == RequireRole.LogicalOperator.ALL ? "ALL" : "ANY",
                    String.join(", ", requiredRoles))
            );
        }

        log.debug("User {} authorized to access {}", userPrincipal.getUsername(), joinPoint.getSignature().toShortString());
    }
}