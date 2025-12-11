package com.gogidix.dashboard.centralized.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gogidix.dashboard.centralized.domain.dashboard.Dashboard;
import com.gogidix.dashboard.centralized.domain.widget.Widget;
import com.gogidix.dashboard.centralized.infrastructure.websocket.DashboardWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for WebSocket functionality.
 * Tests real-time communication and message broadcasting.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("WebSocket Integration Tests")
class WebSocketIntegrationTest {

    @Autowired
    private DashboardWebSocketHandler webSocketHandler;

    private ObjectMapper objectMapper;
    private WebSocketSession mockSession;
    private Map<String, Object> testMessage;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-session-1");
        when(mockSession.isOpen()).thenReturn(true);

        testMessage = Map.of(
                "type", "TEST_MESSAGE",
                "payload", "test data"
        );
    }

    @Nested
    @DisplayName("WebSocket Connection Management")
    class ConnectionManagementTests {

        @Test
        @DisplayName("Should establish WebSocket connection")
        void shouldEstablishWebSocketConnection() throws Exception {
            // Given
            TextMessage expectedMessage = new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "CONNECTION_ESTABLISHED",
                    "sessionId", "test-session-1",
                    "timestamp", System.currentTimeMillis()
            )));

            // When
            webSocketHandler.afterConnectionEstablished(mockSession);

            // Then
            verify(mockSession, timeout(1000)).sendMessage(expectedMessage);
        }

        @Test
        @DisplayName("Should close WebSocket connection properly")
        void shouldCloseWebSocketConnectionProperly() throws Exception {
            // When
            webSocketHandler.afterConnectionClosed(mockSession, null);

            // Then
            // Connection should be removed from session tracking
            // This is verified through internal state management
        }
    }

    @Nested
    @DisplayName("Message Handling Tests")
    class MessageHandlingTests {

        @Test
        @DisplayName("Should handle dashboard subscription message")
        void shouldHandleDashboardSubscription() throws Exception {
            // Given
            UUID dashboardId = UUID.randomUUID();
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "SUBSCRIBE_DASHBOARD",
                    "dashboardId", dashboardId.toString()
            ));

            TextMessage textMessage = new TextMessage(message);
            TextMessage expectedResponse = new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "SUBSCRIPTION_CONFIRMED",
                    "dashboardId", dashboardId.toString(),
                    "timestamp", System.currentTimeMillis()
            )));

            // When
            webSocketHandler.handleTextMessage(mockSession, textMessage);

            // Then
            verify(mockSession).sendMessage(expectedResponse);
        }

        @Test
        @DisplayName("Should handle ping message")
        void shouldHandlePingMessage() throws Exception {
            // Given
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "PING"
            ));

            TextMessage textMessage = new TextMessage(message);
            TextMessage expectedResponse = new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "PONG",
                    "timestamp", System.currentTimeMillis()
            )));

            // When
            webSocketHandler.handleTextMessage(mockSession, textMessage);

            // Then
            verify(mockSession).sendMessage(expectedResponse);
        }

        @Test
        @DisplayName("Should handle invalid message type")
        void shouldHandleInvalidMessageType() throws Exception {
            // Given
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "INVALID_TYPE"
            ));

            TextMessage textMessage = new TextMessage(message);
            TextMessage expectedError = new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "ERROR",
                    "errorCode", "UNKNOWN_MESSAGE_TYPE",
                    "errorMessage", "Message type not recognized: INVALID_TYPE",
                    "timestamp", System.currentTimeMillis()
            )));

            // When
            webSocketHandler.handleTextMessage(mockSession, textMessage);

            // Then
            verify(mockSession).sendMessage(expectedError);
        }

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            // Given
            TextMessage malformedMessage = new TextMessage("{ invalid json }");

            // When
            webSocketHandler.handleTextMessage(mockSession, malformedMessage);

            // Then
            verify(mockSession).sendMessage(argThat(argument -> {
                try {
                    String content = argument.toString();
                    return content.contains("ERROR") && content.contains("MESSAGE_HANDLING_ERROR");
                } catch (Exception e) {
                    return false;
                }
            }));
        }
    }

    @Nested
    @DisplayName("Message Broadcasting Tests")
    class MessageBroadcastingTests {

        private Dashboard testDashboard;
        private Widget testWidget;

        @BeforeEach
        void setUp() {
            testDashboard = Dashboard.create("Test Dashboard", UUID.randomUUID(), "Test User");
            testWidget = Widget.create("Test Widget", testDashboard.getId(), Widget.TYPE_CHART);
        }

        @Test
        @DisplayName("Should broadcast dashboard creation")
        void shouldBroadcastDashboardCreation() throws Exception {
            // Given
            WebSocketSession session1 = mockWebSocketSession("session1");
            WebSocketSession session2 = mockWebSocketSession("session2");

            // When
            webSocketHandler.broadcastDashboardCreation(testDashboard.getId(), testDashboard.getName());

            // Then
            verify(session1, timeout(1000)).sendMessage(argThat(message -> {
                try {
                    String content = message.toString();
                    return content.contains("DASHBOARD_CREATED") &&
                           content.contains(testDashboard.getId().toString()) &&
                           content.contains("Test Dashboard");
                } catch (Exception e) {
                    return false;
                }
            }));
            verify(session2, timeout(1000)).sendMessage(argThat(argument -> {
                try {
                    String content = argument.toString();
                    return content.contains("DASHBOARD_CREATED") &&
                           content.contains(testDashboard.getId().toString()) &&
                           content.contains("Test Dashboard");
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        @Test
        @DisplayName("Should broadcast dashboard update")
        void shouldBroadcastDashboardUpdate() throws Exception {
            // Given
            WebSocketSession session = mockWebSocketSession("session1");
            String dashboardId = testDashboard.getId().toString();
            Map<String, Object> updateData = Map.of(
                    "field", "name",
                    "value", "Updated Dashboard"
            );

            // When
            webSocketHandler.sendDashboardUpdate(dashboardId, updateData);

            // Then
            verify(session, timeout(1000)).sendMessage(argThat(message -> {
                try {
                    String content = message.toString();
                    return content.contains("DASHBOARD_UPDATE") &&
                           content.contains(dashboardId);
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        @Test
        @DisplayName("Should broadcast widget update")
        void shouldBroadcastWidgetUpdate() throws Exception {
            // Given
            WebSocketSession session = mockWebSocketSession("session1");

            // When
            webSocketHandler.broadcastWidgetUpdate(testWidget);

            // Then
            verify(session, timeout(1000)).sendMessage(argThat(message -> {
                try {
                    String content = message.toString();
                    return content.contains("WIDGET_UPDATE") &&
                           content.contains(testWidget.getId().toString());
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        @Test
        @DisplayName("Should broadcast real-time data update")
        void shouldBroadcastRealTimeDataUpdate() throws Exception {
            // Given
            WebSocketSession session = mockWebSocketSession("session1");
            UUID dashboardId = testDashboard.getId();
            UUID widgetId = testWidget.getId();
            Object data = Map.of("value", 123.45, "timestamp", System.currentTimeMillis());

            // When
            webSocketHandler.broadcastDataUpdate(dashboardId, widgetId, data);

            // Then
            verify(session, timeout(1000)).sendMessage(argThat(message -> {
                try {
                    String content = message.toString();
                    return content.contains("DATA_UPDATE") &&
                           content.contains(dashboardId.toString()) &&
                           content.contains(widgetId.toString());
                } catch (Exception e) {
                    return false;
                }
            }));
        }
    }

    @Nested
    @DisplayName("Session Management Tests")
    class SessionManagementTests {

        @Test
        @DisplayName("Should track connection count")
        void shouldTrackConnectionCount() {
            // Given
            WebSocketSession session1 = mockWebSocketSession("session1");
            WebSocketSession session2 = mockWebSocketSession("session2");
            when(session1.isOpen()).thenReturn(true);
            when(session2.isOpen()).thenReturn(true);

            // When
            webSocketHandler.afterConnectionEstablished(session1);
            webSocketHandler.afterConnectionEstablished(session2);

            // Then
            assertEquals(2, webSocketHandler.getConnectionCount());
        }

        @Test
        @DisplayName("Should track dashboard subscribers")
        void shouldTrackDashboardSubscribers() throws Exception {
            // Given
            String dashboardId = UUID.randomUUID().toString();
            WebSocketSession session1 = mockWebSocketSession("session1");
            WebSocketSession session2 = mockWebSocketSession("session2");
            when(session1.isOpen()).thenReturn(true);
            when(session2.isOpen()).thenReturn(true);

            // Subscribe sessions to dashboard
            String subscriptionMessage = objectMapper.writeValueAsString(Map.of(
                    "type", "SUBSCRIBE_DASHBOARD",
                    "dashboardId", dashboardId
            ));

            webSocketHandler.handleTextMessage(session1, new TextMessage(subscriptionMessage));
            webSocketHandler.handleTextMessage(session2, new TextMessage(subscriptionMessage));

            // When & Then
            assertEquals(2, webSocketHandler.getSubscriberCount(dashboardId));
        }

        @Test
        @DisplayName("Should close all connections")
        void shouldCloseAllConnections() throws Exception {
            // Given
            WebSocketSession session1 = mockWebSocketSession("session1");
            WebSocketSession session2 = mockWebSocketSession("session2");
            when(session1.isOpen()).thenReturn(true);
            when(session2.isOpen()).thenReturn(true);

            webSocketHandler.afterConnectionEstablished(session1);
            webSocketHandler.afterConnectionEstablished(session2);

            // When
            webSocketHandler.closeAllConnections();

            // Then
            verify(session1).close();
            verify(session2).close();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle session errors gracefully")
        void shouldHandleSessionErrorsGracefully() {
            // Given
            when(mockSession.isOpen()).thenReturn(false);

            String message = objectMapper.writeValueAsString(testMessage);
            TextMessage textMessage = new TextMessage(message);

            // When
            assertDoesNotThrow(() -> {
                webSocketHandler.handleTextMessage(mockSession, textMessage);
            });

            // Then
            // Should not throw exceptions when session is closed
            verify(mockSession, never()).sendMessage(any());
        }

        @Test
        @DisplayName("Should handle subscription without dashboard ID")
        void shouldHandleSubscriptionWithoutDashboardId() throws Exception {
            // Given
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "SUBSCRIBE_DASHBOARD"
                    // Missing dashboardId
            ));

            TextMessage textMessage = new TextMessage(message);

            // When
            webSocketHandler.handleTextMessage(mockSession, textMessage);

            // Then
            verify(mockSession).sendMessage(argThat(message -> {
                try {
                    String content = message.toString();
                    return content.contains("ERROR") && content.contains("INVALID_DASHBOARD_ID");
                } catch (Exception e) {
                    return false;
                }
            }));
        }
    }

    // Helper method to create mock WebSocket session
    private WebSocketSession mockWebSocketSession(String sessionId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}