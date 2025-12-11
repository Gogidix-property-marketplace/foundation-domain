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
 * Data Transfer Objects for Legal System and Compliance Adaptation AI Service
 */

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalSystemAnalysisRequestDto {
    @NotBlank
    private String targetCountry;

    private String businessType;
    private String propertyType;
    private List<String> legalAreasToAnalyze;
    private Map<String, Object> businessObjectives;
    private String analysisScope;
    private List<String> specificConcerns;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceAdaptationRequestDto {
    @NotBlank
    private String targetJurisdiction;

    @NotBlank
    private String businessType;

    private Map<String, Object> originalFramework;
    private List<String> adaptationRequirements;
    private Map<String, Object> businessConstraints;
    private String complianceLevel;
    private List<String> targetRegulations;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossBorderLegalRequestDto {
    @NotEmpty
    private List<String> involvedCountries;

    @NotBlank
    private String transactionType;

    private Map<String, Object> transactionDetails;
    private List<String> partiesInvolved;
    private Map<String, Object> assetDetails;
    private String transactionValue;
    private List<String> specificLegalConcerns;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegulatoryComplianceRequestDto {
    @NotBlank
    private String jurisdiction;

    @NotBlank
    private String businessType;

    private Map<String, Object> currentOperations;
    private List<String> regulationsToCheck;
    private String complianceStandard;
    private Map<String, Object> businessMetrics;
    private List<String> areasOfConcern;
}

// Result DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalSystemAnalysisDto {
    private String analysisId;
    private String targetCountry;
    private Map<String, Object> legalFramework;
    private Map<String, Object> propertyLaws;
    private List<String> regulatoryBodies;
    private Map<String, Object> contractRequirements;
    private Map<String, Object> foreignOwnershipRestrictions;
    private Map<String, Object> taxLaws;
    private Map<String, Object> zoningRegulations;
    private Map<String, Object> tenantLaws;
    private Map<String, Object> disputeResolutionMechanisms;
    private Map<String, Object> businessEntityRequirements;
    private String complianceComplexity;
    private String legalSystemStability;
    private List<String> recommendedLegalStructures;
    private Map<String, Object> riskAssessment;
    private List<String> complianceChecklist;
    private LocalDateTime analysisDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceAdaptationDto {
    private String adaptationId;
    private String targetJurisdiction;
    private Map<String, Object> originalFramework;
    private Map<String, Object> adaptedPolicies;
    private List<String> requiredLicenses;
    private Map<String, Object> complianceProcedures;
    private List<String> legalDocumentationRequirements;
    private Map<String, Object> reportingObligations;
    private Map<String, Object> dataProtectionCompliance;
    private Map<String, Object> antiMoneyLaunderingCompliance;
    private Map<String, Object> employmentLawCompliance;
    private Map<String, Object> consumerProtectionCompliance;
    private Map<String, Object> environmentalCompliance;
    private Map<String, Object> complianceAuditRequirements;
    private Double complianceAdaptationScore;
    private String implementationComplexity;
    private Map<String, Object> recommendedImplementationPlan;
    private LocalDateTime adaptationDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossBorderLegalDto {
    private String crossBorderId;
    private List<String> involvedCountries;
    private String transactionType;
    private Map<String, Object> internationalTreaties;
    private List<String> requiredDocuments;
    private Map<String, Object> taxImplications;
    private List<String> complianceCheckpoints;
    private Map<String, Object> foreignInvestmentRegulations;
    private Map<String, Object> currencyRegulations;
    private List<String> disputeResolutionJurisdictions;
    private Map<String, Object> intellectualPropertyProtection;
    private Map<String, Object> dataTransferCompliance;
    private Map<String, Object> internationalPaymentCompliance;
    private String crossBorderComplexity;
    private String estimatedProcessingTime;
    private Map<String, Object> legalCostEstimates;
    private List<String> recommendedLegalPartners;
    private Map<String, Object> complianceRoadmap;
    private LocalDateTime analysisDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegulatoryComplianceDto {
    private String complianceId;
    private String jurisdiction;
    private String businessType;
    private Map<String, Object> currentComplianceStatus;
    private List<String> identifiedGaps;
    private Map<String, Object> remediationPlan;
    private Map<String, Object> riskAssessment;
    private Double complianceScore;
    private List<String> criticalComplianceIssues;
    private List<String> pendingComplianceActions;
    private List<String> upcomingRegulatoryChanges;
    private List<String> complianceDocumentationRequirements;
    private Double auditReadinessScore;
    private List<String> recommendedImprovements;
    private Map<String, Object> complianceMonitoringPlan;
    private LocalDateTime complianceDate;
}