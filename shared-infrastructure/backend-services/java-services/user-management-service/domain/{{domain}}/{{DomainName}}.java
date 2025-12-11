package com.gogidix.infrastructure.usermgmt.domain.UserManagement;

import com.gogidix.infrastructure.usermgmt.domain.UserManagement.shared.Entity;
import com.gogidix.infrastructure.usermgmt.domain.UserManagement.shared.ValueObject;
import com.gogidix.infrastructure.usermgmt.domain.UserManagement.Id;
import com.gogidix.infrastructure.usermgmt.domain.UserManagement.Status;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain entity representing a Usermanagement.
 * This is a core aggregate root in the UserManagement bounded context.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Getter
@NoArgsConstructor
@Entity
@Table(name = "UserManagement", indexes = {
    @Index(name = "idx_UserManagement_status", columnList = "status"),
    @Index(name = "idx_UserManagement_created_at", columnList = "created_at"),
    @Index(name = "idx_UserManagement_updated_at", columnList = "updated_at")
})
public class  implements Entity<Id> {

    @EmbeddedId
    private Id id;

    @NotNull
    @Embedded
    private Name name;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "UserManagement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();

    /**
     * Private constructor for entity creation.
     *
     * @param name        the name of the Usermanagement
     * @param description the description
     * @param createdBy   who created this entity
     */
    private (Name name, String description, String createdBy) {
        this.id = Id.generate();
        this.name = name;
        this.description = description;
        this.status = Status.ACTIVE;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.version = 1L;

        log.debug("Created new Usermanagement with id: {}", id);
    }

    /**
     * Factory method to create a new Usermanagement.
     *
     * @param name        the name of the Usermanagement
     * @param description the description
     * @param createdBy   who is creating this entity
     * @return new  instance
     */
    public static  create(Name name, String description, String createdBy) {
        validate(name, description);

         UserManagement = new (name, description, createdBy);

        // Add domain event
        UserManagement.addEvent(new CreatedEvent(UserManagement.getId(), name.getValue(), createdBy));

        log.info("Created new Usermanagement: {} by {}", name.getValue(), createdBy);
        return UserManagement;
    }

    /**
     * Factory method to create a new Usermanagement with name as string.
     *
     * @param name        the name as string
     * @param description the description
     * @param createdBy   who is creating this entity
     * @return new  instance
     */
    public static  create(String name, String description, String createdBy) {
        return create(Name.of(name), description, createdBy);
    }

    /**
     * Updates the Usermanagement information.
     *
     * @param name        the new name
     * @param description the new description
     * @param updatedBy   who is updating this entity
     */
    public void update(Name name, String description, String updatedBy) {
        validate(name, description);

        Name oldName = this.name;
        String oldDescription = this.description;

        this.name = name;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
        this.version++;

        // Add domain event if significant changes were made
        if (!oldName.equals(name) || !oldDescription.equals(description)) {
            addEvent(new UpdatedEvent(this.id, name.getValue(), updatedBy));
        }

        log.info("Updated Usermanagement: {} by {}", name.getValue(), updatedBy);
    }

    /**
     * Updates the status of the Usermanagement.
     *
     * @param newStatus the new status
     * @param updatedBy who is updating this entity
     */
    public void updateStatus(Status newStatus, String updatedBy) {
        if (this.status == newStatus) {
            log.warn(" {} already has status {}", id, newStatus);
            return;
        }

        Status oldStatus = this.status;
        this.status = newStatus;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
        this.version++;

        addEvent(new StatusChangedEvent(this.id, oldStatus, newStatus, updatedBy));

        log.info("Updated Usermanagement {} status from {} to {} by {}", id, oldStatus, newStatus, updatedBy);
    }

    /**
     * Deactivates the Usermanagement.
     *
     * @param updatedBy who is deactivating this entity
     */
    public void deactivate(String updatedBy) {
        if (this.status == Status.INACTIVE) {
            log.warn(" {} is already inactive", id);
            return;
        }

        updateStatus(Status.INACTIVE, updatedBy);
        log.info("Deactivated Usermanagement: {} by {}", name.getValue(), updatedBy);
    }

    /**
     * Activates the Usermanagement.
     *
     * @param updatedBy who is activating this entity
     */
    public void activate(String updatedBy) {
        if (this.status == Status.ACTIVE) {
            log.warn(" {} is already active", id);
            return;
        }

        updateStatus(Status.ACTIVE, updatedBy);
        log.info("Activated Usermanagement: {} by {}", name.getValue(), updatedBy);
    }

    /**
     * Archives the Usermanagement.
     *
     * @param updatedBy who is archiving this entity
     */
    public void archive(String updatedBy) {
        if (this.status == Status.ARCHIVED) {
            log.warn(" {} is already archived", id);
            return;
        }

        updateStatus(Status.ARCHIVED, updatedBy);
        log.info("Archived Usermanagement: {} by {}", name.getValue(), updatedBy);
    }

    /**
     * Checks if the Usermanagement is active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }

    /**
     * Checks if the Usermanagement is inactive.
     *
     * @return true if inactive, false otherwise
     */
    public boolean isInactive() {
        return Status.INACTIVE.equals(this.status);
    }

    /**
     * Checks if the Usermanagement is archived.
     *
     * @return true if archived, false otherwise
     */
    public boolean isArchived() {
        return Status.ARCHIVED.equals(this.status);
    }

    /**
     * Gets the last update timestamp.
     *
     * @return last update timestamp or creation timestamp if never updated
     */
    public LocalDateTime getLastUpdated() {
        return updatedAt != null ? updatedAt : createdAt;
    }

    /**
     * Adds a domain event to this entity.
     *
     * @param event the domain event to add
     */
    private void addEvent(Event event) {
        this.events.add(event);
    }

    /**
     * Validates the Usermanagement data.
     *
     * @param name        the name to validate
     * @param description the description to validate
     */
    private static void validate(Name name, String description) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (description.length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters");
        }
    }

    /**
     * Value Object for Usermanagement name.
     */
    @EqualsAndHashCode
    @Embeddable
    @Getter
    @NoArgsConstructor(force = true)
    public static class Name implements ValueObject {

        @NotNull
        @NotBlank
        @Size(min = 1, max = 100)
        @Column(name = "name", nullable = false, length = 100)
        private String value;

        private Name(String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }
            if (value.length() > 100) {
                throw new IllegalArgumentException("Name cannot exceed 100 characters");
            }
            this.value = value.trim();
        }

        public static Name of(String value) {
            return new Name(value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
         that = () o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", name=" + name +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Pre-persist lifecycle callback.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.version == null || this.version == 0L) {
            this.version = 1L;
        }
    }

    /**
     * Pre-update lifecycle callback.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }
}