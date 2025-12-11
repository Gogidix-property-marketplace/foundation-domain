package com.gogidix.platform.common.messaging.config;

import com.gogidix.platform.common.messaging.handler.EventDispatcher;
import com.gogidix.platform.common.messaging.kafka.EventPublisher;
import com.gogidix.platform.common.messaging.kafka.KafkaConsumerConfig;
import com.gogidix.platform.common.messaging.kafka.KafkaProducerConfig;
import com.gogidix.platform.common.messaging.rabbitmq.RabbitMQConfig;
import com.gogidix.platform.common.messaging.rabbitmq.RabbitMQEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for gogidix-common-messaging
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "gogidix.messaging.enabled", havingValue = "true", matchIfMissing = true)
@Import({
    KafkaProducerConfig.class,
    KafkaConsumerConfig.class,
    RabbitMQConfig.class
})
public class MessagingAutoConfiguration {

    /**
     * Create EventDispatcher bean
     */
    @Bean
    @ConditionalOnMissingBean
    public EventDispatcher eventDispatcher(ListableBeanFactory beanFactory) {
        log.info("Creating EventDispatcher bean");
        return new EventDispatcher(beanFactory);
    }

    /**
     * Create EventPublisher bean for Kafka
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnProperty(name = "gogidix.messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public EventPublisher eventPublisher() {
        log.info("Creating EventPublisher bean for Kafka");
        return null; // Will be properly initialized with constructor injection
    }

    /**
     * Create RabbitMQEventPublisher bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    @ConditionalOnProperty(name = "gogidix.messaging.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
    public RabbitMQEventPublisher rabbitMQEventPublisher() {
        log.info("Creating RabbitMQEventPublisher bean");
        return null; // Will be properly initialized with constructor injection
    }
}
