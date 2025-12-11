package com.gogidix.dashboard.centralized.domain.dashboard.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the update of a Dashboard.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardUpdatedEvent {

    private UUID dashboardId;
    private String oldName;
    private String newName;
    private UUID ownerId;
    private LocalDateTime updatedAt = LocalDateTime.now();
}