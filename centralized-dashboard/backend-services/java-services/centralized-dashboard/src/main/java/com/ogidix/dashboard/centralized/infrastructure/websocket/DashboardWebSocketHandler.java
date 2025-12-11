package com.gogidix.dashboard.centralized.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gogidix.dashboard.centralized.domain.dashboard.Dashboard;
import com.gogidix.dashboard.centralized.domain.widget.Widget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time dashboard updates.
 * Manages WebSocket connections and broadcasts dashboard data updates.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionToDashboardMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        sessions.put(session.getId(), session);

        // Send connection acknowledgment
        sendMessage(session, Map.of(
            "type", "CONNECTION_ESTABLISHED",
            "sessionId", session.getId(),
            "timestamp", System.currentTimeMillis()
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {}, status: {}", session.getId(), status);
        sessions.remove(session.getId());
        sessionToDashboardMap.remove(session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("Received message from session {}: {}", session.getId(), payload);

            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String messageType = (String) messageData.get("type");

            switch (messageType) {
                case "SUBSCRIBE_DASHBOARD":
                    handleDashboardSubscription(session, messageData);
                    break;
                case "UNSUBSCRIBE_DASHBOARD":
                    handleDashboardUnsubscription(session, messageData);
                    break;
                case "PING":
                    handlePing(session);
                    break;
                default:
                    log.warn("Unknown message type: {}", messageType);
                    sendError(session, "UNKNOWN_MESSAGE_TYPE", "Message type not recognized: " + messageType);
            }
        } catch (Exception e) {
            log.error("Error handling message from session {}: {}", session.getId(), e.getMessage());
            sendError(session, "MESSAGE_HANDLING_ERROR", e.getMessage());
        }
    }

    /**
     * Handles dashboard subscription.
     *
     * @param session the WebSocket session
     * @param messageData the subscription data
     */
    private void handleDashboardSubscription(WebSocketSession session, Map<String, Object> messageData) {
        try {
            String dashboardId = (String) messageData.get("dashboardId");
            if (dashboardId != null) {
                sessionToDashboardMap.put(session.getId(), UUID.fromString(dashboardId));
                sendMessage(session, Map.of(
                    "type", "SUBSCRIPTION_CONFIRMED",
                    "dashboardId", dashboardId,
                    "timestamp", System.currentTimeMillis()
                ));
                log.info("Session {} subscribed to dashboard {}", session.getId(), dashboardId);
            } else {
                sendError(session, "INVALID_DASHBOARD_ID", "Dashboard ID is required");
            }
        } catch (Exception e) {
            log.error("Error handling dashboard subscription: {}", e.getMessage());
            sendError(session, "SUBSCRIPTION_ERROR", e.getMessage());
        }
    }

    /**
     * Handles dashboard unsubscription.
     *
     * @param session the WebSocket session
     * @param messageData the unsubscription data
     */
    private void handleDashboardUnsubscription(WebSocketSession session, Map<String, Object> messageData) {
        sessionToDashboardMap.remove(session.getId());
        sendMessage(session, Map.of(
            "type", "UNSUBSCRIPTION_CONFIRMED",
            "timestamp", System.currentTimeMillis()
        ));
        log.info("Session {} unsubscribed from dashboard", session.getId());
    }

    /**
     * Handles ping messages.
     *
     * @param session the WebSocket session
     */
    private void handlePing(WebSocketSession session) {
        sendMessage(session, Map.of(
            "type", "PONG",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Broadcasts dashboard update to all subscribed clients.
     *
     * @param dashboard the updated dashboard
     */
    public void broadcastDashboardUpdate(Dashboard dashboard) {
        Map<String, Object> message = Map.of(
            "type", "DASHBOARD_UPDATE",
            "dashboard", dashboard,
            "timestamp", System.currentTimeMillis()
        );

        broadcastToDashboardSubscribers(dashboard.getId().toString(), message);
    }

    /**
     * Broadcasts widget update to all subscribed clients.
     *
     * @param widget the updated widget
     */
    public void broadcastWidgetUpdate(Widget widget) {
        Map<String, Object> message = Map.of(
            "type", "WIDGET_UPDATE",
            "widget", widget,
            "timestamp", System.currentTimeMillis()
        );

        broadcastToDashboardSubscribers(widget.getDashboardId().toString(), message);
    }

    /**
     * Broadcasts real-time data update.
     *
     * @param dashboardId the dashboard ID
     * @param widgetId the widget ID
     * @param data the updated data
     */
    public void broadcastDataUpdate(UUID dashboardId, UUID widgetId, Object data) {
        Map<String, Object> message = Map.of(
            "type", "DATA_UPDATE",
            "dashboardId", dashboardId.toString(),
            "widgetId", widgetId.toString(),
            "data", data,
            "timestamp", System.currentTimeMillis()
        );

        broadcastToDashboardSubscribers(dashboardId.toString(), message);
    }

    /**
     * Broadcasts dashboard deletion notification.
     *
     * @param dashboardId the deleted dashboard ID
     */
    public void broadcastDashboardDeletion(UUID dashboardId) {
        Map<String, Object> message = Map.of(
            "type", "DASHBOARD_DELETED",
            "dashboardId", dashboardId.toString(),
            "timestamp", System.currentTimeMillis()
        );

        broadcastToDashboardSubscribers(dashboardId.toString(), message);
    }

    /**
     * Broadcasts widget deletion notification.
     *
     * @param widgetId the deleted widget ID
     * @param dashboardId the dashboard ID
     */
    public void broadcastWidgetDeletion(UUID widgetId, UUID dashboardId) {
        Map<String, Object> message = Map.of(
            "type", "WIDGET_DELETED",
            "widgetId", widgetId.toString(),
            "dashboardId", dashboardId.toString(),
            "timestamp", System.currentTimeMillis()
        );

        broadcastToDashboardSubscribers(dashboardId.toString(), message);
    }

    /**
     * Sends a message to a specific session.
     *
     * @param session the WebSocket session
     * @param message the message to send
     */
    private void sendMessage(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            }
        } catch (Exception e) {
            log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * Sends an error message to a session.
     *
     * @param session the WebSocket session
     * @param errorCode the error code
     * @param errorMessage the error message
     */
    private void sendError(WebSocketSession session, String errorCode, String errorMessage) {
        Map<String, Object> error = Map.of(
            "type", "ERROR",
            "errorCode", errorCode,
            "errorMessage", errorMessage,
            "timestamp", System.currentTimeMillis()
        );
        sendMessage(session, error);
    }

    /**
     * Broadcasts a message to all sessions subscribed to a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param message the message to broadcast
     */
    private void broadcastToDashboardSubscribers(String dashboardId, Object message) {
        sessions.entrySet().parallelStream()
                .filter(entry -> entry.getValue().isOpen())
                .filter(entry -> dashboardId.equals(sessionToDashboardMap.get(entry.getKey())))
                .forEach(entry -> {
                    try {
                        String jsonMessage = objectMapper.writeValueAsString(message);
                        entry.getValue().sendMessage(new TextMessage(jsonMessage));
                    } catch (Exception e) {
                        log.error("Error broadcasting to session {}: {}", entry.getKey(), e.getMessage());
                    }
                });
    }

    /**
     * Gets the number of active connections.
     *
     * @return number of active connections
     */
    public int getConnectionCount() {
        return (int) sessions.values().stream().filter(WebSocketSession::isOpen).count();
    }

    /**
     * Gets the number of subscribers for a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @return number of subscribers
     */
    public long getSubscriberCount(String dashboardId) {
        return sessionToDashboardMap.values().stream()
                .filter(dashboardId::equals)
                .count();
    }

    /**
     * Broadcasts dashboard creation notification.
     *
     * @param dashboardId the new dashboard ID
     * @param dashboardName the dashboard name
     */
    public void broadcastDashboardCreation(UUID dashboardId, String dashboardName) {
        Map<String, Object> message = Map.of(
            "type", "DASHBOARD_CREATED",
            "dashboardId", dashboardId.toString(),
            "dashboardName", dashboardName,
            "timestamp", System.currentTimeMillis()
        );

        // Broadcast to all sessions for creation events
        sessions.entrySet().parallelStream()
                .filter(entry -> entry.getValue().isOpen())
                .forEach(entry -> {
                    try {
                        String jsonMessage = objectMapper.writeValueAsString(message);
                        entry.getValue().sendMessage(new TextMessage(jsonMessage));
                    } catch (Exception e) {
                        log.error("Error broadcasting to session {}: {}", entry.getKey(), e.getMessage());
                    }
                });
    }

    /**
     * Sends a dashboard update message.
     *
     * @param dashboardId the dashboard ID
     * @param updateData the update data
     */
    public void sendDashboardUpdate(String dashboardId, Map<String, Object> updateData) {
        Map<String, Object> message = Map.of(
            "type", "DASHBOARD_UPDATE",
            "dashboardId", dashboardId,
            "data", updateData,
            "timestamp", System.currentTimeMillis()
        );

        broadcastToDashboardSubscribers(dashboardId, message);
    }

    /**
     * Closes all connections.
     */
    public void closeAllConnections() {
        sessions.values().parallelStream().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (Exception e) {
                log.error("Error closing session {}: {}", session.getId(), e.getMessage());
            }
        });
        sessions.clear();
        sessionToDashboardMap.clear();
    }
}