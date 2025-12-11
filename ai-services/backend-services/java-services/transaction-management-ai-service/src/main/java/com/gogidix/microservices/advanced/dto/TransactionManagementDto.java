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
 * Data Transfer Objects for Transaction Management AI Service
 */

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionWorkflowOptimizationRequestDto {
    @NotBlank
    private String transactionId;

    private String transactionType;
    private Map<String, Object> currentWorkflow;
    private List<String> participants;
    private Map<String, Object> constraints;
    private String optimizationGoal;
    private List<String> priorityAreas;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingRequestDto {
    @NotBlank
    private String transactionId;

    private List<String> documentIds;
    private List<String> documentTypes;
    private Map<String, Object> processingParameters;
    private String extractionLevel;
    private Boolean enableOCR;
    private Boolean enableValidation;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceCheckingRequestDto {
    @NotBlank
    private String transactionId;

    private List<String> regulationsToCheck;
    private String jurisdiction;
    private Map<String, Object> transactionData;
    private List<String> involvedParties;
    private String complianceLevel;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadlineManagementRequestDto {
    @NotBlank
    private String transactionId;

    private List<Map<String, Object>> currentDeadlines;
    private Map<String, Object> timelineConstraints;
    private List<String> priorityDeadlines;
    private String riskTolerance;
    private Map<String, Object> participantResponsibilities;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionInsightsRequestDto {
    @NotBlank
    private String transactionId;

    private List<String> insightTypes;
    private Map<String, Object> transactionHistory;
    private String comparisonScope;
    private List<String> metricsToAnalyze;
    private String timeHorizon;
}

// Result DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionWorkflowOptimizationDto {
    private String optimizationId;
    private String transactionId;
    private String currentWorkflowStatus;
    private List<Map<String, Object>> optimizedWorkflowSteps;
    private Integer timeSavings;
    private Double efficiencyImprovement;
    private List<String> automationOpportunities;
    private Map<String, Object> workflowOptimizationMetrics;
    private List<String> recommendedActions;
    private List<String> potentialRisks;
    private Double roiEstimate;
    private String implementationTimeline;
    private LocalDateTime optimizationDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingDto {
    private String processingId;
    private String transactionId;
    private List<Map<String, Object>> documentsProcessed;
    private Map<String, Object> extractedKeyInformation;
    private List<String> identifiedIssues;
    private String complianceStatus;
    private Map<String, Object> processingMetrics;
    private List<String> aiCapabilities;
    private List<String> nextSteps;
    private LocalDateTime processingDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceCheckingDto {
    private String checkingId;
    private String transactionId;
    private String complianceStatus;
    private Double overallComplianceScore;
    private List<Map<String, Object>> checkedRegulations;
    private List<String> identifiedViolations;
    private List<String> requiredActions;
    private Map<String, Object> complianceMetrics;
    private Map<String, String> riskAssessment;
    private List<String> recommendations;
    private List<String> automatedComplianceFeatures;
    private LocalDateTime checkingDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadlineManagementDto {
    private String managementId;
    private String transactionId;
    private List<Map<String, Object>> currentDeadlines;
    private List<Map<String, Object>> deadlineRisks;
    private List<String> optimizationStrategies;
    private List<String> recommendations;
    private List<String> automatedReminders;
    private Map<String, Object> deadlineAnalytics;
    private LocalDateTime managementDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionInsightsDto {
    private String insightsId;
    private String transactionId;
    private Double transactionHealthScore;
    private List<String> keyInsights;
    private Map<String, Object> performanceMetrics;
    private List<Map<String, Object>> bottleneckAnalysis;
    private Map<String, Object> predictiveAnalytics;
    private List<String> recommendations;
    private Map<String, Object> marketComparison;
    private List<String> opportunitiesForOptimization;
    private LocalDateTime insightsDate;
}