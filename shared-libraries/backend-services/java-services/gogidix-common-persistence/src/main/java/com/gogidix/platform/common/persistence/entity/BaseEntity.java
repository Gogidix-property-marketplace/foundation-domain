package com.gogidix.platform.common.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base entity class providing common fields for all entities
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@MappedSuperclass
@EqualsAndHashCode(callSuper = false)
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "version")
    @Version
    private Long version;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "tenant_id")
    private String tenantId;

    /**
     * Soft delete method
     */
    public void softDelete() {
        this.deleted = true;
    }

    /**
     * Restore soft deleted entity
     */
    public void restore() {
        this.deleted = false;
    }

    /**
     * Check if entity is active (not soft deleted)
     */
    public boolean isActive() {
        return !Boolean.TRUE.equals(deleted);
    }

    /**
     * Pre-persist callback to ensure UUID is set
     */
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /**
     * Get created at as LocalDateTime
     */
    public LocalDateTime getCreatedAtLocal() {
        return createdAt != null ? LocalDateTime.ofInstant(createdAt, java.time.ZoneId.systemDefault()) : null;
    }

    /**
     * Get updated at as LocalDateTime
     */
    public LocalDateTime getUpdatedAtLocal() {
        return updatedAt != null ? LocalDateTime.ofInstant(updatedAt, java.time.ZoneId.systemDefault()) : null;
    }

    /**
     * Set created at from LocalDateTime
     */
    public void setCreatedAtLocal(LocalDateTime localDateTime) {
        this.createdAt = localDateTime != null ? localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant() : null;
    }

    /**
     * Set updated at from LocalDateTime
     */
    public void setUpdatedAtLocal(LocalDateTime localDateTime) {
        this.updatedAt = localDateTime != null ? localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant() : null;
    }
}