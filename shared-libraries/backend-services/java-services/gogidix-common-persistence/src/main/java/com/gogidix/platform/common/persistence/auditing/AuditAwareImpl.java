package com.gogidix.platform.common.persistence.auditing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementation of AuditorAware for JPA auditing
 */
@Slf4j
@Component
public class AuditAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("No authentication found, using system as auditor");
                return Optional.of("system");
            }

            // Get the principal name or use system
            String auditor = authentication.getName();
            if (auditor == null || auditor.trim().isEmpty()) {
                log.debug("Authentication principal name is empty, using system as auditor");
                auditor = "system";
            }

            log.debug("Current auditor: {}", auditor);
            return Optional.of(auditor);

        } catch (Exception e) {
            log.error("Error getting current auditor", e);
            return Optional.of("system");
        }
    }
}