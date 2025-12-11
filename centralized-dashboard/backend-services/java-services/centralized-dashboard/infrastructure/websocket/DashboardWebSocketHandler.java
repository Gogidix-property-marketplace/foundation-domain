package com.gogidix.dashboard.centralized.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gogidix.dashboard.centralized.config.DashboardProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time dashboard communication.
 * Handles dashboard updates, metrics streaming, and alert notifications.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final DashboardProperties dashboardProperties;
    private final ObjectMapper objectMapper;

    // Active sessions tracker
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();

    /**
     * Handles dashboard subscription requests.
     *
     * @param message the subscription message
     * @return dashboard data
     */
    @MessageMapping("/dashboard/subscribe")
    @SendToUser("/queue/dashboard")
    public Map<String, Object> handleDashboardSubscription(@Payload Map<String, Object> message) {
        String dashboardId = (String) message.get("dashboardId");
        String userId = (String) message.get("userId");

        log.debug("Dashboard subscription request for dashboard: {}, user: {}", dashboardId, userId);

        // Track active session
        activeSessions.put(userId, dashboardId);

        // Return dashboard configuration and initial data
        Map<String, Object> response = new HashMap<>();
        response.put("dashboardId", dashboardId);
        response.put("subscriptionTime", LocalDateTime.now());
        response.put("refreshInterval", dashboardProperties.getDashboard().getRefreshIntervalSeconds());
        response.put("status", "connected");
        response.put("activeUsers", activeSessions.size());

        return response;
    }

    /**
     * Sends real-time dashboard updates.
     *
     * @param dashboardId the dashboard ID
     * @param data the dashboard data
     */
    @Async
    public void sendDashboardUpdate(String dashboardId, Map<String, Object> data) {
        log.debug("Sending dashboard update for: {}", dashboardId);

        messagingTemplate.convertAndSend("/topic/dashboard/" + dashboardId, Map.of(
            "type", "dashboard_update",
            "dashboardId", dashboardId,
            "timestamp", LocalDateTime.now(),
            "data", data
        ));
    }

    /**
     * Sends real-time metrics updates.
     *
     * @param metrics the metrics data
     */
    @Async
    public void sendMetricsUpdate(Map<String, Object> metrics) {
        messagingTemplate.convertAndSend("/topic/metrics", Map.of(
            "type", "metrics_update",
            "timestamp", LocalDateTime.now(),
            "metrics", metrics
        ));
    }

    /**
     * Sends alert notifications.
     *
     * @param alert the alert data
     */
    @Async
    public void sendAlertNotification(Map<String, Object> alert) {
        String userId = (String) alert.get("userId");

        if (userId != null) {
            // Send to specific user
            messagingTemplate.convertAndSendToUser(userId, "/queue/alerts", Map.of(
                "type", "alert",
                "timestamp", LocalDateTime.now(),
                "alert", alert
            ));
        } else {
            // Send to all subscribers
            messagingTemplate.convertAndSend("/topic/alerts", Map.of(
                "type", "alert",
                "timestamp", LocalDateTime.now(),
                "alert", alert
            ));
        }
    }

    /**
     * Scheduled task to send periodic dashboard updates.
     */
    @Scheduled(fixedRateString = "#{dashboardProperties.dashboard.refreshIntervalSeconds * 1000}")
    public void sendPeriodicUpdates() {
        if (!activeSessions.isEmpty()) {
            Map<String, Object> systemMetrics = generateSystemMetrics();
            sendMetricsUpdate(systemMetrics);

            // Send active users count
            messagingTemplate.convertAndSend("/topic/system", Map.of(
                "type", "system_update",
                "timestamp", LocalDateTime.now(),
                "activeUsers", activeSessions.size(),
                "activeDashboards", activeSessions.values().stream().distinct().count()
            ));
        }
    }

    /**
     * Handles user disconnection.
     *
     * @param userId the user ID
     */
    public void handleUserDisconnection(String userId) {
        activeSessions.remove(userId);
        log.debug("User {} disconnected. Active sessions: {}", userId, activeSessions.size());
    }

    /**
     * Generates system metrics for real-time updates.
     *
     * @return system metrics map
     */
    private Map<String, Object> generateSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Simulate real-time metrics (in production, these would come from monitoring systems)
        metrics.put("cpuUsage", Math.random() * 100);
        metrics.put("memoryUsage", Math.random() * 100);
        metrics.put("activeUsers", activeSessions.size());
        metrics.put("totalRequests", (int) (Math.random() * 10000));
        metrics.put("errorRate", Math.random() * 5);
        metrics.put("responseTime", Math.random() * 500);

        return metrics;
    }

    /**
     * Broadcasts dashboard creation events.
     *
     * @param dashboardId the new dashboard ID
     * @param ownerName the dashboard owner name
     */
    @Async
    public void broadcastDashboardCreation(String dashboardId, String ownerName) {
        messagingTemplate.convertAndSend("/topic/events", Map.of(
            "type", "dashboard_created",
            "dashboardId", dashboardId,
            "ownerName", ownerName,
            "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Broadcasts dashboard updates.
     *
     * @param dashboardId the dashboard ID
     * @param changeType the type of change
     * @param details change details
     */
    @Async
    public void broadcastDashboardUpdate(String dashboardId, String changeType, Map<String, Object> details) {
        messagingTemplate.convertAndSend("/topic/events", Map.of(
            "type", "dashboard_updated",
            "dashboardId", dashboardId,
            "changeType", changeType,
            "details", details,
            "timestamp", LocalDateTime.now()
        ));
    }
}