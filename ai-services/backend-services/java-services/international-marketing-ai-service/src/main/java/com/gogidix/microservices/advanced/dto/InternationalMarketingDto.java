package com.gogidix.microservices.advanced.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Objects for International Marketing and Cultural Adaptation AI Service
 */

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CulturalMarketAnalysisRequestDto {
    @NotBlank
    private String targetCountry;

    private String propertyType;
    private Map<String, Object> businessObjectives;
    private List<String> targetDemographics;
    private String analysisScope;
    private List<String> culturalFactorsToConsider;
    private String marketSegment;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingLocalizationRequestDto {
    @NotBlank
    private String targetMarket;

    @NotBlank
    private String contentType;

    private Map<String, Object> originalContent;
    private List<String> localizationRequirements;
    private String targetLanguage;
    private Map<String, Object> culturalConstraints;
    private String brandGuidelines;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossCulturalCommunicationRequestDto {
    @NotBlank
    private String targetCulture;

    private String communicationContext;
    private List<String> participants;
    private String communicationObjective;
    private Map<String, Object> situationalFactors;
    private String relationshipLevel;
    private List<String> communicationChannels;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalMarketEntryRequestDto {
    @NotBlank
    private String targetMarket;

    private String businessType;
    private Map<String, Object> businessModel;
    private List<String> targetSegments;
    private Map<String, Object> resourceConstraints;
    private String entryTimeline;
    private List<String> riskTolerance;
}

// Result DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CulturalMarketAnalysisDto {
    private String analysisId;
    private String targetCountry;
    private Map<String, Object> culturalDimensions;
    private Map<String, Object> marketPreferences;
    private List<String> culturalTaboos;
    private Map<String, Object> communicationStyle;
    private Map<String, Object> marketingPreferences;
    private Map<String, Object> buyingBehavior;
    private Map<String, Object> seasonalPatterns;
    private Map<String, Object> negotiationStyles;
    private List<String> recommendedMarketingChannels;
    private Map<String, Object> localizationInsights;
    private Double culturalSensitivityScore;
    private String marketEntryComplexity;
    private List<String> keyCulturalConsiderations;
    private List<String> recommendations;
    private LocalDateTime analysisDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingLocalizationDto {
    private String localizationId;
    private String targetMarket;
    private Map<String, Object> originalContent;
    private Map<String, Object> localizedContent;
    private Map<String, Object> visualAdaptations;
    private List<String> localizedKeywords;
    private Map<String, Object> adaptedMessaging;
    private List<String> culturalOptimizations;
    private Map<String, Object> pricingPresentation;
    private Double localizationQualityScore;
    private Double readabilityScore;
    private Double culturalAppropriatenessScore;
    private List<String> localizedCallToActions;
    private List<String> marketSpecificFeatures;
    private Map<String, Object> localizationInsights;
    private LocalDateTime localizationDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossCulturalCommunicationDto {
    private String communicationId;
    private String targetCulture;
    private Map<String, Object> communicationProtocols;
    private Map<String, Object> businessEtiquette;
    private List<String> appropriateConversationTopics;
    private List<String> topicsToAvoid;
    private Map<String, Object> nonVerbalCommunication;
    private String communicationStyle;
    private List<String> culturalNuances;
    private List<String> trustBuildingStrategies;
    private List<String> relationshipBuildingTips;
    private List<String> commonMisunderstandings;
    private Double communicationEffectivenessScore;
    private String culturalSensitivityLevel;
    private String recommendedApproach;
    private LocalDateTime communicationDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalMarketEntryDto {
    private String entryId;
    private String targetMarket;
    private Map<String, Object> marketReadinessAssessment;
    private List<String> recommendedEntryStrategies;
    private Map<String, Object> competitiveLandscape;
    private List<String> regulatoryRequirements;
    private Map<String, Object> marketEntryTimeline;
    private Map<String, Object> requiredInvestments;
    private List<String> potentialRisks;
    private List<String> successFactors;
    private List<String> localPartnershipNeeds;
    private List<String> marketingAdaptationNeeds;
    private String marketEntryComplexity;
    private Double successProbability;
    private List<String> recommendedFirstSteps;
    private LocalDateTime marketEntryDate;
}