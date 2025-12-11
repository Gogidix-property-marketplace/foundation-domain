package com.gogidix.foundation.dynamic.dto;

import com.gogidix.foundation.dynamic.enums.ChangeType;
import com.gogidix.foundation.dynamic.enums.ConfigScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Configuration change notification for WebSocket clients")
public class ConfigChangeNotification {

    @Schema(description = "Configuration ID")
    private Long configId;

    @Schema(description = "Configuration key")
    private String configKey;

    @Schema(description = "Old configuration value")
    private String oldValue;

    @Schema(description = "New configuration value")
    private String newValue;

    @Schema(description = "Configuration scope")
    private ConfigScope scope;

    @Schema(description = "Application name")
    private String applicationName;

    @Schema(description = "Service name")
    private String serviceName;

    @Schema(description = "Environment")
    private String environment;

    @Schema(description = "User ID")
    private String userId;

    @Schema(description = "Change type")
    private ChangeType changeType;

    @Schema(description = "User who made the change")
    private String changedBy;

    @Schema(description = "Change reason")
    private String changeReason;

    @Schema(description = "Configuration version")
    private Integer version;

    @Schema(description = "Change timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "Whether the change requires application restart")
    private Boolean requiresRestart;

    public ConfigChangeNotification() {}

    public ConfigChangeNotification(Long configId, String configKey, String oldValue, String newValue,
                                   ConfigScope scope, ChangeType changeType, String changedBy) {
        this.configId = configId;
        this.configKey = configKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.scope = scope;
        this.changeType = changeType;
        this.changedBy = changedBy;
        this.timestamp = LocalDateTime.now();
    }

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
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

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getRequiresRestart() {
        return requiresRestart;
    }

    public void setRequiresRestart(Boolean requiresRestart) {
        this.requiresRestart = requiresRestart;
    }
}