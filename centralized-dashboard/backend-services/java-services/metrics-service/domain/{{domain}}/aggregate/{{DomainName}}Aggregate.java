package com.gogidix.dashboard.metrics.domain.UserManagement.aggregate;

import com.gogidix.dashboard.metrics.domain.UserManagement.;
import com.gogidix.dashboard.metrics.domain.UserManagement.Id;
import com.gogidix.dashboard.metrics.domain.UserManagement.Status;
import com.gogidix.dashboard.metrics.domain.UserManagement.shared.AggregateRoot;
import com.gogidix.dashboard.metrics.domain.UserManagement.shared.DomainEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for Usermanagement.
 * Encapsulates the Usermanagement entity and its related business logic.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Getter
public class Aggregate extends AggregateRoot<Id> {

    private  UserManagement;
    private List<Object> relatedEntities;

    /**
     * Constructs a new Aggregate.
     *
     * @param UserManagement the Usermanagement entity
     */
    public Aggregate( UserManagement) {
        this.UserManagement = UserManagement;
        this.relatedEntities = new ArrayList<>();
    }

    /**
     * Factory method to create a new Aggregate.
     *
     * @param name        the name of the Usermanagement
     * @param description the description
     * @param createdBy   who created this aggregate
     * @return new Aggregate
     */
    public static Aggregate create(String name, String description, String createdBy) {
         UserManagement = .create(name, description, createdBy);
        Aggregate aggregate = new Aggregate(UserManagement);

        log.info("Created new Usermanagement aggregate with ID: {}", UserManagement.getId());
        return aggregate;
    }

    /**
     * Adds a related entity to this aggregate.
     *
     * @param entity the related entity to add
     */
    public void addRelatedEntity(Object entity) {
        if (entity != null) {
            relatedEntities.add(entity);
            log.debug("Added related entity to Usermanagement aggregate: {}", UserManagement.getId());
        }
    }

    /**
     * Removes a related entity from this aggregate.
     *
     * @param entity the related entity to remove
     */
    public void removeRelatedEntity(Object entity) {
        if (relatedEntities.remove(entity)) {
            log.debug("Removed related entity from Usermanagement aggregate: {}", UserManagement.getId());
        }
    }

    /**
     * Gets the total count of related entities.
     *
     * @return count of related entities
     */
    public int getRelatedEntityCount() {
        return relatedEntities.size();
    }

    /**
     * Checks if this aggregate can be safely deleted.
     *
     * @return true if safe to delete, false otherwise
     */
    public boolean canBeDeleted() {
        return relatedEntities.isEmpty() && UserManagement.getStatus() == Status.INACTIVE;
    }

    /**
     * Validates the aggregate's business rules.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return UserManagement != null &&
               UserManagement.getId() != null &&
               UserManagement.getId().isValid() &&
               UserManagement.getName() != null &&
               UserManagement.getStatus() != null;
    }

    /**
     * Gets the aggregate's version for optimistic locking.
     *
     * @return version number
     */
    public Long getVersion() {
        return UserManagement != null ? UserManagement.getVersion() : 0L;
    }

    @Override
    public Id getId() {
        return UserManagement != null ? UserManagement.getId() : null;
    }

    @Override
    public List<DomainEvent> getDomainEvents() {
        return UserManagement != null ? UserManagement.getEvents() : new ArrayList<>();
    }

    @Override
    public void clearDomainEvents() {
        if (UserManagement != null) {
            UserManagement.clearEvents();
        }
    }
}