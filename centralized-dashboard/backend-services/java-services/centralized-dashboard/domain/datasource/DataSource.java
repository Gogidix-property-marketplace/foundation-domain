package com.gogidix.dashboard.centralized.domain.datasource;

import com.gogidix.dashboard.centralized.domain.datasource.event.DataSourceCreatedEvent;
import com.gogidix.dashboard.centralized.domain.datasource.event.DataSourceUpdatedEvent;
import com.gogidix.dashboard.centralized.domain.datasource.event.DataSourceDeletedEvent;
import com.gogidix.dashboard.centralized.domain.datasource.event.DataSourceConnectedEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing a DataSource configuration.
 * Implements DDD patterns with business logic encapsulation.
 * DataSources provide data to widgets for visualization.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("data_sources")
public class DataSource extends AbstractAggregateRoot<DataSource> {

    @Id
    @Column("id")
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column("name")
    private String name;

    @Size(max = 500)
    @Column("description")
    private String description;

    @NotNull
    @Column("source_type")
    private String sourceType;

    @Column("connection_config")
    private Map<String, Object> connectionConfig;

    @Column("query_config")
    private Map<String, Object> queryConfig;

    @Column("refresh_interval")
    @Builder.Default
    private Integer refreshInterval = 60;

    @Column("timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 30;

    @Column("retry_attempts")
    @Builder.Default
    private Integer retryAttempts = 3;

    @Column("retry_delay_seconds")
    @Builder.Default
    private Integer retryDelaySeconds = 5;

    @Column("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column("is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column("owner_id")
    private UUID ownerId;

    @Column("tags")
    @Builder.Default
    private List<String> tags = List.of();

    @Column("schema_definition")
    private Map<String, Object> schemaDefinition;

    @Column("last_connection_test")
    private LocalDateTime lastConnectionTest;

    @Column("connection_status")
    @Builder.Default
    private String connectionStatus = "pending";

    @Column("last_data_fetch")
    private LocalDateTime lastDataFetch;

    @Column("data_cache_ttl")
    @Builder.Default
    private Integer dataCacheTtl = 300;

    @Column("max_rows")
    @Builder.Default
    private Integer maxRows = 1000;

    @Column("error_count")
    @Builder.Default
    private Long errorCount = 0L;

    @Column("success_count")
    @Builder.Default
    private Long successCount = 0L;

    @Column("api_endpoint")
    private String apiEndpoint;

    @Column("api_key")
    private String apiKey;

    @Column("authentication_type")
    private String authenticationType;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column("version")
    private Long version;

    /**
     * Static factory method to create a new data source.
     *
     * @param name the data source name
     * @param description the data source description
     * @param sourceType the type of data source
     * @param connectionConfig the connection configuration
     * @return new DataSource instance
     */
    public static DataSource create(String name, String description, String sourceType, Map<String, Object> connectionConfig) {
        validateDataSourceCreation(name, sourceType, connectionConfig);

        DataSource dataSource = DataSource.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description(description)
                .sourceType(sourceType)
                .connectionConfig(connectionConfig)
                .refreshInterval(60)
                .timeoutSeconds(30)
                .retryAttempts(3)
                .retryDelaySeconds(5)
                .isActive(true)
                .isPublic(false)
                .tags(List.of())
                .dataCacheTtl(300)
                .maxRows(1000)
                .errorCount(0L)
                .successCount(0L)
                .connectionStatus("pending")
                .build();

        dataSource.registerEvent(new DataSourceCreatedEvent(dataSource.getId(), dataSource.getName(), dataSource.getSourceType()));
        return dataSource;
    }

    /**
     * Updates the data source configuration.
     *
     * @param name the new name
     * @param description the new description
     * @param connectionConfig the new connection configuration
     */
    public void updateConfiguration(String name, String description, Map<String, Object> connectionConfig) {
        validateConfigurationUpdate(name, connectionConfig);

        String oldName = this.name;
        this.name = name;
        this.description = description;
        this.connectionConfig = connectionConfig;

        registerEvent(new DataSourceUpdatedEvent(this.getId(), oldName, this.getName(), "configuration"));
    }

    /**
     * Updates the query configuration.
     *
     * @param queryConfig the new query configuration
     */
    public void updateQueryConfig(Map<String, Object> queryConfig) {
        this.queryConfig = queryConfig;
        registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "query"));
    }

    /**
     * Updates the refresh and timeout settings.
     *
     * @param refreshInterval the refresh interval in seconds
     * @param timeoutSeconds the timeout in seconds
     * @param retryAttempts the retry attempts
     * @param retryDelaySeconds the retry delay in seconds
     */
    public void updateRefreshSettings(Integer refreshInterval, Integer timeoutSeconds, Integer retryAttempts, Integer retryDelaySeconds) {
        validateRefreshSettings(refreshInterval, timeoutSeconds, retryAttempts, retryDelaySeconds);

        this.refreshInterval = refreshInterval;
        this.timeoutSeconds = timeoutSeconds;
        this.retryAttempts = retryAttempts;
        this.retryDelaySeconds = retryDelaySeconds;

        registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "refresh_settings"));
    }

    /**
     * Updates connection status after a connection test.
     *
     * @param status the connection status
     * @param message optional status message
     */
    public void updateConnectionStatus(String status, String message) {
        validateConnectionStatus(status);

        this.connectionStatus = status;
        this.lastConnectionTest = LocalDateTime.now();

        if ("connected".equals(status)) {
            this.errorCount = 0L;
            registerEvent(new DataSourceConnectedEvent(this.getId(), this.getName(), this.getSourceType()));
        } else {
            this.errorCount++;
        }
    }

    /**
     * Records a successful data fetch.
     */
    public void recordSuccessfulFetch() {
        this.lastDataFetch = LocalDateTime.now();
        this.successCount++;
        this.errorCount = 0L; // Reset error count on success
    }

    /**
     * Records a failed data fetch.
     */
    public void recordFailedFetch() {
        this.errorCount++;
    }

    /**
     * Activates the data source.
     */
    public void activate() {
        if (!this.isActive) {
            this.isActive = true;
            registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "activation"));
        }
    }

    /**
     * Deactivates the data source.
     */
    public void deactivate() {
        if (this.isActive) {
            this.isActive = false;
            this.connectionStatus = "inactive";
            registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "deactivation"));
        }
    }

    /**
     * Makes the data source public.
     */
    public void makePublic() {
        if (!this.isPublic) {
            this.isPublic = true;
            registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "public_access"));
        }
    }

    /**
     * Makes the data source private.
     */
    public void makePrivate() {
        if (this.isPublic) {
            this.isPublic = false;
            registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "private_access"));
        }
    }

    /**
     * Updates the API configuration.
     *
     * @param apiEndpoint the API endpoint
     * @param authenticationType the authentication type
     * @param apiKey the API key (encrypted)
     */
    public void updateApiConfig(String apiEndpoint, String authenticationType, String apiKey) {
        validateApiConfig(apiEndpoint, authenticationType);

        this.apiEndpoint = apiEndpoint;
        this.authenticationType = authenticationType;
        this.apiKey = apiKey;

        registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "api_config"));
    }

    /**
     * Updates the schema definition.
     *
     * @param schemaDefinition the schema definition
     */
    public void updateSchemaDefinition(Map<String, Object> schemaDefinition) {
        this.schemaDefinition = schemaDefinition;
        registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "schema"));
    }

    /**
     * Updates the data processing limits.
     *
     * @param maxRows the maximum rows to fetch
     * @param dataCacheTtl the cache TTL in seconds
     */
    public void updateDataLimits(Integer maxRows, Integer dataCacheTtl) {
        validateDataLimits(maxRows, dataCacheTtl);

        this.maxRows = maxRows;
        this.dataCacheTtl = dataCacheTtl;

        registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "data_limits"));
    }

    /**
     * Updates the tags.
     *
     * @param tags the new list of tags
     */
    public void updateTags(List<String> tags) {
        this.tags = tags;
        registerEvent(new DataSourceUpdatedEvent(this.getId(), this.getName(), this.getName(), "tags"));
    }

    /**
     * Deletes the data source.
     */
    public void delete() {
        registerEvent(new DataSourceDeletedEvent(this.getId(), this.getName(), this.getSourceType()));
    }

    /**
     * Checks if the data source is healthy based on error rate.
     *
     * @return true if healthy, false otherwise
     */
    public boolean isHealthy() {
        long totalRequests = this.successCount + this.errorCount;
        if (totalRequests == 0) {
            return true; // No requests yet
        }
        double errorRate = (double) this.errorCount / totalRequests;
        return errorRate < 0.1; // Less than 10% error rate
    }

    /**
     * Gets the success rate as a percentage.
     *
     * @return success rate percentage
     */
    public double getSuccessRate() {
        long totalRequests = this.successCount + this.errorCount;
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) this.successCount / totalRequests * 100;
    }

    // Private validation methods
    private static void validateDataSourceCreation(String name, String sourceType, Map<String, Object> connectionConfig) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Data source name cannot be null or empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Data source name cannot exceed 100 characters");
        }
        if (sourceType == null || sourceType.trim().isEmpty()) {
            throw new IllegalArgumentException("Data source type cannot be null or empty");
        }
        if (connectionConfig == null || connectionConfig.isEmpty()) {
            throw new IllegalArgumentException("Connection configuration cannot be null or empty");
        }
    }

    private void validateConfigurationUpdate(String name, Map<String, Object> connectionConfig) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Data source name cannot be null or empty");
        }
        if (connectionConfig == null || connectionConfig.isEmpty()) {
            throw new IllegalArgumentException("Connection configuration cannot be null or empty");
        }
    }

    private void validateRefreshSettings(Integer refreshInterval, Integer timeoutSeconds, Integer retryAttempts, Integer retryDelaySeconds) {
        if (refreshInterval != null && (refreshInterval < 10 || refreshInterval > 3600)) {
            throw new IllegalArgumentException("Refresh interval must be between 10 and 3600 seconds");
        }
        if (timeoutSeconds != null && (timeoutSeconds < 5 || timeoutSeconds > 300)) {
            throw new IllegalArgumentException("Timeout must be between 5 and 300 seconds");
        }
        if (retryAttempts != null && (retryAttempts < 0 || retryAttempts > 10)) {
            throw new IllegalArgumentException("Retry attempts must be between 0 and 10");
        }
        if (retryDelaySeconds != null && (retryDelaySeconds < 1 || retryDelaySeconds > 60)) {
            throw new IllegalArgumentException("Retry delay must be between 1 and 60 seconds");
        }
    }

    private void validateConnectionStatus(String status) {
        List<String> validStatuses = List.of("connected", "disconnected", "error", "pending", "inactive", "testing");
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException("Invalid connection status: " + status);
        }
    }

    private void validateApiConfig(String apiEndpoint, String authenticationType) {
        if (apiEndpoint != null && !apiEndpoint.startsWith("http")) {
            throw new IllegalArgumentException("API endpoint must start with http:// or https://");
        }
        if (authenticationType != null) {
            List<String> validTypes = List.of("none", "api_key", "oauth2", "basic", "bearer");
            if (!validTypes.contains(authenticationType)) {
                throw new IllegalArgumentException("Invalid authentication type: " + authenticationType);
            }
        }
    }

    private void validateDataLimits(Integer maxRows, Integer dataCacheTtl) {
        if (maxRows != null && (maxRows < 1 || maxRows > 100000)) {
            throw new IllegalArgumentException("Max rows must be between 1 and 100000");
        }
        if (dataCacheTtl != null && (dataCacheTtl < 0 || dataCacheTtl > 3600)) {
            throw new IllegalArgumentException("Cache TTL must be between 0 and 3600 seconds");
        }
    }
}