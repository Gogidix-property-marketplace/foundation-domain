package com.gogidix.foundation.dynamic.dto;

import com.gogidix.foundation.dynamic.enums.ConfigScope;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Dynamic Configuration DTO")
public class DynamicConfigDto {

    private Long id;

    @NotBlank(message = "Configuration key is required")
    @Size(max = 255, message = "Configuration key must not exceed 255 characters")
    @Schema(description = "Configuration key", example = "feature.newDashboard.enabled")
    private String configKey;

    @Schema(description = "Configuration value", example = "true")
    private String configValue;

    @Schema(description = "Default configuration value", example = "false")
    private String defaultValue;

    @NotNull(message = "Configuration scope is required")
    @Schema(description = "Configuration scope", allowableValues = {"GLOBAL", "APPLICATION", "SERVICE", "ENVIRONMENT", "USER"})
    private ConfigScope scope;

    @Size(max = 255, message = "Application name must not exceed 255 characters")
    @Schema(description = "Application name", example = "user-service")
    private String applicationName;

    @Size(max = 255, message = "Service name must not exceed 255 characters")
    @Schema(description = "Service name", example = "authentication-service")
    private String serviceName;

    @Size(max = 100, message = "Environment must not exceed 100 characters")
    @Schema(description = "Environment", example = "production")
    private String environment;

    @Size(max = 100, message = "User ID must not exceed 100 characters")
    @Schema(description = "User ID for user-scoped configurations")
    private String userId;

    @Schema(description = "Configuration description", example = "Enable the new dashboard feature")
    private String description;

    @Schema(description = "User who created this configuration")
    private String createdBy;

    @Schema(description = "User who last updated this configuration")
    private String updatedBy;

    @Schema(description = "Configuration version")
    private Integer version;

    @Schema(description = "Whether this configuration is active")
    private Boolean active;

    @Schema(description = "Configuration tags")
    private List<String> tags;

    @Schema(description = "Whether this configuration value is encrypted")
    private Boolean encrypted;

    @Schema(description = "Whether this configuration change requires application restart")
    private Boolean requiresRestart;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public DynamicConfigDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ConfigScope getScope() {
        return scope;
    }

    public void setScope(ConfigScope scope) {
        this.scope = scope;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public Boolean getRequiresRestart() {
        return requiresRestart;
    }

    public void setRequiresRestart(Boolean requiresRestart) {
        this.requiresRestart = requiresRestart;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}