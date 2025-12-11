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
 * Data Transfer Objects for Cross-Border Transaction Management AI Service
 */

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternationalTransactionRequestDto {
    @NotBlank
    private String sourceCountry;

    @NotBlank
    private String targetCountry;

    @NotNull
    @Positive
    private Double transactionValue;

    private String transactionType;
    private Map<String, Object> propertyDetails;
    private List<String> currencies;
    private Map<String, Object> participantDetails;
    private String transactionStructure;
    private List<String> complianceRequirements;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyManagementRequestDto {
    @NotBlank
    private String sourceCurrency;

    @NotBlank
    private String targetCurrency;

    @NotNull
    @Positive
    private Double exchangeAmount;

    private String timeframe;
    private String riskTolerance;
    private Map<String, Object> marketConditions;
    private List<String> preferredChannels;
    private String hedgingPreference;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegulatoryApprovalRequestDto {
    @NotBlank
    private String jurisdiction;

    @NotBlank
    private String transactionType;

    private Map<String, Object> transactionDetails;
    private List<String> approvalRequirements;
    private String urgencyLevel;
    private Map<String, Object> applicantDetails;
    private List<String> supportingDocuments;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternationalEscrowRequestDto {
    @NotNull
    @Positive
    private Double transactionValue;

    private List<String> currencies;
    private String escrowStructure;
    private Map<String, Object> releaseConditions;
    private List<String> involvedParties;
    private String duration;
    private Map<String, Object> specialRequirements;
}

// Result DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternationalTransactionDto {
    private String transactionId;
    private String sourceCountry;
    private String targetCountry;
    private Map<String, Object> transactionStructure;
    private Map<String, Object> currencyOptimization;
    private List<String> requiredComplianceSteps;
    private Map<String, Object> taxOptimization;
    private Map<String, Object> paymentProcessingPlan;
    private List<String> regulatoryApprovals;
    private Map<String, Object> timeline;
    private Map<String, Object> costAnalysis;
    private Map<String, Object> riskAssessment;
    private List<String> documentRequirements;
    private List<String> intermediaryRequirements;
    private Map<String, Object> complianceMonitoring;
    private String transactionComplexity;
    private String estimatedCompletionTime;
    private Double successProbability;
    private Double aiConfidenceScore;
    private LocalDateTime transactionDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyManagementDto {
    private String managementId;
    private String sourceCurrency;
    private String targetCurrency;
    private Double exchangeAmount;
    private Map<String, Object> exchangeStrategy;
    private List<Map<String, Object>> optimalTiming;
    private Map<String, Object> riskMitigation;
    private Map<String, Object> forwardContractRecommendations;
    private Map<String, Object> hedgingStrategies;
    private Map<String, Object> exchangeRateForecast;
    private Map<String, Object> costOptimization;
    private Map<String, Object> regulatoryCompliance;
    private Map<String, Object> paymentChannelOptimization;
    private Map<String, Object> liquidityManagement;
    private Double exchangeEfficiencyScore;
    private String expectedSavings;
    private Map<String, Object> implementationPlan;
    private LocalDateTime managementDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegulatoryApprovalDto {
    private String approvalId;
    private String jurisdiction;
    private String transactionType;
    private Map<String, Object> approvalRequirements;
    private List<String> requiredDocuments;
    private Map<String, Object> approvalTimeline;
    private List<String> regulatoryBodies;
    private Map<String, Object> applicationProcedures;
    private String expectedProcessingTime;
    private Double approvalProbability;
    private List<String> potentialObstacles;
    private List<String> mitigationStrategies;
    private List<String> escalationPaths;
    private List<String> complianceCheckpoints;
    private Map<String, Object> approvalMonitoring;
    private String status;
    private Double aiConfidenceScore;
    private LocalDateTime approvalDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternationalEscrowDto {
    private String escrowId;
    private Double transactionValue;
    private List<String> currencies;
    private Map<String, Object> escrowStructure;
    private List<String> escrowConditions;
    private Map<String, Object> releaseTriggers;
    private List<String> escrowProviderRecommendations;
    private Map<String, Object> multiCurrencyManagement;
    private Map<String, Object> regulatoryCompliance;
    private Map<String, Object> disputeResolution;
    private Map<String, Object> securityProtocols;
    private Map<String, Object> insuranceRequirements;
    private Map<String, Object> feeStructure;
    private Map<String, Object> timeline;
    private Map<String, Object> riskMitigation;
    private String securityLevel;
    private LocalDateTime escrowDate;
}