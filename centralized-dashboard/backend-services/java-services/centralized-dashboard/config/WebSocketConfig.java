package com.gogidix.dashboard.centralized.config;

import com.gogidix.dashboard.centralized.infrastructure.websocket.DashboardWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time dashboard updates.
 * Enables STOMP messaging for dashboard data streaming.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final DashboardProperties dashboardProperties;

    /**
     * Configure the message broker for real-time communication.
     *
     * @param config the message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for real-time updates
        config.enableSimpleBroker("/topic/dashboard", "/topic/metrics", "/topic/alerts");

        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");

        // Enable user destination prefix for private messages
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints for WebSocket connections.
     *
     * @param registry the STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/dashboard")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addEndpoint("/ws/dashboard")
                .setAllowedOriginPatterns("*");
    }
}