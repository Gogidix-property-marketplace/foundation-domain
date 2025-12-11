package com.gogidix.infrastructure.mesh.domain.UserManagement;

import com.gogidix.infrastructure.mesh.domain.UserManagement.shared.DomainEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Domain event representing the deletion of a Usermanagement.
 * This event is fired when a Usermanagement is deleted or archived.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeletedEvent extends DomainEvent {

    private final Id UserManagementId;
    private final String name;
    private final String deletedBy;
    private final String reason;

    /**
     * Constructs a new DeletedEvent.
     *
     * @param UserManagementId the ID of the deleted Usermanagement
     * @param name        the name of the deleted Usermanagement
     * @param deletedBy   who deleted the Usermanagement
     */
    public DeletedEvent(Id UserManagementId, String name, String deletedBy) {
        super();
        this.UserManagementId = UserManagementId;
        this.name = name;
        this.deletedBy = deletedBy;
        this.reason = null;
    }

    /**
     * Constructs a new DeletedEvent with reason.
     *
     * @param UserManagementId the ID of the deleted Usermanagement
     * @param name        the name of the deleted Usermanagement
     * @param deletedBy   who deleted the Usermanagement
     * @param reason      the reason for deletion
     */
    public DeletedEvent(Id UserManagementId, String name, String deletedBy, String reason) {
        super();
        this.UserManagementId = UserManagementId;
        this.name = name;
        this.deletedBy = deletedBy;
        this.reason = reason;
    }

    /**
     * Constructs a new DeletedEvent with custom timestamp.
     *
     * @param UserManagementId the ID of the deleted Usermanagement
     * @param name        the name of the deleted Usermanagement
     * @param deletedBy   who deleted the Usermanagement
     * @param reason      the reason for deletion
     * @param occurredOn  when the event occurred
     */
    public DeletedEvent(Id UserManagementId, String name, String deletedBy, String reason, LocalDateTime occurredOn) {
        super(occurredOn);
        this.UserManagementId = UserManagementId;
        this.name = name;
        this.deletedBy = deletedBy;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "Deleted";
    }

    @Override
    public String getAggregateId() {
        return UserManagementId.asString();
    }

    @Override
    public String getDescription() {
        String desc = String.format(" '%s' (ID: %s) was deleted by %s", name, UserManagementId.asString(), deletedBy);
        if (reason != null && !reason.trim().isEmpty()) {
            desc += String.format(" - Reason: %s", reason);
        }
        return desc;
    }

    public String getRoutingKey() {
        return "UserManagement.deleted";
    }

    public String getTopic() {
        return "EnterpriseTestService-UserManagement-events";
    }

    public String getPartitionKey() {
        return UserManagementId.asString();
    }

    @Override
    public int getPriority() {
        return HIGH_PRIORITY;
    }

    @Override
    public boolean isRetryable() {
        return false;
    }

    @Override
    public int getMaxRetryAttempts() {
        return 1;
    }

    @Override
    public boolean requiresImmediateProcessing() {
        return true;
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
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("eventType", getEventType());
        data.put("UserManagementId", UserManagementId.asString());
        data.put("name", name);
        data.put("deletedBy", deletedBy);
        data.put("occurredAt", getOccurredOn().toString());
        if (reason != null) {
            data.put("reason", reason);
        }
        return data;
    }
}