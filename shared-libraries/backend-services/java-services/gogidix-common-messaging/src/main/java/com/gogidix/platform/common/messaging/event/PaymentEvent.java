package com.gogidix.platform.common.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment-related domain events
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PaymentEvent extends DomainEvent {

    public enum PaymentEventType {
        PAYMENT_INITIATED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        PAYMENT_CANCELLED,
        PAYMENT_REFUNDED,
        PAYMENT_PARTIALLY_REFUNDED,
        PAYMENT_DISPUTED
    }

    private PaymentEventType paymentEventType;
    private String paymentId;
    private String bookingId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String transactionId;
    private String gatewayResponse;
    private Instant paymentDate;
}
