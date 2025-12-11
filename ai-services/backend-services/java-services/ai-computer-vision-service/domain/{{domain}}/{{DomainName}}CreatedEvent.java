package com.gogidix.ai.vision.domain.UserManagement;

import com.gogidix.ai.vision.domain.UserManagement.shared.DomainEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Domain event representing the creation of a Usermanagement.
 * This event is fired when a new Usermanagement is created in the system.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CreatedEvent extends DomainEvent {

    private final Id UserManagementId;
    private final String name;
    private final String createdBy;

    /**
     * Constructs a new CreatedEvent.
     *
     * @param UserManagementId the ID of the created Usermanagement
     * @param name        the name of the created Usermanagement
     * @param createdBy   who created the Usermanagement
     */
    public CreatedEvent(Id UserManagementId, String name, String createdBy) {
        super();
        this.UserManagementId = UserManagementId;
        this.name = name;
        this.createdBy = createdBy;
    }

    /**
     * Constructs a new CreatedEvent with custom timestamp.
     *
     * @param UserManagementId the ID of the created Usermanagement
     * @param name        the name of the created Usermanagement
     * @param createdBy   who created the Usermanagement
     * @param occurredOn  when the event occurred
     */
    public CreatedEvent(Id UserManagementId, String name, String createdBy, LocalDateTime occurredOn) {
        super(occurredOn);
        this.UserManagementId = UserManagementId;
        this.name = name;
        this.createdBy = createdBy;
    }

    /**
     * Gets the event type identifier.
     *
     * @return the event type
     */
    @Override
    public String getEventType() {
        return "Created";
    }

    /**
     * Gets the aggregate root ID that this event relates to.
     *
     * @return the aggregate ID
     */
    @Override
    public String getAggregateId() {
        return UserManagementId.asString();
    }

    /**
     * Gets a human-readable description of the event.
     *
     * @return event description
     */
    @Override
    public String getDescription() {
        return String.format(" '%s' (ID: %s) was created by %s", name, UserManagementId.asString(), createdBy);
    }

    /**
     * Gets the routing key for this event (for messaging systems).
     *
     * @return routing key
     */
    public String getRoutingKey() {
        return "UserManagement.created";
    }

    /**
     * Gets the topic for this event (for publish-subscribe systems).
     *
     * @return topic name
     */
    public String getTopic() {
        return "EnterpriseTestService-UserManagement-events";
    }

    /**
     * Gets the partition key for this event (for partitioned messaging).
     *
     * @return partition key
     */
    public String getPartitionKey() {
        return UserManagementId.asString();
    }

    /**
     * Gets the priority of this event.
     *
     * @return event priority
     */
    @Override
    public int getPriority() {
        return NORMAL_PRIORITY;
    }

    /**
     * Checks if this event can be retried if processing fails.
     *
     * @return true if retryable, false otherwise
     */
    @Override
    public boolean isRetryable() {
        return true;
    }

    /**
     * Gets the maximum number of retry attempts for this event.
     *
     * @return maximum retry attempts
     */
    @Override
    public int getMaxRetryAttempts() {
        return 3;
    }

    /**
     * Checks if this event requires immediate processing.
     *
     * @return true if immediate processing required
     */
    @Override
    public boolean requiresImmediateProcessing() {
        return true;
    }

    /**
     * Gets the version of this event schema.
     *
     * @return schema version
     */
    @Override
    public int getSchemaVersion() {
        return 1;
    }

    /**
     * Checks if this event should be persisted for auditing.
     *
     * @return true if should be audited
     */
    @Override
    public boolean shouldAudit() {
        return true;
    }

    /**
     * Gets correlation data for this event.
     *
     * @return correlation data map
     */
    @Override
    public Object getCorrelationData() {
        return java.util.Map.of(
                "eventType", getEventType(),
                "UserManagementId", UserManagementId.asString(),
                "name", name,
                "createdBy", createdBy,
                "occurredAt", getOccurredOn().toString()
        );
    }
}