package com.gogidix.foundation.dynamic.controller;

import com.gogidix.foundation.dynamic.dto.DynamicConfigDto;
import com.gogidix.foundation.dynamic.dto.DynamicConfigSearchRequest;
import com.gogidix.foundation.dynamic.service.DynamicConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dynamic-configs")
@Tag(name = "Dynamic Configuration Management", description = "APIs for managing dynamic configurations with real-time updates")
@SecurityRequirement(name = "bearerAuth")
public class DynamicConfigController {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfigController.class);

    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Operation(summary = "Search dynamic configurations", description = "Search and filter dynamic configurations with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved dynamic configurations"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    @PostMapping("/search")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<Page<DynamicConfigDto>> searchDynamicConfigs(
            @Valid @RequestBody DynamicConfigSearchRequest request) {
        try {
            Page<DynamicConfigDto> configs = dynamicConfigService.searchConfigs(request);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            logger.error("Error searching dynamic configurations", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get all dynamic configurations", description = "Retrieve all dynamic configurations with pagination")
    @GetMapping
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<Page<DynamicConfigDto>> getAllDynamicConfigs(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "configKey") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") String sortDir) {

        DynamicConfigSearchRequest request = new DynamicConfigSearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDir);

        Page<DynamicConfigDto> configs = dynamicConfigService.searchConfigs(request);
        return ResponseEntity.ok(configs);
    }

    @Operation(summary = "Get dynamic configuration by ID", description = "Retrieve a specific dynamic configuration by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dynamic configuration found"),
        @ApiResponse(responseCode = "404", description = "Dynamic configuration not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<DynamicConfigDto> getDynamicConfigById(
            @Parameter(description = "Dynamic configuration ID") @PathVariable Long id) {
        Optional<DynamicConfigDto> config = dynamicConfigService.getConfigById(id);
        return config.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get dynamic configuration by key and scope",
                description = "Retrieve a specific dynamic configuration by its key and scope")
    @GetMapping("/by-key")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<DynamicConfigDto> getDynamicConfigByKey(
            @Parameter(description = "Configuration key") @RequestParam String key,
            @Parameter(description = "Configuration scope") @RequestParam String scope) {
        try {
            com.gogidix.foundation.dynamic.enums.ConfigScope configScope =
                com.gogidix.foundation.dynamic.enums.ConfigScope.fromValue(scope);
            Optional<DynamicConfigDto> config = dynamicConfigService.getConfigByKeyAndScope(key, configScope);
            return config.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid scope parameter: {}", scope, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get active dynamic configurations by key and scope",
                description = "Retrieve active dynamic configurations by key and scope")
    @GetMapping("/active/by-key")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<DynamicConfigDto>> getActiveDynamicConfigsByKey(
            @Parameter(description = "Configuration key") @RequestParam String key,
            @Parameter(description = "Configuration scope") @RequestParam String scope) {
        try {
            com.gogidix.foundation.dynamic.enums.ConfigScope configScope =
                com.gogidix.foundation.dynamic.enums.ConfigScope.fromValue(scope);
            List<DynamicConfigDto> configs = dynamicConfigService.getActiveConfigsByKeyAndScope(key, configScope);
            return ResponseEntity.ok(configs);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid scope parameter: {}", scope, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Create new dynamic configuration", description = "Create a new dynamic configuration entry")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Dynamic configuration created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid configuration data or duplicate key"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<DynamicConfigDto> createDynamicConfig(
            @Valid @RequestBody DynamicConfigDto configDto,
            HttpServletRequest request) {
        try {
            String username = extractUsernameFromRequest(request);
            DynamicConfigDto created = dynamicConfigService.createConfig(configDto, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.error("Error creating dynamic configuration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update dynamic configuration", description = "Update an existing dynamic configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dynamic configuration updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid configuration data"),
        @ApiResponse(responseCode = "404", description = "Dynamic configuration not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<DynamicConfigDto> updateDynamicConfig(
            @Parameter(description = "Dynamic configuration ID") @PathVariable Long id,
            @Valid @RequestBody DynamicConfigDto configDto,
            HttpServletRequest request) {
        try {
            String username = extractUsernameFromRequest(request);
            DynamicConfigDto updated = dynamicConfigService.updateConfig(id, configDto, username);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating dynamic configuration with ID: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Delete dynamic configuration", description = "Delete a dynamic configuration entry")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Dynamic configuration deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Dynamic configuration not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<Void> deleteDynamicConfig(
            @Parameter(description = "Dynamic configuration ID") @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String username = extractUsernameFromRequest(request);
            dynamicConfigService.deleteConfig(id, username);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting dynamic configuration with ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get configurations by scope",
                description = "Retrieve all dynamic configurations for a specific scope")
    @GetMapping("/scope/{scope}")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<DynamicConfigDto>> getConfigsByScope(
            @Parameter(description = "Configuration scope") @PathVariable String scope) {
        try {
            com.gogidix.foundation.dynamic.enums.ConfigScope configScope =
                com.gogidix.foundation.dynamic.enums.ConfigScope.fromValue(scope);
            List<DynamicConfigDto> configs = dynamicConfigService.getConfigsByScope(configScope);
            return ResponseEntity.ok(configs);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid scope parameter: {}", scope, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get configurations by application",
                description = "Retrieve all dynamic configurations for a specific application")
    @GetMapping("/application/{applicationName}")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<DynamicConfigDto>> getConfigsByApplication(
            @Parameter(description = "Application name") @PathVariable String applicationName) {
        List<DynamicConfigDto> configs = dynamicConfigService.getConfigsByApplication(applicationName);
        return ResponseEntity.ok(configs);
    }

    @Operation(summary = "Get configurations by service",
                description = "Retrieve all dynamic configurations for a specific service")
    @GetMapping("/service/{serviceName}")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<DynamicConfigDto>> getConfigsByService(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        List<DynamicConfigDto> configs = dynamicConfigService.getConfigsByService(serviceName);
        return ResponseEntity.ok(configs);
    }

    @Operation(summary = "Get configurations by environment",
                description = "Retrieve all dynamic configurations for a specific environment")
    @GetMapping("/environment/{environment}")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<DynamicConfigDto>> getConfigsByEnvironment(
            @Parameter(description = "Environment") @PathVariable String environment) {
        List<DynamicConfigDto> configs = dynamicConfigService.getConfigsByEnvironment(environment);
        return ResponseEntity.ok(configs);
    }

    @Operation(summary = "Get configurations by user",
                description = "Retrieve all dynamic configurations for a specific user")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<DynamicConfigDto>> getConfigsByUser(
            @Parameter(description = "User ID") @PathVariable String userId) {
        List<DynamicConfigDto> configs = dynamicConfigService.getConfigsByUser(userId);
        return ResponseEntity.ok(configs);
    }

    @Operation(summary = "Get all application names", description = "Retrieve list of all application names")
    @GetMapping("/applications")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<String>> getAllApplicationNames() {
        List<String> applications = dynamicConfigService.getAllApplicationNames();
        return ResponseEntity.ok(applications);
    }

    @Operation(summary = "Get all service names", description = "Retrieve list of all service names")
    @GetMapping("/services")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<String>> getAllServiceNames() {
        List<String> services = dynamicConfigService.getAllServiceNames();
        return ResponseEntity.ok(services);
    }

    @Operation(summary = "Get all environments", description = "Retrieve list of all environments")
    @GetMapping("/environments")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<String>> getAllEnvironments() {
        List<String> environments = dynamicConfigService.getAllEnvironments();
        return ResponseEntity.ok(environments);
    }

    @Operation(summary = "Get configuration history", description = "Retrieve audit history for a dynamic configuration")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<Page<DynamicConfigDto>> getConfigurationHistory(
            @Parameter(description = "Dynamic configuration ID") @PathVariable Long id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(page, size);
        Page<DynamicConfigDto> history = dynamicConfigService.getConfigHistory(id, pageable);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Get recent changes", description = "Retrieve recent configuration changes since a specific time")
    @GetMapping("/recent-changes")
    @PreAuthorize("hasRole('DYNAMIC_CONFIG_READER') or hasRole('DYNAMIC_CONFIG_ADMIN')")
    public ResponseEntity<List<DynamicConfigDto>> getRecentChanges(
            @Parameter(description = "Since date (ISO format)") @RequestParam(required = false) String since) {
        LocalDateTime sinceDateTime = (since != null) ? LocalDateTime.parse(since) :
                                      LocalDateTime.now().minusHours(24);
        List<DynamicConfigDto> changes = dynamicConfigService.getRecentChanges(sinceDateTime);
        return ResponseEntity.ok(changes);
    }

    private String extractUsernameFromRequest(HttpServletRequest request) {
        String username = request.getHeader("X-User-Name");
        if (username == null || username.trim().isEmpty()) {
            username = request.getUserPrincipal() != null ?
                       request.getUserPrincipal().getName() : "system";
        }
        return username;
    }
}