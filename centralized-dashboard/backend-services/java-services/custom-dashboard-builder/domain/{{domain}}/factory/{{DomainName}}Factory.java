package com.gogidix.dashboard.builder.domain.UserManagement.factory;

import com.gogidix.dashboard.builder.domain.UserManagement.;
import com.gogidix.dashboard.builder.domain.UserManagement.Id;
import com.gogidix.dashboard.builder.domain.UserManagement.Status;
import com.gogidix.dashboard.builder.domain.UserManagement.shared.ValueObject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory for creating  entities.
 * Implements the Factory pattern for Usermanagement creation.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
public class Factory {

    /**
     * Creates a new  with minimal information.
     *
     * @param name        the name of the Usermanagement
     * @param description the description
     * @param createdBy   who created this Usermanagement
     * @return new  instance
     */
    public static  create(String name, String description, String createdBy) {
        log.debug("Creating new Usermanagement with name: {}", name);

        validateName(name);
        validateDescription(description);
        validateCreatedBy(createdBy);

         UserManagement = .create(name, description, createdBy);

        log.info("Successfully created Usermanagement with ID: {}", UserManagement.getId());
        return UserManagement;
    }

    /**
     * Creates a new  with default values.
     *
     * @param name the name of the Usermanagement
     * @return new  instance
     */
    public static  createWithDefaults(String name) {
        log.debug("Creating new Usermanagement with defaults for name: {}", name);

        validateName(name);

         UserManagement = .create(
                name,
                "Default description for " + name,
                "system"
        );

        log.info("Successfully created Usermanagement with defaults and ID: {}", UserManagement.getId());
        return UserManagement;
    }

    /**
     * Creates a new  from template.
     *
     * @param template the template Usermanagement
     * @param newName  the new name
     * @param createdBy who created this Usermanagement
     * @return new  instance
     */
    public static  createFromTemplate( template, String newName, String createdBy) {
        log.debug("Creating new Usermanagement from template with new name: {}", newName);

        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }

        validateName(newName);
        validateCreatedBy(createdBy);

         UserManagement = .create(
                newName,
                template.getDescription(),
                createdBy
        );

        log.info("Successfully created Usermanagement from template with ID: {}", UserManagement.getId());
        return UserManagement;
    }

    /**
     * Reconstructs a  from existing data (for persistence layer).
     *
     * @param id          the ID
     * @param name        the name
     * @param description the description
     * @param status      the status
     * @param createdBy   who created this
     * @param createdAt   when it was created
     * @param updatedBy   who last updated this
     * @param updatedAt   when it was last updated
     * @param version     the version number
     * @return reconstructed  instance
     */
    public static  reconstruct(
            Id id,
            String name,
            String description,
            Status status,
            String createdBy,
            LocalDateTime createdAt,
            String updatedBy,
            LocalDateTime updatedAt,
            Long version) {

        log.debug("Reconstructing Usermanagement with ID: {}", id);

        // This would typically be implemented using a private constructor or builder
        // For now, return a basic instance
        return .create(name, description, createdBy);
    }

    /**
     * Validates the name field.
     *
     * @param name the name to validate
     */
    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Name cannot exceed 100 characters");
        }
        if (!name.matches("^[a-zA-Z0-9\\s\\-_]+$")) {
            throw new IllegalArgumentException("Name contains invalid characters");
        }
    }

    /**
     * Validates the description field.
     *
     * @param description the description to validate
     */
    private static void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (description.length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters");
        }
    }

    /**
     * Validates the createdBy field.
     *
     * @param createdBy the createdBy to validate
     */
    private static void validateCreatedBy(String createdBy) {
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by cannot be null or empty");
        }
        if (createdBy.length() > 100) {
            throw new IllegalArgumentException("Created by cannot exceed 100 characters");
        }
    }

    /**
     * Builder for  creation.
     */
    public static class Builder {
        private String name;
        private String description = "";
        private String createdBy;
        private Status status = Status.ACTIVE;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public  build() {
             UserManagement = create(name, description, createdBy);
            if (!status.equals(Status.ACTIVE)) {
                UserManagement.updateStatus(status, createdBy);
            }
            return UserManagement;
        }
    }

    /**
     * Creates a new builder instance.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}