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
 * Data Transfer Objects for Negotiation Assistant AI Service
 */

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketConditionAnalysisRequestDto {
    @NotBlank
    private String location;

    @NotBlank
    private String marketType;

    private String propertyType;
    private List<String> comparableProperties;
    private Map<String, Object> marketFactors;
    private String timeHorizon;
    private List<String> analysisTypes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationStrategyRequestDto {
    @NotBlank
    private String propertyId;

    @NotBlank
    private String clientType;

    private Double targetPrice;
    private Map<String, Object> propertyDetails;
    private List<String> negotiationGoals;
    private String clientPosition;
    private Map<String, Object> constraints;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpponentAnalysisRequestDto {
    @NotBlank
    private String opponentType;

    private String opponentRole;
    private Map<String, Object> opponentProfile;
    private List<String> previousInteractions;
    private String negotiationContext;
    private Map<String, Object> availableInformation;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationSimulationRequestDto {
    @NotBlank
    private String propertyId;

    @NotBlank
    private String scenarioType;

    private Map<String, Object> negotiationParameters;
    private List<String> participantProfiles;
    private String objective;
    private Integer simulationRounds;
    private Map<String, Object> marketConditions;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeNegotiationAssistanceRequestDto {
    @NotBlank
    private String sessionId;

    @NotBlank
    private String currentStage;

    private Map<String, Object> currentOffer;
    private Map<String, Object> counterOffer;
    private List<String> previousOffers;
    private Integer roundNumber;
    private Map<String, Object> context;
}

// Result DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketConditionAnalysisDto {
    private String analysisId;
    private String location;
    private String marketType;
    private String marketStatus;
    private Double marketStrengthIndex;
    private Map<String, Object> keyIndicators;
    private Map<String, Object> supplyDemandAnalysis;
    private Map<String, Object> priceAnalysis;
    private List<Map<String, Object>> competitorAnalysis;
    private List<String> negotiationInsights;
    private List<String> recommendedTactics;
    private List<String> riskFactors;
    private LocalDateTime analysisDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationStrategyDto {
    private String strategyId;
    private String propertyId;
    private String clientType;
    private String strategyType;
    private Double recommendedOfferPrice;
    private String negotiationPosition;
    private List<String> keyNegotiationPoints;
    private Map<String, Object> openingPosition;
    private Map<String, Object> concessionStrategy;
    private Double walkAwayPoint;
    private List<String> successMetrics;
    private List<String> contingencyPlans;
    private List<String> psychologicalInsights;
    private Double probabilityOfSuccess;
    private String estimatedNegotiationDuration;
    private LocalDateTime strategyDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpponentAnalysisDto {
    private String analysisId;
    private String opponentType;
    private String experienceLevel;
    private String negotiationStyle;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> commonTactics;
    private Map<String, String> responseStrategies;
    private Double emotionalIntelligenceScore;
    private Double flexibilityRating;
    private Double successRate;
    private List<String> behavioralPredictions;
    private String recommendedApproach;
    private List<String> counterTactics;
    private LocalDateTime analysisDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationSimulationDto {
    private String simulationId;
    private String propertyId;
    private String scenarioType;
    private List<Map<String, Object>> simulationRounds;
    private String outcome;
    private Integer totalNegotiationRange;
    private String optimalStrategy;
    private Double successProbability;
    private List<Map<String, Object>> alternativeScenarios;
    private List<String> keyDecisionPoints;
    private List<String> lessonsLearned;
    private Map<String, Double> riskAssessment;
    private List<String> recommendations;
    private LocalDateTime simulationDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeNegotiationAssistanceDto {
    private String assistanceId;
    private String sessionId;
    private String currentStage;
    private List<String> immediateRecommendations;
    private List<String> tacticalAdvice;
    private List<String> riskAlerts;
    private List<String> negotiationLevers;
    private Double confidenceScore;
    private String nextMove;
    private Double successProbability;
    private List<String> timeSensitiveFactors;
    private String communicationScript;
    private LocalDateTime assistanceTimestamp;
}