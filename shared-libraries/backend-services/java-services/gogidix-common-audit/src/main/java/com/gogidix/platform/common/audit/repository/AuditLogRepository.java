package com.gogidix.platform.common.audit.repository;

import com.gogidix.platform.common.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Find audit logs by user ID
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    /**
     * Find audit logs by resource type and resource ID
     */
    Page<AuditLog> findByResourceTypeAndResourceIdOrderByTimestampDesc(String resourceType, String resourceId, Pageable pageable);

    /**
     * Find audit logs by event type
     */
    Page<AuditLog> findByEventTypeOrderByTimestampDesc(String eventType, Pageable pageable);

    /**
     * Find audit logs by event category
     */
    Page<AuditLog> findByEventCategoryOrderByTimestampDesc(String eventCategory, Pageable pageable);

    /**
     * Find audit logs by tenant ID
     */
    Page<AuditLog> findByTenantIdOrderByTimestampDesc(String tenantId, Pageable pageable);

    /**
     * Find audit logs by service name
     */
    Page<AuditLog> findByServiceNameOrderByTimestampDesc(String serviceName, Pageable pageable);

    /**
     * Find audit logs by result
     */
    Page<AuditLog> findByResultOrderByTimestampDesc(String result, Pageable pageable);

    /**
     * Find audit logs by severity
     */
    Page<AuditLog> findBySeverityOrderByTimestampDesc(String severity, Pageable pageable);

    /**
     * Find audit logs within a date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByTimestampBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate, Pageable pageable);

    /**
     * Find audit logs by user ID and date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserIdAndTimestampBetween(@Param("userId") String userId, 
                                                  @Param("startDate") Instant startDate, 
                                                  @Param("endDate") Instant endDate, 
                                                  Pageable pageable);

    /**
     * Find audit logs by event type and date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.eventType = :eventType AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByEventTypeAndTimestampBetween(@Param("eventType") String eventType, 
                                                    @Param("startDate") Instant startDate, 
                                                    @Param("endDate") Instant endDate, 
                                                    Pageable pageable);

    /**
     * Find audit logs by multiple criteria
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:resourceType IS NULL OR a.resourceType = :resourceType) AND " +
           "(:result IS NULL OR a.result = :result) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:tenantId IS NULL OR a.tenantId = :tenantId) AND " +
           "a.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByMultipleCriteria(@Param("userId") String userId,
                                           @Param("eventType") String eventType,
                                           @Param("resourceType") String resourceType,
                                           @Param("result") String result,
                                           @Param("severity") String severity,
                                           @Param("tenantId") String tenantId,
                                           @Param("startDate") Instant startDate,
                                           @Param("endDate") Instant endDate,
                                           Pageable pageable);

    /**
     * Count audit logs by event type within date range
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate GROUP BY a.eventType")
    List<Object[]> countByEventTypeInDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Count audit logs by result within date range
     */
    @Query("SELECT a.result, COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate GROUP BY a.result")
    List<Object[]> countByResultInDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Count audit logs by severity within date range
     */
    @Query("SELECT a.severity, COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate GROUP BY a.severity")
    List<Object[]> countBySeverityInDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Find audit logs by request ID
     */
    Optional<AuditLog> findByRequestId(String requestId);

    /**
     * Find audit logs by trace ID
     */
    Page<AuditLog> findByTraceIdOrderByTimestampDesc(String traceId, Pageable pageable);

    /**
     * Find audit logs by session ID
     */
    Page<AuditLog> findBySessionIdOrderByTimestampDesc(String sessionId, Pageable pageable);

    /**
     * Find failed audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.result = 'FAILURE' ORDER BY a.timestamp DESC")
    Page<AuditLog> findFailedAuditLogs(Pageable pageable);

    /**
     * Find critical audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.severity = 'CRITICAL' ORDER BY a.timestamp DESC")
    Page<AuditLog> findCriticalAuditLogs(Pageable pageable);

    /**
     * Get recent audit logs by user
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Get recent audit logs by resource
     */
    @Query("SELECT a FROM AuditLog a WHERE a.resourceType = :resourceType AND a.resourceId = :resourceId ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentByResource(@Param("resourceType") String resourceType, 
                                       @Param("resourceId") String resourceId, 
                                       Pageable pageable);
}
