package com.gogidix.dashboard.centralized.infrastructure.repository;

import com.gogidix.dashboard.centralized.domain.dashboard.Dashboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Dashboard entity operations.
 * Follows the hexagonal architecture pattern for data access.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {

    /**
     * Finds dashboards by owner ID.
     *
     * @param ownerId the owner's user ID
     * @param pageable pagination information
     * @return page of dashboards owned by the user
     */
    Page<Dashboard> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Finds dashboards by owner ID and active status.
     *
     * @param ownerId the owner's user ID
     * @param isActive the active status
     * @param pageable pagination information
     * @return page of active dashboards owned by the user
     */
    Page<Dashboard> findByOwnerIdAndIsActive(UUID ownerId, Boolean isActive, Pageable pageable);

    /**
     * Finds public dashboards.
     *
     * @param pageable pagination information
     * @return page of public dashboards
     */
    Page<Dashboard> findByIsPublicTrue(Pageable pageable);

    /**
     * Finds public and active dashboards.
     *
     * @param pageable pagination information
     * @return page of public and active dashboards
     */
    Page<Dashboard> findByIsPublicTrueAndIsActiveTrue(Pageable pageable);

    /**
     * Finds dashboards by category.
     *
     * @param category the dashboard category
     * @param pageable pagination information
     * @return page of dashboards in the specified category
     */
    Page<Dashboard> findByCategory(String category, Pageable pageable);

    /**
     * Finds dashboards by multiple categories.
     *
     * @param categories list of categories
     * @param pageable pagination information
     * @return page of dashboards in the specified categories
     */
    @Query("SELECT d FROM Dashboard d WHERE d.category IN :categories AND d.isActive = true")
    Page<Dashboard> findByCategoriesAndIsActive(@Param("categories") List<String> categories, Pageable pageable);

    /**
     * Searches dashboards by name or description.
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of matching dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE (d.name ILIKE %:searchTerm% OR d.description ILIKE %:searchTerm%) AND d.isActive = true")
    Page<Dashboard> searchDashboards(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds dashboards owned by a user that match a search term.
     *
     * @param ownerId the owner's user ID
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of matching dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.ownerId = :ownerId AND (d.name ILIKE %:searchTerm% OR d.description ILIKE %:searchTerm%)")
    Page<Dashboard> searchUserDashboards(@Param("ownerId") UUID ownerId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds recently updated dashboards.
     *
     * @param since the timestamp to search from
     * @param pageable pagination information
     * @return page of recently updated dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.updatedAt >= :since AND d.isActive = true ORDER BY d.updatedAt DESC")
    Page<Dashboard> findRecentlyUpdated(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Finds popular dashboards based on access count.
     *
     * @param pageable pagination information
     * @return page of popular dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.isPublic = true AND d.isActive = true ORDER BY d.accessCount DESC")
    Page<Dashboard> findPopularDashboards(Pageable pageable);

    /**
     * Finds dashboards by tags.
     *
     * @param tags list of tags to match
     * @param pageable pagination information
     * @return page of dashboards with matching tags
     */
    @Query("SELECT d FROM Dashboard d JOIN d.tags t WHERE t IN :tags AND d.isActive = true")
    Page<Dashboard> findByTagsContaining(@Param("tags") List<String> tags, Pageable pageable);

    /**
     * Counts dashboards by owner.
     *
     * @param ownerId the owner's user ID
     * @return count of dashboards owned by the user
     */
    @Query("SELECT COUNT(d) FROM Dashboard d WHERE d.ownerId = :ownerId")
    long countByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Counts active dashboards by owner.
     *
     * @param ownerId the owner's user ID
     * @return count of active dashboards owned by the user
     */
    @Query("SELECT COUNT(d) FROM Dashboard d WHERE d.ownerId = :ownerId AND d.isActive = true")
    long countActiveByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Finds dashboards with widget count below maximum.
     *
     * @param maxWidgets the maximum widget count
     * @param pageable pagination information
     * @return page of dashboards that can accept more widgets
     */
    @Query("SELECT d FROM Dashboard d WHERE d.widgetCount < d.maxWidgets AND d.isActive = true")
    Page<Dashboard> findDashboardsWithCapacity(Pageable pageable, @Param("maxWidgets") Integer maxWidgets);

    /**
     * Finds dashboards that haven't been accessed recently.
     *
     * @param before the timestamp threshold
     * @return list of inactive dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.lastAccessedAt < :before OR d.lastAccessedAt IS NULL")
    List<Dashboard> findInactiveDashboards(@Param("before") LocalDateTime before);

    /**
     * Updates the access statistics for a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param lastAccessedAt the last access time
     * @param accessCount the new access count
     */
    @Query("UPDATE Dashboard d SET d.lastAccessedAt = :lastAccessedAt, d.accessCount = :accessCount WHERE d.id = :dashboardId")
    void updateAccessStats(@Param("dashboardId") UUID dashboardId, @Param("lastAccessedAt") LocalDateTime lastAccessedAt, @Param("accessCount") Long accessCount);

    /**
     * Finds dashboards by theme.
     *
     * @param theme the dashboard theme
     * @param pageable pagination information
     * @return page of dashboards with specified theme
     */
    Page<Dashboard> findByTheme(String theme, Pageable pageable);

    /**
     * Gets dashboard statistics by category.
     *
     * @return list of category statistics
     */
    @Query("SELECT d.category, COUNT(d) as count, AVG(d.accessCount) as avgAccess FROM Dashboard d WHERE d.isActive = true GROUP BY d.category")
    List<Object[]> getDashboardStatisticsByCategory();

    /**
     * Finds dashboards created within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination information
     * @return page of dashboards created in the date range
     */
    @Query("SELECT d FROM Dashboard d WHERE d.createdAt BETWEEN :startDate AND :endDate")
    Page<Dashboard> findByCreationDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
}