package com.gogidix.dashboard.centralized.domain.widget.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the deletion of a Widget.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WidgetDeletedEvent {

    private UUID widgetId;
    private UUID dashboardId;
    private String widgetName;
    private LocalDateTime deletedAt = LocalDateTime.now();
}
