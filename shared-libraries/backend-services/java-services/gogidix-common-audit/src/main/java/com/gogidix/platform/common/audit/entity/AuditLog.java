package com.gogidix.platform.common.audit.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log entity for tracking system events
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "userId"),
    @Index(name = "idx_audit_event_type", columnList = "eventType"),
    @Index(name = "idx_audit_resource_type", columnList = "resourceType"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_tenant_id", columnList = "tenantId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "event_category", length = 50)
    private String eventCategory;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "result", length = 20)
    private String result; // SUCCESS, FAILURE, PARTIAL

    @Column(name = "result_message", length = 1000)
    private String resultMessage;

    @Column(name = "source_ip", length = 45) // IPv6 compatible
    private String sourceIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_id", length = 255)
    private String requestId;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "operation_name", length = 100)
    private String operationName;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(name = "trace_id", length = 255)
    private String traceId;

    @Column(name = "span_id", length = 255)
    private String spanId;

    @Column(name = "severity", length = 20)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @PrePersist
    private void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (result == null) {
            result = "SUCCESS";
        }
        if (severity == null) {
            severity = "LOW";
        }
    }
}
