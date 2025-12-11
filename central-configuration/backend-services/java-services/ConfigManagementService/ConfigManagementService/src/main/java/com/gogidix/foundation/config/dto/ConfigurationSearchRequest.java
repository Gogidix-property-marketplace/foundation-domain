package com.gogidix.foundation.config.dto;

import com.gogidix.foundation.config.enums.Environment;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configuration search request parameters")
public class ConfigurationSearchRequest {

    @Schema(description = "Search by configuration key (partial match)", example = "database")
    private String key;

    @Schema(description = "Filter by environment", example = "PRODUCTION")
    private Environment environment;

    @Schema(description = "Filter by application name", example = "user-service")
    private String applicationName;

    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    private Integer size = 20;

    @Schema(description = "Sort field", example = "key", defaultValue = "key")
    private String sortBy = "key";

    @Schema(description = "Sort direction", example = "ASC", allowableValues = {"ASC", "DESC"}, defaultValue = "ASC")
    private String sortDirection = "ASC";

    public ConfigurationSearchRequest() {}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
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