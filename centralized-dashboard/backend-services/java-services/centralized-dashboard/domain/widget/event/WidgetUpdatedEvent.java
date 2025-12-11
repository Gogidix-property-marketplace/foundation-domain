package com.gogidix.dashboard.centralized.domain.widget.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event representing the update of a Widget.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WidgetUpdatedEvent {

    private UUID widgetId;
    private UUID dashboardId;
    private String updateType;
    private Map<String, Object> changes;
    private LocalDateTime updatedAt = LocalDateTime.now();
}
