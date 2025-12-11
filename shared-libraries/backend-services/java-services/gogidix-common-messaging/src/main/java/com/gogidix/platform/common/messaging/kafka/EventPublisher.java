package com.gogidix.platform.common.messaging.kafka;

import com.gogidix.platform.common.messaging.event.DomainEvent;
import com.gogidix.platform.common.dto.ApiResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing domain events to Kafka topics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish a domain event to the default topic
     */
    @Retry(name = "kafkaPublisher", fallbackMethod = "publishEventFallback")
    public CompletableFuture<ApiResponse<Void>> publishEvent(DomainEvent event) {
        return publishEvent("gogidix-events", event);
    }

    /**
     * Publish a domain event to a specific topic
     */
    @Retry(name = "kafkaPublisher", fallbackMethod = "publishEventFallback")
    public CompletableFuture<ApiResponse<Void>> publishEvent(String topic, DomainEvent event) {
        log.debug("Publishing event {} to topic {}", event.getEventType(), topic);
        
        CompletableFuture<ApiResponse<Void>> future = new CompletableFuture<>();
        
        try {
            CompletableFuture<SendResult<String, Object>> kafkaFuture = kafkaTemplate.send(topic, event.getAggregateId(), event);

            kafkaFuture.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published event {} to topic {} with offset {}",
                            event.getEventId(), topic, result.getRecordMetadata().offset());
                    future.complete(ApiResponse.success(null, "Event published successfully"));
                } else {
                    log.error("Failed to publish event {} to topic {}", event.getEventId(), topic, ex);
                    future.completeExceptionally(new KafkaException("Failed to publish event", ex));
                }
            });
            
        } catch (Exception ex) {
            log.error("Error publishing event {} to topic {}", event.getEventId(), topic, ex);
            future.completeExceptionally(new KafkaException("Error publishing event", ex));
        }
        
        return future;
    }

    /**
     * Publish multiple events in a transaction
     */
    @Retry(name = "kafkaPublisher", fallbackMethod = "publishEventsTransactionalFallback")
    public CompletableFuture<ApiResponse<Void>> publishEventsTransactional(DomainEvent... events) {
        return publishEventsTransactional("gogidix-events", events);
    }

    /**
     * Publish multiple events to a specific topic in a transaction
     */
    @Retry(name = "kafkaPublisher", fallbackMethod = "publishEventsTransactionalFallback")
    public CompletableFuture<ApiResponse<Void>> publishEventsTransactional(String topic, DomainEvent... events) {
        log.debug("Publishing {} events transactionally to topic {}", events.length, topic);
        
        CompletableFuture<ApiResponse<Void>> future = new CompletableFuture<>();
        
        try {
            kafkaTemplate.executeInTransaction(operations -> {
                for (DomainEvent event : events) {
                    operations.send(topic, event.getAggregateId(), event);
                    log.debug("Added event {} to transaction", event.getEventId());
                }
                return true;
            });
            
            log.info("Successfully published {} events transactionally to topic {}", events.length, topic);
            future.complete(ApiResponse.success(null, "Events published successfully"));
            
        } catch (Exception ex) {
            log.error("Error publishing events transactionally to topic {}", topic, ex);
            future.completeExceptionally(new KafkaException("Error publishing events transactionally", ex));
        }
        
        return future;
    }

    /**
     * Fallback method for publishEvent
     */
    public CompletableFuture<ApiResponse<Void>> publishEventFallback(DomainEvent event, Exception ex) {
        log.warn("Fallback triggered for event {}. Storing in dead letter queue or retrying later.", event.getEventId(), ex);
        
        // Here you could implement:
        // 1. Store in a dead letter queue
        // 2. Persist to database for later retry
        // 3. Send to a retry topic
        
        CompletableFuture<ApiResponse<Void>> future = new CompletableFuture<>();
        future.complete(ApiResponse.error(500, "EVENT_PUBLISH_FAILED: " + ex.getMessage()));
        return future;
    }

    /**
     * Fallback method for publishEventsTransactional
     */
    public CompletableFuture<ApiResponse<Void>> publishEventsTransactionalFallback(String topic, DomainEvent[] events, Exception ex) {
        log.warn("Fallback triggered for transactional publish of {} events to topic {}. Storing for retry.", events.length, topic, ex);
        
        CompletableFuture<ApiResponse<Void>> future = new CompletableFuture<>();
        future.complete(ApiResponse.error(500, "TRANSACTIONAL_PUBLISH_FAILED: " + ex.getMessage()));
        return future;
    }

    /**
     * Fallback method for publishEventsTransactional with varargs
     */
    public CompletableFuture<ApiResponse<Void>> publishEventsTransactionalFallback(DomainEvent[] events, Exception ex) {
        return publishEventsTransactionalFallback("gogidix-events", events, ex);
    }
}
