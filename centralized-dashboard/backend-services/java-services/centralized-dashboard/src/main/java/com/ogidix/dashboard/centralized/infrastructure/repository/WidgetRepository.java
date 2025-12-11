package com.gogidix.dashboard.centralized.infrastructure.repository;

import com.gogidix.dashboard.centralized.domain.widget.Widget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Widget entities.
 * Provides comprehensive data access methods for widget management.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Repository
public interface WidgetRepository extends JpaRepository<Widget, UUID> {

    /**
     * Find all widgets belonging to a specific dashboard.
     *
     * @param dashboardId the dashboard ID
     * @return List of widgets on the dashboard
     */
    List<Widget> findByDashboardId(UUID dashboardId);

    /**
     * Find all widgets belonging to a specific dashboard ordered by position.
     *
     * @param dashboardId the dashboard ID
     * @return List of widgets on the dashboard ordered by position
     */
    @Query("SELECT w FROM Widget w WHERE w.dashboardId = :dashboardId ORDER BY w.positionY, w.positionX")
    List<Widget> findAllByDashboardIdOrderByPosition(@Param("dashboardId") UUID dashboardId);

    /**
     * Find widget by name within a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param name the widget name
     * @return Optional containing the widget if found
     */
    Optional<Widget> findByDashboardIdAndName(UUID dashboardId, String name);

    /**
     * Find all active widgets.
     *
     * @return List of active widgets
     */
    List<Widget> findByIsActiveTrue();

    /**
     * Find all visible widgets.
     *
     * @return List of visible widgets
     */
    List<Widget> findByIsVisibleTrue();

    /**
     * Find all active and visible widgets for a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @return List of active and visible widgets
     */
    @Query("SELECT w FROM Widget w WHERE w.dashboardId = :dashboardId AND w.isActive = true AND w.isVisible = true")
    List<Widget> findActiveVisibleWidgets(@Param("dashboardId") UUID dashboardId);

    /**
     * Find widgets by type.
     *
     * @param widgetType the widget type
     * @return List of widgets of the specified type
     */
    List<Widget> findByWidgetType(String widgetType);

    /**
     * Find widgets by data source.
     *
     * @param dataSource the data source
     * @return List of widgets using the specified data source
     */
    List<Widget> findByDataSource(String dataSource);

    /**
     * Find widgets by type within a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param widgetType the widget type
     * @return List of widgets of the specified type on the dashboard
     */
    List<Widget> findByDashboardIdAndWidgetType(UUID dashboardId, String widgetType);

    /**
     * Find widgets with specific refresh interval.
     *
     * @param refreshInterval the refresh interval in seconds
     * @return List of widgets with the specified refresh interval
     */
    List<Widget> findByRefreshInterval(Integer refreshInterval);

    /**
     * Find widgets that need refreshing (last refreshed before threshold).
     *
     * @param threshold the time threshold
     * @return List of widgets that need refreshing
     */
    @Query("SELECT w FROM Widget w WHERE w.isActive = true AND " +
           "(w.lastRefreshedAt IS NULL OR w.lastRefreshedAt < :threshold)")
    List<Widget> findWidgetsNeedingRefresh(@Param("threshold") java.time.LocalDateTime threshold);

    /**
     * Count widgets on a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @return Number of widgets on the dashboard
     */
    Long countByDashboardId(UUID dashboardId);

    /**
     * Count widgets by type.
     *
     * @param widgetType the widget type
     * @return Number of widgets of the specified type
     */
    Long countByWidgetType(String widgetType);

    /**
     * Search widgets by name or description.
     *
     * @param searchTerm the search term
     * @return List of matching widgets
     */
    @Query("SELECT w FROM Widget w WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(w.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Widget> searchWidgets(@Param("searchTerm") String searchTerm);

    /**
     * Find widgets at a specific position.
     *
     * @param dashboardId the dashboard ID
     * @param positionX the x position
     * @param positionY the y position
     * @return List of widgets at the specified position
     */
    List<Widget> findByDashboardIdAndPositionXAndPositionY(UUID dashboardId, Integer positionX, Integer positionY);

    /**
     * Update widget position.
     *
     * @param widgetId the widget ID
     * @param positionX the new x position
     * @param positionY the new y position
     */
    @Query(value = "UPDATE widgets SET position_x = :x, position_y = :y WHERE id = :widgetId", nativeQuery = true)
    void updateWidgetPosition(@Param("widgetId") UUID widgetId, @Param("x") Integer positionX, @Param("y") Integer positionY);

    /**
     * Update widget size.
     *
     * @param widgetId the widget ID
     * @param width the new width
     * @param height the new height
     */
    @Query(value = "UPDATE widgets SET width = :width, height = :height WHERE id = :widgetId", nativeQuery = true)
    void updateWidgetSize(@Param("widgetId") UUID widgetId, @Param("width") Integer width, @Param("height") Integer height);

    /**
     * Update last refresh timestamp.
     *
     * @param widgetId the widget ID
     * @param timestamp the current timestamp
     */
    @Query(value = "UPDATE widgets SET last_refreshed_at = :timestamp WHERE id = :widgetId", nativeQuery = true)
    void updateLastRefreshedAt(@Param("widgetId") UUID widgetId, @Param("timestamp") java.time.LocalDateTime timestamp);

    /**
     * Find widgets in a specific area of a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param startX the starting x position
     * @param startY the starting y position
     * @param endX the ending x position
     * @param endY the ending y position
     * @return List of widgets in the specified area
     */
    @Query(value = "SELECT * FROM widgets WHERE dashboard_id = :dashboardId " +
                   "AND position_x >= :startX AND position_y >= :startY " +
                   "AND position_x < :endX AND position_y < :endY", nativeQuery = true)
    List<Widget> findWidgetsInArea(@Param("dashboardId") UUID dashboardId,
                                   @Param("startX") Integer startX, @Param("startY") Integer startY,
                                   @Param("endX") Integer endX, @Param("endY") Integer endY);
}