package com.gogidix.infrastructure.ai.repository;

import com.gogidix.infrastructure.ai.entity.PropertyDescription;
import com.gogidix.platform.common.core.dto.PaginationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Property Description entities
 */
@Repository
public interface PropertyDescriptionRepository extends
        JpaRepository<PropertyDescription, String>,
        JpaSpecificationExecutor<PropertyDescription> {

    /**
     * Find description by property ID
     */
    Optional<PropertyDescription> findByPropertyIdAndIsActiveTrue(String propertyId);

    /**
     * Find all descriptions for a property
     */
    List<PropertyDescription> findByPropertyIdOrderByCreatedAtDesc(String propertyId);

    /**
     * Find descriptions by property type
     */
    Page<PropertyDescription> findByPropertyTypeAndIsActiveTrue(String propertyType, Pageable pageable);

    /**
     * Find descriptions by location
     */
    Page<PropertyDescription> findByLocationContainingIgnoreCaseAndIsActiveTrue(String location, Pageable pageable);

    /**
     * Find descriptions with quality score above threshold
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.qualityScore >= :minScore AND pd.isActive = true")
    Page<PropertyDescription> findByMinQualityScore(@Param("minScore") Double minScore, Pageable pageable);

    /**
     * Find descriptions by model used
     */
    Page<PropertyDescription> findByModelUsedAndIsActiveTrue(String modelUsed, Pageable pageable);

    /**
     * Find optimized descriptions
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.isOptimized = true AND pd.isActive = true")
    Page<PropertyDescription> findOptimizedDescriptions(Pageable pageable);

    /**
     * Find published descriptions
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.isPublished = true AND pd.isActive = true")
    Page<PropertyDescription> findPublishedDescriptions(Pageable pageable);

    /**
     * Find template descriptions
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.isTemplate = true AND pd.isActive = true")
    List<PropertyDescription> findTemplateDescriptions();

    /**
     * Find templates by category
     */
    List<PropertyDescription> findByIsTemplateTrueAndTemplateCategoryAndIsActiveTrue(String category);

    /**
     * Find descriptions created in date range
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.createdAt BETWEEN :startDate AND :endDate AND pd.isActive = true")
    Page<PropertyDescription> findByCreationDateRange(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     Pageable pageable);

    /**
     * Find descriptions by language
     */
    Page<PropertyDescription> findByLanguageAndIsActiveTrue(String language, Pageable pageable);

    /**
     * Find descriptions by target audience
     */
    Page<PropertyDescription> findByTargetAudienceAndIsActiveTrue(String targetAudience, Pageable pageable);

    /**
     * Find descriptions with A/B test data
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.abTestId IS NOT NULL AND pd.isActive = true")
    List<PropertyDescription> findABTestDescriptions();

    /**
     * Find descriptions by A/B test group
     */
    List<PropertyDescription> findByAbTestIdAndExperimentGroupAndIsActiveTrue(String abTestId, String group);

    /**
     * Get quality score statistics
     */
    @Query("SELECT MIN(pd.qualityScore), MAX(pd.qualityScore), AVG(pd.qualityScore), COUNT(pd) " +
           "FROM PropertyDescription pd WHERE pd.isActive = true")
    Object[] getQualityScoreStatistics();

    /**
     * Get average quality score by property type
     */
    @Query("SELECT pd.propertyType, AVG(pd.qualityScore), COUNT(pd) " +
           "FROM PropertyDescription pd WHERE pd.isActive = true " +
           "GROUP BY pd.propertyType")
    List<Object[]> getAverageQualityScoreByPropertyType();

    /**
     * Get most used models
     */
    @Query("SELECT pd.modelUsed, COUNT(pd) as usage " +
           "FROM PropertyDescription pd WHERE pd.isActive = true " +
           "GROUP BY pd.modelUsed " +
           "ORDER BY usage DESC")
    List<Object[]> getMostUsedModels();

    /**
     * Find high-performing descriptions
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.qualityScore >= 80 AND pd.engagementScore >= 70 AND pd.isActive = true")
    Page<PropertyDescription> findHighPerformingDescriptions(Pageable pageable);

    /**
     * Find descriptions needing optimization
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.qualityScore < 70 AND pd.isOptimized = false AND pd.isActive = true")
    List<PropertyDescription> findDescriptionsNeedingOptimization();

    /**
     * Get performance metrics by description
     */
    @Query("SELECT pd.id, pd.primaryDescription, pd.viewCount, pd.clickCount, pd.inquiryCount, pd.conversionCount " +
           "FROM PropertyDescription pd WHERE pd.isActive = true AND pd.viewCount > 0")
    List<Object[]> getPerformanceMetrics();

    /**
     * Find recently created descriptions
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.createdAt >= :since AND pd.isActive = true")
    Page<PropertyDescription> findRecentlyCreated(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Search descriptions by text content
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE " +
           "(LOWER(pd.primaryDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(pd.optimizedDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND pd.isActive = true")
    Page<PropertyDescription> searchByContent(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Get description count by property
     */
    @Query("SELECT pd.propertyId, COUNT(pd) as descriptionCount " +
           "FROM PropertyDescription pd WHERE pd.isActive = true " +
           "GROUP BY pd.propertyId " +
           "ORDER BY descriptionCount DESC")
    List<Object[]> getDescriptionCountByProperty();

    /**
     * Custom query with filter support
     */
    @Query(value = "SELECT pd FROM PropertyDescription pd WHERE " +
           "(:propertyId IS NULL OR pd.propertyId = :propertyId) AND " +
           "(:propertyType IS NULL OR pd.propertyType = :propertyType) AND " +
           "(:location IS NULL OR LOWER(pd.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:minQualityScore IS NULL OR pd.qualityScore >= :minQualityScore) AND " +
           "(:maxQualityScore IS NULL OR pd.qualityScore <= :maxQualityScore) AND " +
           "(:language IS NULL OR pd.language = :language) AND " +
           "(:isOptimized IS NULL OR pd.isOptimized = :isOptimized) AND " +
           "(:isPublished IS NULL OR pd.isPublished = :isPublished) AND " +
           "pd.isActive = true")
    Page<PropertyDescription> findWithFilters(
            @Param("propertyId") String propertyId,
            @Param("propertyType") String propertyType,
            @Param("location") String location,
            @Param("minQualityScore") Double minQualityScore,
            @Param("maxQualityScore") Double maxQualityScore,
            @Param("language") String language,
            @Param("isOptimized") Boolean isOptimized,
            @Param("isPublished") Boolean isPublished,
            Pageable pageable);

    /**
     * Find descriptions with cache keys
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.cacheKey IS NOT NULL AND pd.isActive = true")
    List<PropertyDescription> findCachedDescriptions();

    /**
     * Clear expired cache entries
     */
    @Query("UPDATE PropertyDescription pd SET pd.cacheKey = NULL WHERE pd.updatedAt < :expiryDate")
    int clearExpiredCacheEntries(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Find descriptions by parent
     */
    List<PropertyDescription> findByParentDescriptionIdAndIsActiveTrue(String parentDescriptionId);

    /**
     * Get description versions for a property
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.propertyId = :propertyId AND pd.isActive = true " +
           "ORDER BY pd.version DESC, pd.createdAt DESC")
    List<PropertyDescription> getDescriptionVersions(@Param("propertyId") String propertyId);

    /**
     * Find descriptions with performance data
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.performanceMetrics IS NOT NULL AND pd.isActive = true")
    Page<PropertyDescription> findDescriptionsWithPerformanceData(Pageable pageable);

    /**
     * Get cost analysis
     */
    @Query("SELECT SUM(pd.costEstimate), AVG(pd.costEstimate), COUNT(pd) " +
           "FROM PropertyDescription pd WHERE pd.costEstimate IS NOT NULL AND pd.isActive = true")
    Object[] getCostAnalysis();

    /**
     * Find descriptions by region
     */
    Page<PropertyDescription> findByRegionAndIsActiveTrue(String region, Pageable pageable);

    /**
     * Find descriptions by market segment
     */
    Page<PropertyDescription> findByMarketSegmentAndIsActiveTrue(String marketSegment, Pageable pageable);

    /**
     * Find descriptions with tags
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.tags LIKE CONCAT('%', :tag, '%') AND pd.isActive = true")
    Page<PropertyDescription> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * Get trending descriptions (high engagement in recent period)
     */
    @Query("SELECT pd FROM PropertyDescription pd WHERE pd.createdAt >= :since AND " +
           "(pd.clickCount > 10 OR pd.shareCount > 5 OR pd.inquiryCount > 2) AND pd.isActive = true " +
           "ORDER BY (pd.clickCount + pd.shareCount * 2 + pd.inquiryCount * 5) DESC")
    Page<PropertyDescription> findTrendingDescriptions(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Find duplicate descriptions
     */
    @Query("SELECT pd1, pd2 FROM PropertyDescription pd1 JOIN PropertyDescription pd2 ON " +
           "pd1.propertyId = pd2.propertyId AND pd1.id != pd2.id AND " +
           "pd1.primaryDescription = pd2.primaryDescription AND pd1.isActive = true AND pd2.isActive = true")
    List<Object[]> findDuplicateDescriptions();

    /**
     * Get description statistics dashboard
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total_descriptions, " +
           "AVG(quality_score) as avg_quality_score, " +
           "COUNT(CASE WHEN is_optimized = true THEN 1 END) as optimized_count, " +
           "COUNT(CASE WHEN is_published = true THEN 1 END) as published_count, " +
           "SUM(view_count) as total_views, " +
           "SUM(click_count) as total_clicks " +
           "FROM ai_property_descriptions WHERE is_active = true", nativeQuery = true)
    Object[] getDescriptionDashboard();
}