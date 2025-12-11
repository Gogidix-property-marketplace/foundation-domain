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
 * AI-powered Property Matching Service
 *
 * This service provides intelligent property matching using machine learning algorithms,
 * user behavior analysis, and preference learning to deliver highly personalized property recommendations.
 *
 * Features:
 * - ML-based property matching algorithms
 * - User preference learning and adaptation
 * - Behavioral analysis and pattern recognition
 * - Similarity scoring and ranking
 * - Multi-criteria matching engine
 * - Context-aware recommendations
 * - Real-time matching updates
 * - Collaborative filtering
 * - Location intelligence matching
 * - Budget optimization matching
 */
@RestController
@RequestMapping("/ai/v1/property-matching")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Property Matching AI Service", description = "AI-powered property matching and recommendation engine")
public class PropertyMatchingAIService {

    private final CacheService cacheService;
    private final MetricsService metricsService;
    private final AuditService auditService;
    private final SecurityService securityService;
    private final ValidationService validationService;

    // Property Matching Models
    private final MLMatchingEngine mlMatchingEngine;
    private final PreferenceLearningEngine preferenceLearningEngine;
    private final BehavioralAnalyzer behavioralAnalyzer;
    private final SimilarityCalculator similarityCalculator;
    private final RankingAlgorithm rankingAlgorithm;
    private final ContextAwareMatcher contextAwareMatcher;
    private final CollaborativeFilteringEngine collaborativeFilteringEngine;
    private final LocationIntelligenceMatcher locationIntelligenceMatcher;

    /**
     * Find property matches for a user
     */
    @PostMapping("/match/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Find property matches",
        description = "Provides AI-powered property matching based on user preferences, behavior, and ML algorithms"
    )
    public CompletableFuture<ResponseEntity<PropertyMatchingResult>> findPropertyMatches(
            @PathVariable String userId,
            @Valid @RequestBody PropertyMatchingRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.property.matching.find");

            try {
                log.info("Finding property matches for user: {}", userId);

                // Validate request
                validationService.validate(request);
                securityService.validateUserAccess(userId);

                // Generate property matches
                PropertyMatchingResult result = generatePropertyMatches(userId, request);

                // Cache results
                cacheService.set("property-matches:" + userId + ":" + request.hashCode(),
                               result, java.time.Duration.ofMinutes(30));

                // Record metrics
                metricsService.recordCounter("ai.property.matching.success");
                metricsService.recordTimer("ai.property.matching.find", stopwatch);

                // Audit
                auditService.audit(
                    "PROPERTY_MATCHES_GENERATED",
                    "userId=" + userId + ",matchesCount=" + result.getMatches().size(),
                    "ai-property-matching",
                    "success"
                );

                log.info("Successfully generated {} property matches for user: {}",
                        result.getMatches().size(), userId);
                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.matching.error");
                log.error("Error finding property matches for user: {}", userId, e);
                throw new RuntimeException("Property matching failed", e);
            }
        });
    }

    /**
     * Get personalized recommendations
     */
    @PostMapping("/recommendations/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Get personalized recommendations",
        description = "Provides personalized property recommendations using ML and behavioral analysis"
    )
    public CompletableFuture<ResponseEntity<PersonalizedRecommendations>> getPersonalizedRecommendations(
            @PathVariable String userId,
            @Valid @RequestBody RecommendationRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.property.matching.recommendations");

            try {
                log.info("Generating personalized recommendations for user: {}", userId);

                PersonalizedRecommendations recommendations = generatePersonalizedRecommendations(userId, request);

                metricsService.recordCounter("ai.property.recommendations.success");
                metricsService.recordTimer("ai.property.matching.recommendations", stopwatch);

                auditService.audit(
                    "PERSONALIZED_RECOMMENDATIONS_GENERATED",
                    "userId=" + userId + ",recommendationsCount=" + recommendations.getProperties().size(),
                    "ai-property-matching",
                    "success"
                );

                return ResponseEntity.ok(recommendations);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.recommendations.error");
                log.error("Error generating recommendations for user: {}", userId, e);
                throw new RuntimeException("Recommendation generation failed", e);
            }
        });
    }

    /**
     * Learn user preferences from behavior
     */
    @PostMapping("/learn-preferences/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Learn user preferences",
        description = "Updates user preference model based on behavior and feedback"
    )
    public CompletableFuture<ResponseEntity<PreferenceLearningResult>> learnUserPreferences(
            @PathVariable String userId,
            @Valid @RequestBody PreferenceLearningRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Learning user preferences for user: {}", userId);

                PreferenceLearningResult learning = preferenceLearningEngine.learnPreferences(userId, request);

                metricsService.recordCounter("ai.property.preference-learning.success");
                auditService.audit(
                    "USER_PREFERENCES_LEARNED",
                    "userId=" + userId + ",actionsCount=" + request.getUserActions().size(),
                    "ai-property-matching",
                    "success"
                );

                return ResponseEntity.ok(learning);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.preference-learning.error");
                log.error("Error learning preferences for user: {}", userId, e);
                throw new RuntimeException("Preference learning failed", e);
            }
        });
    }

    /**
     * Calculate property similarity
     */
    @PostMapping("/similarity")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Calculate property similarity",
        description = "Calculates similarity score between properties based on multiple criteria"
    )
    public CompletableFuture<ResponseEntity<SimilarityResult>> calculateSimilarity(
            @Valid @RequestBody SimilarityRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Calculating similarity for {} properties", request.getProperties().size());

                SimilarityResult similarity = similarityCalculator.calculateSimilarity(request);

                metricsService.recordCounter("ai.property.similarity.success");

                return ResponseEntity.ok(similarity);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.similarity.error");
                log.error("Error calculating property similarity", e);
                throw new RuntimeException("Similarity calculation failed", e);
            }
        });
    }

    /**
     * Find similar properties
     */
    @PostMapping("/similar/{propertyId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Find similar properties",
        description = "Finds properties similar to the specified property using ML algorithms"
    )
    public CompletableFuture<ResponseEntity<List<SimilarProperty>>> findSimilarProperties(
            @PathVariable String propertyId,
            @Valid @RequestBody SimilarPropertyRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Finding similar properties for: {}", propertyId);

                List<SimilarProperty> similarProperties = mlMatchingEngine.findSimilarProperties(propertyId, request);

                metricsService.recordCounter("ai.property.similar-properties.success");

                return ResponseEntity.ok(similarProperties);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.similar-properties.error");
                log.error("Error finding similar properties for: {}", propertyId, e);
                throw new RuntimeException("Similar properties search failed", e);
            }
        });
    }

    /**
     * Analyze user behavior patterns
     */
    @PostMapping("/behavior-analysis/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Analyze user behavior",
        description = "Analyzes user behavior patterns to improve matching accuracy"
    )
    public CompletableFuture<ResponseEntity<BehaviorAnalysisResult>> analyzeUserBehavior(
            @PathVariable String userId,
            @Valid @RequestBody BehaviorAnalysisRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing behavior patterns for user: {}", userId);

                BehaviorAnalysisResult behavior = behavioralAnalyzer.analyzeBehavior(userId, request);

                metricsService.recordCounter("ai.property.behavior-analysis.success");

                return ResponseEntity.ok(behavior);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.behavior-analysis.error");
                log.error("Error analyzing behavior for user: {}", userId, e);
                throw new RuntimeException("Behavior analysis failed", e);
            }
        });
    }

    /**
     * Collaborative filtering recommendations
     */
    @PostMapping("/collaborative-filtering/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Collaborative filtering recommendations",
        description = "Provides recommendations based on similar users' preferences"
    )
    public CompletableFuture<ResponseEntity<CollaborativeFilteringResult>> getCollaborativeRecommendations(
            @PathVariable String userId,
            @Valid @RequestBody CollaborativeFilteringRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating collaborative filtering recommendations for user: {}", userId);

                CollaborativeFilteringResult collaborative = collaborativeFilteringEngine.generateRecommendations(userId, request);

                metricsService.recordCounter("ai.property.collaborative-filtering.success");

                return ResponseEntity.ok(collaborative);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.collaborative-filtering.error");
                log.error("Error generating collaborative recommendations for user: {}", userId, e);
                throw new RuntimeException("Collaborative filtering failed", e);
            }
        });
    }

    /**
     * Context-aware matching
     */
    @PostMapping("/context-aware/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Context-aware matching",
        description = "Provides context-aware property matching considering time, location, and user state"
    )
    public CompletableFuture<ResponseEntity<ContextAwareResult>> getContextAwareMatches(
            @PathVariable String userId,
            @Valid @RequestBody ContextAwareRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating context-aware matches for user: {}", userId);

                ContextAwareResult contextAware = contextAwareMatcher.generateMatches(userId, request);

                metricsService.recordCounter("ai.property.context-aware.success");

                return ResponseEntity.ok(contextAware);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.context-aware.error");
                log.error("Error generating context-aware matches for user: {}", userId, e);
                throw new RuntimeException("Context-aware matching failed", e);
            }
        });
    }

    /**
     * Location intelligence matching
     */
    @PostMapping("/location-intelligence/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Location intelligence matching",
        description = "Provides location-based property matching using geographic intelligence"
    )
    public CompletableFuture<ResponseEntity<LocationIntelligenceResult>> getLocationIntelligenceMatches(
            @PathVariable String userId,
            @Valid @RequestBody LocationIntelligenceRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating location intelligence matches for user: {}", userId);

                LocationIntelligenceResult locationIntelligence = locationIntelligenceMatcher.generateMatches(userId, request);

                metricsService.recordCounter("ai.property.location-intelligence.success");

                return ResponseEntity.ok(locationIntelligence);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.location-intelligence.error");
                log.error("Error generating location intelligence matches for user: {}", userId, e);
                throw new RuntimeException("Location intelligence matching failed", e);
            }
        });
    }

    /**
     * Update matching feedback
     */
    @PostMapping("/feedback/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Update matching feedback",
        description = "Updates ML models based on user feedback to improve future matching"
    )
    public CompletableFuture<ResponseEntity<FeedbackUpdateResult>> updateMatchingFeedback(
            @PathVariable String userId,
            @Valid @RequestBody FeedbackUpdateRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Updating matching feedback for user: {}", userId);

                FeedbackUpdateResult feedback = mlMatchingEngine.updateFeedback(userId, request);

                metricsService.recordCounter("ai.property.feedback-update.success");
                auditService.audit(
                    "MATCHING_FEEDBACK_UPDATED",
                    "userId=" + userId + ",feedbackCount=" + request.getFeedback().size(),
                    "ai-property-matching",
                    "success"
                );

                return ResponseEntity.ok(feedback);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.feedback-update.error");
                log.error("Error updating feedback for user: {}", userId, e);
                throw new RuntimeException("Feedback update failed", e);
            }
        });
    }

    // Helper Methods
    private PropertyMatchingResult generatePropertyMatches(String userId, PropertyMatchingRequest request) {
        PropertyMatchingResult result = new PropertyMatchingResult();
        result.setUserId(userId);
        result.setMatchingDate(LocalDateTime.now());
        result.setGeneratedBy("AI Property Matching Service");

        // ML-based matching
        List<PropertyMatch> mlMatches = mlMatchingEngine.findMatches(userId, request);
        result.setMatches(mlMatches);

        // Apply ranking algorithm
        rankingAlgorithm.rankMatches(result.getMatches(), request.getPreferences());

        // Calculate matching score statistics
        result.setAverageMatchScore(calculateAverageMatchScore(result.getMatches()));
        result.setTotalMatches(result.getMatches().size());

        // Generate matching insights
        result.setMatchingInsights(generateMatchingInsights(result.getMatches()));

        return result;
    }

    private PersonalizedRecommendations generatePersonalizedRecommendations(String userId, RecommendationRequest request) {
        PersonalizedRecommendations recommendations = new PersonalizedRecommendations();
        recommendations.setUserId(userId);
        recommendations.setGenerationDate(LocalDateTime.now());

        // Behavioral analysis
        BehaviorAnalysisResult behavior = behavioralAnalyzer.analyzeBehavior(userId,
            new BehaviorAnalysisRequest());

        // Preference learning
        UserPreferences learnedPreferences = preferenceLearningEngine.getLearnedPreferences(userId);

        // Generate recommendations based on behavior and preferences
        List<RecommendedProperty> properties = rankingAlgorithm.generatePersonalizedRecommendations(
            userId, behavior, learnedPreferences, request);

        recommendations.setProperties(properties);
        recommendations.setConfidenceScore(calculateRecommendationConfidence(properties));
        recommendations.setPersonalizationLevel(calculatePersonalizationLevel(behavior, learnedPreferences));

        return recommendations;
    }

    private double calculateAverageMatchScore(List<PropertyMatch> matches) {
        return matches.stream()
                .mapToDouble(PropertyMatch::getMatchScore)
                .average()
                .orElse(0.0);
    }

    private List<String> generateMatchingInsights(List<PropertyMatch> matches) {
        return List.of(
            "Top matching criteria: " + getTopMatchingCriteria(matches),
            "Price range alignment: " + calculatePriceRangeAlignment(matches) + "%",
            "Location preference match: " + calculateLocationMatchScore(matches) + "%",
            "Property type preference: " + getTopPropertyType(matches)
        );
    }

    private String getTopMatchingCriteria(List<PropertyMatch> matches) {
        return "Location proximity, budget alignment, and property features";
    }

    private double calculatePriceRangeAlignment(List<PropertyMatch> matches) {
        return Math.round(matches.stream()
                .mapToDouble(m -> m.getPriceAlignmentScore() * 100)
                .average()
                .orElse(0.0) * 100.0) / 100.0;
    }

    private double calculateLocationMatchScore(List<PropertyMatch> matches) {
        return Math.round(matches.stream()
                .mapToDouble(m -> m.getLocationScore() * 100)
                .average()
                .orElse(0.0) * 100.0) / 100.0;
    }

    private String getTopPropertyType(List<PropertyMatch> matches) {
        return matches.stream()
                .findFirst()
                .map(PropertyMatch::getPropertyType)
                .orElse("Not specified");
    }

    private double calculateRecommendationConfidence(List<RecommendedProperty> properties) {
        return Math.round(properties.stream()
                .mapToDouble(RecommendedProperty::getConfidenceScore)
                .average()
                .orElse(0.0) * 100.0) / 100.0;
    }

    private double calculatePersonalizationLevel(BehaviorAnalysisResult behavior, UserPreferences preferences) {
        // Calculate personalization level based on behavior data and preference accuracy
        double behaviorScore = behavior.getDataCompleteness() * 0.6;
        double preferenceScore = preferences.getConfidenceLevel() * 0.4;
        return Math.round((behaviorScore + preferenceScore) * 100.0) / 100.0;
    }
}

// Data Transfer Objects and Models

class PropertyMatchingRequest {
    private SearchCriteria criteria;
    private UserPreferences preferences;
    private List<String> excludeProperties;
    private int maxResults = 20;
    private double minMatchScore = 0.6;

    // Getters and setters
    public SearchCriteria getCriteria() { return criteria; }
    public void setCriteria(SearchCriteria criteria) { this.criteria = criteria; }
    public UserPreferences getPreferences() { return preferences; }
    public void setPreferences(UserPreferences preferences) { this.preferences = preferences; }
    public List<String> getExcludeProperties() { return excludeProperties; }
    public void setExcludeProperties(List<String> excludeProperties) { this.excludeProperties = excludeProperties; }
    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
    public double getMinMatchScore() { return minMatchScore; }
    public void setMinMatchScore(double minMatchScore) { this.minMatchScore = minMatchScore; }
}

class PropertyMatchingResult {
    private String userId;
    private LocalDateTime matchingDate;
    private String generatedBy;
    private List<PropertyMatch> matches;
    private int totalMatches;
    private double averageMatchScore;
    private List<String> matchingInsights;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getMatchingDate() { return matchingDate; }
    public void setMatchingDate(LocalDateTime matchingDate) { this.matchingDate = matchingDate; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public List<PropertyMatch> getMatches() { return matches; }
    public void setMatches(List<PropertyMatch> matches) { this.matches = matches; }
    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
    public double getAverageMatchScore() { return averageMatchScore; }
    public void setAverageMatchScore(double averageMatchScore) { this.averageMatchScore = averageMatchScore; }
    public List<String> getMatchingInsights() { return matchingInsights; }
    public void setMatchingInsights(List<String> matchingInsights) { this.matchingInsights = matchingInsights; }
}

class PropertyMatch {
    private String propertyId;
    private String propertyTitle;
    private double matchScore;
    private double priceAlignmentScore;
    private double locationScore;
    private double featureScore;
    private double preferenceScore;
    private String propertyType;
    private Map<String, String> matchedFeatures;
    private List<String> matchReasons;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }
    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
    public double getPriceAlignmentScore() { return priceAlignmentScore; }
    public void setPriceAlignmentScore(double priceAlignmentScore) { this.priceAlignmentScore = priceAlignmentScore; }
    public double getLocationScore() { return locationScore; }
    public void setLocationScore(double locationScore) { this.locationScore = locationScore; }
    public double getFeatureScore() { return featureScore; }
    public void setFeatureScore(double featureScore) { this.featureScore = featureScore; }
    public double getPreferenceScore() { return preferenceScore; }
    public void setPreferenceScore(double preferenceScore) { this.preferenceScore = preferenceScore; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public Map<String, String> getMatchedFeatures() { return matchedFeatures; }
    public void setMatchedFeatures(Map<String, String> matchedFeatures) { this.matchedFeatures = matchedFeatures; }
    public List<String> getMatchReasons() { return matchReasons; }
    public void setMatchReasons(List<String> matchReasons) { this.matchReasons = matchReasons; }
}

class RecommendationRequest {
    private int maxRecommendations = 10;
    private String recommendationType; // explore, refine, similar
    private boolean includeInsights = true;

    public int getMaxRecommendations() { return maxRecommendations; }
    public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public boolean isIncludeInsights() { return includeInsights; }
    public void setIncludeInsights(boolean includeInsights) { this.includeInsights = includeInsights; }
}

class PersonalizedRecommendations {
    private String userId;
    private LocalDateTime generationDate;
    private List<RecommendedProperty> properties;
    private double confidenceScore;
    private double personalizationLevel;
    private List<String> recommendationInsights;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getGenerationDate() { return generationDate; }
    public void setGenerationDate(LocalDateTime generationDate) { this.generationDate = generationDate; }
    public List<RecommendedProperty> getProperties() { return properties; }
    public void setProperties(List<RecommendedProperty> properties) { this.properties = properties; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public double getPersonalizationLevel() { return personalizationLevel; }
    public void setPersonalizationLevel(double personalizationLevel) { this.personalizationLevel = personalizationLevel; }
    public List<String> getRecommendationInsights() { return recommendationInsights; }
    public void setRecommendationInsights(List<String> recommendationInsights) { this.recommendationInsights = recommendationInsights; }
}

class RecommendedProperty {
    private String propertyId;
    private String propertyTitle;
    private double confidenceScore;
    private List<String> recommendationReasons;
    private Map<String, Double> scoreBreakdown;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public List<String> getRecommendationReasons() { return recommendationReasons; }
    public void setRecommendationReasons(List<String> recommendationReasons) { this.recommendationReasons = recommendationReasons; }
    public Map<String, Double> getScoreBreakdown() { return scoreBreakdown; }
    public void setScoreBreakdown(Map<String, Double> scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }
}

class PreferenceLearningRequest {
    private List<UserAction> userActions;
    private String learningType; // explicit, implicit, hybrid

    // Getters and setters
    public List<UserAction> getUserActions() { return userActions; }
    public void setUserActions(List<UserAction> userActions) { this.userActions = userActions; }
    public String getLearningType() { return learningType; }
    public void setLearningType(String learningType) { this.learningType = learningType; }
}

class PreferenceLearningResult {
    private String userId;
    private LocalDateTime learningDate;
    private UserPreferences updatedPreferences;
    private double learningAccuracy;
    private List<String> learnedInsights;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getLearningDate() { return learningDate; }
    public void setLearningDate(LocalDateTime learningDate) { this.learningDate = learningDate; }
    public UserPreferences getUpdatedPreferences() { return updatedPreferences; }
    public void setUpdatedPreferences(UserPreferences updatedPreferences) { this.updatedPreferences = updatedPreferences; }
    public double getLearningAccuracy() { return learningAccuracy; }
    public void setLearningAccuracy(double learningAccuracy) { this.learningAccuracy = learningAccuracy; }
    public List<String> getLearnedInsights() { return learnedInsights; }
    public void setLearnedInsights(List<String> learnedInsights) { this.learnedInsights = learnedInsights; }
}

class SimilarityRequest {
    private List<String> properties;
    private List<String> similarityCriteria;
    private double minSimilarityScore = 0.5;

    // Getters and setters
    public List<String> getProperties() { return properties; }
    public void setProperties(List<String> properties) { this.properties = properties; }
    public List<String> getSimilarityCriteria() { return similarityCriteria; }
    public void setSimilarityCriteria(List<String> similarityCriteria) { this.similarityCriteria = similarityCriteria; }
    public double getMinSimilarityScore() { return minSimilarityScore; }
    public void setMinSimilarityScore(double minSimilarityScore) { this.minSimilarityScore = minSimilarityScore; }
}

class SimilarityResult {
    private Map<String, Map<String, Double>> similarityMatrix;
    private List<String> similarityInsights;

    // Getters and setters
    public Map<String, Map<String, Double>> getSimilarityMatrix() { return similarityMatrix; }
    public void setSimilarityMatrix(Map<String, Map<String, Double>> similarityMatrix) { this.similarityMatrix = similarityMatrix; }
    public List<String> getSimilarityInsights() { return similarityInsights; }
    public void setSimilarityInsights(List<String> similarityInsights) { this.similarityInsights = similarityInsights; }
}

class SimilarPropertyRequest {
    private String propertyId;
    private int maxSimilarProperties = 10;
    private List<String> similarityCriteria;
    private double minSimilarityScore = 0.7;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public int getMaxSimilarProperties() { return maxSimilarProperties; }
    public void setMaxSimilarProperties(int maxSimilarProperties) { this.maxSimilarProperties = maxSimilarProperties; }
    public List<String> getSimilarityCriteria() { return similarityCriteria; }
    public void setSimilarityCriteria(List<String> similarityCriteria) { this.similarityCriteria = similarityCriteria; }
    public double getMinSimilarityScore() { return minSimilarityScore; }
    public void setMinSimilarityScore(double minSimilarityScore) { this.minSimilarityScore = minSimilarityScore; }
}

class SimilarProperty {
    private String propertyId;
    private String propertyTitle;
    private double similarityScore;
    private List<String> similarFeatures;
    private List<String> differentFeatures;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }
    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
    public List<String> getSimilarFeatures() { return similarFeatures; }
    public void setSimilarFeatures(List<String> similarFeatures) { this.similarFeatures = similarFeatures; }
    public List<String> getDifferentFeatures() { return differentFeatures; }
    public void setDifferentFeatures(List<String> differentFeatures) { this.differentFeatures = differentFeatures; }
}

class BehaviorAnalysisRequest {
    private String timeFrame = "30days";
    private List<String> behaviorTypes;

    // Getters and setters
    public String getTimeFrame() { return timeFrame; }
    public void setTimeFrame(String timeFrame) { this.timeFrame = timeFrame; }
    public List<String> getBehaviorTypes() { return behaviorTypes; }
    public void setBehaviorTypes(List<String> behaviorTypes) { this.behaviorTypes = behaviorTypes; }
}

class BehaviorAnalysisResult {
    private String userId;
    private LocalDateTime analysisDate;
    private Map<String, Double> behaviorPatterns;
    private List<String> behavioralInsights;
    private double dataCompleteness;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(LocalDateTime analysisDate) { this.analysisDate = analysisDate; }
    public Map<String, Double> getBehaviorPatterns() { return behaviorPatterns; }
    public void setBehaviorPatterns(Map<String, Double> behaviorPatterns) { this.behaviorPatterns = behaviorPatterns; }
    public List<String> getBehavioralInsights() { return behavioralInsights; }
    public void setBehavioralInsights(List<String> behavioralInsights) { this.behavioralInsights = behavioralInsights; }
    public double getDataCompleteness() { return dataCompleteness; }
    public void setDataCompleteness(double dataCompleteness) { this.dataCompleteness = dataCompleteness; }
}

class CollaborativeFilteringRequest {
    private int maxRecommendations = 10;
    private double minSimilarity = 0.3;
    private String userSegment;

    // Getters and setters
    public int getMaxRecommendations() { return maxRecommendations; }
    public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }
    public double getMinSimilarity() { return minSimilarity; }
    public void setMinSimilarity(double minSimilarity) { this.minSimilarity = minSimilarity; }
    public String getUserSegment() { return userSegment; }
    public void setUserSegment(String userSegment) { this.userSegment = userSegment; }
}

class CollaborativeFilteringResult {
    private List<String> similarUsers;
    private List<RecommendedProperty> collaborativeRecommendations;
    private double confidenceLevel;

    // Getters and setters
    public List<String> getSimilarUsers() { return similarUsers; }
    public void setSimilarUsers(List<String> similarUsers) { this.similarUsers = similarUsers; }
    public List<RecommendedProperty> getCollaborativeRecommendations() { return collaborativeRecommendations; }
    public void setCollaborativeRecommendations(List<RecommendedProperty> collaborativeRecommendations) { this.collaborativeRecommendations = collaborativeRecommendations; }
    public double getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
}

class ContextAwareRequest {
    private String currentTime;
    private String currentLocation;
    private String userState; // browsing, serious, ready-to-buy
    private Map<String, Object> contextData;

    // Getters and setters
    public String getCurrentTime() { return currentTime; }
    public void setCurrentTime(String currentTime) { this.currentTime = currentTime; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    public String getUserState() { return userState; }
    public void setUserState(String userState) { this.userState = userState; }
    public Map<String, Object> getContextData() { return contextData; }
    public void setContextData(Map<String, Object> contextData) { this.contextData = contextData; }
}

class ContextAwareResult {
    private List<PropertyMatch> contextualMatches;
    private Map<String, Double> contextFactors;
    private List<String> contextInsights;

    // Getters and setters
    public List<PropertyMatch> getContextualMatches() { return contextualMatches; }
    public void setContextualMatches(List<PropertyMatch> contextualMatches) { this.contextualMatches = contextualMatches; }
    public Map<String, Double> getContextFactors() { return contextFactors; }
    public void setContextFactors(Map<String, Double> contextFactors) { this.contextFactors = contextFactors; }
    public List<String> getContextInsights() { return contextInsights; }
    public void setContextInsights(List<String> contextInsights) { this.contextInsights = contextInsights; }
}

class LocationIntelligenceRequest {
    private String preferredAreas;
    private List<String> locationFactors;
    private double maxDistance = 50; // km

    // Getters and setters
    public String getPreferredAreas() { return preferredAreas; }
    public void setPreferredAreas(String preferredAreas) { this.preferredAreas = preferredAreas; }
    public List<String> getLocationFactors() { return locationFactors; }
    public void setLocationFactors(List<String> locationFactors) { this.locationFactors = locationFactors; }
    public double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(double maxDistance) { this.maxDistance = maxDistance; }
}

class LocationIntelligenceResult {
    private List<PropertyMatch> locationBasedMatches;
    private Map<String, Double> locationScores;
    private List<String> locationInsights;

    // Getters and setters
    public List<PropertyMatch> getLocationBasedMatches() { return locationBasedMatches; }
    public void setLocationBasedMatches(List<PropertyMatch> locationBasedMatches) { this.locationBasedMatches = locationBasedMatches; }
    public Map<String, Double> getLocationScores() { return locationScores; }
    public void setLocationScores(Map<String, Double> locationScores) { this.locationScores = locationScores; }
    public List<String> getLocationInsights() { return locationInsights; }
    public void setLocationInsights(List<String> locationInsights) { this.locationInsights = locationInsights; }
}

class FeedbackUpdateRequest {
    private List<UserFeedback> feedback;
    private String feedbackType;

    // Getters and setters
    public List<UserFeedback> getFeedback() { return feedback; }
    public void setFeedback(List<UserFeedback> feedback) { this.feedback = feedback; }
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
}

class FeedbackUpdateResult {
    private String userId;
    private LocalDateTime updateDate;
    private int feedbackProcessed;
    private double modelImprovement;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getUpdateDate() { return updateDate; }
    public void setUpdateDate(LocalDateTime updateDate) { this.updateDate = updateDate; }
    public int getFeedbackProcessed() { return feedbackProcessed; }
    public void setFeedbackProcessed(int feedbackProcessed) { this.feedbackProcessed = feedbackProcessed; }
    public double getModelImprovement() { return modelImprovement; }
    public void setModelImprovement(double modelImprovement) { this.modelImprovement = modelImprovement; }
}

// Supporting classes
class SearchCriteria {
    private String location;
    private String propertyType;
    private String priceRange;
    private String bedrooms;
    private String bathrooms;
    private List<String> amenities;

    // Getters and setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }
    public String getBedrooms() { return bedrooms; }
    public void setBedrooms(String bedrooms) { this.bedrooms = bedrooms; }
    public String getBathrooms() { return bathrooms; }
    public void setBathrooms(String bathrooms) { this.bathrooms = bathrooms; }
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
}

class UserPreferences {
    private Map<String, Double> locationPreferences;
    private Map<String, Double> propertyTypePreferences;
    private Map<String, Double> pricePreferences;
    private Map<String, Double> amenityPreferences;
    private double confidenceLevel;

    // Getters and setters
    public Map<String, Double> getLocationPreferences() { return locationPreferences; }
    public void setLocationPreferences(Map<String, Double> locationPreferences) { this.locationPreferences = locationPreferences; }
    public Map<String, Double> getPropertyTypePreferences() { return propertyTypePreferences; }
    public void setPropertyTypePreferences(Map<String, Double> propertyTypePreferences) { this.propertyTypePreferences = propertyTypePreferences; }
    public Map<String, Double> getPricePreferences() { return pricePreferences; }
    public void setPricePreferences(Map<String, Double> pricePreferences) { this.pricePreferences = pricePreferences; }
    public Map<String, Double> getAmenityPreferences() { return amenityPreferences; }
    public void setAmenityPreferences(Map<String, Double> amenityPreferences) { this.amenityPreferences = amenityPreferences; }
    public double getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
}

class UserAction {
    private String actionType;
    private String propertyId;
    private LocalDateTime timestamp;
    private Map<String, Object> actionData;

    // Getters and setters
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Map<String, Object> getActionData() { return actionData; }
    public void setActionData(Map<String, Object> actionData) { this.actionData = actionData; }
}

class UserFeedback {
    private String propertyId;
    private String feedbackType; // like, dislike, save, contact
    private double rating;
    private String comments;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}

// AI Service Interfaces (to be implemented)
interface MLMatchingEngine {
    List<PropertyMatch> findMatches(String userId, PropertyMatchingRequest request);
    List<SimilarProperty> findSimilarProperties(String propertyId, SimilarPropertyRequest request);
    FeedbackUpdateResult updateFeedback(String userId, FeedbackUpdateRequest request);
}

interface PreferenceLearningEngine {
    PreferenceLearningResult learnPreferences(String userId, PreferenceLearningRequest request);
    UserPreferences getLearnedPreferences(String userId);
}

interface BehavioralAnalyzer {
    BehaviorAnalysisResult analyzeBehavior(String userId, BehaviorAnalysisRequest request);
}

interface SimilarityCalculator {
    SimilarityResult calculateSimilarity(SimilarityRequest request);
}

interface RankingAlgorithm {
    void rankMatches(List<PropertyMatch> matches, UserPreferences preferences);
    List<RecommendedProperty> generatePersonalizedRecommendations(String userId,
        BehaviorAnalysisResult behavior, UserPreferences preferences, RecommendationRequest request);
}

interface ContextAwareMatcher {
    ContextAwareResult generateMatches(String userId, ContextAwareRequest request);
}

interface CollaborativeFilteringEngine {
    CollaborativeFilteringResult generateRecommendations(String userId, CollaborativeFilteringRequest request);
}

interface LocationIntelligenceMatcher {
    LocationIntelligenceResult generateMatches(String userId, LocationIntelligenceRequest request);
}