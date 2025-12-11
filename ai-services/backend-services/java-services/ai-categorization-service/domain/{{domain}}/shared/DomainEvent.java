package com.gogidix.ai.categorization.domain.UserManagement.shared;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base class for all domain events.
 * Domain events represent something that happened in the domain that domain experts care about.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Getter
@ToString
public abstract class DomainEvent {

    public static final int LOW_PRIORITY = 1;
    public static final int NORMAL_PRIORITY = 5;
    public static final int HIGH_PRIORITY = 10;
    public static final int CRITICAL_PRIORITY = 15;

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateId;
    private final String eventType;

    /**
     * Default constructor with current timestamp.
     */
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = generateAggregateId();
        this.eventType = getEventType();
    }

    /**
     * Constructor with custom timestamp.
     *
     * @param occurredOn when the event occurred
     */
    protected DomainEvent(LocalDateTime occurredOn) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = occurredOn;
        this.aggregateId = generateAggregateId();
        this.eventType = getEventType();
    }

    /**
     * Gets the event type identifier.
     *
     * @return the event type
     */
    public abstract String getEventType();

    /**
     * Gets the aggregate root ID that this event relates to.
     *
     * @return the aggregate ID
     */
    public abstract String getAggregateId();

    /**
     * Gets a human-readable description of the event.
     *
     * @return event description
     */
    public abstract String getDescription();

    /**
     * Gets the priority of this event.
     *
     * @return event priority
     */
    public int getPriority() {
        return NORMAL_PRIORITY;
    }

    /**
     * Checks if this event can be retried if processing fails.
     *
     * @return true if retryable, false otherwise
     */
    public boolean isRetryable() {
        return true;
    }

    /**
     * Gets the maximum number of retry attempts for this event.
     *
     * @return maximum retry attempts
     */
    public int getMaxRetryAttempts() {
        return 3;
    }

    /**
     * Checks if this event requires immediate processing.
     *
     * @return true if immediate processing required
     */
    public boolean requiresImmediateProcessing() {
        return false;
    }

    /**
     * Gets the version of this event schema.
     *
     * @return schema version
     */
    public int getSchemaVersion() {
        return 1;
    }

    /**
     * Checks if this event should be persisted for auditing.
     *
     * @return true if should be audited
     */
    public boolean shouldAudit() {
        return true;
    }

    /**
     * Gets correlation data for this event.
     *
     * @return correlation data map
     */
    public Object getCorrelationData() {
        return java.util.Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "aggregateId", aggregateId,
                "occurredAt", occurredOn.toString()
        );
    }

    /**
     * Generates aggregate ID if not provided by subclass.
     *
     * @return generated aggregate ID
     */
    protected String generateAggregateId() {
        return getAggregateId();
    }

    /**
     * Checks if this event occurred after another event.
     *
     * @param other the other event to compare with
     * @return true if this event occurred after the other
     */
    public boolean occurredAfter(DomainEvent other) {
        return other != null && this.occurredOn.isAfter(other.occurredOn);
    }

    /**
     * Checks if this event occurred before another event.
     *
     * @param other the other event to compare with
     * @return true if this event occurred before the other
     */
    public boolean occurredBefore(DomainEvent other) {
        return other != null && this.occurredOn.isBefore(other.occurredOn);
    }

    /**
     * Gets the age of this event in milliseconds.
     *
     * @return age in milliseconds
     */
    public long getAgeInMillis() {
        return java.time.Duration.between(occurredOn, LocalDateTime.now()).toMillis();
    }

    /**
     * Gets the age of this event in seconds.
     *
     * @return age in seconds
     */
    public long getAgeInSeconds() {
        return getAgeInMillis() / 1000;
    }

    /**
     * Checks if this event is older than the specified duration.
     *
     * @param duration the duration to check against
     * @return true if event is older
     */
    public boolean isOlderThan(java.time.Duration duration) {
        return java.time.Duration.between(occurredOn, LocalDateTime.now()).compareTo(duration) > 0;
    }
}