package com.gogidix.infrastructure.ai.service;

import com.gogidix.platform.audit.AuditService;
import com.gogidix.platform.caching.CacheService;
import com.gogidix.platform.monitoring.MetricsService;
import com.gogidix.platform.security.SecurityService;
import com.gogidix.platform.validation.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-powered Predictive Lead Scoring Service
 *
 * This service provides advanced lead scoring and qualification using machine learning,
 * behavioral analysis, and predictive analytics to identify high-quality leads.
 *
 * Features:
 * - ML-based lead scoring algorithms
 * - Behavioral pattern analysis
 * - Predictive lead qualification
 * - Lead conversion probability prediction
 * - Real-time lead scoring updates
 * - Lead segment classification
 * - Engagement scoring
 * - Lead lifecycle prediction
 * - Risk assessment for leads
 * - Automated lead routing
 */
@RestController
@RequestMapping("/ai/v1/predictive-lead-scoring")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Predictive Lead Scoring AI Service", description = "AI-powered lead scoring and qualification system")
public class PredictiveLeadScoringAIService {

    private final CacheService cacheService;
    private final MetricsService metricsService;
    private final AuditService auditService;
    private final SecurityService securityService;
    private final ValidationService validationService;

    // Lead Scoring Models
    private final LeadScoringEngine leadScoringEngine;
    private final BehavioralAnalysisEngine behavioralEngine;
    private final LeadQualificationPredictor qualificationPredictor;
    private final ConversionProbabilityAnalyzer conversionAnalyzer;
    private final LeadSegmentationEngine segmentationEngine;
    private final EngagementScoringEngine engagementEngine;
    private final LeadLifecyclePredictor lifecyclePredictor;
    private final LeadRiskAssessmentEngine riskAssessmentEngine;
    private final LeadRoutingEngine routingEngine;
    private final LeadPerformanceAnalytics performanceAnalytics;

    /**
     * Score leads using ML algorithms
     */
    @PostMapping("/score/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Score leads",
        description = "Provides ML-based lead scoring with detailed analysis"
    )
    public CompletableFuture<ResponseEntity<LeadScoringResult>> scoreLeads(
            @PathVariable String agentId,
            @Valid @RequestBody LeadScoringRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.lead.scoring.score");

            try {
                log.info("Scoring leads for agent: {}", agentId);

                // Validate request
                validationService.validate(request);
                securityService.validateAgentAccess(agentId);

                // Score leads
                LeadScoringResult result = leadScoringEngine.scoreLeads(agentId, request);

                // Cache results
                cacheService.set("lead-scores:" + agentId + ":" + request.getLeads().hashCode(),
                               result, java.time.Duration.ofMinutes(15));

                // Record metrics
                metricsService.recordCounter("ai.lead.scoring.success");
                metricsService.recordTimer("ai.lead.scoring.score", stopwatch);

                // Audit
                auditService.audit(
                    "LEADS_SCORED",
                    "agentId=" + agentId + ",leadsCount=" + request.getLeads().size(),
                    "ai-predictive-lead-scoring",
                    "success"
                );

                log.info("Successfully scored {} leads for agent: {}", result.getScoredLeads().size(), agentId);
                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.scoring.error");
                log.error("Error scoring leads for agent: {}", agentId, e);
                throw new RuntimeException("Lead scoring failed", e);
            }
        });
    }

    /**
     * Analyze lead behavioral patterns
     */
    @PostMapping("/behavioral-analysis/{leadId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Analyze lead behavior",
        description = "Analyzes behavioral patterns to predict lead quality"
    )
    public CompletableFuture<ResponseEntity<BehavioralAnalysisResult>> analyzeBehavior(
            @PathVariable String leadId,
            @Valid @RequestBody BehavioralAnalysisRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing behavior for lead: {}", leadId);

                BehavioralAnalysisResult result = behavioralEngine.analyzeBehavior(leadId, request);

                metricsService.recordCounter("ai.lead.behavioral-analysis.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.behavioral-analysis.error");
                log.error("Error analyzing behavior for lead: {}", leadId, e);
                throw new RuntimeException("Behavioral analysis failed", e);
            }
        });
    }

    /**
     * Predict lead qualification
     */
    @PostMapping("/qualification-prediction/{leadId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Predict lead qualification",
        description = "Predicts the probability of lead qualification"
    )
    public CompletableFuture<ResponseEntity<QualificationPredictionResult>> predictQualification(
            @PathVariable String leadId,
            @Valid @RequestBody QualificationPredictionRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.lead.qualification.prediction");

            try {
                log.info("Predicting qualification for lead: {}", leadId);

                QualificationPredictionResult result = qualificationPredictor.predictQualification(leadId, request);

                metricsService.recordCounter("ai.lead.qualification-prediction.success");
                metricsService.recordTimer("ai.lead.qualification.prediction", stopwatch);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.qualification-prediction.error");
                log.error("Error predicting qualification for lead: {}", leadId, e);
                throw new RuntimeException("Qualification prediction failed", e);
            }
        });
    }

    /**
     * Analyze conversion probability
     */
    @PostMapping("/conversion-probability/{leadId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Analyze conversion probability",
        description = "Analyzes the probability of lead conversion"
    )
    public CompletableFuture<ResponseEntity<ConversionProbabilityResult>> analyzeConversionProbability(
            @PathVariable String leadId,
            @Valid @RequestBody ConversionProbabilityRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing conversion probability for lead: {}", leadId);

                ConversionProbabilityResult result = conversionAnalyzer.analyzeConversionProbability(leadId, request);

                metricsService.recordCounter("ai.lead.conversion-probability.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.conversion-probability.error");
                log.error("Error analyzing conversion probability for lead: {}", leadId, e);
                throw new RuntimeException("Conversion probability analysis failed", e);
            }
        });
    }

    /**
     * Segment leads
     */
    @PostMapping("/segmentation/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Segment leads",
        description = "Segments leads based on ML classification"
    )
    public CompletableFuture<ResponseEntity<LeadSegmentationResult>> segmentLeads(
            @PathVariable String agentId,
            @Valid @RequestBody LeadSegmentationRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.lead.segmentation");

            try {
                log.info("Segmenting leads for agent: {}", agentId);

                LeadSegmentationResult result = segmentationEngine.segmentLeads(agentId, request);

                metricsService.recordCounter("ai.lead.segmentation.success");
                metricsService.recordTimer("ai.lead.segmentation", stopwatch);

                auditService.audit(
                    "LEADS_SEGMENTED",
                    "agentId=" + agentId + ",segments=" + result.getSegments().size(),
                    "ai-predictive-lead-scoring",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.segmentation.error");
                log.error("Error segmenting leads for agent: {}", agentId, e);
                throw new RuntimeException("Lead segmentation failed", e);
            }
        });
    }

    /**
     * Score lead engagement
     */
    @PostMapping("/engagement-scoring/{leadId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Score lead engagement",
        description = "Calculates engagement score based on lead interactions"
    )
    public CompletableFuture<ResponseEntity<EngagementScoringResult>> scoreEngagement(
            @PathVariable String leadId,
            @Valid @RequestBody EngagementScoringRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Scoring engagement for lead: {}", leadId);

                EngagementScoringResult result = engagementEngine.scoreEngagement(leadId, request);

                metricsService.recordCounter("ai.lead.engagement-scoring.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.engagement-scoring.error");
                log.error("Error scoring engagement for lead: {}", leadId, e);
                throw new RuntimeException("Engagement scoring failed", e);
            }
        });
    }

    /**
     * Predict lead lifecycle
     */
    @PostMapping("/lifecycle-prediction/{leadId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Predict lead lifecycle",
        description = "Predicts the entire lead lifecycle timeline"
    )
    public CompletableFuture<ResponseEntity<LifecyclePredictionResult>> predictLifecycle(
            @PathVariable String leadId,
            @Valid @RequestBody LifecyclePredictionRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Predicting lifecycle for lead: {}", leadId);

                LifecyclePredictionResult result = lifecyclePredictor.predictLifecycle(leadId, request);

                metricsService.recordCounter("ai.lead.lifecycle-prediction.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.lifecycle-prediction.error");
                log.error("Error predicting lifecycle for lead: {}", leadId, e);
                throw new RuntimeException("Lifecycle prediction failed", e);
            }
        });
    }

    /**
     * Assess lead risk
     */
    @PostMapping("/risk-assessment/{leadId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Assess lead risk",
        description = "Assesses risks associated with the lead"
    )
    public CompletableFuture<ResponseEntity<RiskAssessmentResult>> assessRisk(
            @PathVariable String leadId,
            @Valid @RequestBody RiskAssessmentRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.lead.risk-assessment");

            try {
                log.info("Assessing risk for lead: {}", leadId);

                RiskAssessmentResult result = riskAssessmentEngine.assessRisk(leadId, request);

                metricsService.recordCounter("ai.lead.risk-assessment.success");
                metricsService.recordTimer("ai.lead.risk-assessment", stopwatch);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.risk-assessment.error");
                log.error("Error assessing risk for lead: {}", leadId, e);
                throw new RuntimeException("Risk assessment failed", e);
            }
        });
    }

    /**
     * Route leads automatically
     */
    @PostMapping("/routing/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_MANAGER')")
    @Operation(
        summary = "Route leads",
        description = "Automatically routes leads to appropriate agents"
    )
    public CompletableFuture<ResponseEntity<LeadRoutingResult>> routeLeads(
            @PathVariable String agentId,
            @Valid @RequestBody LeadRoutingRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.lead.routing");

            try {
                log.info("Routing leads for agent: {}", agentId);

                LeadRoutingResult result = routingEngine.routeLeads(agentId, request);

                metricsService.recordCounter("ai.lead.routing.success");
                metricsService.recordTimer("ai.lead.routing", stopwatch);

                auditService.audit(
                    "LEADS_ROUTED",
                    "agentId=" + agentId + ",routedCount=" + result.getRoutedLeads().size(),
                    "ai-predictive-lead-scoring",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.routing.error");
                log.error("Error routing leads for agent: {}", agentId, e);
                throw new RuntimeException("Lead routing failed", e);
            }
        });
    }

    /**
     * Analyze lead performance
     */
    @PostMapping("/performance-analytics/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_MANAGER')")
    @Operation(
        summary = "Analyze lead performance",
        description = "Provides analytics on lead performance and conversion"
    )
    public CompletableFuture<ResponseEntity<LeadPerformanceAnalytics>> analyzePerformance(
            @PathVariable String agentId,
            @Valid @RequestBody PerformanceAnalyticsRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing lead performance for agent: {}", agentId);

                LeadPerformanceAnalytics analytics = performanceAnalytics.analyzePerformance(agentId, request);

                metricsService.recordCounter("ai.lead.performance-analytics.success");

                return ResponseEntity.ok(analytics);

            } catch (Exception e) {
                metricsService.recordCounter("ai.lead.performance-analytics.error");
                log.error("Error analyzing performance for agent: {}", agentId, e);
                throw new RuntimeException("Performance analytics failed", e);
            }
        });
    }

    // Helper Methods
    private void updateLeadScoresCache(String agentId, List<ScoredLead> scoredLeads) {
        scoredLeads.forEach(lead ->
            cacheService.set("lead-score:" + lead.getLeadId(), lead, java.time.Duration.ofHours(1))
        );
    }
}

// Data Transfer Objects and Models

class LeadScoringRequest {
    private List<LeadData> leads;
    private String scoringModel;
    private boolean includeDetailedAnalysis = true;
    private List<String> scoringFactors;

    // Getters and setters
    public List<LeadData> getLeads() { return leads; }
    public void setLeads(List<LeadData> leads) { this.leads = leads; }
    public String getScoringModel() { return scoringModel; }
    public void setScoringModel(String scoringModel) { this.scoringModel = scoringModel; }
    public boolean isIncludeDetailedAnalysis() { return includeDetailedAnalysis; }
    public void setIncludeDetailedAnalysis(boolean includeDetailedAnalysis) { this.includeDetailedAnalysis = includeDetailedAnalysis; }
    public List<String> getScoringFactors() { return scoringFactors; }
    public void setScoringFactors(List<String> scoringFactors) { this.scoringFactors = scoringFactors; }
}

class LeadScoringResult {
    private String agentId;
    private LocalDateTime scoringDate;
    private String modelUsed;
    private List<ScoredLead> scoredLeads;
    private Map<String, Double> factorWeights;
    private List<String> scoringInsights;

    // Getters and setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public LocalDateTime getScoringDate() { return scoringDate; }
    public void setScoringDate(LocalDateTime scoringDate) { this.scoringDate = scoringDate; }
    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    public List<ScoredLead> getScoredLeads() { return scoredLeads; }
    public void setScoredLeads(List<ScoredLead> scoredLeads) { this.scoredLeads = scoredLeads; }
    public Map<String, Double> getFactorWeights() { return factorWeights; }
    public void setFactorWeights(Map<String, Double> factorWeights) { this.factorWeights = factorWeights; }
    public List<String> getScoringInsights() { return scoringInsights; }
    public void setScoringInsights(List<String> scoringInsights) { this.scoringInsights = scoringInsights; }
}

class ScoredLead {
    private String leadId;
    private String leadName;
    private double overallScore;
    private double qualityScore;
    private double conversionProbability;
    private String leadGrade;
    private Map<String, Double> factorScores;
    private List<String> strengths;
    private List<String> weaknesses;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
    public double getConversionProbability() { return conversionProbability; }
    public void setConversionProbability(double conversionProbability) { this.conversionProbability = conversionProbability; }
    public String getLeadGrade() { return leadGrade; }
    public void setLeadGrade(String leadGrade) { this.leadGrade = leadGrade; }
    public Map<String, Double> getFactorScores() { return factorScores; }
    public void setFactorScores(Map<String, Double> factorScores) { this.factorScores = factorScores; }
    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }
    public List<String> getWeaknesses() { return weaknesses; }
    public void setWeaknesses(List<String> weaknesses) { this.weaknesses = weaknesses; }
}

class LeadData {
    private String leadId;
    private String contactInfo;
    private String propertyInterest;
    private String budgetRange;
    private String timeline;
    private List<String> activities;
    private Map<String, Object> demographics;
    private Map<String, Object> behaviorData;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public String getPropertyInterest() { return propertyInterest; }
    public void setPropertyInterest(String propertyInterest) { this.propertyInterest = propertyInterest; }
    public String getBudgetRange() { return budgetRange; }
    public void setBudgetRange(String budgetRange) { this.budgetRange = budgetRange; }
    public String getTimeline() { return timeline; }
    public void setTimeline(String timeline) { this.timeline = timeline; }
    public List<String> getActivities() { return activities; }
    public void setActivities(List<String> activities) { this.activities = activities; }
    public Map<String, Object> getDemographics() { return demographics; }
    public void setDemographics(Map<String, Object> demographics) { this.demographics = demographics; }
    public Map<String, Object> getBehaviorData() { return behaviorData; }
    public void setBehaviorData(Map<String, Object> behaviorData) { this.behaviorData = behaviorData; }
}

class BehavioralAnalysisRequest {
    private String leadId;
    private String analysisPeriod;
    private List<String> behaviorTypes;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getAnalysisPeriod() { return analysisPeriod; }
    public void setAnalysisPeriod(String analysisPeriod) { this.analysisPeriod = analysisPeriod; }
    public List<String> getBehaviorTypes() { return behaviorTypes; }
    public void setBehaviorTypes(List<String> behaviorTypes) { this.behaviorTypes = behaviorTypes; }
}

class BehavioralAnalysisResult {
    private String leadId;
    private Map<String, Double> behaviorScores;
    private List<String> behaviorPatterns;
    private String behaviorCategory;
    private double behaviorQualityScore;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public Map<String, Double> getBehaviorScores() { return behaviorScores; }
    public void setBehaviorScores(Map<String, Double> behaviorScores) { this.behaviorScores = behaviorScores; }
    public List<String> getBehaviorPatterns() { return behaviorPatterns; }
    public void setBehaviorPatterns(List<String> behaviorPatterns) { this.behaviorPatterns = behaviorPatterns; }
    public String getBehaviorCategory() { return behaviorCategory; }
    public void setBehaviorCategory(String behaviorCategory) { this.behaviorCategory = behaviorCategory; }
    public double getBehaviorQualityScore() { return behaviorQualityScore; }
    public void setBehaviorQualityScore(double behaviorQualityScore) { this.behaviorQualityScore = behaviorQualityScore; }
}

class QualificationPredictionRequest {
    private String leadId;
    private List<String> qualificationCriteria;
    private String predictionTimeframe;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public List<String> getQualificationCriteria() { return qualificationCriteria; }
    public void setQualificationCriteria(List<String> qualificationCriteria) { this.qualificationCriteria = qualificationCriteria; }
    public String getPredictionTimeframe() { return predictionTimeframe; }
    public void setPredictionTimeframe(String predictionTimeframe) { this.predictionTimeframe = predictionTimeframe; }
}

class QualificationPredictionResult {
    private String leadId;
    private double qualificationProbability;
    private LocalDateTime predictedQualificationDate;
    private List<String> qualificationFactors;
    private List<String> improvementRecommendations;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public double getQualificationProbability() { return qualificationProbability; }
    public void setQualificationProbability(double qualificationProbability) { this.qualificationProbability = qualificationProbability; }
    public LocalDateTime getPredictedQualificationDate() { return predictedQualificationDate; }
    public void setPredictedQualificationDate(LocalDateTime predictedQualificationDate) { this.predictedQualificationDate = predictedQualificationDate; }
    public List<String> getQualificationFactors() { return qualificationFactors; }
    public void setQualificationFactors(List<String> qualificationFactors) { this.qualificationFactors = qualificationFactors; }
    public List<String> getImprovementRecommendations() { return improvementRecommendations; }
    public void setImprovementRecommendations(List<String> improvementRecommendations) { this.improvementRecommendations = improvementRecommendations; }
}

class ConversionProbabilityRequest {
    private String leadId;
    private String conversionType;
    private List<String> conversionFactors;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getConversionType() { return conversionType; }
    public void setConversionType(String conversionType) { this.conversionType = conversionType; }
    public List<String> getConversionFactors() { return conversionFactors; }
    public void setConversionFactors(List<String> conversionFactors) { this.conversionFactors = conversionFactors; }
}

class ConversionProbabilityResult {
    private String leadId;
    private double conversionProbability;
    private String confidenceLevel;
    private LocalDateTime predictedConversionDate;
    private Map<String, Double> factorContributions;
    private List<String> conversionBarriers;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public double getConversionProbability() { return conversionProbability; }
    public void setConversionProbability(double conversionProbability) { this.conversionProbability = conversionProbability; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public LocalDateTime getPredictedConversionDate() { return predictedConversionDate; }
    public void setPredictedConversionDate(LocalDateTime predictedConversionDate) { this.predictedConversionDate = predictedConversionDate; }
    public Map<String, Double> getFactorContributions() { return factorContributions; }
    public void setFactorContributions(Map<String, Double> factorContributions) { this.factorContributions = factorContributions; }
    public List<String> getConversionBarriers() { return conversionBarriers; }
    public void setConversionBarriers(List<String> conversionBarriers) { this.conversionBarriers = conversionBarriers; }
}

class LeadSegmentationRequest {
    private List<String> leadIds;
    private String segmentationType;
    private List<String> segmentationCriteria;

    // Getters and setters
    public List<String> getLeadIds() { return leadIds; }
    public void setLeadIds(List<String> leadIds) { this.leadIds = leadIds; }
    public String getSegmentationType() { return segmentationType; }
    public void setSegmentationType(String segmentationType) { this.segmentationType = segmentationType; }
    public List<String> getSegmentationCriteria() { return segmentationCriteria; }
    public void setSegmentationCriteria(List<String> segmentationCriteria) { this.segmentationCriteria = segmentationCriteria; }
}

class LeadSegmentationResult {
    private List<LeadSegment> segments;
    private Map<String, List<String>> segmentLeads;
    private String segmentationModel;

    // Getters and setters
    public List<LeadSegment> getSegments() { return segments; }
    public void setSegments(List<LeadSegment> segments) { this.segments = segments; }
    public Map<String, List<String>> getSegmentLeads() { return segmentLeads; }
    public void setSegmentLeads(Map<String, List<String>> segmentLeads) { this.segmentLeads = segmentLeads; }
    public String getSegmentationModel() { return segmentationModel; }
    public void setSegmentationModel(String segmentationModel) { this.segmentationModel = segmentationModel; }
}

class LeadSegment {
    private String segmentName;
    private String description;
    private double averageScore;
    private int leadCount;
    private List<String> characteristics;

    // Getters and setters
    public String getSegmentName() { return segmentName; }
    public void setSegmentName(String segmentName) { this.segmentName = segmentName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
    public int getLeadCount() { return leadCount; }
    public void setLeadCount(int leadCount) { this.leadCount = leadCount; }
    public List<String> getCharacteristics() { return characteristics; }
    public void setCharacteristics(List<String> characteristics) { this.characteristics = characteristics; }
}

class EngagementScoringRequest {
    private String leadId;
    private String scoringPeriod;
    private List<String> engagementMetrics;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getScoringPeriod() { return scoringPeriod; }
    public void setScoringPeriod(String scoringPeriod) { this.scoringPeriod = scoringPeriod; }
    public List<String> getEngagementMetrics() { return engagementMetrics; }
    public void setEngagementMetrics(List<String> engagementMetrics) { this.engagementMetrics = engagementMetrics; }
}

class EngagementScoringResult {
    private String leadId;
    private double engagementScore;
    private Map<String, Double> metricScores;
    private String engagementLevel;
    private List<String> engagementTrends;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public double getEngagementScore() { return engagementScore; }
    public void setEngagementScore(double engagementScore) { this.engagementScore = engagementScore; }
    public Map<String, Double> getMetricScores() { return metricScores; }
    public void setMetricScores(Map<String, Double> metricScores) { this.metricScores = metricScores; }
    public String getEngagementLevel() { return engagementLevel; }
    public void setEngagementLevel(String engagementLevel) { this.engagementLevel = engagementLevel; }
    public List<String> getEngagementTrends() { return engagementTrends; }
    public void setEngagementTrends(List<String> engagementTrends) { this.engagementTrends = engagementTrends; }
}

class LifecyclePredictionRequest {
    private String leadId;
    private String predictionScope;
    private List<String> lifecycleStages;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getPredictionScope() { return predictionScope; }
    public void setPredictionScope(String predictionScope) { this.predictionScope = predictionScope; }
    public List<String> getLifecycleStages() { return lifecycleStages; }
    public void setLifecycleStages(List<String> lifecycleStages) { this.lifecycleStages = lifecycleStages; }
}

class LifecyclePredictionResult {
    private String leadId;
    private List<LifecycleStage> predictedStages;
    private LocalDateTime conversionDate;
    private double lifecycleConfidence;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public List<LifecycleStage> getPredictedStages() { return predictedStages; }
    public void setPredictedStages(List<LifecycleStage> predictedStages) { this.predictedStages = predictedStages; }
    public LocalDateTime getConversionDate() { return conversionDate; }
    public void setConversionDate(LocalDateTime conversionDate) { this.conversionDate = conversionDate; }
    public double getLifecycleConfidence() { return lifecycleConfidence; }
    public void setLifecycleConfidence(double lifecycleConfidence) { this.lifecycleConfidence = lifecycleConfidence; }
}

class LifecycleStage {
    private String stageName;
    private LocalDateTime predictedDate;
    private double probability;
    private int estimatedDays;

    // Getters and setters
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public LocalDateTime getPredictedDate() { return predictedDate; }
    public void setPredictedDate(LocalDateTime predictedDate) { this.predictedDate = predictedDate; }
    public double getProbability() { return probability; }
    public void setProbability(double probability) { this.probability = probability; }
    public int getEstimatedDays() { return estimatedDays; }
    public void setEstimatedDays(int estimatedDays) { this.estimatedDays = estimatedDays; }
}

class RiskAssessmentRequest {
    private String leadId;
    private List<String> riskFactors;
    private String assessmentScope;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }
    public String getAssessmentScope() { return assessmentScope; }
    public void setAssessmentScope(String assessmentScope) { this.assessmentScope = assessmentScope; }
}

class RiskAssessmentResult {
    private String leadId;
    private double overallRiskScore;
    private String riskCategory;
    private Map<String, Double> riskFactorScores;
    private List<String> riskMitigationStrategies;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public double getOverallRiskScore() { return overallRiskScore; }
    public void setOverallRiskScore(double overallRiskScore) { this.overallRiskScore = overallRiskScore; }
    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
    public Map<String, Double> getRiskFactorScores() { return riskFactorScores; }
    public void setRiskFactorScores(Map<String, Double> riskFactorScores) { this.riskFactorScores = riskFactorScores; }
    public List<String> getRiskMitigationStrategies() { return riskMitigationStrategies; }
    public void setRiskMitigationStrategies(List<String> riskMitigationStrategies) { this.riskMitigationStrategies = riskMitigationStrategies; }
}

class LeadRoutingRequest {
    private List<String> leadIds;
    private List<String> availableAgents;
    private String routingStrategy;

    // Getters and setters
    public List<String> getLeadIds() { return leadIds; }
    public void setLeadIds(List<String> leadIds) { this.leadIds = leadIds; }
    public List<String> getAvailableAgents() { return availableAgents; }
    public void setAvailableAgents(List<String> availableAgents) { this.availableAgents = availableAgents; }
    public String getRoutingStrategy() { return routingStrategy; }
    public void setRoutingStrategy(String routingStrategy) { this.routingStrategy = routingStrategy; }
}

class LeadRoutingResult {
    private List<RoutedLead> routedLeads;
    private String routingAlgorithm;
    private double routingEfficiency;

    // Getters and setters
    public List<RoutedLead> getRoutedLeads() { return routedLeads; }
    public void setRoutedLeads(List<RoutedLead> routedLeads) { this.routedLeads = routedLeads; }
    public String getRoutingAlgorithm() { return routingAlgorithm; }
    public void setRoutingAlgorithm(String routingAlgorithm) { this.routingAlgorithm = routingAlgorithm; }
    public double getRoutingEfficiency() { return routingEfficiency; }
    public void setRoutingEfficiency(double routingEfficiency) { this.routingEfficiency = routingEfficiency; }
}

class RoutedLead {
    private String leadId;
    private String assignedAgent;
    private double assignmentScore;
    private String assignmentReason;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(String assignedAgent) { this.assignedAgent = assignedAgent; }
    public double getAssignmentScore() { return assignmentScore; }
    public void setAssignmentScore(double assignmentScore) { this.assignmentScore = assignmentScore; }
    public String getAssignmentReason() { return assignmentReason; }
    public void setAssignmentReason(String assignmentReason) { this.assignmentReason = assignmentReason; }
}

class PerformanceAnalyticsRequest {
    private String agentId;
    private String analyticsPeriod;
    private List<String> metrics;

    // Getters and setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getAnalyticsPeriod() { return analyticsPeriod; }
    public void setAnalyticsPeriod(String analyticsPeriod) { this.analyticsPeriod = analyticsPeriod; }
    public List<String> getMetrics() { return metrics; }
    public void setMetrics(List<String> metrics) { this.metrics = metrics; }
}

class LeadPerformanceAnalytics {
    private String agentId;
    private LocalDateTime analysisDate;
    private double overallPerformanceScore;
    private Map<String, Double> metricScores;
    private List<String> performanceInsights;
    private List<String> improvementRecommendations;

    // Getters and setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public LocalDateTime getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(LocalDateTime analysisDate) { this.analysisDate = analysisDate; }
    public double getOverallPerformanceScore() { return overallPerformanceScore; }
    public void setOverallPerformanceScore(double overallPerformanceScore) { this.overallPerformanceScore = overallPerformanceScore; }
    public Map<String, Double> getMetricScores() { return metricScores; }
    public void setMetricScores(Map<String, Double> metricScores) { this.metricScores = metricScores; }
    public List<String> getPerformanceInsights() { return performanceInsights; }
    public void setPerformanceInsights(List<String> performanceInsights) { this.performanceInsights = performanceInsights; }
    public List<String> getImprovementRecommendations() { return improvementRecommendations; }
    public void setImprovementRecommendations(List<String> improvementRecommendations) { this.improvementRecommendations = improvementRecommendations; }
}

// AI Service Interfaces (to be implemented)
interface LeadScoringEngine {
    LeadScoringResult scoreLeads(String agentId, LeadScoringRequest request);
}

interface BehavioralAnalysisEngine {
    BehavioralAnalysisResult analyzeBehavior(String leadId, BehavioralAnalysisRequest request);
}

interface LeadQualificationPredictor {
    QualificationPredictionResult predictQualification(String leadId, QualificationPredictionRequest request);
}

interface ConversionProbabilityAnalyzer {
    ConversionProbabilityResult analyzeConversionProbability(String leadId, ConversionProbabilityRequest request);
}

interface LeadSegmentationEngine {
    LeadSegmentationResult segmentLeads(String agentId, LeadSegmentationRequest request);
}

interface EngagementScoringEngine {
    EngagementScoringResult scoreEngagement(String leadId, EngagementScoringRequest request);
}

interface LeadLifecyclePredictor {
    LifecyclePredictionResult predictLifecycle(String leadId, LifecyclePredictionRequest request);
}

interface LeadRiskAssessmentEngine {
    RiskAssessmentResult assessRisk(String leadId, RiskAssessmentRequest request);
}

interface LeadRoutingEngine {
    LeadRoutingResult routeLeads(String agentId, LeadRoutingRequest request);
}

interface LeadPerformanceAnalytics {
    LeadPerformanceAnalytics analyzePerformance(String agentId, PerformanceAnalyticsRequest request);
}