package com.gogidix.platform.common.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Property-related domain events
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PropertyEvent extends DomainEvent {

    public enum PropertyEventType {
        PROPERTY_CREATED,
        PROPERTY_UPDATED,
        PROPERTY_DELETED,
        PROPERTY_STATUS_CHANGED,
        PROPERTY_LISTED,
        PROPERTY_SOLD,
        PROPERTY_RENTED
    }

    private PropertyEventType propertyEventType;
    private String propertyId;
    private String propertyType;
    private String location;
    private Double price;
    private String status;
    private String ownerId;
    private String agentId;
}
