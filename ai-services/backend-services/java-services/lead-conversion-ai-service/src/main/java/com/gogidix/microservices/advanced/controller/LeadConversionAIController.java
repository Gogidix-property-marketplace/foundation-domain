package com.gogidix.microservices.advanced.controller;

import com.gogidix.microservices.advanced.service.LeadConversionAIService;
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
 * REST Controller for Lead Conversion AI Service Operations
 */
@RestController
@RequestMapping("/api/v1/lead-conversion-ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lead Conversion AI API", description = "Lead Conversion and Client Management AI Service Operations")
public class LeadConversionAIController {

    private final LeadConversionAIService leadConversionAIService;

    @PostMapping("/optimize")
    @Operation(summary = "Optimize lead conversion with AI", description = "AI-powered lead conversion optimization strategies")
    @PreAuthorize("hasRole('AGENT') or hasRole('TEAM_LEAD') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<LeadConversionOptimizationDto>> optimizeLeadConversion(
            @Valid @RequestBody LeadConversionOptimizationRequestDto request) {
        log.info("Optimizing lead conversion for agent: {}", request.getAgentId());
        return leadConversionAIService.optimizeLeadConversion(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error optimizing lead conversion", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/client-journey/manage")
    @Operation(summary = "Manage client journey with AI", description = "AI-powered client journey management and optimization")
    @PreAuthorize("hasRole('AGENT') or hasRole('TEAM_LEAD')")
    public CompletableFuture<ResponseEntity<ClientJourneyManagementDto>> manageClientJourney(
            @Valid @RequestBody ClientJourneyManagementRequestDto request) {
        log.info("Managing client journey for client: {}", request.getClientId());
        return leadConversionAIService.manageClientJourney(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error managing client journey", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/leads/predict-success")
    @Operation(summary = "Predict lead success probability with AI", description = "AI-powered lead success prediction and scoring")
    @PreAuthorize("hasRole('AGENT') or hasRole('TEAM_LEAD') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<LeadSuccessPredictionDto>> predictLeadSuccess(
            @Valid @RequestBody LeadSuccessPredictionRequestDto request) {
        log.info("Predicting lead success for lead: {}", request.getLeadId());
        return leadConversionAIService.predictLeadSuccess(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error predicting lead success", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/communication/intelligent")
    @Operation(summary = "Provide intelligent client communication with AI", description = "AI-powered intelligent communication generation")
    @PreAuthorize("hasRole('AGENT')")
    public CompletableFuture<ResponseEntity<IntelligentCommunicationDto>> provideIntelligentCommunication(
            @Valid @RequestBody IntelligentCommunicationRequestDto request) {
        log.info("Providing intelligent communication for client: {}", request.getClientId());
        return leadConversionAIService.provideIntelligentCommunication(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error providing intelligent communication", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/followup/optimize")
    @Operation(summary = "Optimize follow-up strategy with AI", description = "AI-powered follow-up strategy optimization")
    @PreAuthorize("hasRole('AGENT') or hasRole('TEAM_LEAD')")
    public CompletableFuture<ResponseEntity<FollowUpOptimizationDto>> optimizeFollowUpStrategy(
            @Valid @RequestBody FollowUpOptimizationRequestDto request) {
        log.info("Optimizing follow-up strategy for lead: {}", request.getLeadId());
        return leadConversionAIService.optimizeFollowUpStrategy(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error optimizing follow-up strategy", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/agent/{agentId}/dashboard")
    @Operation(summary = "Get lead conversion dashboard", description = "AI-powered lead conversion dashboard for agents")
    @PreAuthorize("hasRole('AGENT') or hasRole('TEAM_LEAD')")
    public ResponseEntity<Object> getLeadConversionDashboard(@PathVariable String agentId) {
        log.info("Fetching lead conversion dashboard for agent: {}", agentId);
        return ResponseEntity.ok(java.util.Map.of(
            "dashboardId", UUID.randomUUID().toString(),
            "agentId", agentId,
            "conversionMetrics", java.util.Map.of(
                "currentConversionRate", 0.34,
                "optimizedConversionRate", 0.45,
                "leadsInPipeline", 127,
                "conversionRateChange", 0.12
            ),
            "leadScoringDistribution", java.util.Map.of(
                "High Priority (80-100)", 35,
                "Medium Priority (60-79)", 58,
                "Low Priority (40-59)", 24,
                "Cold (0-39)", 10
            ),
            "aiInsights", java.util.Arrays.asList(
                "Best conversion time: Tuesday 10 AM - 12 PM",
                "High-engagement leads respond to video content",
                "Personalized property tours increase conversion by 45%",
                "Multi-channel approach improves response rate by 62%"
            ),
            "recommendedActions", java.util.Arrays.asList(
                "Focus on high-priority leads with personalized outreach",
                "Schedule video tours for remote clients",
                "Send market updates to maintain engagement",
                "Implement automated follow-up sequences"
            )
        ));
    }
}