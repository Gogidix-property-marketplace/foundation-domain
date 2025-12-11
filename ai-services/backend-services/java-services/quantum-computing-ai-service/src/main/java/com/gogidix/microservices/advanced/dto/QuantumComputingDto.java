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
 * Data Transfer Objects for Quantum Computing AI Service
 */

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumPortfolioOptimizationRequestDto {
    @NotNull
    @Size(min = 1, max = 1000)
    private List<String> propertyIds;

    @NotNull
    @Min(10000)
    @Max(1000000000)
    private Double budget;

    @Min(0.0)
    @Max(1.0)
    private Double riskTolerance;

    @Min(1)
    @Max(120)
    private Integer timeHorizonMonths;

    private List<String> constraints;
    private Map<String, Object> optimizationParameters;
    private Long startTime;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumMarketPredictionRequestDto {
    @NotBlank
    private String location;

    @NotNull
    @Min(0)
    private Double currentMarketValue;

    @Min(1)
    @Max(60)
    private Integer timeHorizon; // months

    private List<String> predictionFactors;
    private String modelType;
    private Map<String, Object> marketData;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumInvestmentAnalysisRequestDto {
    @NotNull
    @Min(10000)
    @Max(1000000000)
    private Double budget;

    @NotNull
    private Map<String, Object> investmentCriteria;

    @Min(1)
    @Max(100)
    private Integer maxProperties;

    @Min(0.0)
    @Max(1.0)
    private Double riskThreshold;

    private List<String> preferredLocations;
    private List<String> propertyTypes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumRiskAssessmentRequestDto {
    @NotNull
    @Size(min = 1, max = 500)
    private List<String> propertyIds;

    @NotNull
    private Double totalPortfolioValue;

    @Min(0.0)
    @Max(1.0)
    private Double confidenceLevel;

    private List<String> riskFactors;
    private Map<String, Object> portfolioData;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumDevelopmentOptimizationRequestDto {
    @NotBlank
    private String projectId;

    @NotNull
    @Min(100000)
    @Max(1000000000)
    private Double totalBudget;

    @NotNull
    private Map<String, Object> projectConstraints;

    @Min(6)
    @Max(120)
    private Integer projectDuration; // months

    private List<String> resourceTypes;
    private Map<String, Object> optimizationGoals;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumPerformanceAnalysisRequestDto {
    @NotBlank
    private String algorithmType;

    @Min(2)
    @Max(1000)
    private Integer qubitCount;

    @Min(10)
    @Max(1000)
    private Integer circuitDepth;

    private String hardwareType;
    private Map<String, Object> algorithmParameters;
}

// Result DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumPortfolioOptimizationResultDto {
    private String optimizationId;
    private List<String> optimalPortfolio;
    private Double expectedReturn;
    private Double riskScore;
    private Double sharpeRatio;
    private Double quantumAdvantage;
    private Integer convergenceIterations;
    private Integer quantumCircuitDepth;
    private Double optimizationScore;
    private List<String> recommendations;
    private Double confidenceScore;
    private Long computingTime;
    private Integer quantumVolume;
    private Double errorRate;
    private LocalDateTime createdAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumMarketPredictionDto {
    private String predictionId;
    private String location;
    private Double currentValue;
    private Double predictedValue;
    private Double growthRate;
    private Double volatility;
    private List<Double> confidenceInterval;
    private Double quantumAccuracy;
    private String modelType;
    private List<String> quantumFeatures;
    private Integer timeHorizon;
    private List<String> riskFactors;
    private String marketSentiment;
    private Double quantumSpeedup;
    private LocalDateTime predictedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumInvestmentAnalysisDto {
    private String analysisId;
    private List<Map<String, Object>> optimalInvestments;
    private Double totalExpectedReturn;
    private Double portfolioRisk;
    private Double maxDrawdown;
    private Double var95;
    private Double quantumEfficiency;
    private String optimizationMethod;
    private Integer qubitCount;
    private Integer circuitDepth;
    private Double solutionQuality;
    private Long convergenceTime;
    private Double investmentScore;
    private List<String> recommendations;
    private LocalDateTime createdAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumRiskAssessmentDto {
    private String assessmentId;
    private Double overallRiskScore;
    private Double marketRisk;
    private Double creditRisk;
    private Double liquidityRisk;
    private Double operationalRisk;
    private Double quantumUncertainty;
    private Integer monteCarloSimulations;
    private Integer quantumMonteCarloSimulations;
    private Double quantumSpeedup;
    private Map<String, Double> riskDistribution;
    private Map<String, Double> stressTestResults;
    private List<String> quantumCorrelations;
    private List<String> riskMitigationStrategies;
    private LocalDateTime assessmentDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumDevelopmentOptimizationDto {
    private String optimizationId;
    private String projectId;
    private Map<String, Double> optimalResourceAllocation;
    private Integer optimalTimeline;
    private Double estimatedRoi;
    private Double quantumOptimizationScore;
    private Double resourceEfficiency;
    private Double costSavings;
    private String quantumAlgorithm;
    private Integer qubitUtilization;
    private Double solutionSpace;
    private Double exploredSolutions;
    private Long optimizationTime;
    private Double energyEfficiency;
    private Double sustainabilityScore;
    private List<String> recommendations;
    private LocalDateTime createdAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantumPerformanceAnalysisDto {
    private String analysisId;
    private String algorithmType;
    private String quantumHardware;
    private Integer qubitCount;
    private Integer circuitDepth;
    private Double gateFidelity;
    private Double readoutFidelity;
    private Integer coherenceTime;
    private Integer quantumVolume;
    private Double algorithmicSpeedup;
    private Double errorRate;
    private Double successProbability;
    private Double resourceUtilization;
    private Integer energyConsumption;
    private Long classicalComparisonTime;
    private Long quantumExecutionTime;
    private Map<String, Double> performanceMetrics;
    private List<String> optimizationOpportunities;
    private Map<String, Double> benchmarkResults;
    private LocalDateTime analysisDate;
}