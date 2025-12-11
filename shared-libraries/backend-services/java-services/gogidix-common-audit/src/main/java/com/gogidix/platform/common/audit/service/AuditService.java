package com.gogidix.platform.common.audit.service;

import com.gogidix.platform.common.audit.entity.AuditLog;
import com.gogidix.platform.common.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing audit logs
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Create an audit log entry
     */
    @Transactional
    public AuditLog createAuditLog(AuditLog auditLog) {
        auditLog = auditLogRepository.save(auditLog);
        log.debug("Created audit log: {}", auditLog.getId());
        return auditLog;
    }

    /**
     * Create an audit log entry asynchronously
     */
    @Async
    @Transactional
    public CompletableFuture<AuditLog> createAuditLogAsync(AuditLog auditLog) {
        return CompletableFuture.completedFuture(createAuditLog(auditLog));
    }

    /**
     * Get audit log by ID
     */
    public AuditLog getAuditLogById(UUID id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit log not found with id: " + id));
    }

    /**
     * Get audit logs by user ID
     */
    public Page<AuditLog> getAuditLogsByUserId(String userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    /**
     * Get audit logs by resource
     */
    public Page<AuditLog> getAuditLogsByResource(String resourceType, String resourceId, Pageable pageable) {
        return auditLogRepository.findByResourceTypeAndResourceIdOrderByTimestampDesc(resourceType, resourceId, pageable);
    }

    /**
     * Get audit logs by event type
     */
    public Page<AuditLog> getAuditLogsByEventType(String eventType, Pageable pageable) {
        return auditLogRepository.findByEventTypeOrderByTimestampDesc(eventType, pageable);
    }

    /**
     * Get audit logs by date range
     */
    public Page<AuditLog> getAuditLogsByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate, pageable);
    }

    /**
     * Get failed audit logs
     */
    public Page<AuditLog> getFailedAuditLogs(Pageable pageable) {
        return auditLogRepository.findFailedAuditLogs(pageable);
    }

    /**
     * Get critical audit logs
     */
    public Page<AuditLog> getCriticalAuditLogs(Pageable pageable) {
        return auditLogRepository.findCriticalAuditLogs(pageable);
    }

    /**
     * Find audit logs by request ID
     */
    public AuditLog findByRequestId(String requestId) {
        return auditLogRepository.findByRequestId(requestId)
                .orElse(null);
    }
}