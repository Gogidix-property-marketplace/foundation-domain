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
 * Spring Data JPA repository for Dashboard entities.
 * Provides comprehensive data access methods for dashboard management.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {

    /**
     * Find dashboard by name.
     *
     * @param name the dashboard name
     * @return Optional containing the dashboard if found
     */
    Optional<Dashboard> findByName(String name);

    /**
     * Find all dashboards owned by a specific user.
     *
     * @param ownerId the owner's user ID
     * @return List of dashboards owned by the user
     */
    List<Dashboard> findByOwnerId(UUID ownerId);

    /**
     * Find all public dashboards.
     *
     * @return List of public dashboards
     */
    List<Dashboard> findByIsPublicTrue();

    /**
     * Find all active dashboards.
     *
     * @return List of active dashboards
     */
    List<Dashboard> findByIsActiveTrue();

    /**
     * Find dashboards owned by a user that are accessible (owner or public).
     *
     * @param userId the user ID
     * @return List of accessible dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.ownerId = :userId OR d.isPublic = true")
    List<Dashboard> findAccessibleDashboards(@Param("userId") UUID userId);

    /**
     * Find dashboards by category.
     *
     * @param category the category
     * @return List of dashboards in the category
     */
    List<Dashboard> findByCategory(String category);

    /**
     * Find dashboards by tags containing a specific tag.
     *
     * @param tag the tag to search for
     * @return List of dashboards with the specified tag
     */
    @Query("SELECT d FROM Dashboard d WHERE :tag MEMBER OF d.tags")
    List<Dashboard> findByTag(@Param("tag") String tag);

    /**
     * Find dashboards created after a specific date.
     *
     * @param date the creation date threshold
     * @return List of dashboards created after the date
     */
    List<Dashboard> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find dashboards last accessed after a specific date.
     *
     * @param date the last access date threshold
     * @return List of dashboards last accessed after the date
     */
    List<Dashboard> findByLastAccessedAtAfter(LocalDateTime date);

    /**
     * Count dashboards by owner.
     *
     * @param ownerId the owner's user ID
     * @return Number of dashboards owned by the user
     */
    Long countByOwnerId(UUID ownerId);

    /**
     * Count public dashboards.
     *
     * @return Number of public dashboards
     */
    Long countByIsPublicTrue();

    /**
     * Search dashboards by name or description.
     *
     * @param searchTerm the search term
     * @return List of matching dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Dashboard> searchDashboards(@Param("searchTerm") String searchTerm);

    /**
     * Find most recently accessed dashboards.
     *
     * @param userId the user ID
     * @param limit the maximum number of results
     * @return List of recently accessed dashboards
     */
    @Query(value = "SELECT * FROM dashboards WHERE owner_id = :userId OR is_public = true " +
                   "ORDER BY last_accessed_at DESC LIMIT :limit", nativeQuery = true)
    List<Dashboard> findRecentlyAccessedDashboards(@Param("userId") UUID userId, @Param("limit") int limit);

    /**
     * Find most viewed dashboards.
     *
     * @param limit the maximum number of results
     * @return List of most viewed dashboards
     */
    @Query(value = "SELECT * FROM dashboards WHERE is_public = true " +
                   "ORDER BY view_count DESC LIMIT :limit", nativeQuery = true)
    List<Dashboard> findMostViewedDashboards(@Param("limit") int limit);

    /**
     * Update view count for a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param currentTimestamp the current timestamp
     */
    @Query(value = "UPDATE dashboards SET view_count = view_count + 1, last_accessed_at = :timestamp " +
                   "WHERE id = :dashboardId", nativeQuery = true)
    void incrementViewCount(@Param("dashboardId") UUID dashboardId, @Param("timestamp") LocalDateTime currentTimestamp);

    /**
     * Find inactive dashboards (not accessed for more than specified days).
     *
     * @param days the number of days
     * @return List of inactive dashboards
     */
    @Query(value = "SELECT * FROM dashboards WHERE last_accessed_at < DATE_SUB(NOW(), INTERVAL :days DAY)", nativeQuery = true)
    List<Dashboard> findInactiveDashboards(@Param("days") int days);

    // Additional methods needed by DashboardService

    /**
     * Find dashboards by owner and active status with pagination.
     *
     * @param ownerId the owner's user ID
     * @param isActive the active status
     * @param pageable pagination information
     * @return Page of dashboards
     */
    Page<Dashboard> findByOwnerIdAndIsActive(UUID ownerId, boolean isActive, Pageable pageable);

    /**
     * Find public and active dashboards with pagination.
     *
     * @param pageable pagination information
     * @return Page of public active dashboards
     */
    Page<Dashboard> findByIsPublicTrueAndIsActiveTrue(Pageable pageable);

    /**
     * Search dashboards with pagination.
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return Page of matching dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Dashboard> searchDashboards(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count active dashboards by owner.
     *
     * @param ownerId the owner's user ID
     * @return Number of active dashboards owned by the user
     */
    @Query("SELECT COUNT(d) FROM Dashboard d WHERE d.ownerId = :ownerId AND d.isActive = true")
    Long countActiveByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Find popular dashboards with pagination.
     *
     * @param pageable pagination information
     * @return Page of popular dashboards
     */
    @Query(value = "SELECT * FROM dashboards WHERE is_public = true AND is_active = true " +
                   "ORDER BY view_count DESC", nativeQuery = true)
    Page<Dashboard> findPopularDashboards(Pageable pageable);
}