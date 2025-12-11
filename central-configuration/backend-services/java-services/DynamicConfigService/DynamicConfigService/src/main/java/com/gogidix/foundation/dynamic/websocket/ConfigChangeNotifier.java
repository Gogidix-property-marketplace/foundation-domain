package com.gogidix.foundation.dynamic.websocket;

import com.gogidix.foundation.dynamic.dto.ConfigChangeNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConfigChangeNotifier {

    private static final Logger logger = LoggerFactory.getLogger(ConfigChangeNotifier.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyConfigChange(ConfigChangeNotification notification) {
        try {
            logger.info("Broadcasting configuration change: key={}, type={}",
                       notification.getConfigKey(), notification.getChangeType());

            // Broadcast to all subscribers
            messagingTemplate.convertAndSend("/topic/config-changes", notification);

            // Send targeted notifications for specific scopes
            if (notification.getApplicationName() != null) {
                messagingTemplate.convertAndSend(
                    "/topic/config-changes/" + notification.getApplicationName(), notification);
            }

            if (notification.getServiceName() != null) {
                messagingTemplate.convertAndSend(
                    "/topic/config-changes/" + notification.getApplicationName() + "/" + notification.getServiceName(),
                    notification);
            }

            logger.debug("Configuration change notification sent successfully");
        } catch (Exception e) {
            logger.error("Error sending configuration change notification", e);
        }
    }

    public void notifyGlobalChange(String message) {
        try {
            logger.info("Broadcasting global configuration message: {}", message);
            messagingTemplate.convertAndSend("/topic/config-changes/global", message);
        } catch (Exception e) {
            logger.error("Error sending global configuration message", e);
        }
    }
}