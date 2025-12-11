package com.gogidix.infrastructure.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing AI-generated property descriptions
 */
@Entity
@Table(name = "ai_property_descriptions")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDescription {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "property_id", nullable = false, length = 100)
    private String propertyId;

    @Column(name = "property_type", nullable = false, length = 50)
    private String propertyType;

    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @Column(name = "primary_description", columnDefinition = "TEXT")
    private String primaryDescription;

    @Column(name = "optimized_description", columnDefinition = "TEXT")
    private String optimizedDescription;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "optimization_score")
    private Double optimizationScore;

    @Column(name = "seo_score")
    private Double seoScore;

    @Column(name = "readability_score")
    private Double readabilityScore;

    @Column(name = "engagement_score")
    private Double engagementScore;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "model_used", length = 50)
    private String modelUsed;

    @Column(name = "model_version", length = 20)
    private String modelVersion;

    @Column(name = "generation_prompt", columnDefinition = "TEXT")
    private String generationPrompt;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "language", length = 10, nullable = false)
    private String language = "en";

    @Column(name = "target_audience", length = 50)
    private String targetAudience;

    @Column(name = "tone", length = 50)
    private String tone;

    @Column(name = "seo_keywords", columnDefinition = "TEXT")
    private String seoKeywords; // JSON string

    @Column(name = "variations", columnDefinition = "TEXT")
    private String variations; // JSON string

    @Column(name = "quality_metrics", columnDefinition = "TEXT")
    private String qualityMetrics; // JSON string

    @Column(name = "suggested_improvements", columnDefinition = "TEXT")
    private String suggestedImprovements; // JSON string

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "is_optimized")
    private Boolean isOptimized = false;

    @Column(name = "optimization_goals", columnDefinition = "TEXT")
    private String optimizationGoals; // JSON string

    @Column(name = "performance_metrics", columnDefinition = "TEXT")
    private String performanceMetrics; // JSON string

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "click_count")
    private Long clickCount = 0L;

    @Column(name = "share_count")
    private Long shareCount = 0L;

    @Column(name = "inquiry_count")
    private Long inquiryCount = 0L;

    @Column(name = "conversion_count")
    private Long conversionCount = 0L;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_template")
    private Boolean isTemplate = false;

    @Column(name = "template_category", length = 100)
    private String templateCategory;

    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback; // JSON string

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback; // JSON string

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "optimized_at")
    private LocalDateTime optimizedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "parent_description_id", length = 100)
    private String parentDescriptionId;

    @Column(name = "a_b_test_id", length = 100)
    private String abTestId;

    @Column(name = "experiment_group", length = 50)
    private String experimentGroup;

    @Column(name = "cache_key", length = 255)
    private String cacheKey;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "cost_estimate")
    private Double costEstimate;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "market_segment", length = 100)
    private String marketSegment;

    @Column(name = "price_range")
    private String priceRange;

    @Column(name = "competitor_analysis", columnDefinition = "TEXT")
    private String competitorAnalysis; // JSON string

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON string

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        calculateMetrics();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateMetrics();
    }

    private void calculateMetrics() {
        if (primaryDescription != null) {
            characterCount = primaryDescription.length();
            wordCount = primaryDescription.split("\\s+").length;
        }
    }
}