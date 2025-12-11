package com.gogidix.dashboard.centralized.domain.widget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AbstractAggregateRoot;
import jakarta.persistence.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing a Widget in the Centralized Dashboard System.
 * Implements DDD patterns with comprehensive widget management capabilities.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "widgets")
public class Widget extends AbstractAggregateRoot<Widget> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "dashboard_id")
    private UUID dashboardId;

    @NotBlank
    @Column(name = "widget_type")
    private String widgetType;

    @Column(name = "data_source")
    private String dataSource;

    @Column(name = "configuration", length = 10000)
    private String configuration; // JSON string for configuration

    @Column(name = "position_x")
    @Builder.Default
    private Integer positionX = 0;

    @Column(name = "position_y")
    @Builder.Default
    private Integer positionY = 0;

    @Column(name = "width")
    @Builder.Default
    private Integer width = 4;

    @Column(name = "height")
    @Builder.Default
    private Integer height = 3;

    @Column(name = "refresh_interval")
    @Builder.Default
    private Integer refreshInterval = 30; // seconds

    @Column(name = "is_visible")
    @Builder.Default
    private Boolean isVisible = true;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "last_refreshed_at")
    private LocalDateTime lastRefreshedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // Widget type constants
    public static final String TYPE_CHART = "CHART";
    public static final String TYPE_TABLE = "TABLE";
    public static final String TYPE_METRIC = "METRIC";
    public static final String TYPE_GAUGE = "GAUGE";
    public static final String TYPE_HEATMAP = "HEATMAP";
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_LIST = "LIST";

    /**
     * Static factory method to create a new widget.
     *
     * @param name the widget name
     * @param dashboardId the dashboard ID
     * @param widgetType the widget type
     * @return new Widget instance
     */
    public static Widget create(String name, UUID dashboardId, String widgetType) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Widget name cannot be null or empty");
        }
        if (dashboardId == null) {
            throw new IllegalArgumentException("Dashboard ID cannot be null");
        }
        if (widgetType == null || widgetType.trim().isEmpty()) {
            throw new IllegalArgumentException("Widget type cannot be null or empty");
        }

        return Widget.builder()
                .id(UUID.randomUUID())
                .name(name.trim())
                .dashboardId(dashboardId)
                .widgetType(widgetType)
                .positionX(0)
                .positionY(0)
                .width(4)
                .height(3)
                .refreshInterval(30)
                .isVisible(true)
                .active(true)
                .build();
    }

    /**
     * Updates widget information.
     *
     * @param name the new name
     * @param description the new description
     * @param dataSource the new data source
     */
    public void updateWidget(String name, String description, String dataSource) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
        this.description = description;
        this.dataSource = dataSource;
    }

    /**
     * Updates the widget configuration.
     *
     * @param configuration the new configuration
     */
    public void updateConfiguration(String configuration) {
        this.configuration = configuration;
    }

    /**
     * Updates the widget position.
     *
     * @param x the x position
     * @param y the y position
     */
    public void updatePosition(Integer x, Integer y) {
        this.positionX = x;
        this.positionY = y;
    }

    /**
     * Updates the widget size.
     *
     * @param width the width
     * @param height the height
     */
    public void updateSize(Integer width, Integer height) {
        if (width != null && width > 0) {
            this.width = width;
        }
        if (height != null && height > 0) {
            this.height = height;
        }
    }

    /**
     * Updates the refresh interval.
     *
     * @param refreshInterval the refresh interval in seconds
     */
    public void updateRefreshInterval(Integer refreshInterval) {
        if (refreshInterval != null && refreshInterval > 0) {
            this.refreshInterval = refreshInterval;
        }
    }

    /**
     * Shows the widget.
     */
    public void show() {
        this.isVisible = true;
    }

    /**
     * Hides the widget.
     */
    public void hide() {
        this.isVisible = false;
    }

    /**
     * Activates the widget.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Deactivates the widget.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Records a data refresh.
     */
    public void recordRefresh() {
        this.lastRefreshedAt = LocalDateTime.now();
    }

    /**
     * Checks if the widget needs refreshing.
     *
     * @return true if needs refresh
     */
    public boolean needsRefresh() {
        if (this.lastRefreshedAt == null) {
            return true;
        }
        return java.time.Duration.between(this.lastRefreshedAt, LocalDateTime.now())
                .getSeconds() >= this.refreshInterval;
    }

    /**
     * Validates widget type.
     *
     * @return true if valid type
     */
    public boolean isValidType() {
        return TYPE_CHART.equals(widgetType) ||
               TYPE_TABLE.equals(widgetType) ||
               TYPE_METRIC.equals(widgetType) ||
               TYPE_GAUGE.equals(widgetType) ||
               TYPE_HEATMAP.equals(widgetType) ||
               TYPE_TEXT.equals(widgetType) ||
               TYPE_IMAGE.equals(widgetType) ||
               TYPE_LIST.equals(widgetType);
    }

    // Additional methods needed by service layer
    public void delete() {
        this.active = false;
    }

    public String getStyleConfig() {
        return this.configuration;
    }

    public String getDataSourceId() {
        return this.dataSource;
    }

    public void updatePosition(Integer x, Integer y, Integer width, Integer height, Integer zIndex) {
        updatePosition(x, y);
        updateSize(width, height);
        // zIndex could be stored as part of configuration JSON
    }

    public void setStyleConfig(String styleConfig) {
        this.configuration = styleConfig;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSource = dataSourceId;
    }

    /**
     * Checks if the widget is active.
     * This method provides the expected isActive() behavior for tests.
     *
     * @return true if the widget is active, false otherwise
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

    /**
     * Marks the widget as refreshed (alias for recordRefresh).
     * This method provides the expected markAsRefreshed() behavior for tests.
     */
    public void markAsRefreshed() {
        recordRefresh();
    }

    /**
     * Gets the widget title (alias for name).
     * This method provides the expected getTitle() behavior for tests.
     *
     * @return the widget name/title
     */
    public String getTitle() {
        return this.name;
    }

    /**
     * Sets the widget title (alias for name).
     * This method provides the expected setTitle() behavior for tests.
     *
     * @param title the new widget title
     */
    public void setTitle(String title) {
        this.name = title;
    }
}