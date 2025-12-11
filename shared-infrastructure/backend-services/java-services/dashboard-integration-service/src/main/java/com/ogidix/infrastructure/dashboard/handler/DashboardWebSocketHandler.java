package com.ogidix.infrastructure.dashboard.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

/**
 * WebSocket handler for dashboard integration service
 * Handles real-time dashboard updates and notifications
 */
@Component
public class DashboardWebSocketHandler implements WebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Connection established - add session to registry
        System.out.println("Dashboard WebSocket connection established: " + session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // Handle incoming WebSocket messages
        String payload = message.getPayload().toString();
        System.out.println("Received dashboard message: " + payload);

        // Echo back the message for now
        session.sendMessage(new TextMessage("Echo: " + payload));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // Handle transport errors
        System.err.println("Dashboard WebSocket transport error: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        // Connection closed
        System.out.println("Dashboard WebSocket connection closed: " + session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}