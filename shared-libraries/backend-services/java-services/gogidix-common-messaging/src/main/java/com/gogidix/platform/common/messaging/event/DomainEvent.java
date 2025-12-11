package com.gogidix.platform.common.messaging.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all domain events in the system
 */
@Data
@SuperBuilder
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PropertyEvent.class, name = "PROPERTY_EVENT"),
    @JsonSubTypes.Type(value = UserEvent.class, name = "USER_EVENT"),
    @JsonSubTypes.Type(value = BookingEvent.class, name = "BOOKING_EVENT"),
    @JsonSubTypes.Type(value = PaymentEvent.class, name = "PAYMENT_EVENT")
})
public abstract class DomainEvent {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant timestamp;

    private String eventId;

    private String aggregateId;

    private String aggregateType;

    private String eventType;

    private String version;

    private String sourceService;

    private String correlationId;

    private String userId;

    private String tenantId;

    private Map<String, Object> metadata;

    /**
     * Default constructor that generates unique IDs and sets current timestamp
     */
    public DomainEvent() {
        this.timestamp = Instant.now();
        this.eventId = UUID.randomUUID().toString();
        this.correlationId = UUID.randomUUID().toString();
        this.version = "1.0";
    }

    /**
     * Constructor with required fields
     */
    public DomainEvent(String aggregateId, String aggregateType, String sourceService) {
        this();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.sourceService = sourceService;
        this.eventType = this.getClass().getSimpleName();
    }

    /**
     * Constructor with all fields
     */
    public DomainEvent(String eventId, Instant timestamp, String aggregateId, String aggregateType,
                      String eventType, String version, String sourceService, String correlationId,
                      String userId, String tenantId, Map<String, Object> metadata) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.version = version;
        this.sourceService = sourceService;
        this.correlationId = correlationId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.metadata = metadata;
    }
}
