package com.gogidix.platform.common.messaging.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for event-driven messaging
 */
@Configuration
public class RabbitMQConfig {

    @Value("${gogidix.rabbitmq.exchange:gogidix-events}")
    private String exchangeName;

    @Value("${gogidix.rabbitmq.dlx:gogidix-dlx}")
    private String deadLetterExchange;

    @Value("${gogidix.rabbitmq.dlq:gogidix-dlq}")
    private String deadLetterQueue;

    @Value("${gogidix.rabbitmq.retry.queue:gogidix-retry}")
    private String retryQueue;

    @Value("${gogidix.rabbitmq.retry.exchange:gogidix-retry}")
    private String retryExchange;

    /**
     * Message converter for JSON serialization
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Rabbit template with message converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        
        // Enable publisher confirms
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("Message confirmed: " + correlationData);
            } else {
                System.err.println("Message not confirmed: " + correlationData + ", cause: " + cause);
            }
        });
        
        // Enable returned messages
        template.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage() + 
                             ", replyCode: " + returned.getReplyCode() + 
                             ", replyText: " + returned.getReplyText() + 
                             ", exchange: " + returned.getExchange() + 
                             ", routingKey: " + returned.getRoutingKey());
        });
        
        template.setMandatory(true);
        
        return template;
    }

    /**
     * Main events exchange
     */
    @Bean
    public TopicExchange eventsExchange() {
        return ExchangeBuilder.topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    /**
     * Dead letter exchange
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(deadLetterExchange)
                .durable(true)
                .build();
    }

    /**
     * Retry exchange
     */
    @Bean
    public TopicExchange retryExchange() {
        return ExchangeBuilder.topicExchange(retryExchange)
                .durable(true)
                .build();
    }

    /**
     * Dead letter queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue)
                .build();
    }

    /**
     * Retry queue with TTL
     */
    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(retryQueue)
                .withArgument("x-message-ttl", 30000) // 30 seconds TTL
                .withArgument("x-dead-letter-exchange", exchangeName)
                .build();
    }

    /**
     * Property events queue
     */
    @Bean
    public Queue propertyEventsQueue() {
        return QueueBuilder.durable("property-events")
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", "property.dlq")
                .build();
    }

    /**
     * User events queue
     */
    @Bean
    public Queue userEventsQueue() {
        return QueueBuilder.durable("user-events")
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", "user.dlq")
                .build();
    }

    /**
     * Booking events queue
     */
    @Bean
    public Queue bookingEventsQueue() {
        return QueueBuilder.durable("booking-events")
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", "booking.dlq")
                .build();
    }

    /**
     * Payment events queue
     */
    @Bean
    public Queue paymentEventsQueue() {
        return QueueBuilder.durable("payment-events")
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", "payment.dlq")
                .build();
    }

    // Bindings for main queues
    @Bean
    public Binding propertyEventsBinding() {
        return BindingBuilder.bind(propertyEventsQueue())
                .to(eventsExchange())
                .with("property.*");
    }

    @Bean
    public Binding userEventsBinding() {
        return BindingBuilder.bind(userEventsQueue())
                .to(eventsExchange())
                .with("user.*");
    }

    @Bean
    public Binding bookingEventsBinding() {
        return BindingBuilder.bind(bookingEventsQueue())
                .to(eventsExchange())
                .with("booking.*");
    }

    @Bean
    public Binding paymentEventsBinding() {
        return BindingBuilder.bind(paymentEventsQueue())
                .to(eventsExchange())
                .with("payment.*");
    }

    // Bindings for dead letter queue
    @Bean
    public Binding deadLetterPropertyBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("property.dlq");
    }

    @Bean
    public Binding deadLetterUserBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("user.dlq");
    }

    @Bean
    public Binding deadLetterBookingBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("booking.dlq");
    }

    @Bean
    public Binding deadLetterPaymentBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("payment.dlq");
    }

    /**
     * Listener container factory with custom configuration
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        
        // Enable manual acknowledgment
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        
        // Default requeue rejected messages to false
        factory.setDefaultRequeueRejected(false);
        
        return factory;
    }
}
