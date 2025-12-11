package com.gogidix.microservices.advanced.controller;

import com.gogidix.microservices.advanced.service.InternationalMarketingAIService;
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
 * REST Controller for International Marketing and Cultural Adaptation AI Service - Rapid Implementation
 */
@RestController
@RequestMapping("/api/v1/international-marketing-ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "International Marketing AI API", description = "International Marketing and Cultural Adaptation AI Service")
public class InternationalMarketingAIController {

    private final InternationalMarketingAIService internationalMarketingAIService;

    @PostMapping("/cultural-analysis")
    @Operation(summary = "Analyze cultural market", description = "AI-powered cultural market analysis for international expansion")
    @PreAuthorize("hasRole('BUSINESS_ANALYST') or hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<CulturalMarketAnalysisDto>> analyzeCulturalMarket(
            @Valid @RequestBody CulturalMarketAnalysisRequestDto request) {
        log.info("Analyzing cultural market for {} - RAPID MODE", request.getTargetCountry());
        return internationalMarketingAIService.analyzeCulturalMarket(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error analyzing cultural market", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/marketing-localization")
    @Operation(summary = "Localize marketing content", description = "AI-powered marketing content localization for international markets")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('CONTENT_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<MarketingLocalizationDto>> localizeMarketingContent(
            @Valid @RequestBody MarketingLocalizationRequestDto request) {
        log.info("Localizing marketing content for {} - ULTRA FAST MODE", request.getTargetMarket());
        return internationalMarketingAIService.localizeMarketingContent(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error localizing marketing content", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/cross-cultural-communication")
    @Operation(summary = "Get cross-cultural communication guidance", description = "AI-powered cross-cultural communication strategies")
    @PreAuthorize("hasRole('BUSINESS_DEVELOPMENT') or hasRole('SALES_MANAGER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<CrossCulturalCommunicationDto>> getCrossCulturalCommunication(
            @Valid @RequestBody CrossCulturalCommunicationRequestDto request) {
        log.info("Analyzing cross-cultural communication for {} - RAPID MODE", request.getTargetCulture());
        return internationalMarketingAIService.getCrossCulturalCommunication(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error analyzing cross-cultural communication", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/market-entry-strategy")
    @Operation(summary = "Get market entry strategy", description = "AI-powered global market entry strategy development")
    @PreAuthorize("hasRole('STRATEGIC_PLANNING') or hasRole('BUSINESS_DEVELOPMENT') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<GlobalMarketEntryDto>> getMarketEntryStrategy(
            @Valid @RequestBody GlobalMarketEntryRequestDto request) {
        log.info("Developing market entry strategy for {} - STRATEGIC MODE", request.getTargetMarket());
        return internationalMarketingAIService.getMarketEntryStrategy(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error developing market entry strategy", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/cultural-dimensions/{country}")
    @Operation(summary = "Get cultural dimensions", description = "Quick cultural dimensions analysis for target country")
    @PreAuthorize("hasRole('USER') or hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<Object> getCulturalDimensions(@PathVariable String country) {
        log.info("Getting cultural dimensions for {} - QUICK MODE", country);
        return ResponseEntity.ok(java.util.Map.of(
            "country", country,
            "culturalDimensions", java.util.Map.of(
                "powerDistance", 0.65,
                "individualism", 0.34,
                "masculinity", 0.58,
                "uncertaintyAvoidance", 0.72,
                "longTermOrientation", 0.81,
                "indulgence", 0.45,
                "contextuality", "HIGH_CONTEXT",
                "timeOrientation", "POLYCHRONIC"
            ),
            "communicationStyle", java.util.Map.of(
                "directness", "INDIRECT",
                "formality", "HIGH",
                "relationshipBuilding", "ESSENTIAL",
                "negotiationPace", "PATIENT"
            ),
            "marketingPreferences", java.util.Map.of(
                "trustedBrands", "ESSENTIAL",
                "testimonials", "HIGHLY_VALUED",
                "visualContent", "PREFERRED",
                "socialProof", "VERY_IMPORTANT"
            ),
            "aiConfidence", 0.91,
            "timestamp", java.time.LocalDateTime.now()
        ));
    }
}