package com.gogidix.dashboard.centralized.web.controller;

import com.gogidix.dashboard.centralized.domain.dashboard.Dashboard;
import com.gogidix.dashboard.centralized.infrastructure.repository.DashboardRepository;
import com.gogidix.dashboard.centralized.infrastructure.websocket.DashboardWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Dashboard operations.
 * Provides endpoints for dashboard CRUD operations and management.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboards")
@RequiredArgsConstructor
@Validated
@Tag(name = "Dashboard Management", description = "Dashboard CRUD operations and management")
public class DashboardController {

    private final DashboardRepository dashboardRepository;
    private final DashboardWebSocketHandler webSocketHandler;

    /**
     * Creates a new dashboard.
     *
     * @param createRequest the dashboard creation request
     * @param authentication the current user authentication
     * @return created dashboard
     */
    @PostMapping
    @Operation(summary = "Create Dashboard", description = "Creates a new dashboard with the specified configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Dashboard created successfully",
                    content = @Content(schema = @Schema(implementation = Dashboard.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Dashboard> createDashboard(
            @Valid @RequestBody CreateDashboardRequest createRequest,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        Dashboard dashboard = Dashboard.create(
            createRequest.name(),
            createRequest.description(),
            userId
        );

        dashboard.setCategory(createRequest.category());
        dashboard.setTags(createRequest.tags());
        dashboard.setTheme(createRequest.theme());
        dashboard.setRefreshInterval(createRequest.refreshInterval());

        Dashboard savedDashboard = dashboardRepository.save(dashboard);

        // Broadcast dashboard creation event
        webSocketHandler.broadcastDashboardCreation(
            savedDashboard.getId(),
            authentication.getName()
        );

        log.info("Dashboard created: {} by user: {}", savedDashboard.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDashboard);
    }

    /**
     * Gets a dashboard by ID.
     *
     * @param id the dashboard ID
     * @param authentication the current user authentication
     * @return dashboard details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Dashboard", description = "Retrieves dashboard details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard found"),
        @ApiResponse(responseCode = "404", description = "Dashboard not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Dashboard> getDashboard(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        return dashboardRepository.findById(id)
            .map(dashboard -> {
                // Check access permissions
                if (dashboard.getOwnerId().equals(userId) || dashboard.getIsPublic()) {
                    dashboard.recordAccess();
                    dashboardRepository.save(dashboard);
                    return ResponseEntity.ok(dashboard);
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Dashboard>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing dashboard.
     *
     * @param id the dashboard ID
     * @param updateRequest the update request
     * @param authentication the current user authentication
     * @return updated dashboard
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Dashboard", description = "Updates dashboard configuration")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Dashboard> updateDashboard(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateDashboardRequest updateRequest,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        return dashboardRepository.findById(id)
            .map(dashboard -> {
                if (!dashboard.getOwnerId().equals(userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<Dashboard>build();
                }

                dashboard.updateDashboard(
                    updateRequest.name(),
                    updateRequest.description(),
                    updateRequest.theme(),
                    updateRequest.refreshInterval()
                );

                Dashboard savedDashboard = dashboardRepository.save(dashboard);

                // Send real-time update
                Map<String, Object> updateDetails = new HashMap<>();
                updateDetails.put("name", updateRequest.name());
                updateDetails.put("description", updateRequest.description());
                webSocketHandler.broadcastDashboardUpdate(id, "dashboard_update", updateDetails);

                log.info("Dashboard updated: {} by user: {}", id, userId);
                return ResponseEntity.ok(savedDashboard);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a dashboard.
     *
     * @param id the dashboard ID
     * @param authentication the current user authentication
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Dashboard", description = "Deletes a dashboard by ID")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteDashboard(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        return dashboardRepository.findById(id)
            .map(dashboard -> {
                if (!dashboard.getOwnerId().equals(userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                }

                dashboard.delete();
                dashboardRepository.delete(dashboard);

                log.info("Dashboard deleted: {} by user: {}", id, userId);
                return ResponseEntity.noContent().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lists user's dashboards.
     *
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @param sort sort field (default updatedAt)
     * @param direction sort direction (default DESC)
     * @param authentication the current user authentication
     * @return paginated list of dashboards
     */
    @GetMapping("/my")
    @Operation(summary = "List My Dashboards", description = "Retrieves paginated list of user's dashboards")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Dashboard>> getMyDashboards(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "updatedAt") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String direction,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<Dashboard> dashboards = dashboardRepository.findByOwnerIdAndIsActive(userId, true, pageable);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Lists public dashboards.
     *
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @param sort sort field (default updatedAt)
     * @param direction sort direction (default DESC)
     * @return paginated list of public dashboards
     */
    @GetMapping("/public")
    @Operation(summary = "List Public Dashboards", description = "Retrieves paginated list of public dashboards")
    public ResponseEntity<Page<Dashboard>> getPublicDashboards(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "updatedAt") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<Dashboard> dashboards = dashboardRepository.findByIsPublicTrueAndIsActiveTrue(pageable);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Searches dashboards.
     *
     * @param searchTerm the search term
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @return paginated search results
     */
    @GetMapping("/search")
    @Operation(summary = "Search Dashboards", description = "Searches dashboards by name or description")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Dashboard>> searchDashboards(
            @Parameter(description = "Search term") @RequestParam @NotBlank String searchTerm,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Dashboard> results = dashboardRepository.searchDashboards(searchTerm, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Gets dashboard statistics.
     *
     * @param authentication the current user authentication
     * @return dashboard statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get Dashboard Statistics", description = "Retrieves dashboard usage statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        long totalDashboards = dashboardRepository.countByOwnerId(userId);
        long activeDashboards = dashboardRepository.countActiveByOwnerId(userId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalDashboards", totalDashboards);
        statistics.put("activeDashboards", activeDashboards);
        statistics.put("inactiveDashboards", totalDashboards - activeDashboards);
        statistics.put("publicDashboards", dashboardRepository.findByOwnerIdAndIsPublicTrue(userId, Pageable.unpaged()).getTotalElements());

        return ResponseEntity.ok(statistics);
    }

    /**
     * Duplicates a dashboard.
     *
     * @param id the dashboard ID to duplicate
     * @param duplicateRequest the duplication request
     * @param authentication the current user authentication
     * @return new dashboard
     */
    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplicate Dashboard", description = "Creates a copy of an existing dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Dashboard> duplicateDashboard(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id,
            @Valid @RequestBody DuplicateDashboardRequest duplicateRequest,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        return dashboardRepository.findById(id)
            .map(originalDashboard -> {
                if (!originalDashboard.getIsPublic() && !originalDashboard.getOwnerId().equals(userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<Dashboard>build();
                }

                Dashboard newDashboard = Dashboard.create(
                    duplicateRequest.name(),
                    duplicateRequest.description(),
                    userId
                );

                newDashboard.setCategory(originalDashboard.getCategory());
                newDashboard.setTheme(originalDashboard.getTheme());
                newDashboard.setRefreshInterval(originalDashboard.getRefreshInterval());
                newDashboard.setLayoutConfig(originalDashboard.getLayoutConfig());

                Dashboard savedDashboard = dashboardRepository.save(newDashboard);
                log.info("Dashboard duplicated: {} from: {} by user: {}", savedDashboard.getId(), id, userId);

                return ResponseEntity.status(HttpStatus.CREATED).body(savedDashboard);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Request DTOs
    public record CreateDashboardRequest(
        @NotBlank String name,
        String description,
        String category,
        List<String> tags,
        String theme,
        Integer refreshInterval
    ) {}

    public record UpdateDashboardRequest(
        @NotBlank String name,
        String description,
        String theme,
        Integer refreshInterval
    ) {}

    public record DuplicateDashboardRequest(
        @NotBlank String name,
        String description
    ) {}
}