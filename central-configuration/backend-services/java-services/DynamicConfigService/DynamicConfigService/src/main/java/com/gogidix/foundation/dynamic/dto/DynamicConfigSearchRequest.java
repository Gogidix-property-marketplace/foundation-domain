package com.gogidix.foundation.dynamic.dto;

import com.gogidix.foundation.dynamic.enums.ConfigScope;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Search request for dynamic configurations")
public class DynamicConfigSearchRequest {

    @Schema(description = "Configuration key to search for (partial match)", example = "feature")
    private String configKey;

    @Schema(description = "Configuration scope filter", allowableValues = {"GLOBAL", "APPLICATION", "SERVICE", "ENVIRONMENT", "USER"})
    private ConfigScope scope;

    @Schema(description = "Application name filter", example = "user-service")
    private String applicationName;

    @Schema(description = "Service name filter", example = "authentication-service")
    private String serviceName;

    @Schema(description = "Environment filter", example = "production")
    private String environment;

    @Schema(description = "Active status filter")
    private Boolean active;

    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    private int page = 0;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    private int size = 20;

    @Schema(description = "Sort field", example = "configKey", defaultValue = "configKey")
    private String sortBy = "configKey";

    @Schema(description = "Sort direction", example = "ASC", defaultValue = "ASC")
    private String sortDirection = "ASC";

    public DynamicConfigSearchRequest() {}

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}