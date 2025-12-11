package com.gogidix.ai.recommendation.infrastructure.persistence.postgresql.entity;

import com.gogidix.ai.recommendation.domain.UserManagement.;
import com.gogidix.ai.recommendation.domain.UserManagement.Id;
import com.gogidix.ai.recommendation.domain.UserManagement.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for persisting Usermanagement in PostgreSQL.
 * Maps to the UserManagement table in the database.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "UserManagement", indexes = {
    @Index(name = "idx_UserManagement_status", columnList = "status"),
    @Index(name = "idx_UserManagement_created_at", columnList = "created_at"),
    @Index(name = "idx_UserManagement_updated_at", columnList = "updated_at"),
    @Index(name = "idx_UserManagement_name", columnList = "name")
})
public class Entity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Converts domain entity to JPA entity.
     *
     * @param UserManagement the domain entity
     * @return JPA entity
     */
    public static Entity fromDomain( UserManagement) {
        if (UserManagement == null) {
            return null;
        }

        return Entity.builder()
                .id(UserManagement.getId().asUuid())
                .name(UserManagement.getName().getValue())
                .description(UserManagement.getDescription())
                .status(EntityStatus.fromDomainStatus(UserManagement.getStatus()))
                .createdBy(UserManagement.getCreatedBy())
                .createdAt(UserManagement.getCreatedAt())
                .updatedBy(UserManagement.getUpdatedBy())
                .updatedAt(UserManagement.getUpdatedAt())
                .version(UserManagement.getVersion())
                .build();
    }

    /**
     * Converts JPA entity to domain entity.
     *
     * @return domain entity
     */
    public  toDomain() {
        return .builder()
                .id(Id.of(id))
                .name(.Name.of(name))
                .description(description)
                .status(status.toDomainStatus())
                .createdBy(createdBy)
                .createdAt(createdAt)
                .updatedBy(updatedBy)
                .updatedAt(updatedAt)
                .version(version)
                .build();
    }

    /**
     * Entity status enum for database storage.
     */
    public enum EntityStatus {
        ACTIVE, INACTIVE, ARCHIVED, PENDING, SUSPENDED, UNDER_REVIEW, PENDING_DELETION, TERMINATED;

        /**
         * Converts from domain status to entity status.
         *
         * @param domainStatus the domain status
         * @return entity status
         */
        public static EntityStatus fromDomainStatus(Status domainStatus) {
            if (domainStatus == null) {
                return ACTIVE;
            }
            return valueOf(domainStatus.name());
        }

        /**
         * Converts to domain status.
         *
         * @return domain status
         */
        public Status toDomainStatus() {
            return Status.valueOf(name());
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (version == null) {
            version = 1L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}