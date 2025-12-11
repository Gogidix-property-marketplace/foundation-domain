package com.gogidix.dashboard.centralized.infrastructure.repository;

import com.gogidix.dashboard.centralized.domain.datasource.DataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for DataSource entity operations.
 * Follows the hexagonal architecture pattern for data access.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, UUID> {

    /**
     * Finds data sources by owner ID.
     *
     * @param ownerId the owner's user ID
     * @param pageable pagination information
     * @return page of data sources owned by the user
     */
    Page<DataSource> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Finds active data sources by owner ID.
     *
     * @param ownerId the owner's user ID
     * @param pageable pagination information
     * @return page of active data sources owned by the user
     */
    Page<DataSource> findByOwnerIdAndIsActiveTrue(UUID ownerId, Pageable pageable);

    /**
     * Finds public data sources.
     *
     * @param pageable pagination information
     * @return page of public data sources
     */
    Page<DataSource> findByIsPublicTrue(Pageable pageable);

    /**
     * Finds public and active data sources.
     *
     * @param pageable pagination information
     * @return page of public and active data sources
     */
    Page<DataSource> findByIsPublicTrueAndIsActiveTrue(Pageable pageable);

    /**
     * Finds data sources by type.
     *
     * @param sourceType the data source type
     * @param pageable pagination information
     * @return page of data sources of the specified type
     */
    Page<DataSource> findBySourceType(String sourceType, Pageable pageable);

    /**
     * Finds active data sources by type.
     *
     * @param sourceType the data source type
     * @return list of active data sources of the specified type
     */
    List<DataSource> findBySourceTypeAndIsActiveTrue(String sourceType);

    /**
     * Finds data sources by connection status.
     *
     * @param connectionStatus the connection status
     * @return list of data sources with the specified status
     */
    List<DataSource> findByConnectionStatus(String connectionStatus);

    /**
     * Finds data sources that need connection testing.
     *
     * @param before the timestamp threshold
     * @return list of data sources that need testing
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.isActive = true AND (ds.lastConnectionTest < :before OR ds.lastConnectionTest IS NULL)")
    List<DataSource> findDataSourcesNeedingTest(@Param("before") LocalDateTime before);

    /**
     * Finds data sources with high error rates.
     *
     * @param errorThreshold the minimum error count
     * @return list of data sources with errors
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.errorCount >= :errorThreshold")
    List<DataSource> findDataSourcesWithErrors(@Param("errorThreshold") Long errorThreshold);

    /**
     * Searches data sources by name or description.
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of matching data sources
     */
    @Query("SELECT ds FROM DataSource ds WHERE (ds.name ILIKE %:searchTerm% OR ds.description ILIKE %:searchTerm%) AND ds.isActive = true")
    Page<DataSource> searchDataSources(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds data sources owned by a user that match a search term.
     *
     * @param ownerId the owner's user ID
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of matching data sources
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.ownerId = :ownerId AND (ds.name ILIKE %:searchTerm% OR ds.description ILIKE %:searchTerm%)")
    Page<DataSource> searchUserDataSources(@Param("ownerId") UUID ownerId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds data sources by tags.
     *
     * @param tags list of tags to match
     * @param pageable pagination information
     * @return page of data sources with matching tags
     */
    @Query("SELECT ds FROM DataSource ds JOIN ds.tags t WHERE t IN :tags AND ds.isActive = true")
    Page<DataSource> findByTagsContaining(@Param("tags") List<String> tags, Pageable pageable);

    /**
     * Counts data sources by owner.
     *
     * @param ownerId the owner's user ID
     * @return count of data sources owned by the user
     */
    @Query("SELECT COUNT(ds) FROM DataSource ds WHERE ds.ownerId = :ownerId")
    long countByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Counts active data sources by owner.
     *
     * @param ownerId the owner's user ID
     * @return count of active data sources owned by the user
     */
    @Query("SELECT COUNT(ds) FROM DataSource ds WHERE ds.ownerId = :ownerId AND ds.isActive = true")
    long countActiveByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Updates connection status and test timestamp.
     *
     * @param dataSourceId the data source ID
     * @param connectionStatus the new connection status
     * @param lastConnectionTest the test timestamp
     */
    @Modifying
    @Query("UPDATE DataSource ds SET ds.connectionStatus = :connectionStatus, ds.lastConnectionTest = :lastConnectionTest WHERE ds.id = :dataSourceId")
    void updateConnectionStatus(@Param("dataSourceId") UUID dataSourceId,
                               @Param("connectionStatus") String connectionStatus,
                               @Param("lastConnectionTest") LocalDateTime lastConnectionTest);

    /**
     * Updates fetch statistics.
     *
     * @param dataSourceId the data source ID
     * @param lastDataFetch the last fetch timestamp
     * @param successCount the success count
     * @param errorCount the error count
     */
    @Modifying
    @Query("UPDATE DataSource ds SET ds.lastDataFetch = :lastDataFetch, ds.successCount = :successCount, ds.errorCount = :errorCount WHERE ds.id = :dataSourceId")
    void updateFetchStats(@Param("dataSourceId") UUID dataSourceId,
                         @Param("lastDataFetch") LocalDateTime lastDataFetch,
                         @Param("successCount") Long successCount,
                         @Param("errorCount") Long errorCount);

    /**
     * Increments success count.
     *
     * @param dataSourceId the data source ID
     */
    @Modifying
    @Query("UPDATE DataSource ds SET ds.successCount = ds.successCount + 1, ds.lastDataFetch = CURRENT_TIMESTAMP WHERE ds.id = :dataSourceId")
    void incrementSuccessCount(@Param("dataSourceId") UUID dataSourceId);

    /**
     * Increments error count.
     *
     * @param dataSourceId the data source ID
     */
    @Modifying
    @Query("UPDATE DataSource ds SET ds.errorCount = ds.errorCount + 1 WHERE ds.id = :dataSourceId")
    void incrementErrorCount(@Param("dataSourceId") UUID dataSourceId);

    /**
     * Finds recently created data sources.
     *
     * @param since the timestamp threshold
     * @param pageable pagination information
     * @return page of recently created data sources
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.createdAt >= :since ORDER BY ds.createdAt DESC")
    Page<DataSource> findRecentlyCreated(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Gets data source statistics by type.
     *
     * @return list of data source type statistics
     */
    @Query("SELECT ds.sourceType, COUNT(ds) as count, AVG(ds.successCount) as avgSuccesses, AVG(ds.errorCount) as avgErrors FROM DataSource ds WHERE ds.isActive = true GROUP BY ds.sourceType")
    List<Object[]> getDataSourceStatisticsByType();

    /**
     * Gets connection status statistics.
     *
     * @return list of connection status statistics
     */
    @Query("SELECT ds.connectionStatus, COUNT(ds) as count FROM DataSource ds WHERE ds.isActive = true GROUP BY ds.connectionStatus")
    List<Object[]> getConnectionStatusStatistics();

    /**
     * Finds data sources by refresh interval.
     *
     * @param minInterval minimum refresh interval
     * @param maxInterval maximum refresh interval
     * @return list of data sources with specified refresh intervals
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.refreshInterval BETWEEN :minInterval AND :maxInterval AND ds.isActive = true")
    List<DataSource> findByRefreshIntervalRange(@Param("minInterval") Integer minInterval,
                                              @Param("maxInterval") Integer maxInterval);

    /**
     * Finds data sources with API configuration.
     *
     * @return list of data sources with API endpoints
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.apiEndpoint IS NOT NULL AND ds.isActive = true")
    List<DataSource> findApiDataSources();

    /**
     * Finds data sources by authentication type.
     *
     * @param authenticationType the authentication type
     * @return list of data sources with specified authentication
     */
    List<DataSource> findByAuthenticationType(String authenticationType);

    /**
     * Finds data sources that haven't been tested recently.
     *
     * @param hours the number of hours
     * @return list of data sources needing recent testing
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.isActive = true AND (ds.lastConnectionTest < :threshold OR ds.lastConnectionTest IS NULL)")
    List<DataSource> findDataSourcesNotTestedRecently(@Param("threshold") LocalDateTime threshold);

    /**
     * Finds healthy data sources.
     *
     * @return list of healthy data sources
     */
    @Query("SELECT ds FROM DataSource ds WHERE ds.isActive = true AND ds.connectionStatus = 'connected' AND ds.errorCount < 10")
    List<DataSource> findHealthyDataSources();

    /**
     * Gets data source usage statistics.
     *
     * @return list of usage statistics
     */
    @Query("SELECT ds.id, ds.name, ds.successCount, ds.errorCount, ds.lastDataFetch FROM DataSource ds WHERE ds.isActive = true ORDER BY ds.successCount DESC")
    List<Object[]> getDataSourceUsageStatistics();
}