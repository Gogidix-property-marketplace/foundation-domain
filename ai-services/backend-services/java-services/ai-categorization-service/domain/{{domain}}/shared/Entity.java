package com.gogidix.ai.categorization.domain.UserManagement.shared;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base interface for all domain entities.
 * Provides common contract for entity identification and behavior.
 *
 * @param <ID> the type of the entity identifier
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public interface Entity<ID> {

    /**
     * Gets the unique identifier of this entity.
     *
     * @return the entity identifier
     */
    ID getId();

    /**
     * Gets the creation timestamp of this entity.
     *
     * @return creation timestamp
     */
    LocalDateTime getCreatedAt();

    /**
     * Gets the last update timestamp of this entity.
     *
     * @return last update timestamp
     */
    LocalDateTime getUpdatedAt();

    /**
     * Gets the version number of this entity for optimistic locking.
     *
     * @return version number
     */
    Long getVersion();

    /**
     * Checks if this entity is the same as another entity.
     * Two entities are considered the same if they have the same ID.
     *
     * @param other the other entity to compare with
     * @return true if entities are the same, false otherwise
     */
    default boolean sameIdentityAs(Entity<ID> other) {
        return other != null && this.getId().equals(other.getId());
    }

    /**
     * Checks if this entity is transient (not yet persisted).
     *
     * @return true if transient, false otherwise
     */
    default boolean isTransient() {
        return getId() == null;
    }

    /**
     * Checks if this entity has been persisted.
     *
     * @return true if persisted, false otherwise
     */
    default boolean isPersisted() {
        return !isTransient();
    }

    /**
     * Default implementation for equals based on entity identity.
     *
     * @param obj the object to compare with
     * @return true if equal, false otherwise
     */
    default boolean entityEquals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Entity<?> other = (Entity<?>) obj;
        return Objects.equals(getId(), other.getId());
    }

    /**
     * Default implementation for hashCode based on entity identity.
     *
     * @return hash code
     */
    default int entityHashCode() {
        return Objects.hash(getId());
    }

    /**
     * Default implementation for toString.
     *
     * @return string representation
     */
    default String entityToString() {
        return getClass().getSimpleName() + "{id=" + getId() + "}";
    }
}