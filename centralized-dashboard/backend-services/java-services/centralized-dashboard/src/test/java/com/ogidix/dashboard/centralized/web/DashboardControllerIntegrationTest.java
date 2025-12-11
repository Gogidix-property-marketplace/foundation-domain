package com.gogidix.dashboard.centralized.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gogidix.dashboard.centralized.application.service.DashboardService;
import com.gogidix.dashboard.centralized.domain.dashboard.Dashboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DashboardController REST API endpoints.
 * Tests all HTTP endpoints with proper request/response handling.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureWebMvc
@DisplayName("Dashboard Controller Integration Tests")
class DashboardControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private DashboardService dashboardService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Dashboard CRUD Operations")
    class DashboardCrudOperations {

        private UUID dashboardId;
        UUID userId;
        Dashboard testDashboard;

        @BeforeEach
        void setUp() {
            dashboardId = UUID.randomUUID();
            userId = UUID.randomUUID();
            testDashboard = Dashboard.create("Test Dashboard", userId, "Test User");
            testDashboard.setId(dashboardId);
        }

        @Test
        @DisplayName("Should create dashboard successfully")
        void shouldCreateDashboardSuccessfully() throws Exception {
            // Given
            Map<String, Object> request = Map.of(
                "name", "New Dashboard",
                "description", "Test description",
                "category", "Analytics",
                "tags", List.of("analytics", "dashboard")
            );

            Dashboard createdDashboard = Dashboard.create("New Dashboard", userId, "Test User");
            when(dashboardService.createDashboard(anyString(), anyString(), any(), anyString(), any()))
                    .thenReturn(createdDashboard);

            // When & Then
            mockMvc.perform(post("/api/dashboards")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Dashboard"))
                    .andExpect(jsonPath("$.ownerId").value(userId.toString()))
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        @DisplayName("Should get dashboard by ID")
        void shouldGetDashboardById() throws Exception {
            // Given
            when(dashboardService.getDashboard(dashboardId)).thenReturn(testDashboard);

            // When & Then
            mockMvc.perform(get("/api/dashboards/{id}", dashboardId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(dashboardId.toString()))
                    .andExpect(jsonPath("$.name").value("Test Dashboard"))
                    .andExpect(jsonPath("$.ownerId").value(userId.toString()));
        }

        @Test
        @DisplayName("Should update dashboard successfully")
        void shouldUpdateDashboardSuccessfully() throws Exception {
            // Given
            Map<String, Object> request = Map.of(
                "name", "Updated Dashboard",
                "description", "Updated description",
                "theme", "dark"
            );

            when(dashboardService.updateDashboard(eq(dashboardId), eq(userId), anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(testDashboard);

            // When & Then
            mockMvc.perform(put("/api/dashboards/{id}", dashboardId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Test Dashboard"));
        }

        @Test
        @DisplayName("Should delete dashboard successfully")
        void shouldDeleteDashboardSuccessfully() throws Exception {
            // Given
            doNothing().when(dashboardService).deleteDashboard(dashboardId, userId);

            // When & Then
            mockMvc.perform(delete("/api/dashboards/{id}", dashboardId)
                    .header("X-User-Id", userId.toString()))
                    .andExpect(status().isNoContent());

            verify(dashboardService).deleteDashboard(dashboardId, userId);
        }

        @Test
        @DisplayName("Should return 404 when dashboard not found")
        void shouldReturn404WhenDashboardNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(dashboardService.getDashboard(nonExistentId))
                    .thenThrow(new RuntimeException("Dashboard not found"));

            // When & Then
            mockMvc.perform(get("/api/dashboards/{id}", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Dashboard Query Operations")
    class DashboardQueryOperations {

        private UUID userId;

        @BeforeEach
        void setUp() {
            userId = UUID.randomUUID();
        }

        @Test
        @DisplayName("Should get user dashboards")
        void shouldGetUserDashboards() throws Exception {
            // Given
            List<Dashboard> userDashboards = List.of(
                    Dashboard.create("Dashboard 1", userId, "User 1"),
                    Dashboard.create("Dashboard 2", userId, "User 1")
            );
            when(dashboardService.getUserDashboards(userId, any()))
                    .thenReturn(userDashboards);

            // When & Then
            mockMvc.perform(get("/api/dashboards/user/{userId}", userId)
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Should get public dashboards")
        void shouldGetPublicDashboards() throws Exception {
            // Given
            List<Dashboard> publicDashboards = List.of(
                    Dashboard.create("Public Dashboard 1", UUID.randomUUID(), "User 1"),
                    Dashboard.create("Public Dashboard 2", UUID.randomUUID(), "User 2")
            );
            publicDashboards.forEach(d -> d.makePublic());
            when(dashboardService.getPublicDashboards(any()))
                    .thenReturn(publicDashboards);

            // When & Then
            mockMvc.perform(get("/api/dashboards/public")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("Should search dashboards")
        void shouldSearchDashboards() throws Exception {
            // Given
            List<Dashboard> searchResults = List.of(
                    Dashboard.create("Analytics Dashboard", userId, "User"),
                    Dashboard.create("Dashboard with Analytics", userId, "User")
            );
            when(dashboardService.searchDashboards("Analytics", any()))
                    .thenReturn(searchResults);

            // When & Then
            mockMvc.perform(get("/api/dashboards/search")
                    .param("q", "Analytics")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("Should get dashboard statistics")
        void shouldGetDashboardStatistics() throws Exception {
            // Given
            when(dashboardService.getDashboardStatistics(userId))
                    .thenReturn(Map.of(
                            "totalDashboards", 5L,
                            "activeDashboards", 4L,
                            "publicDashboards", 2L,
                            "totalViews", 150L
                    ));

            // When & Then
            mockMvc.perform(get("/api/dashboards/statistics/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDashboards").value(5))
                    .andExpect(jsonPath("$.activeDashboards").value(4))
                    .andExpect(jsonPath("$.publicDashboards").value(2))
                    .andExpect(jsonPath("$.totalViews").value(150));
        }
    }

    @Nested
    @DisplayName("Widget Management Operations")
    class WidgetManagementOperations {

        private UUID dashboardId;
        private UUID userId;
        private UUID widgetId;

        @BeforeEach
        void setUp() {
            dashboardId = UUID.randomUUID();
            userId = UUID.randomUUID();
            widgetId = UUID.randomUUID();
        }

        @Test
        @DisplayName("Should add widget to dashboard")
        void shouldAddWidgetToDashboard() throws Exception {
            // Given
            Map<String, Object> request = Map.of(
                    "widgetName", "New Widget",
                    "widgetType", "CHART",
                    "positionX", 100,
                    "positionY", 200,
                    "width", 300,
                    "height", 400
            );

            when(dashboardService.addWidget(eq(dashboardId), eq(userId), anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(Map.of("widgetId", widgetId));

            // When & Then
            mockMvc.perform(post("/api/dashboards/{dashboardId}/widgets", dashboardId)
                    .header("X-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.widgetId").value(widgetId.toString()));
        }

        @Test
        @DisplayName("Should update widget position")
        void shouldUpdateWidgetPosition() throws Exception {
            // Given
            Map<String, Object> request = Map.of(
                    "positionX", 150,
                    "positionY", 250,
                    "width", 350,
                    "height", 450
            );

            when(dashboardService.updateWidgetPosition(eq(widgetId), eq(userId), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(true);

            // When & Then
            mockMvc.perform(put("/api/dashboards/widgets/{widgetId}/position", widgetId)
                    .header("X-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should delete widget from dashboard")
        void shouldDeleteWidgetFromDashboard() throws Exception {
            // Given
            doNothing().when(dashboardService).deleteWidget(widgetId, userId);

            // When & Then
            mockMvc.perform(delete("/api/dashboards/widgets/{widgetId}", widgetId)
                    .header("X-User-Id", userId.toString()))
                    .andExpect(status().isNoContent());

            verify(dashboardService).deleteWidget(widgetId, userId);
        }
    }

    @Nested
    @DisplayName("Dashboard Analytics Operations")
    class DashboardAnalyticsOperations {

        private UUID dashboardId;
        private UUID userId;

        @BeforeEach
        void setUp() {
            dashboardId = UUID.randomUUID();
            userId = UUID.randomUUID();
        }

        @Test
        @DisplayName("Should record dashboard view")
        void shouldRecordDashboardView() throws Exception {
            // Given
            when(dashboardService.recordView(dashboardId, userId)).thenReturn(Map.of(
                    "viewCount", 5L,
                    "lastAccessed", LocalDateTime.now().toString()
            ));

            // When & Then
            mockMvc.perform(post("/api/dashboards/{dashboardId}/view", dashboardId)
                    .header("X-User-Id", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.viewCount").value(5));
        }

        @Test
        @DisplayName("Should get dashboard analytics")
        void shouldGetDashboardAnalytics() throws Exception {
            // Given
            when(dashboardService.getDashboardAnalytics(dashboardId, userId))
                    .thenReturn(Map.of(
                            "totalViews", 25L,
                            "uniqueViewers", 10L,
                            "averageViewDuration", 45.5,
                            "popularWidgets", List.of("Chart 1", "Table 1")
                    ));

            // When & Then
            mockMvc.perform(get("/api/dashboards/{dashboardId}/analytics", dashboardId)
                    .header("X-User-Id", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalViews").value(25))
                    .andExpect(jsonPath("$.uniqueViewers").value(10))
                    .andExpect(jsonPath("$.averageViewDuration").value(45.5))
                    .andExpect(jsonPath("$.popularWidgets").isArray());
        }

        @Test
        @DisplayName("Should get popular dashboards")
        void shouldGetPopularDashboards() throws Exception {
            // Given
            List<Map<String, Object>> popularDashboards = List.of(
                    Map.of("id", UUID.randomUUID(), "name", "Popular Dashboard 1", "viewCount", 100L),
                    Map.of("id", UUID.randomUUID(), "name", "Popular Dashboard 2", "viewCount", 85L)
            );
            when(dashboardService.getPopularDashboards(any()))
                    .thenReturn(popularDashboards);

            // When & Then
            mockMvc.perform(get("/api/dashboards/popular")
                    .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].viewCount").value(100))
                    .andExpect(jsonPath("$[1].viewCount").value(85));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle validation errors")
        void shouldHandleValidationErrors() throws Exception {
            // Given
            Map<String, Object> invalidRequest = Map.of(
                    "name", "",  // Invalid: empty name
                    "description", "Description"
            );

            // When & Then
            mockMvc.perform(post("/api/dashboards")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle missing headers")
        void shouldHandleMissingHeaders() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/dashboards/{id}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized()); // Missing X-User-Id header
        }

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/dashboards")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return health status")
        void shouldReturnHealthStatus() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.version").exists());
        }
    }
}