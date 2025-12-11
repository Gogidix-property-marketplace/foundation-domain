package com.gogidix.platform.common.messaging.rabbitmq;

import com.gogidix.platform.common.messaging.event.DomainEvent;
import com.gogidix.platform.common.dto.ApiResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing domain events to RabbitMQ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${gogidix.rabbitmq.exchange:gogidix-events}")
    private String exchangeName;

    /**
     * Publish a domain event to RabbitMQ
     */
    @Retry(name = "rabbitmqPublisher", fallbackMethod = "publishEventFallback")
    public CompletableFuture<ApiResponse<Void>> publishEvent(DomainEvent event) {
        log.debug("Publishing event {} to exchange {}", event.getEventType(), exchangeName);
        
        CompletableFuture<ApiResponse<Void>> future = new CompletableFuture<>();
        
        try {
            String routingKey = getRoutingKey(event);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            
            log.info("Successfully published event {} to exchange {} with routing key {}",
                    event.getEventId(), exchangeName, routingKey);
            
            future.complete(ApiResponse.success(null, "Event published successfully"));
            
        } catch (Exception ex) {
            log.error("Error publishing event {} to exchange {}", event.getEventId(), exchangeName, ex);
            future.completeExceptionally(new RuntimeException("Error publishing event", ex));
        }
        
        return future;
    }

    /**
     * Publish a domain event with custom routing key
     */
    @Retry(name = "rabbitmqPublisher", fallbackMethod = "publishEventFallback")
    public CompletableFuture<ApiResponse<Void>> publishEvent(DomainEvent event, String routingKey) {
        log.debug("Publishing event {} to exchange {} with routing key {}", 
                 event.getEventType(), exchangeName, routingKey);
        
        CompletableFuture<ApiResponse<Void>> future = new CompletableFuture<>();
        
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            
            log.info("Successfully published event {} to exchange {} with routing key {}",
                    event.getEventId(), exchangeName, routingKey);
            
            future.complete(ApiResponse.success(null, "Event published successfully"));
            
        } catch (Exception ex) {
            log.error("Error publishing event {} to exchange {} with routing key {}", 
                     event.getEventId(), exchangeName, routingKey, ex);
            future.completeExceptionally(new RuntimeException("Error publishing event", ex));
        }
        
        return future;
    }

    /**
     * Publish an event with delay
     */
    public CompletableFuture<ApiResponse<Void>> publishEventWithDelay(DomainEvent event, long delayMillis) {
        log.debug("Scheduling event {} for publication in {} ms", event.getEventId(), delayMillis);
        
        CompletableFuture<ApiResponse<Void>> future = new CompletableFuture<>();
        
        CompletableFuture.delayedExecutor(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> {
                    try {
                        publishEvent(event).thenAccept(future::complete)
                                        .exceptionally(throwable -> {
                                            future.completeExceptionally(throwable);
                                            return null;
                                        });
                    } catch (Exception ex) {
                        future.completeExceptionally(ex);
                    }
                });
        
        return future;
    }

    /**
     * Get routing key based on event type
     */
    private String getRoutingKey(DomainEvent event) {
        String eventType = event.getEventType();
        String aggregateType = event.getAggregateType();
        
        if (aggregateType != null && eventType != null) {
            // Generate routing key based on aggregate type and event type
            return aggregateType.toLowerCase() + "." + eventType.toLowerCase().replace("_", ".");
        }
        
        // Fallback routing key
        return "events.default";
    }

    /**
     * Fallback method for publishEvent
     */
    public CompletableFuture<ApiResponse<Void>> publishEventFallback(DomainEvent event, Exception ex) {
        log.warn("Fallback triggered for event {}. Storing in dead letter queue or retrying later.", 
                event.getEventId(), ex);
        
        // Here you could implement:
        // 1. Store in a local queue for retry
        // 2. Persist to database for later processing
        // 3. Send to a monitoring service
        
        CompletableFuture<ApiResponse<Void>> future = new CompletableFuture<>();
        future.complete(ApiResponse.error(500, "EVENT_PUBLISH_FAILED: " + ex.getMessage()));
        return future;
    }

    /**
     * Fallback method for publishEvent with routing key
     */
    public CompletableFuture<ApiResponse<Void>> publishEventFallback(DomainEvent event, String routingKey, Exception ex) {
        return publishEventFallback(event, ex);
    }
}
