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
 * Data Transfer Objects for Lead Conversion AI Service
 */

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadConversionOptimizationRequestDto {
    @NotBlank
    private String agentId;

    private String timePeriod;
    private List<String> leadSegments;
    private Map<String, Object> currentStrategy;
    private List<String> optimizationGoals;
    private Map<String, Object> marketData;
    private String targetMarket;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientJourneyManagementRequestDto {
    @NotBlank
    private String clientId;

    private String agentId;
    private List<String> journeyStages;
    private Map<String, Object> clientPreferences;
    private Map<String, Object> interactionHistory;
    private String currentStage;
    private List<String> nextActions;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadSuccessPredictionRequestDto {
    @NotBlank
    private String leadId;

    private Map<String, Object> leadData;
    private List<String> behaviors;
    private Map<String, Object> demographics;
    private String propertyType;
    private Map<String, Object> engagementMetrics;
    private String timeHorizon;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntelligentCommunicationRequestDto {
    @NotBlank
    private String clientId;

    @NotBlank
    private String communicationType;

    private String purpose;
    private Map<String, Object> context;
    private List<String> keyPoints;
    private String tone;
    private Map<String, Object> clientPreferences;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpOptimizationRequestDto {
    @NotBlank
    private String leadId;

    private String currentStrategy;
    private List<String> communicationHistory;
    private Map<String, Object> clientProfile;
    private List<String> availableChannels;
    private String optimizationGoal;
}

// Result DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadConversionOptimizationDto {
    private String optimizationId;
    private String agentId;
    private Double currentConversionRate;
    private Double optimizedConversionRate;
    private Double improvementPotential;
    private List<String> conversionOptimizationStrategies;
    private Map<String, Double> leadScoringModel;
    private List<String> recommendedActions;
    private List<String> aBTestSuggestions;
    private Map<String, Double> predictedImpact;
    private List<String> implementationRoadmap;
    private Double roiEstimate;
    private LocalDateTime optimizationDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientJourneyManagementDto {
    private String journeyId;
    private String clientId;
    private String currentJourneyStage;
    private List<Map<String, Object>> journeyStages;
    private Map<String, Object> clientPreferences;
    private Map<String, Object> engagementMetrics;
    private List<String> nextBestActions;
    private List<String> riskFactors;
    private Double successProbability;
    private LocalDateTime estimatedClosingDate;
    private Double personalizationScore;
    private LocalDateTime journeyDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadSuccessPredictionDto {
    private String predictionId;
    private String leadId;
    private Double successProbability;
    private Double confidenceLevel;
    private String conversionTimeline;
    private Integer leadScore;
    private Map<String, Double> predictionFactors;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendedActions;
    private List<Map<String, Object>> similarLeadsAnalysis;
    private Map<String, String> marketConditionsImpact;
    private LocalDateTime predictionDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntelligentCommunicationDto {
    private String communicationId;
    private String clientId;
    private String communicationType;
    private Map<String, String> aiGeneratedContent;
    private Double personalizationLevel;
    private String tone;
    private String optimalSendTime;
    private String channelPreference;
    private Map<String, Double> engagementPrediction;
    private List<Map<String, Object>> aBTestVariants;
    private List<String> followUpStrategy;
    private List<Map<String, Object>> communicationHistory;
    private String nextBestCommunication;
    private List<String> aiInsights;
    private LocalDateTime communicationDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpOptimizationDto {
    private String optimizationId;
    private String leadId;
    private String currentFollowUpStrategy;
    private String optimizedFollowUpStrategy;
    private List<Map<String, Object>> followUpSchedule;
    private String optimalContactFrequency;
    private List<String> bestContactTimes;
    private List<String> preferredChannels;
    private Map<String, String> contentPersonalization;
    private List<String> automationRules;
    private Map<String, Double> expectedOutcomes;
    private List<String> successMetrics;
    private Double roiEstimate;
    private LocalDateTime optimizationDate;
}