package com.gogidix.foundation.config.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gogidix.foundation.config.enums.ConfigType;
import com.gogidix.foundation.config.enums.Environment;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Configuration data transfer object")
public class ConfigurationDto {

    @Schema(description = "Configuration ID", example = "1")
    private Long id;

    @NotBlank(message = "Configuration key is required")
    @Size(max = 255, message = "Configuration key must not exceed 255 characters")
    @Schema(description = "Configuration key", example = "database.connection.timeout")
    private String key;

    @Schema(description = "Configuration value", example = "30000")
    private String value;

    @NotNull(message = "Configuration type is required")
    @Schema(description = "Configuration type", example = "INTEGER")
    private ConfigType type;

    @NotNull(message = "Environment is required")
    @Schema(description = "Environment", example = "PRODUCTION")
    private Environment environment;

    @Schema(description = "Application name", example = "user-service")
    @Size(max = 255, message = "Application name must not exceed 255 characters")
    private String applicationName;

    @Schema(description = "Configuration description", example = "Database connection timeout in milliseconds")
    private String description;

    @Schema(description = "Created by", example = "admin")
    private String createdBy;

    @Schema(description = "Updated by", example = "admin")
    private String updatedBy;

    @Schema(description = "Configuration version", example = "1")
    private Integer version;

    @Schema(description = "Active status", example = "true")
    private Boolean active;

    @Schema(description = "Tags for categorization", example = "database,connection,timeout")
    private List<String> tags;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public ConfigurationDto() {}

    public ConfigurationDto(String key, String value, ConfigType type, Environment environment) {
        this.key = key;
        this.value = value;
        this.type = type;
        this.environment = environment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ConfigType getType() {
        return type;
    }

    public void setType(ConfigType type) {
        this.type = type;
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