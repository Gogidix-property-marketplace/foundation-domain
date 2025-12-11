package com.gogidix.ai.image.infrastructure.messaging.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for  events.
 * Handles publishing  domain events to Kafka topics.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Producer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "EnterpriseTestService-EnterpriseTestService-usermanagement.enterprisetestservice";

    /**
     * Publishes an  to Kafka.
     *
     * @param event the  to publish
     */
    public void publish(Object event) {
        log.info("Publishing : {}", event);

        try {
            kafkaTemplate.send(TOPIC, event);
            log.debug("Successfully published  to topic: {}", TOPIC);
        } catch (Exception e) {
            log.error("Failed to publish  to topic: {}", TOPIC, e);
            throw new PublishException("Failed to publish ", e);
        }
    }

    /**
     * Publishes an  to Kafka with specific key.
     *
     * @param key the partition key
     * @param event the  to publish
     */
    public void publish(String key, Object event) {
        log.info("Publishing  with key: {}", key);

        try {
            kafkaTemplate.send(TOPIC, key, event);
            log.debug("Successfully published  to topic: {} with key: {}", TOPIC, key);
        } catch (Exception e) {
            log.error("Failed to publish  to topic: {} with key: {}", TOPIC, key, e);
            throw new PublishException("Failed to publish ", e);
        }
    }

    /**
     * Custom exception for  publish failures.
     */
    public static class PublishException extends RuntimeException {
        public PublishException(String message) {
            super(message);
        }

        public PublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}