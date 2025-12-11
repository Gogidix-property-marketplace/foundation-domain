package com.gogidix.dashboard.centralized.domain.dashboard;

import com.gogidix.dashboard.centralized.domain.dashboard.event.DashboardCreatedEvent;
import com.gogidix.dashboard.centralized.domain.dashboard.event.DashboardUpdatedEvent;
import com.gogidix.dashboard.centralized.domain.dashboard.event.DashboardDeletedEvent;
import com.gogidix.dashboard.centralized.domain.dashboard.event.WidgetAddedEvent;
import com.gogidix.dashboard.centralized.domain.dashboard.event.WidgetRemovedEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain entity representing a Dashboard configuration.
 * Implements DDD patterns with business logic encapsulation.
 * Centralized dashboard that can contain multiple widgets for data visualization.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("dashboards")
public class Dashboard extends AbstractAggregateRoot<Dashboard> {

    @Id
    @Column("id")
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column("name")
    private String name;

    @Size(max = 500)
    @Column("description")
    private String description;

    @NotBlank
    @Column("owner_id")
    private UUID ownerId;

    @Column("is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column("category")
    @Builder.Default
    private String category = "general";

    @Column("tags")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column("layout_config")
    private String layoutConfig;

    @Column("theme")
    @Builder.Default
    private String theme = "light";

    @Column("refresh_interval")
    @Builder.Default
    private Integer refreshInterval = 30;

    @Column("widget_count")
    @Builder.Default
    private Integer widgetCount = 0;

    @Column("max_widgets")
    @Builder.Default
    private Integer maxWidgets = 20;

    @Column("last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column("access_count")
    @Builder.Default
    private Long accessCount = 0L;

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
     * Static factory method to create a new dashboard.
     *
     * @param name the dashboard name
     * @param description the dashboard description
     * @param ownerId the owner's user ID
     * @return new Dashboard instance
     */
    public static Dashboard create(String name, String description, UUID ownerId) {
        validateDashboardCreation(name, ownerId);

        Dashboard dashboard = Dashboard.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description(description)
                .ownerId(ownerId)
                .isActive(true)
                .isPublic(false)
                .theme("light")
                .refreshInterval(30)
                .widgetCount(0)
                .maxWidgets(20)
                .tags(new ArrayList<>())
                .build();

        dashboard.registerEvent(new DashboardCreatedEvent(dashboard.getId(), dashboard.getName(), dashboard.getOwnerId()));
        return dashboard;
    }

    /**
     * Updates the dashboard configuration.
     *
     * @param name the new name
     * @param description the new description
     * @param theme the new theme
     * @param refreshInterval the new refresh interval
     */
    public void updateDashboard(String name, String description, String theme, Integer refreshInterval) {
        validateDashboardUpdate(name, theme, refreshInterval);

        String oldName = this.name;
        this.name = name;
        this.description = description;
        this.theme = theme;
        this.refreshInterval = refreshInterval;

        registerEvent(new DashboardUpdatedEvent(this.getId(), oldName, this.getName(), this.getOwnerId()));
    }

    /**
     * Adds a widget to the dashboard.
     *
     * @param widgetId the widget ID to add
     */
    public void addWidget(UUID widgetId) {
        validateWidgetCapacity();

        this.widgetCount++;
        this.lastAccessedAt = LocalDateTime.now();

        registerEvent(new WidgetAddedEvent(this.getId(), widgetId, this.getOwnerId()));
    }

    /**
     * Removes a widget from the dashboard.
     *
     * @param widgetId the widget ID to remove
     */
    public void removeWidget(UUID widgetId) {
        if (this.widgetCount > 0) {
            this.widgetCount--;
            this.lastAccessedAt = LocalDateTime.now();

            registerEvent(new WidgetRemovedEvent(this.getId(), widgetId, this.getOwnerId()));
        }
    }

    /**
     * Records dashboard access.
     */
    public void recordAccess() {
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount++;
    }

    /**
     * Activates the dashboard.
     */
    public void activate() {
        if (!this.isActive) {
            this.isActive = true;
            registerEvent(new DashboardUpdatedEvent(this.getId(), this.getName(), this.getName(), this.getOwnerId()));
        }
    }

    /**
     * Deactivates the dashboard.
     */
    public void deactivate() {
        if (this.isActive) {
            this.isActive = false;
            registerEvent(new DashboardUpdatedEvent(this.getId(), this.getName(), this.getName(), this.getOwnerId()));
        }
    }

    /**
     * Makes the dashboard public.
     */
    public void makePublic() {
        if (!this.isPublic) {
            this.isPublic = true;
            registerEvent(new DashboardUpdatedEvent(this.getId(), this.getName(), this.getName(), this.getOwnerId()));
        }
    }

    /**
     * Makes the dashboard private.
     */
    public void makePrivate() {
        if (this.isPublic) {
            this.isPublic = false;
            registerEvent(new DashboardUpdatedEvent(this.getId(), this.getName(), this.getName(), this.getOwnerId()));
        }
    }

    /**
     * Updates the layout configuration.
     *
     * @param layoutConfig the new layout configuration
     */
    public void updateLayout(String layoutConfig) {
        this.layoutConfig = layoutConfig;
        registerEvent(new DashboardUpdatedEvent(this.getId(), this.getName(), this.getName(), this.getOwnerId()));
    }

    /**
     * Validates dashboard creation parameters.
     */
    private static void validateDashboardCreation(String name, UUID ownerId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Dashboard name cannot be null or empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Dashboard name cannot exceed 100 characters");
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("Dashboard owner ID cannot be null");
        }
    }

    /**
     * Validates dashboard update parameters.
     */
    private void validateDashboardUpdate(String name, String theme, Integer refreshInterval) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Dashboard name cannot be null or empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Dashboard name cannot exceed 100 characters");
        }
        if (theme != null && !List.of("light", "dark", "auto").contains(theme)) {
            throw new IllegalArgumentException("Invalid theme. Must be light, dark, or auto");
        }
        if (refreshInterval != null && (refreshInterval < 5 || refreshInterval > 3600)) {
            throw new IllegalArgumentException("Refresh interval must be between 5 and 3600 seconds");
        }
    }

    /**
     * Validates widget capacity.
     */
    private void validateWidgetCapacity() {
        if (this.widgetCount >= this.maxWidgets) {
            throw new IllegalStateException("Dashboard has reached maximum widget capacity");
        }
    }

    /**
     * Deletes the dashboard.
     */
    public void delete() {
        registerEvent(new DashboardDeletedEvent(this.getId(), this.getName(), this.getOwnerId()));
    }
}