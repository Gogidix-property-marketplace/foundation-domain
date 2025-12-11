package com.gogidix.dashboard.centralized.domain.dashboard.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the addition of a Widget to a Dashboard.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WidgetAddedEvent {

    private UUID dashboardId;
    private UUID widgetId;
    private UUID ownerId;
    private LocalDateTime addedAt = LocalDateTime.now();
}