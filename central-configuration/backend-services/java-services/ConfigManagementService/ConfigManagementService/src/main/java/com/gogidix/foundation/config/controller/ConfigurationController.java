package com.gogidix.foundation.config.controller;

import com.gogidix.foundation.config.dto.ConfigurationDto;
import com.gogidix.foundation.config.dto.ConfigurationSearchRequest;
import com.gogidix.foundation.config.service.ConfigurationService;
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

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/configs")
@Tag(name = "Configuration Management", description = "APIs for managing application configurations")
@SecurityRequirement(name = "bearerAuth")
public class ConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);

    @Autowired
    private ConfigurationService configurationService;

    @Operation(summary = "Search configurations", description = "Search and filter configurations with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved configurations"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    @PostMapping("/search")
    @PreAuthorize("hasRole('CONFIG_READER') or hasRole('CONFIG_ADMIN')")
    public ResponseEntity<Page<ConfigurationDto>> searchConfigurations(
            @Valid @RequestBody ConfigurationSearchRequest request) {
        try {
            Page<ConfigurationDto> configurations = configurationService.searchConfigurations(request);
            return ResponseEntity.ok(configurations);
        } catch (Exception e) {
            logger.error("Error searching configurations", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get all configurations", description = "Retrieve all configurations with pagination")
    @GetMapping
    @PreAuthorize("hasRole('CONFIG_READER') or hasRole('CONFIG_ADMIN')")
    public ResponseEntity<Page<ConfigurationDto>> getAllConfigurations(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "key") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") String sortDir) {

        ConfigurationSearchRequest request = new ConfigurationSearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDir);

        Page<ConfigurationDto> configurations = configurationService.searchConfigurations(request);
        return ResponseEntity.ok(configurations);
    }

    @Operation(summary = "Get configuration by ID", description = "Retrieve a specific configuration by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration found"),
            @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONFIG_READER') or hasRole('CONFIG_ADMIN')")
    public ResponseEntity<ConfigurationDto> getConfigurationById(
            @Parameter(description = "Configuration ID") @PathVariable Long id) {
        Optional<ConfigurationDto> configuration = configurationService.getConfigurationById(id);
        return configuration.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get configuration by key and environment",
                description = "Retrieve a specific configuration by its key and environment")
    @GetMapping("/by-key")
    @PreAuthorize("hasRole('CONFIG_READER') or hasRole('CONFIG_ADMIN')")
    public ResponseEntity<ConfigurationDto> getConfigurationByKey(
            @Parameter(description = "Configuration key") @RequestParam String key,
            @Parameter(description = "Environment") @RequestParam String environment) {
        try {
            Optional<ConfigurationDto> configuration = configurationService
                    .getConfigurationByKeyAndEnvironment(key,
                        com.gogidix.foundation.config.enums.Environment.fromValue(environment));
            return configuration.map(ResponseEntity::ok)
                               .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid environment parameter: {}", environment, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Create new configuration", description = "Create a new configuration entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Configuration created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration data or duplicate key"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping
    @PreAuthorize("hasRole('CONFIG_ADMIN')")
    public ResponseEntity<ConfigurationDto> createConfiguration(
            @Valid @RequestBody ConfigurationDto configurationDto,
            HttpServletRequest request) {
        try {
            String username = extractUsernameFromRequest(request);
            ConfigurationDto created = configurationService.createConfiguration(configurationDto, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.error("Error creating configuration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update configuration", description = "Update an existing configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration data"),
            @ApiResponse(responseCode = "404", description = "Configuration not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CONFIG_ADMIN')")
    public ResponseEntity<ConfigurationDto> updateConfiguration(
            @Parameter(description = "Configuration ID") @PathVariable Long id,
            @Valid @RequestBody ConfigurationDto configurationDto,
            HttpServletRequest request) {
        try {
            String username = extractUsernameFromRequest(request);
            ConfigurationDto updated = configurationService.updateConfiguration(id, configurationDto, username);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating configuration with ID: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Delete configuration", description = "Delete a configuration entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Configuration deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Configuration not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CONFIG_ADMIN')")
    public ResponseEntity<Void> deleteConfiguration(
            @Parameter(description = "Configuration ID") @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String username = extractUsernameFromRequest(request);
            configurationService.deleteConfiguration(id, username);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting configuration with ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get configurations by environment",
                description = "Retrieve all configurations for a specific environment")
    @GetMapping("/environment/{environment}")
    @PreAuthorize("hasRole('CONFIG_READER') or hasRole('CONFIG_ADMIN')")
    public ResponseEntity<List<ConfigurationDto>> getConfigurationsByEnvironment(
            @Parameter(description = "Environment") @PathVariable String environment) {
        try {
            List<ConfigurationDto> configurations = configurationService
                    .getConfigurationsByEnvironment(
                        com.gogidix.foundation.config.enums.Environment.fromValue(environment));
            return ResponseEntity.ok(configurations);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid environment parameter: {}", environment, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get configurations by application",
                description = "Retrieve all configurations for a specific application")
    @GetMapping("/application/{applicationName}")
    @PreAuthorize("hasRole('CONFIG_READER') or hasRole('CONFIG_ADMIN')")
    public ResponseEntity<List<ConfigurationDto>> getConfigurationsByApplication(
            @Parameter(description = "Application name") @PathVariable String applicationName) {
        List<ConfigurationDto> configurations = configurationService
                .getConfigurationsByApplication(applicationName);
        return ResponseEntity.ok(configurations);
    }

    @Operation(summary = "Get all application names", description = "Retrieve list of all application names")
    @GetMapping("/applications")
    @PreAuthorize("hasRole('CONFIG_READER') or hasRole('CONFIG_ADMIN')")
    public ResponseEntity<List<String>> getAllApplicationNames() {
        List<String> applications = configurationService.getAllApplicationNames();
        return ResponseEntity.ok(applications);
    }

    @Operation(summary = "Get configuration history", description = "Retrieve audit history for a configuration")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('CONFIG_READER') or hasRole('CONFIG_ADMIN')")
    public ResponseEntity<Page<ConfigurationDto>> getConfigurationHistory(
            @Parameter(description = "Configuration ID") @PathVariable Long id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(page, size);
        Page<ConfigurationDto> history = configurationService.getConfigurationHistory(id, pageable);
        return ResponseEntity.ok(history);
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