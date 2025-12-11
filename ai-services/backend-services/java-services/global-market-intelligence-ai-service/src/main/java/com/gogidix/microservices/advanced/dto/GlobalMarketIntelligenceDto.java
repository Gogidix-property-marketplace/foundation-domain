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
 * Data Transfer Objects for Global Market Intelligence AI Service
 */

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalMarketAnalysisRequestDto {
    @NotBlank
    private String marketScope;

    private List<String> targetRegions;
    private List<String> analysisTypes;
    private String timeHorizon;
    private Map<String, Object> specificFocus;
    private List<String> marketSegments;
    private Map<String, Object> parameters;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentIntelligenceRequestDto {
    @NotEmpty
    private List<String> targetRegions;

    private String investmentType;
    private Double investmentRangeMin;
    private Double investmentRangeMax;
    private String riskTolerance;
    private List<String> sectorsOfInterest;
    private Map<String, Object> investmentCriteria;
    private String timeHorizon;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitiveIntelligenceRequestDto {
    private List<String> targetCompetitors;
    private String marketSegment;
    private String geographicScope;
    private List<String> intelligenceTypes;
    private Map<String, Object> focusAreas;
    private String timeFrame;
    private Map<String, Object> parameters;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategicForecastRequestDto {
    @NotBlank
    private String forecastHorizon;

    private List<String> targetMarkets;
    private List<String> forecastTypes;
    private Map<String, Object> scenarioParameters;
    private String granularity;
    private Map<String, Object> assumptions;
}

// Result DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalMarketAnalysisDto {
    private String analysisId;
    private String marketScope;
    private Map<String, Object> marketOverview;
    private List<Map<String, Object>> regionalAnalysis;
    private Map<String, Object> investmentClimate;
    private List<String> emergingTrends;
    private List<String> marketOpportunities;
    private Map<String, Object> riskAssessment;
    private Map<String, Object> competitiveLandscape;
    private Map<String, Object> regulatoryEnvironment;
    private Map<String, Object> technologicalAdoption;
    private Map<String, Object> demographicTrends;
    private Map<String, Object> economicIndicators;
    private Map<String, Object> marketForecasts;
    private List<String> investmentRecommendations;
    private String marketMaturityLevel;
    private Double marketConfidenceScore;
    private List<String> strategicInsights;
    private LocalDateTime analysisDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentIntelligenceDto {
    private String intelligenceId;
    private List<String> targetRegions;
    private Map<String, Object> investmentOpportunities;
    private List<Map<String, Object>> roiAnalysis;
    private Map<String, Object> marketTiming;
    private Map<String, Object> riskReturnProfiles;
    private Map<String, Object> portfolioOptimization;
    private List<String> entryExitStrategies;
    private Map<String, Object> marketCorrelations;
    private Map<String, Object> economicScenarios;
    private Map<String, Object> investorSentiment;
    private Map<String, Object> liquidityAnalysis;
    private List<String> competitiveAdvantages;
    private Double intelligenceConfidence;
    private String investmentHorizon;
    private List<String> strategicRecommendations;
    private LocalDateTime intelligenceDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitiveIntelligenceDto {
    private String intelligenceId;
    private Map<String, Object> competitorAnalysis;
    private List<Map<String, Object>> marketShareAnalysis;
    private Map<String, Object> strategicMoves;
    private Map<String, Object> competitivePositioning;
    private List<String> marketGaps;
    private List<String> competitiveAdvantages;
    private Map<String, Object> threatAssessment;
    private List<String> strategicRecommendations;
    private Map<String, Object> marketEvolution;
    private List<String> innovationTrends;
    private List<String> partnershipOpportunities;
    private String competitiveIntensity;
    private String marketDynamics;
    private Double intelligenceConfidence;
    private List<String> competitiveInsights;
    private LocalDateTime intelligenceDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategicForecastDto {
    private String forecastId;
    private String forecastHorizon;
    private Map<String, Object> marketProjections;
    private List<Map<String, Object>> scenarioAnalysis;
    private Map<String, Object> strategicRoadmap;
    private List<String> trendAnalysis;
    private List<String> growthDrivers;
    private List<String> marketDisruptions;
    private Map<String, Object> opportunityRadar;
    private Map<String, Object> riskLandscape;
    private List<String> strategicImperatives;
    private List<String> marketMilestones;
    private List<String> competitiveShifts;
    private Double forecastConfidence;
    private List<String> actionableInsights;
    private List<String> strategicRecommendations;
    private LocalDateTime forecastDate;
}