package com.gogidix.foundation.config.entity;

import com.gogidix.foundation.config.enums.ConfigType;
import com.gogidix.foundation.config.enums.Environment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "configurations", indexes = {
    @Index(name = "idx_config_key_env", columnList = "key,environment"),
    @Index(name = "idx_config_app", columnList = "applicationName"),
    @Index(name = "idx_config_env", columnList = "environment")
})
public class Configuration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Configuration key is required")
    @Size(max = 255, message = "Configuration key must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @NotNull(message = "Configuration type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigType type;

    @NotNull(message = "Environment is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Environment environment;

    @Size(max = 255, message = "Application name must not exceed 255 characters")
    @Column(length = 255)
    private String applicationName;

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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Configuration() {}

    public Configuration(String key, String value, ConfigType type, Environment environment) {
        this.key = key;
        this.value = value;
        this.type = type;
        this.environment = environment;
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Configuration that = (Configuration) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(key, that.key) &&
               Objects.equals(environment, that.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, key, environment);
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", type=" + type +
                ", environment=" + environment +
                ", version=" + version +
                '}';
    }
}