package com.gogidix.foundation.config.entity;

import com.gogidix.foundation.config.enums.ConfigType;
import com.gogidix.foundation.config.enums.Environment;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuration_history", indexes = {
    @Index(name = "idx_hist_config_id", columnList = "configurationId"),
    @Index(name = "idx_hist_key_env", columnList = "configKey,environment"),
    @Index(name = "idx_hist_created_at", columnList = "createdAt")
})
public class ConfigurationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long configurationId;

    @Column(nullable = false, length = 255)
    private String configKey;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Environment environment;

    @Column(length = 255)
    private String applicationName;

    @Column(length = 100)
    private String changedBy;

    @Column(length = 50)
    private String changeType;

    @Column(columnDefinition = "TEXT")
    private String changeReason;

    @Column(nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ConfigurationHistory() {}

    public ConfigurationHistory(Long configurationId, String configKey, String oldValue, String newValue,
                              ConfigType type, Environment environment, String changedBy, String changeType) {
        this.configurationId = configurationId;
        this.configKey = configKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.type = type;
        this.environment = environment;
        this.changedBy = changedBy;
        this.changeType = changeType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(Long configurationId) {
        this.configurationId = configurationId;
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

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}