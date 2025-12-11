package com.gogidix.foundation.dynamic.entity;

import com.gogidix.foundation.dynamic.enums.ChangeType;
import com.gogidix.foundation.dynamic.enums.ConfigScope;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_config_history", indexes = {
    @Index(name = "idx_hist_config_id", columnList = "configId"),
    @Index(name = "idx_hist_key_scope", columnList = "configKey,scope"),
    @Index(name = "idx_hist_created_at", columnList = "createdAt"),
    @Index(name = "idx_hist_change_type", columnList = "changeType")
})
public class DynamicConfigHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_id", nullable = false)
    private Long configId;

    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigScope scope;

    @Column(name = "application_name", length = 255)
    private String applicationName;

    @Column(name = "service_name", length = 255)
    private String serviceName;

    @Column(length = 100)
    private String environment;

    @Column(length = 100)
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public DynamicConfigHistory() {}

    public DynamicConfigHistory(Long configId, String configKey, String oldValue, String newValue,
                               ConfigScope scope, String changedBy, ChangeType changeType) {
        this.configId = configId;
        this.configKey = configKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.scope = scope;
        this.changedBy = changedBy;
        this.changeType = changeType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}