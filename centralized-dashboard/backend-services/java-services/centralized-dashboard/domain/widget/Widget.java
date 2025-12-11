package com.gogidix.dashboard.centralized.domain.widget;

import com.gogidix.dashboard.centralized.domain.widget.event.WidgetCreatedEvent;
import com.gogidix.dashboard.centralized.domain.widget.event.WidgetUpdatedEvent;
import com.gogidix.dashboard.centralized.domain.widget.event.WidgetDeletedEvent;
import com.gogidix.dashboard.centralized.domain.widget.event.WidgetConfiguredEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing a Widget configuration.
 * Implements DDD patterns with business logic encapsulation.
 * Widgets are individual components that display data on dashboards.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("widgets")
public class Widget extends AbstractAggregateRoot<Widget> {

    @Id
    @Column("id")
    private UUID id;

    @NotNull
    @Column("dashboard_id")
    private UUID dashboardId;

    @NotBlank
    @Size(max = 100)
    @Column("name")
    private String name;

    @Size(max = 500)
    @Column("description")
    private String description;

    @NotBlank
    @Column("widget_type")
    private String widgetType;

    @Column("data_source_id")
    private UUID dataSourceId;

    @Column("position_x")
    @Builder.Default
    private Integer positionX = 0;

    @Column("position_y")
    @Builder.Default
    private Integer positionY = 0;

    @Column("width")
    @Builder.Default
    private Integer width = 4;

    @Column("height")
    @Builder.Default
    private Integer height = 3;

    @Column("z_index")
    @Builder.Default
    private Integer zIndex = 1;

    @Column("is_visible")
    @Builder.Default
    private Boolean isVisible = true;

    @Column("is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column("refresh_interval")
    @Builder.Default
    private Integer refreshInterval = 30;

    @Column("configuration")
    private Map<String, Object> configuration;

    @Column("style_config")
    private Map<String, Object> styleConfig;

    @Column("data_filter")
    private Map<String, Object> dataFilter;

    @Column("last_data_refresh")
    private LocalDateTime lastDataRefresh;

    @Column("refresh_count")
    @Builder.Default
    private Long refreshCount = 0L;

    @Column("error_count")
    @Builder.Default
    private Long errorCount = 0L;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column("version")
    private Long version;

    /**
     * Static factory method to create a new widget.
     *
     * @param dashboardId the dashboard ID
     * @param name the widget name
     * @param widgetType the type of widget
     * @return new Widget instance
     */
    public static Widget create(UUID dashboardId, String name, String widgetType) {
        validateWidgetCreation(dashboardId, name, widgetType);

        Widget widget = Widget.builder()
                .id(UUID.randomUUID())
                .dashboardId(dashboardId)
                .name(name)
                .widgetType(widgetType)
                .positionX(0)
                .positionY(0)
                .width(4)
                .height(3)
                .zIndex(1)
                .isVisible(true)
                .isEnabled(true)
                .refreshInterval(30)
                .refreshCount(0L)
                .errorCount(0L)
                .build();

        widget.registerEvent(new WidgetCreatedEvent(widget.getId(), widget.getDashboardId(), widget.getName(), widget.getWidgetType()));
        return widget;
    }

    /**
     * Updates widget position and size.
     *
     * @param positionX X position
     * @param positionY Y position
     * @param width width in grid units
     * @param height height in grid units
     * @param zIndex Z-index for layering
     */
    public void updatePosition(Integer positionX, Integer positionY, Integer width, Integer height, Integer zIndex) {
        validatePosition(positionX, positionY, width, height, zIndex);

        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.zIndex = zIndex;

        registerEvent(new WidgetUpdatedEvent(this.getId(), this.getDashboardId(), "position", Map.of(
            "positionX", positionX,
            "positionY", positionY,
            "width", width,
            "height", height,
            "zIndex", zIndex
        )));
    }

    /**
     * Updates widget configuration.
     *
     * @param configuration the widget configuration
     */
    public void updateConfiguration(Map<String, Object> configuration) {
        validateConfiguration(configuration);

        this.configuration = configuration;

        registerEvent(new WidgetConfiguredEvent(this.getId(), this.getDashboardId(), this.getName(), "configuration", configuration));
    }

    /**
     * Updates widget style configuration.
     *
     * @param styleConfig the style configuration
     */
    public void updateStyleConfig(Map<String, Object> styleConfig) {
        validateStyleConfig(styleConfig);

        this.styleConfig = styleConfig;

        registerEvent(new WidgetConfiguredEvent(this.getId(), this.getDashboardId(), this.getName(), "style", styleConfig));
    }

    /**
     * Updates data filter configuration.
     *
     * @param dataFilter the data filter configuration
     */
    public void updateDataFilter(Map<String, Object> dataFilter) {
        this.dataFilter = dataFilter;

        registerEvent(new WidgetConfiguredEvent(this.getId(), this.getDashboardId(), this.getName(), "filter", dataFilter));
    }

    /**
     * Sets the data source for the widget.
     *
     * @param dataSourceId the data source ID
     */
    public void setDataSource(UUID dataSourceId) {
        this.dataSourceId = dataSourceId;

        registerEvent(new WidgetConfiguredEvent(this.getId(), this.getDashboardId(), this.getName(), "dataSource", Map.of(
            "dataSourceId", dataSourceId
        )));
    }

    /**
     * Updates the refresh interval.
     *
     * @param refreshInterval the refresh interval in seconds
     */
    public void updateRefreshInterval(Integer refreshInterval) {
        validateRefreshInterval(refreshInterval);

        this.refreshInterval = refreshInterval;

        registerEvent(new WidgetUpdatedEvent(this.getId(), this.getDashboardId(), "refreshInterval", Map.of(
            "refreshInterval", refreshInterval
        )));
    }

    /**
     * Records a successful data refresh.
     */
    public void recordDataRefresh() {
        this.lastDataRefresh = LocalDateTime.now();
        this.refreshCount++;
    }

    /**
     * Records a data refresh error.
     */
    public void recordRefreshError() {
        this.errorCount++;
    }

    /**
     * Shows the widget.
     */
    public void show() {
        if (!this.isVisible) {
            this.isVisible = true;
            registerEvent(new WidgetUpdatedEvent(this.getId(), this.getDashboardId(), "visibility", Map.of(
                "isVisible", true
            )));
        }
    }

    /**
     * Hides the widget.
     */
    public void hide() {
        if (this.isVisible) {
            this.isVisible = false;
            registerEvent(new WidgetUpdatedEvent(this.getId(), this.getDashboardId(), "visibility", Map.of(
                "isVisible", false
            )));
        }
    }

    /**
     * Enables the widget.
     */
    public void enable() {
        if (!this.isEnabled) {
            this.isEnabled = true;
            registerEvent(new WidgetUpdatedEvent(this.getId(), this.getDashboardId(), "enabled", Map.of(
                "isEnabled", true
            )));
        }
    }

    /**
     * Disables the widget.
     */
    public void disable() {
        if (this.isEnabled) {
            this.isEnabled = false;
            registerEvent(new WidgetUpdatedEvent(this.getId(), this.getDashboardId(), "enabled", Map.of(
                "isEnabled", false
            )));
        }
    }

    /**
     * Deletes the widget.
     */
    public void delete() {
        registerEvent(new WidgetDeletedEvent(this.getId(), this.getDashboardId(), this.getName()));
    }

    /**
     * Validates widget creation parameters.
     */
    private static void validateWidgetCreation(UUID dashboardId, String name, String widgetType) {
        if (dashboardId == null) {
            throw new IllegalArgumentException("Dashboard ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Widget name cannot be null or empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Widget name cannot exceed 100 characters");
        }
        if (widgetType == null || widgetType.trim().isEmpty()) {
            throw new IllegalArgumentException("Widget type cannot be null or empty");
        }
    }

    /**
     * Validates widget position and size.
     */
    private void validatePosition(Integer positionX, Integer positionY, Integer width, Integer height, Integer zIndex) {
        if (positionX != null && positionX < 0) {
            throw new IllegalArgumentException("Position X cannot be negative");
        }
        if (positionY != null && positionY < 0) {
            throw new IllegalArgumentException("Position Y cannot be negative");
        }
        if (width != null && (width < 1 || width > 12)) {
            throw new IllegalArgumentException("Widget width must be between 1 and 12");
        }
        if (height != null && (height < 1 || height > 20)) {
            throw new IllegalArgumentException("Widget height must be between 1 and 20");
        }
        if (zIndex != null && zIndex < 1) {
            throw new IllegalArgumentException("Z-index must be positive");
        }
    }

    /**
     * Validates widget configuration.
     */
    private void validateConfiguration(Map<String, Object> configuration) {
        if (configuration != null && configuration.size() > 1000) {
            throw new IllegalArgumentException("Widget configuration cannot exceed 1000 properties");
        }
    }

    /**
     * Validates style configuration.
     */
    private void validateStyleConfig(Map<String, Object> styleConfig) {
        if (styleConfig != null && styleConfig.size() > 100) {
            throw new IllegalArgumentException("Widget style configuration cannot exceed 100 properties");
        }
    }

    /**
     * Validates refresh interval.
     */
    private void validateRefreshInterval(Integer refreshInterval) {
        if (refreshInterval != null && (refreshInterval < 5 || refreshInterval > 3600)) {
            throw new IllegalArgumentException("Refresh interval must be between 5 and 3600 seconds");
        }
    }
}