package com.gogidix.infrastructure.monitoring.infrastructure.external.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * External payment service client.
 * Handles communication with external payment providers.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
public class PaymentClient {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;
    private final String apiKey;

    public PaymentClient(RestTemplate restTemplate,
                        @Value("${app.external.payment.url}") String paymentServiceUrl,
                        @Value("${app.external.payment.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
        this.apiKey = apiKey;
    }

    /**
     * Processes a payment transaction.
     *
     * @param request the payment request
     * @return payment response
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for amount: {}", request.getAmount());

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                paymentServiceUrl + "/api/payments",
                HttpMethod.POST,
                entity,
                PaymentResponse.class
            );

            log.info("Payment processed successfully: {}", response.getBody());
            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to process payment", e);
            throw new PaymentException("Payment processing failed", e);
        }
    }

    /**
     * Refunds a payment transaction.
     *
     * @param transactionId the transaction ID to refund
     * @param amount the refund amount
     * @return refund response
     */
    public RefundResponse refundPayment(UUID transactionId, Double amount) {
        log.info("Refunding payment: {} for amount: {}", transactionId, amount);

        try {
            Map<String, Object> refundRequest = Map.of(
                "transactionId", transactionId.toString(),
                "amount", amount,
                "reason", "Customer refund"
            );

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(refundRequest, headers);

            ResponseEntity<RefundResponse> response = restTemplate.exchange(
                paymentServiceUrl + "/api/refunds",
                HttpMethod.POST,
                entity,
                RefundResponse.class
            );

            log.info("Refund processed successfully: {}", response.getBody());
            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to process refund", e);
            throw new PaymentException("Refund processing failed", e);
        }
    }

    /**
     * Gets payment status.
     *
     * @param transactionId the transaction ID
     * @return payment status
     */
    public PaymentStatus getPaymentStatus(UUID transactionId) {
        log.debug("Getting payment status for: {}", transactionId);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<PaymentStatus> response = restTemplate.exchange(
                paymentServiceUrl + "/api/payments/" + transactionId + "/status",
                HttpMethod.GET,
                entity,
                PaymentStatus.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to get payment status", e);
            throw new PaymentException("Failed to get payment status", e);
        }
    }

    /**
     * Creates HTTP headers with authentication.
     *
     * @return HTTP headers
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        return headers;
    }

    /**
     * Payment request DTO.
     */
    public static class PaymentRequest {
        private UUID orderId;
        private Double amount;
        private String currency;
        private String paymentMethod;
        private Map<String, String> metadata;

        // Getters and setters
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }

    /**
     * Payment response DTO.
     */
    public static class PaymentResponse {
        private UUID transactionId;
        private String status;
        private String message;
        private Map<String, Object> details;

        // Getters and setters
        public UUID getTransactionId() { return transactionId; }
        public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }

    /**
     * Refund response DTO.
     */
    public static class RefundResponse {
        private UUID refundId;
        private UUID transactionId;
        private String status;
        private Double refundedAmount;

        // Getters and setters
        public UUID getRefundId() { return refundId; }
        public void setRefundId(UUID refundId) { this.refundId = refundId; }
        public UUID getTransactionId() { return transactionId; }
        public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Double getRefundedAmount() { return refundedAmount; }
        public void setRefundedAmount(Double refundedAmount) { this.refundedAmount = refundedAmount; }
    }

    /**
     * Payment status DTO.
     */
    public static class PaymentStatus {
        private String status;
        private String lastUpdated;
        private Map<String, Object> additionalInfo;

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
        public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
        public void setAdditionalInfo(Map<String, Object> additionalInfo) { this.additionalInfo = additionalInfo; }
    }

    /**
     * Payment service exception.
     */
    public static class PaymentException extends RuntimeException {
        public PaymentException(String message) {
            super(message);
        }

        public PaymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}