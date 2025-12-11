package com.gogidix.dashboard.centralized.infrastructure.repository;

import com.gogidix.dashboard.centralized.domain.widget.Widget;
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
 * Repository interface for Widget entity operations.
 * Follows the hexagonal architecture pattern for data access.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Repository
public interface WidgetRepository extends JpaRepository<Widget, UUID> {

    /**
     * Finds widgets by dashboard ID.
     *
     * @param dashboardId the dashboard ID
     * @param pageable pagination information
     * @return page of widgets in the dashboard
     */
    Page<Widget> findByDashboardId(UUID dashboardId, Pageable pageable);

    /**
     * Finds all widgets by dashboard ID (no pagination).
     *
     * @param dashboardId the dashboard ID
     * @return list of all widgets in the dashboard
     */
    @Query("SELECT w FROM Widget w WHERE w.dashboardId = :dashboardId ORDER BY w.zIndex, w.positionY, w.positionX")
    List<Widget> findAllByDashboardIdOrderByPosition(@Param("dashboardId") UUID dashboardId);

    /**
     * Finds visible widgets by dashboard ID.
     *
     * @param dashboardId the dashboard ID
     * @return list of visible widgets in the dashboard
     */
    @Query("SELECT w FROM Widget w WHERE w.dashboardId = :dashboardId AND w.isVisible = true ORDER BY w.zIndex, w.positionY, w.positionX")
    List<Widget> findVisibleByDashboardId(@Param("dashboardId") UUID dashboardId);

    /**
     * Finds enabled widgets by dashboard ID.
     *
     * @param dashboardId the dashboard ID
     * @return list of enabled widgets in the dashboard
     */
    @Query("SELECT w FROM Widget w WHERE w.dashboardId = :dashboardId AND w.isEnabled = true ORDER BY w.zIndex")
    List<Widget> findEnabledByDashboardId(@Param("dashboardId") UUID dashboardId);

    /**
     * Finds widgets by dashboard ID and widget type.
     *
     * @param dashboardId the dashboard ID
     * @param widgetType the widget type
     * @return list of widgets of the specified type in the dashboard
     */
    List<Widget> findByDashboardIdAndWidgetType(UUID dashboardId, String widgetType);

    /**
     * Finds widgets by data source ID.
     *
     * @param dataSourceId the data source ID
     * @return list of widgets using the specified data source
     */
    List<Widget> findByDataSourceId(UUID dataSourceId);

    /**
     * Finds widgets that need refreshing.
     *
     * @param before the timestamp threshold
     * @return list of widgets that need data refresh
     */
    @Query("SELECT w FROM Widget w WHERE w.isEnabled = true AND (w.lastDataRefresh < :before OR w.lastDataRefresh IS NULL)")
    List<Widget> findWidgetsNeedingRefresh(@Param("before") LocalDateTime before);

    /**
     * Finds widgets with errors.
     *
     * @param errorThreshold the minimum error count
     * @return list of widgets with errors
     */
    @Query("SELECT w FROM Widget w WHERE w.errorCount >= :errorThreshold")
    List<Widget> findWidgetsWithErrors(@Param("errorThreshold") Long errorThreshold);

    /**
     * Counts widgets by dashboard.
     *
     * @param dashboardId the dashboard ID
     * @return count of widgets in the dashboard
     */
    @Query("SELECT COUNT(w) FROM Widget w WHERE w.dashboardId = :dashboardId")
    long countByDashboardId(@Param("dashboardId") UUID dashboardId);

    /**
     * Counts visible widgets by dashboard.
     *
     * @param dashboardId the dashboard ID
     * @return count of visible widgets in the dashboard
     */
    @Query("SELECT COUNT(w) FROM Widget w WHERE w.dashboardId = :dashboardId AND w.isVisible = true")
    long countVisibleByDashboardId(@Param("dashboardId") UUID dashboardId);

    /**
     * Counts enabled widgets by dashboard.
     *
     * @param dashboardId the dashboard ID
     * @return count of enabled widgets in the dashboard
     */
    @Query("SELECT COUNT(w) FROM Widget w WHERE w.dashboardId = :dashboardId AND w.isEnabled = true")
    long countEnabledByDashboardId(@Param("dashboardId") UUID dashboardId);

    /**
     * Finds widgets in a specific position range.
     *
     * @param dashboardId the dashboard ID
     * @param startX starting X position
     * @param endX ending X position
     * @param startY starting Y position
     * @param endY ending Y position
     * @return list of widgets in the position range
     */
    @Query("SELECT w FROM Widget w WHERE w.dashboardId = :dashboardId AND w.positionX >= :startX AND w.positionX <= :endX AND w.positionY >= :startY AND w.positionY <= :endY")
    List<Widget> findByDashboardIdAndPositionRange(@Param("dashboardId") UUID dashboardId,
                                                   @Param("startX") Integer startX,
                                                   @Param("endX") Integer endX,
                                                   @Param("startY") Integer startY,
                                                   @Param("endY") Integer endY);

    /**
     * Updates widget position.
     *
     * @param widgetId the widget ID
     * @param positionX the X position
     * @param positionY the Y position
     * @param zIndex the Z-index
     */
    @Modifying
    @Query("UPDATE Widget w SET w.positionX = :positionX, w.positionY = :positionY, w.zIndex = :zIndex WHERE w.id = :widgetId")
    void updateWidgetPosition(@Param("widgetId") UUID widgetId,
                             @Param("positionX") Integer positionX,
                             @Param("positionY") Integer positionY,
                             @Param("zIndex") Integer zIndex);

    /**
     * Updates widget refresh statistics.
     *
     * @param widgetId the widget ID
     * @param lastDataRefresh the last refresh time
     * @param refreshCount the refresh count
     */
    @Modifying
    @Query("UPDATE Widget w SET w.lastDataRefresh = :lastDataRefresh, w.refreshCount = :refreshCount WHERE w.id = :widgetId")
    void updateRefreshStats(@Param("widgetId") UUID widgetId,
                          @Param("lastDataRefresh") LocalDateTime lastDataRefresh,
                          @Param("refreshCount") Long refreshCount);

    /**
     * Increments widget error count.
     *
     * @param widgetId the widget ID
     */
    @Modifying
    @Query("UPDATE Widget w SET w.errorCount = w.errorCount + 1 WHERE w.id = :widgetId")
    void incrementErrorCount(@Param("widgetId") UUID widgetId);

    /**
     * Resets widget error count.
     *
     * @param widgetId the widget ID
     */
    @Modifying
    @Query("UPDATE Widget w SET w.errorCount = 0 WHERE w.id = :widgetId")
    void resetErrorCount(@Param("widgetId") UUID widgetId);

    /**
     * Finds widgets by name pattern.
     *
     * @param dashboardId the dashboard ID
     * @param namePattern the name pattern to search for
     * @return list of matching widgets
     */
    @Query("SELECT w FROM Widget w WHERE w.dashboardId = :dashboardId AND w.name ILIKE %:namePattern%")
    List<Widget> findByNamePattern(@Param("dashboardId") UUID dashboardId,
                                 @Param("namePattern") String namePattern);

    /**
     * Gets widget statistics by type.
     *
     * @return list of widget type statistics
     */
    @Query("SELECT w.widgetType, COUNT(w) as count FROM Widget w GROUP BY w.widgetType")
    List<Object[]> getWidgetStatisticsByType();

    /**
     * Gets widget statistics for a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @return list of dashboard widget statistics
     */
    @Query("SELECT w.widgetType, COUNT(w) as count, SUM(w.refreshCount) as totalRefreshes, SUM(w.errorCount) as totalErrors FROM Widget w WHERE w.dashboardId = :dashboardId GROUP BY w.widgetType")
    List<Object[]> getDashboardWidgetStatistics(@Param("dashboardId") UUID dashboardId);

    /**
     * Finds recently updated widgets.
     *
     * @param dashboardId the dashboard ID
     * @param since the timestamp threshold
     * @return list of recently updated widgets
     */
    @Query("SELECT w FROM Widget w WHERE w.dashboardId = :dashboardId AND w.updatedAt >= :since ORDER BY w.updatedAt DESC")
    List<Widget> findRecentlyUpdated(@Param("dashboardId") UUID dashboardId, @Param("since") LocalDateTime since);

    /**
     * Finds widgets with refresh intervals.
     *
     * @param minInterval minimum refresh interval
     * @param maxInterval maximum refresh interval
     * @return list of widgets with specified refresh intervals
     */
    @Query("SELECT w FROM Widget w WHERE w.refreshInterval BETWEEN :minInterval AND :maxInterval AND w.isEnabled = true")
    List<Widget> findByRefreshIntervalRange(@Param("minInterval") Integer minInterval,
                                          @Param("maxInterval") Integer maxInterval);
}