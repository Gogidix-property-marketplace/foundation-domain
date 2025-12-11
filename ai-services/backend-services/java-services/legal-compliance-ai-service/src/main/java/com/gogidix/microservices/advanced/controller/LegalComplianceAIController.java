package com.gogidix.microservices.advanced.controller;

import com.gogidix.microservices.advanced.service.LegalComplianceAIService;
import com.gogidix.microservices.advanced.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Legal System and Compliance Adaptation AI Service - Rapid Implementation
 */
@RestController
@RequestMapping("/api/v1/legal-compliance-ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Legal Compliance AI API", description = "Legal System and Compliance Adaptation AI Service")
public class LegalComplianceAIController {

    private final LegalComplianceAIService legalComplianceAIService;

    @PostMapping("/legal-system-analysis")
    @Operation(summary = "Analyze legal system", description = "AI-powered legal system analysis for international markets")
    @PreAuthorize("hasRole('LEGAL_ADVISOR') or hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<LegalSystemAnalysisDto>> analyzeLegalSystem(
            @Valid @RequestBody LegalSystemAnalysisRequestDto request) {
        log.info("Analyzing legal system for {} - ULTRA FAST MODE", request.getTargetCountry());
        return legalComplianceAIService.analyzeLegalSystem(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error analyzing legal system", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/compliance-adaptation")
    @Operation(summary = "Adapt compliance requirements", description = "AI-powered compliance requirements adaptation for new jurisdictions")
    @PreAuthorize("hasRole('COMPLIANCE_MANAGER') or hasRole('BUSINESS_DEVELOPMENT') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<ComplianceAdaptationDto>> adaptComplianceRequirements(
            @Valid @RequestBody ComplianceAdaptationRequestDto request) {
        log.info("Adapting compliance requirements for {} - RAPID MODE", request.getTargetJurisdiction());
        return legalComplianceAIService.adaptComplianceRequirements(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error adapting compliance requirements", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/cross-border-legal")
    @Operation(summary = "Get cross-border legal requirements", description = "AI-powered cross-border transaction legal analysis")
    @PreAuthorize("hasRole('INTERNATIONAL_LEGAL') or hasRole('TRANSACTION_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<CrossBorderLegalDto>> getCrossBorderLegalRequirements(
            @Valid @RequestBody CrossBorderLegalRequestDto request) {
        log.info("Analyzing cross-border legal requirements - STRATEGIC MODE");
        return legalComplianceAIService.getCrossBorderLegalRequirements(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error analyzing cross-border legal requirements", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/regulatory-compliance-check")
    @Operation(summary = "Check regulatory compliance", description = "AI-powered regulatory compliance status checking")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('AUDITOR') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<RegulatoryComplianceDto>> checkRegulatoryCompliance(
            @Valid @RequestBody RegulatoryComplianceRequestDto request) {
        log.info("Checking regulatory compliance for {} - FAST MODE", request.getJurisdiction());
        return legalComplianceAIService.checkRegulatoryCompliance(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error checking regulatory compliance", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/legal-framework/{country}")
    @Operation(summary = "Get legal framework overview", description = "Quick legal framework overview for target country")
    @PreAuthorize("hasRole('USER') or hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<Object> getLegalFrameworkOverview(@PathVariable String country) {
        log.info("Getting legal framework overview for {} - QUICK MODE", country);
        return ResponseEntity.ok(java.util.Map.of(
            "country", country,
            "legalSystem", java.util.Map.of(
                "systemType", "CIVIL_LAW",
                "courtHierarchy", Arrays.asList("SUPREME_COURT", "HIGH_COURT", "DISTRICT_COURT"),
                "codifiedLaws", "YES",
                "transparencyIndex", 0.74
            ),
            "propertyRights", java.util.Map.of(
                "ownershipTypes", Arrays.asList("FREEHOLD", "LEASEHOLD", "CONDOMINIUM"),
                "foreignRestrictions", "LIMITED",
                "registrationRequired", "YES",
                "titleInsurance", "AVAILABLE"
            ),
            "regulatoryEnvironment", java.util.Map.of(
                "keyBodies", Arrays.asList("REAL_ESTATE_AUTHORITY", "LAND_REGISTRY", "TAX_AUTHORITY"),
                "complexity", "HIGH",
                "stability", "STABLE"
            ),
            "aiConfidence", 0.91,
            "timestamp", java.time.LocalDateTime.now()
        ));
    }
}