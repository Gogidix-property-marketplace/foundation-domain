package com.gogidix.dashboard.centralized.domain.dashboard.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the deletion of a Dashboard.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDeletedEvent {

    private UUID dashboardId;
    private String dashboardName;
    private UUID ownerId;
    private LocalDateTime deletedAt = LocalDateTime.now();
}