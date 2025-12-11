package com.gogidix.ai.anomaly.domain.UserManagement;

import com.gogidix.ai.anomaly.domain.UserManagement.shared.DomainEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Domain event representing the update of a Usermanagement.
 * This event is fired when an existing Usermanagement is modified.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UpdatedEvent extends DomainEvent {

    private final Id UserManagementId;
    private final String name;
    private final String updatedBy;

    /**
     * Constructs a new UpdatedEvent.
     *
     * @param UserManagementId the ID of the updated Usermanagement
     * @param name        the new name of the Usermanagement
     * @param updatedBy   who updated the Usermanagement
     */
    public UpdatedEvent(Id UserManagementId, String name, String updatedBy) {
        super();
        this.UserManagementId = UserManagementId;
        this.name = name;
        this.updatedBy = updatedBy;
    }

    /**
     * Constructs a new UpdatedEvent with custom timestamp.
     *
     * @param UserManagementId the ID of the updated Usermanagement
     * @param name        the new name of the Usermanagement
     * @param updatedBy   who updated the Usermanagement
     * @param occurredOn  when the event occurred
     */
    public UpdatedEvent(Id UserManagementId, String name, String updatedBy, LocalDateTime occurredOn) {
        super(occurredOn);
        this.UserManagementId = UserManagementId;
        this.name = name;
        this.updatedBy = updatedBy;
    }

    @Override
    public String getEventType() {
        return "Updated";
    }

    @Override
    public String getAggregateId() {
        return UserManagementId.asString();
    }

    @Override
    public String getDescription() {
        return String.format(" '%s' (ID: %s) was updated by %s", name, UserManagementId.asString(), updatedBy);
    }

    public String getRoutingKey() {
        return "UserManagement.updated";
    }

    public String getTopic() {
        return "EnterpriseTestService-UserManagement-events";
    }

    public String getPartitionKey() {
        return UserManagementId.asString();
    }

    @Override
    public int getPriority() {
        return NORMAL_PRIORITY;
    }

    @Override
    public boolean isRetryable() {
        return true;
    }

    @Override
    public int getMaxRetryAttempts() {
        return 3;
    }

    @Override
    public boolean requiresImmediateProcessing() {
        return false;
    }

    @Override
    public int getSchemaVersion() {
        return 1;
    }

    @Override
    public boolean shouldAudit() {
        return true;
    }

    @Override
    public Object getCorrelationData() {
        return java.util.Map.of(
                "eventType", getEventType(),
                "UserManagementId", UserManagementId.asString(),
                "name", name,
                "updatedBy", updatedBy,
                "occurredAt", getOccurredOn().toString()
        );
    }
}