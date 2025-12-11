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
 * AI-powered Sales Support Service
 *
 * This service provides intelligent sales support, conversation assistance, and automated sales processes
 * using AI to enhance agent productivity and close rates.
 *
 * Features:
 * - AI-powered conversation assistance
 * - Automated follow-up recommendations
 * - Sales script generation
 * - Objection handling suggestions
 * - Real-time conversation analysis
 * - Sales opportunity identification
 * - Customer sentiment analysis
 * - Deal probability prediction
 * - Sales performance optimization
 * - Automated meeting scheduling
 */
@RestController
@RequestMapping("/ai/v1/sales-support")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sales Support AI Service", description = "AI-powered sales automation and support system")
public class SalesSupportAIService {

    private final CacheService cacheService;
    private final MetricsService metricsService;
    private final AuditService auditService;
    private final SecurityService securityService;
    private final ValidationService validationService;

    // Sales Support Models
    private final ConversationAssistant conversationAssistant;
    private final FollowUpRecommendationEngine followUpEngine;
    private final SalesScriptGenerator scriptGenerator;
    private final ObjectionHandlingEngine objectionEngine;
    private final ConversationAnalyzer conversationAnalyzer;
    private final SalesOpportunityDetector opportunityDetector;
    private final CustomerSentimentAnalyzer sentimentAnalyzer;
    private final DealProbabilityPredictor dealPredictor;
    private final SalesPerformanceOptimizer performanceOptimizer;
    private final MeetingSchedulerAI meetingScheduler;

    /**
     * Get conversation assistance
     */
    @PostMapping("/conversation-assist/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Conversation assistance",
        description = "Provides real-time AI assistance during sales conversations"
    )
    public CompletableFuture<ResponseEntity<ConversationAssistanceResult>> assistConversation(
            @PathVariable String agentId,
            @Valid @RequestBody ConversationAssistanceRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.sales.conversation.assist");

            try {
                log.info("Providing conversation assistance for agent: {}", agentId);

                // Validate request
                validationService.validate(request);
                securityService.validateAgentAccess(agentId);

                // Get conversation assistance
                ConversationAssistanceResult result = conversationAssistant.assistConversation(agentId, request);

                // Cache results
                cacheService.set("conversation-assist:" + agentId + ":" + request.getConversationId(),
                               result, java.time.Duration.ofMinutes(5));

                // Record metrics
                metricsService.recordCounter("ai.sales.conversation-assist.success");
                metricsService.recordTimer("ai.sales.conversation.assist", stopwatch);

                // Audit
                auditService.audit(
                    "CONVERSATION_ASSISTED",
                    "agentId=" + agentId + ",conversationId=" + request.getConversationId(),
                    "ai-sales-support",
                    "success"
                );

                log.info("Successfully provided conversation assistance for agent: {}", agentId);
                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.conversation-assist.error");
                log.error("Error providing conversation assistance for agent: {}", agentId, e);
                throw new RuntimeException("Conversation assistance failed", e);
            }
        });
    }

    /**
     * Generate follow-up recommendations
     */
    @PostMapping("/follow-up-recommendations/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Generate follow-up recommendations",
        description = "Provides AI-powered follow-up recommendations for leads"
    )
    public CompletableFuture<ResponseEntity<FollowUpRecommendationResult>> generateFollowUpRecommendations(
            @PathVariable String agentId,
            @Valid @RequestBody FollowUpRecommendationRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.sales.follow-up.recommendations");

            try {
                log.info("Generating follow-up recommendations for agent: {}", agentId);

                FollowUpRecommendationResult result = followUpEngine.generateRecommendations(agentId, request);

                metricsService.recordCounter("ai.sales.follow-up-recommendations.success");
                metricsService.recordTimer("ai.sales.follow-up.recommendations", stopwatch);

                auditService.audit(
                    "FOLLOW_UP_RECOMMENDATIONS_GENERATED",
                    "agentId=" + agentId + ",leadsCount=" + request.getLeads().size(),
                    "ai-sales-support",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.follow-up-recommendations.error");
                log.error("Error generating follow-up recommendations for agent: {}", agentId, e);
                throw new RuntimeException("Follow-up recommendation generation failed", e);
            }
        });
    }

    /**
     * Generate sales scripts
     */
    @PostMapping("/script-generator/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Generate sales scripts",
        description = "Generates customized sales scripts for different scenarios"
    )
    public CompletableFuture<ResponseEntity<SalesScriptResult>> generateSalesScript(
            @PathVariable String agentId,
            @Valid @RequestBody SalesScriptRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating sales script for agent: {}", agentId);

                SalesScriptResult result = scriptGenerator.generateScript(agentId, request);

                metricsService.recordCounter("ai.sales.script-generator.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.script-generator.error");
                log.error("Error generating sales script for agent: {}", agentId, e);
                throw new RuntimeException("Sales script generation failed", e);
            }
        });
    }

    /**
     * Handle objections
     */
    @PostMapping("/objection-handling/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Handle objections",
        description = "Provides AI-powered objection handling strategies"
    )
    public CompletableFuture<ResponseEntity<ObjectionHandlingResult>> handleObjections(
            @PathVariable String agentId,
            @Valid @RequestBody ObjectionHandlingRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.sales.objection-handling");

            try {
                log.info("Handling objections for agent: {}", agentId);

                ObjectionHandlingResult result = objectionEngine.handleObjections(agentId, request);

                metricsService.recordCounter("ai.sales.objection-handling.success");
                metricsService.recordTimer("ai.sales.objection-handling", stopwatch);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.objection-handling.error");
                log.error("Error handling objections for agent: {}", agentId, e);
                throw new RuntimeException("Objection handling failed", e);
            }
        });
    }

    /**
     * Analyze conversation
     */
    @PostMapping("/conversation-analysis/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Analyze conversation",
        description = "Analyzes sales conversations for insights and improvements"
    )
    public CompletableFuture<ResponseEntity<ConversationAnalysisResult>> analyzeConversation(
            @PathVariable String agentId,
            @Valid @RequestBody ConversationAnalysisRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing conversation for agent: {}", agentId);

                ConversationAnalysisResult result = conversationAnalyzer.analyzeConversation(agentId, request);

                metricsService.recordCounter("ai.sales.conversation-analysis.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.conversation-analysis.error");
                log.error("Error analyzing conversation for agent: {}", agentId, e);
                throw new RuntimeException("Conversation analysis failed", e);
            }
        });
    }

    /**
     * Detect sales opportunities
     */
    @PostMapping("/opportunity-detection/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Detect sales opportunities",
        description = "Identifies potential sales opportunities from interactions"
    )
    public CompletableFuture<ResponseEntity<SalesOpportunityResult>> detectOpportunities(
            @PathVariable String agentId,
            @Valid @RequestBody SalesOpportunityRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.sales.opportunity-detection");

            try {
                log.info("Detecting sales opportunities for agent: {}", agentId);

                SalesOpportunityResult result = opportunityDetector.detectOpportunities(agentId, request);

                metricsService.recordCounter("ai.sales.opportunity-detection.success");
                metricsService.recordTimer("ai.sales.opportunity-detection", stopwatch);

                auditService.audit(
                    "SALES_OPPORTUNITIES_DETECTED",
                    "agentId=" + agentId + ",opportunities=" + result.getOpportunities().size(),
                    "ai-sales-support",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.opportunity-detection.error");
                log.error("Error detecting opportunities for agent: {}", agentId, e);
                throw new RuntimeException("Opportunity detection failed", e);
            }
        });
    }

    /**
     * Analyze customer sentiment
     */
    @PostMapping("/sentiment-analysis/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Analyze customer sentiment",
        description = "Analyzes customer sentiment during sales interactions"
    )
    public CompletableFuture<ResponseEntity<SentimentAnalysisResult>> analyzeSentiment(
            @PathVariable String agentId,
            @Valid @RequestBody SentimentAnalysisRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing customer sentiment for agent: {}", agentId);

                SentimentAnalysisResult result = sentimentAnalyzer.analyzeSentiment(agentId, request);

                metricsService.recordCounter("ai.sales.sentiment-analysis.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.sentiment-analysis.error");
                log.error("Error analyzing sentiment for agent: {}", agentId, e);
                throw new RuntimeException("Sentiment analysis failed", e);
            }
        });
    }

    /**
     * Predict deal probability
     */
    @PostMapping("/deal-probability/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Predict deal probability",
        description = "Predicts the probability of closing deals"
    )
    public CompletableFuture<ResponseEntity<DealProbabilityResult>> predictDealProbability(
            @PathVariable String agentId,
            @Valid @RequestBody DealProbabilityRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.sales.deal-probability");

            try {
                log.info("Predicting deal probability for agent: {}", agentId);

                DealProbabilityResult result = dealPredictor.predictDealProbability(agentId, request);

                metricsService.recordCounter("ai.sales.deal-probability.success");
                metricsService.recordTimer("ai.sales.deal-probability", stopwatch);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.deal-probability.error");
                log.error("Error predicting deal probability for agent: {}", agentId, e);
                throw new RuntimeException("Deal probability prediction failed", e);
            }
        });
    }

    /**
     * Optimize sales performance
     */
    @PostMapping("/performance-optimization/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Optimize sales performance",
        description = "Provides recommendations to optimize sales performance"
    )
    public CompletableFuture<ResponseEntity<SalesPerformanceResult>> optimizePerformance(
            @PathVariable String agentId,
            @Valid @RequestBody SalesPerformanceRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Optimizing sales performance for agent: {}", agentId);

                SalesPerformanceResult result = performanceOptimizer.optimizePerformance(agentId, request);

                metricsService.recordCounter("ai.sales.performance-optimization.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.performance-optimization.error");
                log.error("Error optimizing performance for agent: {}", agentId, e);
                throw new RuntimeException("Performance optimization failed", e);
            }
        });
    }

    /**
     * Schedule meetings
     */
    @PostMapping("/meeting-scheduler/{agentId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Schedule meetings",
        description = "AI-powered meeting scheduling and optimization"
    )
    public CompletableFuture<ResponseEntity<MeetingSchedulingResult>> scheduleMeetings(
            @PathVariable String agentId,
            @Valid @RequestBody MeetingSchedulingRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.sales.meeting-scheduling");

            try {
                log.info("Scheduling meetings for agent: {}", agentId);

                MeetingSchedulingResult result = meetingScheduler.scheduleMeetings(agentId, request);

                metricsService.recordCounter("ai.sales.meeting-scheduling.success");
                metricsService.recordTimer("ai.sales.meeting-scheduling", stopwatch);

                auditService.audit(
                    "MEETINGS_SCHEDULED",
                    "agentId=" + agentId + ",meetingsCount=" + result.getScheduledMeetings().size(),
                    "ai-sales-support",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.sales.meeting-scheduling.error");
                log.error("Error scheduling meetings for agent: {}", agentId, e);
                throw new RuntimeException("Meeting scheduling failed", e);
            }
        });
    }

    // Helper Methods
    private void cacheConversationInsights(String agentId, String conversationId, ConversationAnalysisResult result) {
        cacheService.set("conversation-insights:" + agentId + ":" + conversationId,
                       result, java.time.Duration.ofHours(24));
    }
}

// Data Transfer Objects and Models

class ConversationAssistanceRequest {
    private String conversationId;
    private String conversationContext;
    private String currentCustomerMessage;
    private List<String> conversationHistory;
    private String salesStage;

    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getConversationContext() { return conversationContext; }
    public void setConversationContext(String conversationContext) { this.conversationContext = conversationContext; }
    public String getCurrentCustomerMessage() { return currentCustomerMessage; }
    public void setCurrentCustomerMessage(String currentCustomerMessage) { this.currentCustomerMessage = currentCustomerMessage; }
    public List<String> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<String> conversationHistory) { this.conversationHistory = conversationHistory; }
    public String getSalesStage() { return salesStage; }
    public void setSalesStage(String salesStage) { this.salesStage = salesStage; }
}

class ConversationAssistanceResult {
    private String conversationId;
    private String suggestedResponse;
    private List<String> talkingPoints;
    private List<String> questionsToAsk;
    private String recommendedAction;
    private double confidenceScore;

    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getSuggestedResponse() { return suggestedResponse; }
    public void setSuggestedResponse(String suggestedResponse) { this.suggestedResponse = suggestedResponse; }
    public List<String> getTalkingPoints() { return talkingPoints; }
    public void setTalkingPoints(List<String> talkingPoints) { this.talkingPoints = talkingPoints; }
    public List<String> getQuestionsToAsk() { return questionsToAsk; }
    public void setQuestionsToAsk(List<String> questionsToAsk) { this.questionsToAsk = questionsToAsk; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
}

class FollowUpRecommendationRequest {
    private List<String> leads;
    private String recommendationType;
    private int maxRecommendations = 5;

    // Getters and setters
    public List<String> getLeads() { return leads; }
    public void setLeads(List<String> leads) { this.leads = leads; }
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public int getMaxRecommendations() { return maxRecommendations; }
    public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }
}

class FollowUpRecommendationResult {
    private List<FollowUpRecommendation> recommendations;
    private String recommendationStrategy;
    private LocalDateTime nextFollowUpDate;

    // Getters and setters
    public List<FollowUpRecommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<FollowUpRecommendation> recommendations) { this.recommendations = recommendations; }
    public String getRecommendationStrategy() { return recommendationStrategy; }
    public void setRecommendationStrategy(String recommendationStrategy) { this.recommendationStrategy = recommendationStrategy; }
    public LocalDateTime getNextFollowUpDate() { return nextFollowUpDate; }
    public void setNextFollowUpDate(LocalDateTime nextFollowUpDate) { this.nextFollowUpDate = nextFollowUpDate; }
}

class FollowUpRecommendation {
    private String leadId;
    private String recommendationType;
    private String recommendedAction;
    private LocalDateTime recommendedTime;
    private String messageTemplate;
    private double priorityScore;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public LocalDateTime getRecommendedTime() { return recommendedTime; }
    public void setRecommendedTime(LocalDateTime recommendedTime) { this.recommendedTime = recommendedTime; }
    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }
    public double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
}

class SalesScriptRequest {
    private String scriptType;
    private String targetAudience;
    private String propertyType;
    private List<String> keyFeatures;
    private String salesObjective;

    // Getters and setters
    public String getScriptType() { return scriptType; }
    public void setScriptType(String scriptType) { this.scriptType = scriptType; }
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public List<String> getKeyFeatures() { return keyFeatures; }
    public void setKeyFeatures(List<String> keyFeatures) { this.keyFeatures = keyFeatures; }
    public String getSalesObjective() { return salesObjective; }
    public void setSalesObjective(String salesObjective) { this.salesObjective = salesObjective; }
}

class SalesScriptResult {
    private String scriptType;
    private String generatedScript;
    private List<String> keyTalkingPoints;
    private List<String> objectionPreparation;
    private String closingTechnique;

    // Getters and setters
    public String getScriptType() { return scriptType; }
    public void setScriptType(String scriptType) { this.scriptType = scriptType; }
    public String getGeneratedScript() { return generatedScript; }
    public void setGeneratedScript(String generatedScript) { this.generatedScript = generatedScript; }
    public List<String> getKeyTalkingPoints() { return keyTalkingPoints; }
    public void setKeyTalkingPoints(List<String> keyTalkingPoints) { this.keyTalkingPoints = keyTalkingPoints; }
    public List<String> getObjectionPreparation() { return objectionPreparation; }
    public void setObjectionPreparation(List<String> objectionPreparation) { this.objectionPreparation = objectionPreparation; }
    public String getClosingTechnique() { return closingTechnique; }
    public void setClosingTechnique(String closingTechnique) { this.closingTechnique = closingTechnique; }
}

class ObjectionHandlingRequest {
    private String objectionType;
    private String customerObjection;
    private String context;
    private String customerProfile;

    // Getters and setters
    public String getObjectionType() { return objectionType; }
    public void setObjectionType(String objectionType) { this.objectionType = objectionType; }
    public String getCustomerObjection() { return customerObjection; }
    public void setCustomerObjection(String customerObjection) { this.customerObjection = customerObjection; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    public String getCustomerProfile() { return customerProfile; }
    public void setCustomerProfile(String customerProfile) { this.customerProfile = customerProfile; }
}

class ObjectionHandlingResult {
    private String objectionType;
    private List<String> responseStrategies;
    private String recommendedResponse;
    private List<String> supportingPoints;
    private String followUpQuestion;

    // Getters and setters
    public String getObjectionType() { return objectionType; }
    public void setObjectionType(String objectionType) { this.objectionType = objectionType; }
    public List<String> getResponseStrategies() { return responseStrategies; }
    public void setResponseStrategies(List<String> responseStrategies) { this.responseStrategies = responseStrategies; }
    public String getRecommendedResponse() { return recommendedResponse; }
    public void setRecommendedResponse(String recommendedResponse) { this.recommendedResponse = recommendedResponse; }
    public List<String> getSupportingPoints() { return supportingPoints; }
    public void setSupportingPoints(List<String> supportingPoints) { this.supportingPoints = supportingPoints; }
    public String getFollowUpQuestion() { return followUpQuestion; }
    public void setFollowUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; }
}

class ConversationAnalysisRequest {
    private String conversationId;
    private String conversationTranscript;
    private String analysisType;
    private List<String> analysisMetrics;

    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getConversationTranscript() { return conversationTranscript; }
    public void setConversationTranscript(String conversationTranscript) { this.conversationTranscript = conversationTranscript; }
    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
    public List<String> getAnalysisMetrics() { return analysisMetrics; }
    public void setAnalysisMetrics(List<String> analysisMetrics) { this.analysisMetrics = analysisMetrics; }
}

class ConversationAnalysisResult {
    private String conversationId;
    private Map<String, Double> performanceMetrics;
    private List<String> conversationInsights;
    private List<String> improvementSuggestions;
    private double overallScore;

    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public Map<String, Double> getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(Map<String, Double> performanceMetrics) { this.performanceMetrics = performanceMetrics; }
    public List<String> getConversationInsights() { return conversationInsights; }
    public void setConversationInsights(List<String> conversationInsights) { this.conversationInsights = conversationInsights; }
    public List<String> getImprovementSuggestions() { return improvementSuggestions; }
    public void setImprovementSuggestions(List<String> improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
}

class SalesOpportunityRequest {
    private List<String> leads;
    private String opportunityType;
    private List<String> interactionData;

    // Getters and setters
    public List<String> getLeads() { return leads; }
    public void setLeads(List<String> leads) { this.leads = leads; }
    public String getOpportunityType() { return opportunityType; }
    public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
    public List<String> getInteractionData() { return interactionData; }
    public void setInteractionData(List<String> interactionData) { this.interactionData = interactionData; }
}

class SalesOpportunityResult {
    private List<SalesOpportunity> opportunities;
    private String detectionStrategy;
    private double opportunityScore;

    // Getters and setters
    public List<SalesOpportunity> getOpportunities() { return opportunities; }
    public void setOpportunities(List<SalesOpportunity> opportunities) { this.opportunities = opportunities; }
    public String getDetectionStrategy() { return detectionStrategy; }
    public void setDetectionStrategy(String detectionStrategy) { this.detectionStrategy = detectionStrategy; }
    public double getOpportunityScore() { return opportunityScore; }
    public void setOpportunityScore(double opportunityScore) { this.opportunityScore = opportunityScore; }
}

class SalesOpportunity {
    private String leadId;
    private String opportunityType;
    private String description;
    private double potentialValue;
    private String recommendedAction;
    private int closingProbability;

    // Getters and setters
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getOpportunityType() { return opportunityType; }
    public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPotentialValue() { return potentialValue; }
    public void setPotentialValue(double potentialValue) { this.potentialValue = potentialValue; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public int getClosingProbability() { return closingProbability; }
    public void setClosingProbability(int closingProbability) { this.closingProbability = closingProbability; }
}

class SentimentAnalysisRequest {
    private String conversationId;
    private String conversationText;
    private String analysisScope;

    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getConversationText() { return conversationText; }
    public void setConversationText(String conversationText) { this.conversationText = conversationText; }
    public String getAnalysisScope() { return analysisScope; }
    public void setAnalysisScope(String analysisScope) { this.analysisScope = analysisScope; }
}

class SentimentAnalysisResult {
    private String conversationId;
    private String overallSentiment;
    private double sentimentScore;
    private Map<String, Double> sentimentBreakdown;
    private List<String> sentimentInsights;

    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getOverallSentiment() { return overallSentiment; }
    public void setOverallSentiment(String overallSentiment) { this.overallSentiment = overallSentiment; }
    public double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }
    public Map<String, Double> getSentimentBreakdown() { return sentimentBreakdown; }
    public void setSentimentBreakdown(Map<String, Double> sentimentBreakdown) { this.sentimentBreakdown = sentimentBreakdown; }
    public List<String> getSentimentInsights() { return sentimentInsights; }
    public void setSentimentInsights(List<String> sentimentInsights) { this.sentimentInsights = sentimentInsights; }
}

class DealProbabilityRequest {
    private List<String> deals;
    private String probabilityModel;
    private List<String> probabilityFactors;

    // Getters and setters
    public List<String> getDeals() { return deals; }
    public void setDeals(List<String> deals) { this.deals = deals; }
    public String getProbabilityModel() { return probabilityModel; }
    public void setProbabilityModel(String probabilityModel) { this.probabilityModel = probabilityModel; }
    public List<String> getProbabilityFactors() { return probabilityFactors; }
    public void setProbabilityFactors(List<String> probabilityFactors) { this.probabilityFactors = probabilityFactors; }
}

class DealProbabilityResult {
    private List<DealProbability> dealProbabilities;
    private String modelUsed;
    private double confidenceLevel;

    // Getters and setters
    public List<DealProbability> getDealProbabilities() { return dealProbabilities; }
    public void setDealProbabilities(List<DealProbability> dealProbabilities) { this.dealProbabilities = dealProbabilities; }
    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    public double getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
}

class DealProbability {
    private String dealId;
    private double closingProbability;
    private LocalDateTime predictedCloseDate;
    private List<String> successFactors;
    private List<String> riskFactors;

    // Getters and setters
    public String getDealId() { return dealId; }
    public void setDealId(String dealId) { this.dealId = dealId; }
    public double getClosingProbability() { return closingProbability; }
    public void setClosingProbability(double closingProbability) { this.closingProbability = closingProbability; }
    public LocalDateTime getPredictedCloseDate() { return predictedCloseDate; }
    public void setPredictedCloseDate(LocalDateTime predictedCloseDate) { this.predictedCloseDate = predictedCloseDate; }
    public List<String> getSuccessFactors() { return successFactors; }
    public void setSuccessFactors(List<String> successFactors) { this.successFactors = successFactors; }
    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }
}

class SalesPerformanceRequest {
    private String agentId;
    private String performancePeriod;
    private List<String> performanceMetrics;

    // Getters and setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getPerformancePeriod() { return performancePeriod; }
    public void setPerformancePeriod(String performancePeriod) { this.performancePeriod = performancePeriod; }
    public List<String> getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(List<String> performanceMetrics) { this.performanceMetrics = performanceMetrics; }
}

class SalesPerformanceResult {
    private String agentId;
    private Map<String, Double> performanceScores;
    private List<String> strengthAreas;
    private List<String> improvementAreas;
    private List<String> optimizationRecommendations;
    private double overallPerformanceScore;

    // Getters and setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public Map<String, Double> getPerformanceScores() { return performanceScores; }
    public void setPerformanceScores(Map<String, Double> performanceScores) { this.performanceScores = performanceScores; }
    public List<String> getStrengthAreas() { return strengthAreas; }
    public void setStrengthAreas(List<String> strengthAreas) { this.strengthAreas = strengthAreas; }
    public List<String> getImprovementAreas() { return improvementAreas; }
    public void setImprovementAreas(List<String> improvementAreas) { this.improvementAreas = improvementAreas; }
    public List<String> getOptimizationRecommendations() { return optimizationRecommendations; }
    public void setOptimizationRecommendations(List<String> optimizationRecommendations) { this.optimizationRecommendations = optimizationRecommendations; }
    public double getOverallPerformanceScore() { return overallPerformanceScore; }
    public void setOverallPerformanceScore(double overallPerformanceScore) { this.overallPerformanceScore = overallPerformanceScore; }
}

class MeetingSchedulingRequest {
    private List<String> participants;
    private List<String> timePreferences;
    private String meetingType;
    private int meetingDuration;
    private String meetingPurpose;

    // Getters and setters
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
    public List<String> getTimePreferences() { return timePreferences; }
    public void setTimePreferences(List<String> timePreferences) { this.timePreferences = timePreferences; }
    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }
    public int getMeetingDuration() { return meetingDuration; }
    public void setMeetingDuration(int meetingDuration) { this.meetingDuration = meetingDuration; }
    public String getMeetingPurpose() { return meetingPurpose; }
    public void setMeetingPurpose(String meetingPurpose) { this.meetingPurpose = meetingPurpose; }
}

class MeetingSchedulingResult {
    private List<ScheduledMeeting> scheduledMeetings;
    private String schedulingStrategy;
    private double optimizationScore;

    // Getters and setters
    public List<ScheduledMeeting> getScheduledMeetings() { return scheduledMeetings; }
    public void setScheduledMeetings(List<ScheduledMeeting> scheduledMeetings) { this.scheduledMeetings = scheduledMeetings; }
    public String getSchedulingStrategy() { return schedulingStrategy; }
    public void setSchedulingStrategy(String schedulingStrategy) { this.schedulingStrategy = schedulingStrategy; }
    public double getOptimizationScore() { return optimizationScore; }
    public void setOptimizationScore(double optimizationScore) { this.optimizationScore = optimizationScore; }
}

class ScheduledMeeting {
    private String meetingId;
    private String participant;
    private LocalDateTime scheduledTime;
    private String meetingType;
    private double suitabilityScore;

    // Getters and setters
    public String getMeetingId() { return meetingId; }
    public void setMeetingId(String meetingId) { this.meetingId = meetingId; }
    public String getParticipant() { return participant; }
    public void setParticipant(String participant) { this.participant = participant; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }
    public double getSuitabilityScore() { return suitabilityScore; }
    public void setSuitabilityScore(double suitabilityScore) { this.suitabilityScore = suitabilityScore; }
}

// AI Service Interfaces (to be implemented)
interface ConversationAssistant {
    ConversationAssistanceResult assistConversation(String agentId, ConversationAssistanceRequest request);
}

interface FollowUpRecommendationEngine {
    FollowUpRecommendationResult generateRecommendations(String agentId, FollowUpRecommendationRequest request);
}

interface SalesScriptGenerator {
    SalesScriptResult generateScript(String agentId, SalesScriptRequest request);
}

interface ObjectionHandlingEngine {
    ObjectionHandlingResult handleObjections(String agentId, ObjectionHandlingRequest request);
}

interface ConversationAnalyzer {
    ConversationAnalysisResult analyzeConversation(String agentId, ConversationAnalysisRequest request);
}

interface SalesOpportunityDetector {
    SalesOpportunityResult detectOpportunities(String agentId, SalesOpportunityRequest request);
}

interface CustomerSentimentAnalyzer {
    SentimentAnalysisResult analyzeSentiment(String agentId, SentimentAnalysisRequest request);
}

interface DealProbabilityPredictor {
    DealProbabilityResult predictDealProbability(String agentId, DealProbabilityRequest request);
}

interface SalesPerformanceOptimizer {
    SalesPerformanceResult optimizePerformance(String agentId, SalesPerformanceRequest request);
}

interface MeetingSchedulerAI {
    MeetingSchedulingResult scheduleMeetings(String agentId, MeetingSchedulingRequest request);
}