package com.gogidix.foundation.dynamic.entity;

import com.gogidix.foundation.dynamic.enums.ConfigScope;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "dynamic_configs", indexes = {
    @Index(name = "idx_dynamic_key_scope", columnList = "configKey,scope"),
    @Index(name = "idx_dynamic_application", columnList = "applicationName"),
    @Index(name = "idx_dynamic_service", columnList = "serviceName"),
    @Index(name = "idx_dynamic_environment", columnList = "environment"),
    @Index(name = "idx_dynamic_active", columnList = "active")
})
public class DynamicConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Configuration key is required")
    @Size(max = 255, message = "Configuration key must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String configKey;

    @Column(columnDefinition = "TEXT")
    private String configValue;

    @Column(columnDefinition = "TEXT")
    private String defaultValue;

    @NotNull(message = "Configuration scope is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigScope scope;

    @Size(max = 255, message = "Application name must not exceed 255 characters")
    @Column(length = 255)
    private String applicationName;

    @Size(max = 255, message = "Service name must not exceed 255 characters")
    @Column(length = 255)
    private String serviceName;

    @Size(max = 100, message = "Environment must not exceed 100 characters")
    @Column(length = 100)
    private String environment;

    @Size(max = 100, message = "User ID must not exceed 100 characters")
    @Column(length = 100)
    private String userId;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 500)
    private String tags;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean encrypted = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean requiresRestart = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public DynamicConfig() {}

    public DynamicConfig(String configKey, String configValue, ConfigScope scope) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.scope = scope;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        version++;
    }

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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicConfig that = (DynamicConfig) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(configKey, that.configKey) &&
               Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, configKey, scope);
    }

    @Override
    public String toString() {
        return "DynamicConfig{" +
                "id=" + id +
                ", configKey='" + configKey + '\'' +
                ", scope=" + scope +
                ", version=" + version +
                '}';
    }
}