package com.gogidix.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for Property Description Generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDescriptionResponse {

    @JsonProperty("descriptionId")
    private String descriptionId;

    @JsonProperty("primaryDescription")
    private String primaryDescription;

    @JsonProperty("optimizedDescription")
    private String optimizedDescription;

    @JsonProperty("variations")
    private List<String> variations;

    @JsonProperty("qualityScore")
    private Double qualityScore;

    @JsonProperty("seoScore")
    private Double seoScore;

    @JsonProperty("readabilityScore")
    private Double readabilityScore;

    @JsonProperty("engagementScore")
    private Double engagementScore;

    @JsonProperty("originalQuality")
    private Double originalQuality;

    @JsonProperty("optimizedQuality")
    private Double optimizedQuality;

    @JsonProperty("improvementPercentage")
    private Double improvementPercentage;

    @JsonProperty("suggestedImprovements")
    private List<String> suggestedImprovements;

    @JsonProperty("optimizationsApplied")
    private List<String> optimizationsApplied;

    @JsonProperty("generatedAt")
    private LocalDateTime generatedAt;

    @JsonProperty("optimizedAt")
    private LocalDateTime optimizedAt;

    @JsonProperty("modelUsed")
    private String modelUsed;

    @JsonProperty("wordCount")
    private Integer wordCount;

    @JsonProperty("characterCount")
    private Integer characterCount;

    @JsonProperty("estimatedReadTime")
    private Integer estimatedReadTime; // in seconds

    @JsonProperty("seoKeywords")
    private List<String> seoKeywords;

    @JsonProperty("keywordDensity")
    private Map<String, Double> keywordDensity;

    @JsonProperty("sentimentAnalysis")
    private SentimentAnalysis sentimentAnalysis;

    @JsonProperty("targetAudienceMatch")
    private Double targetAudienceMatch;

    @JsonProperty("marketComparisons")
    private List<MarketComparison> marketComparisons;

    @JsonProperty("performanceMetrics")
    private PerformanceMetrics performanceMetrics;

    @JsonProperty("cacheHit")
    private Boolean cacheHit = false;

    @JsonProperty("processingTime")
    private Long processingTime; // in milliseconds

    /**
     * Sentiment analysis details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentAnalysis {
        @JsonProperty("overall")
        private String overall; // POSITIVE, NEUTRAL, NEGATIVE

        @JsonProperty("confidence")
        private Double confidence;

        @JsonProperty("emotions")
        private Map<String, Double> emotions;

        @JsonProperty("tone")
        private String tone;
    }

    /**
     * Market comparison data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketComparison {
        @JsonProperty("propertyType")
        private String propertyType;

        @JsonProperty("location")
        private String location;

        @JsonProperty("averageDescriptionLength")
        private Integer averageDescriptionLength;

        @JsonProperty("commonKeywords")
        private List<String> commonKeywords;

        @JsonProperty("performancePercentile")
        private Double performancePercentile;
    }

    /**
     * Performance metrics for the description
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        @JsonProperty("clickThroughRate")
        private Double clickThroughRate;

        @JsonProperty("viewDuration")
        private Double viewDuration;

        @JsonProperty("shareRate")
        private Double shareRate;

        @JsonProperty("inquiryRate")
        private Double inquiryRate;

        @JsonProperty("conversionRate")
        private Double conversionRate;

        @JsonProperty("searchRanking")
        private Double searchRanking;

        @JsonProperty("userEngagement")
        private Map<String, Double> userEngagement;
    }
}