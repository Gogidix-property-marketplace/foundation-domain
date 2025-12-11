package com.gogidix.dashboard.centralized.application.service;

import com.gogidix.dashboard.centralized.domain.dashboard.Dashboard;
import com.gogidix.dashboard.centralized.domain.widget.Widget;
import com.gogidix.dashboard.centralized.infrastructure.repository.DashboardRepository;
import com.gogidix.dashboard.centralized.infrastructure.repository.WidgetRepository;
import com.gogidix.dashboard.centralized.infrastructure.websocket.DashboardWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for Dashboard operations.
 * Provides business logic orchestration between controllers and repositories.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final WidgetRepository widgetRepository;
    private final DashboardWebSocketHandler webSocketHandler;

    /**
     * Creates a new dashboard.
     *
     * @param name the dashboard name
     * @param description the dashboard description
     * @param ownerId the owner's user ID
     * @param category the dashboard category
     * @param tags list of tags
     * @return created dashboard
     */
    public Dashboard createDashboard(String name, String description, UUID ownerId, String category, List<String> tags) {
        log.info("Creating dashboard: {} for user: {}", name, ownerId);

        Dashboard dashboard = Dashboard.create(name, ownerId, "System User"); // Create with proper parameters
        dashboard.setDescription(description);
        dashboard.setCategory(category);
        dashboard.setTags(tags != null ? String.join(",", tags) : null); // Convert List to String
        dashboard.setTheme("light");
        dashboard.setRefreshInterval(30);

        Dashboard savedDashboard = dashboardRepository.save(dashboard);

        // Broadcast dashboard creation
        Map<String, Object> creationDetails = Map.of(
            "name", name,
            "category", category,
            "tags", tags
        );
        webSocketHandler.broadcastDashboardCreation(savedDashboard.getId(), dashboard.getName());

        return savedDashboard;
    }

    /**
     * Updates an existing dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param userId the user ID
     * @param name the new name
     * @param description the new description
     * @param theme the new theme
     * @param refreshInterval the refresh interval
     * @return updated dashboard
     */
    public Dashboard updateDashboard(UUID dashboardId, UUID userId, String name, String description, String theme, Integer refreshInterval) {
        log.info("Updating dashboard: {} by user: {}", dashboardId, userId);

        Dashboard dashboard = findDashboardByIdAndValidateOwnership(dashboardId, userId);

        dashboard.setDescription(description);
        dashboard.setName(name);
        dashboard.setTheme(theme);
        dashboard.setRefreshInterval(refreshInterval);
        Dashboard savedDashboard = dashboardRepository.save(dashboard);

        // Send real-time update
        Map<String, Object> updateDetails = Map.of(
            "name", name,
            "description", description,
            "theme", theme,
            "refreshInterval", refreshInterval
        );
        webSocketHandler.sendDashboardUpdate(dashboardId.toString(), updateDetails);

        return savedDashboard;
    }

    /**
     * Deletes a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param userId the user ID
     */
    public void deleteDashboard(UUID dashboardId, UUID userId) {
        log.info("Deleting dashboard: {} by user: {}", dashboardId, userId);

        Dashboard dashboard = findDashboardByIdAndValidateOwnership(dashboardId, userId);

        // Remove all widgets first
        List<Widget> widgets = widgetRepository.findAllByDashboardIdOrderByPosition(dashboardId);
        widgetRepository.deleteAll(widgets);

        dashboard.delete();
        dashboardRepository.delete(dashboard);
    }

    /**
     * Gets a dashboard by ID with access validation.
     *
     * @param dashboardId the dashboard ID
     * @param userId the user ID
     * @return dashboard details
     */
    public Dashboard getDashboard(UUID dashboardId, UUID userId) {
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
            .orElseThrow(() -> new RuntimeException("Dashboard not found: " + dashboardId));

        // Check access permissions
        if (!dashboard.getOwnerId().equals(userId) && !dashboard.getIsPublic()) {
            throw new RuntimeException("Access denied to dashboard: " + dashboardId);
        }

        dashboard.recordAccess();
        dashboardRepository.save(dashboard);

        return dashboard;
    }

    /**
     * Gets user's dashboards.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of user's dashboards
     */
    public Page<Dashboard> getUserDashboards(UUID userId, Pageable pageable) {
        return dashboardRepository.findByOwnerIdAndIsActive(userId, true, pageable);
    }

    /**
     * Gets public dashboards.
     *
     * @param pageable pagination information
     * @return page of public dashboards
     */
    public Page<Dashboard> getPublicDashboards(Pageable pageable) {
        return dashboardRepository.findByIsPublicTrueAndIsActiveTrue(pageable);
    }

    /**
     * Searches dashboards.
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of search results
     */
    public Page<Dashboard> searchDashboards(String searchTerm, Pageable pageable) {
        return dashboardRepository.searchDashboards(searchTerm, pageable);
    }

    /**
     * Duplicates a dashboard.
     *
     * @param dashboardId the dashboard ID to duplicate
     * @param userId the user ID
     * @param newName the new dashboard name
     * @param description the new description
     * @return duplicated dashboard
     */
    public Dashboard duplicateDashboard(UUID dashboardId, UUID userId, String newName, String description) {
        log.info("Duplicating dashboard: {} for user: {} with new name: {}", dashboardId, userId, newName);

        Dashboard originalDashboard = dashboardRepository.findById(dashboardId)
            .orElseThrow(() -> new RuntimeException("Dashboard not found: " + dashboardId));

        // Check if user can access the original dashboard
        if (!originalDashboard.getOwnerId().equals(userId) && !originalDashboard.getIsPublic()) {
            throw new RuntimeException("Access denied to dashboard: " + dashboardId);
        }

        Dashboard newDashboard = Dashboard.create(newName, userId, "System User");
        newDashboard.setDescription(description);
        newDashboard.setCategory(originalDashboard.getCategory());
        newDashboard.setTheme(originalDashboard.getTheme());
        newDashboard.setRefreshInterval(originalDashboard.getRefreshInterval());
        newDashboard.setLayoutConfig(originalDashboard.getLayoutConfig());

        Dashboard savedDashboard = dashboardRepository.save(newDashboard);

        // Copy widgets
        List<Widget> originalWidgets = widgetRepository.findAllByDashboardIdOrderByPosition(dashboardId);
        for (Widget originalWidget : originalWidgets) {
            Widget newWidget = Widget.create(
                originalWidget.getName() + " (Copy)",
                savedDashboard.getId(),
                originalWidget.getWidgetType()
            );
            newWidget.setDescription(originalWidget.getDescription());
            newWidget.setPositionX(originalWidget.getPositionX());
            newWidget.setPositionY(originalWidget.getPositionY() + 100); // Offset to avoid overlap
            newWidget.setWidth(originalWidget.getWidth());
            newWidget.setHeight(originalWidget.getHeight());
            newWidget.setConfiguration(originalWidget.getConfiguration());
            newWidget.setStyleConfig(originalWidget.getStyleConfig());
            newWidget.setDataSourceId(originalWidget.getDataSourceId());
            newWidget.setRefreshInterval(originalWidget.getRefreshInterval());
            widgetRepository.save(newWidget);
        }

        newDashboard.setWidgetCount(originalWidgets.size());
        dashboardRepository.save(newDashboard);

        return newDashboard;
    }

    /**
     * Adds a widget to a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param userId the user ID
     * @param widgetName the widget name
     * @param widgetType the widget type
     * @return created widget
     */
    public Widget addWidget(UUID dashboardId, UUID userId, String widgetName, String widgetType) {
        log.info("Adding widget: {} to dashboard: {} by user: {}", widgetName, dashboardId, userId);

        Dashboard dashboard = findDashboardByIdAndValidateOwnership(dashboardId, userId);

        Widget widget = Widget.create(widgetName, dashboardId, widgetType);
        Widget savedWidget = widgetRepository.save(widget);

        dashboard.addWidget(savedWidget.getId());
        dashboardRepository.save(dashboard);

        // Send real-time update
        Map<String, Object> widgetData = Map.of(
            "widgetId", savedWidget.getId(),
            "widgetName", widgetName,
            "widgetType", widgetType
        );
        webSocketHandler.sendDashboardUpdate(dashboardId.toString(), widgetData);

        return savedWidget;
    }

    /**
     * Updates a widget's position.
     *
     * @param widgetId the widget ID
     * @param userId the user ID
     * @param positionX X position
     * @param positionY Y position
     * @param width width
     * @param height height
     * @param zIndex Z-index
     */
    public void updateWidgetPosition(UUID widgetId, UUID userId, Integer positionX, Integer positionY, Integer width, Integer height, Integer zIndex) {
        Widget widget = findWidgetByIdAndValidateOwnership(widgetId, userId);

        widget.updatePosition(positionX, positionY, width, height, zIndex);
        widgetRepository.save(widget);

        // Send real-time update
        Map<String, Object> positionData = Map.of(
            "widgetId", widgetId,
            "positionX", positionX,
            "positionY", positionY,
            "width", width,
            "height", height,
            "zIndex", zIndex
        );
        webSocketHandler.sendDashboardUpdate(widget.getDashboardId().toString(), positionData);
    }

    /**
     * Deletes a widget.
     *
     * @param widgetId the widget ID
     * @param userId the user ID
     */
    public void deleteWidget(UUID widgetId, UUID userId) {
        Widget widget = findWidgetByIdAndValidateOwnership(widgetId, userId);
        UUID dashboardId = widget.getDashboardId();

        widget.delete();
        widgetRepository.delete(widget);

        // Update dashboard widget count
        Dashboard dashboard = dashboardRepository.findById(dashboardId).orElse(null);
        if (dashboard != null) {
            dashboard.removeWidget(widgetId);
            dashboardRepository.save(dashboard);
        }

        // Send real-time update
        Map<String, Object> widgetData = Map.of(
            "widgetId", widgetId,
            "action", "deleted"
        );
        webSocketHandler.sendDashboardUpdate(dashboardId.toString(), widgetData);
    }

    /**
     * Gets dashboard statistics.
     *
     * @param userId the user ID
     * @return dashboard statistics
     */
    public Map<String, Object> getDashboardStatistics(UUID userId) {
        long totalDashboards = dashboardRepository.countByOwnerId(userId);
        long activeDashboards = dashboardRepository.countActiveByOwnerId(userId);

        return Map.of(
            "totalDashboards", totalDashboards,
            "activeDashboards", activeDashboards,
            "inactiveDashboards", totalDashboards - activeDashboards,
            "totalWidgets", widgetRepository.countByDashboardId(userId)
        );
    }

    /**
     * Gets popular dashboards.
     *
     * @param pageable pagination information
     * @return page of popular dashboards
     */
    public Page<Dashboard> getPopularDashboards(Pageable pageable) {
        return dashboardRepository.findPopularDashboards(pageable);
    }

    /**
     * Updates dashboard layout configuration.
     *
     * @param dashboardId the dashboard ID
     * @param userId the user ID
     * @param layoutConfig the layout configuration
     */
    public void updateDashboardLayout(UUID dashboardId, UUID userId, String layoutConfig) {
        Dashboard dashboard = findDashboardByIdAndValidateOwnership(dashboardId, userId);
        dashboard.updateLayout(layoutConfig);
        dashboardRepository.save(dashboard);

        // Send real-time update
        Map<String, Object> layoutData = Map.of(
            "layoutConfig", layoutConfig
        );
        webSocketHandler.sendDashboardUpdate(dashboardId.toString(), layoutData);
    }

    /**
     * Toggles dashboard visibility.
     *
     * @param dashboardId the dashboard ID
     * @param userId the user ID
     * @param isPublic whether the dashboard should be public
     */
    public void toggleDashboardVisibility(UUID dashboardId, UUID userId, boolean isPublic) {
        Dashboard dashboard = findDashboardByIdAndValidateOwnership(dashboardId, userId);

        if (isPublic) {
            dashboard.makePublic();
        } else {
            dashboard.makePrivate();
        }

        dashboardRepository.save(dashboard);

        // Send real-time update
        Map<String, Object> visibilityData = Map.of(
            "isPublic", isPublic
        );
        webSocketHandler.sendDashboardUpdate(dashboardId.toString(), visibilityData);
    }

    /**
     * Gets dashboard health metrics.
     *
     * @return dashboard health metrics
     */
    public Map<String, Object> getDashboardHealthMetrics() {
        long totalDashboards = dashboardRepository.count();
        long activeDashboards = dashboardRepository.count();
        long totalWidgets = widgetRepository.count();

        return Map.of(
            "totalDashboards", totalDashboards,
            "activeDashboards", activeDashboards,
            "totalWidgets", totalWidgets,
            "avgWidgetsPerDashboard", totalDashboards > 0 ? (double) totalWidgets / totalDashboards : 0,
            "lastUpdated", LocalDateTime.now()
        );
    }

    /**
     * Records a view for a dashboard.
     * This method provides the expected recordView(UUID, UUID) behavior for tests.
     *
     * @param dashboardId the dashboard ID
     * @param userId the user ID
     */
    public void recordView(UUID dashboardId, UUID userId) {
        Dashboard dashboard = findDashboardByIdAndValidateOwnership(dashboardId, userId);
        dashboard.recordView();
        dashboardRepository.save(dashboard);
    }

    /**
     * Gets dashboard analytics.
     * This method provides the expected getDashboardAnalytics(UUID, UUID) behavior for tests.
     *
     * @param dashboardId the dashboard ID
     * @param userId the user ID
     * @return dashboard analytics
     */
    public List<Map<String, Object>> getDashboardAnalytics(UUID dashboardId, UUID userId) {
        Dashboard dashboard = findDashboardByIdAndValidateOwnership(dashboardId, userId);

        List<Widget> widgets = widgetRepository.findAllByDashboardIdOrderByPosition(dashboardId);

        return widgets.stream()
                .map(widget -> {
                    Map<String, Object> widgetMap = new HashMap<>();
                    widgetMap.put("widgetId", widget.getId());
                    widgetMap.put("widgetName", widget.getName());
                    widgetMap.put("widgetType", widget.getWidgetType());
                    widgetMap.put("positionX", widget.getPositionX());
                    widgetMap.put("positionY", widget.getPositionY());
                    widgetMap.put("width", widget.getWidth());
                    widgetMap.put("height", widget.getHeight());
                    widgetMap.put("isActive", widget.isActive());
                    widgetMap.put("lastRefreshedAt", widget.getLastRefreshedAt() != null ? widget.getLastRefreshedAt().toString() : "Never");
                    return widgetMap;
                })
                .collect(Collectors.toList());
    }

    // Private helper methods

    private Dashboard findDashboardByIdAndValidateOwnership(UUID dashboardId, UUID userId) {
        return dashboardRepository.findById(dashboardId)
            .filter(dashboard -> dashboard.getOwnerId().equals(userId))
            .orElseThrow(() -> new RuntimeException("Dashboard not found or access denied: " + dashboardId));
    }

    private Widget findWidgetByIdAndValidateOwnership(UUID widgetId, UUID userId) {
        return widgetRepository.findById(widgetId)
            .map(widget -> {
                Dashboard dashboard = dashboardRepository.findById(widget.getDashboardId()).orElse(null);
                if (dashboard == null || !dashboard.getOwnerId().equals(userId)) {
                    throw new RuntimeException("Widget not found or access denied: " + widgetId);
                }
                return widget;
            })
            .orElseThrow(() -> new RuntimeException("Widget not found: " + widgetId));
    }
}