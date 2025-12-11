package com.gogidix.platform.common.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Booking-related domain events
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class BookingEvent extends DomainEvent {

    public enum BookingEventType {
        BOOKING_CREATED,
        BOOKING_CONFIRMED,
        BOOKING_CANCELLED,
        BOOKING_COMPLETED,
        BOOKING_PAYMENT_INITIATED,
        BOOKING_PAYMENT_COMPLETED,
        BOOKING_PAYMENT_FAILED,
        BOOKING_RESCHEDULED
    }

    private BookingEventType bookingEventType;
    private String bookingId;
    private String propertyId;
    private String userId;
    private String agentId;
    private Instant startDate;
    private Instant endDate;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
}
