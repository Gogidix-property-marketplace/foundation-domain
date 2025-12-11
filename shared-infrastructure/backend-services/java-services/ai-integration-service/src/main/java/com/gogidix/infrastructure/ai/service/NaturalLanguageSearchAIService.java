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
 * AI-powered Natural Language Search Service
 *
 * This service provides conversational property search capabilities using natural language processing,
 * intent recognition, semantic understanding, and context-aware search to deliver human-like property search.
 *
 * Features:
 * - Natural language property search queries
 * - Intent recognition and extraction
 * - Semantic query understanding
 * - Conversational search interface
 * - Multi-turn dialog support
 * - Context-aware search refinement
 * - Voice search capabilities
 * - Query suggestion and auto-completion
 * - Multi-language support
 * - Fuzzy matching and typo tolerance
 */
@RestController
@RequestMapping("/ai/v1/natural-language-search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Natural Language Search AI Service", description = "AI-powered conversational property search using NLP")
public class NaturalLanguageSearchAIService {

    private final CacheService cacheService;
    private final MetricsService metricsService;
    private final AuditService auditService;
    private final SecurityService securityService;
    private final ValidationService validationService;

    // NLP Search Models
    private final NaturalLanguageProcessor nlpProcessor;
    private final IntentRecognitionEngine intentEngine;
    private final SemanticQueryAnalyzer semanticAnalyzer;
    private final ConversationalSearchEngine conversationalEngine;
    private final ContextAwareSearchProcessor contextProcessor;
    private final VoiceSearchProcessor voiceProcessor;
    private final MultiLanguageProcessor multiLanguageProcessor;
    private final QuerySuggestionEngine suggestionEngine;

    /**
     * Process natural language search query
     */
    @PostMapping("/search/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Natural language property search",
        description = "Processes natural language search queries and returns matching properties"
    )
    public CompletableFuture<ResponseEntity<NaturalLanguageSearchResult>> searchWithNaturalLanguage(
            @PathVariable String userId,
            @Valid @RequestBody NaturalLanguageSearchRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.nlp.search.process");

            try {
                log.info("Processing natural language search for user: {}", userId);

                // Validate request
                validationService.validate(request);
                securityService.validateUserAccess(userId);

                // Process natural language search
                NaturalLanguageSearchResult result = processNaturalLanguageSearch(userId, request);

                // Cache results
                cacheService.set("nlp-search:" + userId + ":" + request.getQuery().hashCode(),
                               result, java.time.Duration.ofMinutes(15));

                // Record metrics
                metricsService.recordCounter("ai.nlp.search.success");
                metricsService.recordTimer("ai.nlp.search.process", stopwatch);

                // Audit
                auditService.audit(
                    "NATURAL_LANGUAGE_SEARCH",
                    "userId=" + userId + ",query=" + request.getQuery() + ",results=" + result.getProperties().size(),
                    "ai-nlp-search",
                    "success"
                );

                log.info("Successfully processed NLP search for user: {}, found {} results",
                        userId, result.getProperties().size());
                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.search.error");
                log.error("Error processing NLP search for user: {}", userId, e);
                throw new RuntimeException("Natural language search failed", e);
            }
        });
    }

    /**
     * Conversational search interaction
     */
    @PostMapping("/converse/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Conversational search",
        description = "Engages in conversational search with multi-turn dialog support"
    )
    public CompletableFuture<ResponseEntity<ConversationalSearchResult>> conversationalSearch(
            @PathVariable String userId,
            @Valid @RequestBody ConversationalSearchRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.nlp.search.conversational");

            try {
                log.info("Processing conversational search for user: {}", userId);

                ConversationalSearchResult result = conversationalEngine.processConversation(userId, request);

                metricsService.recordCounter("ai.nlp.conversational.success");
                metricsService.recordTimer("ai.nlp.search.conversational", stopwatch);

                auditService.audit(
                    "CONVERSATIONAL_SEARCH",
                    "userId=" + userId + ",turn=" + request.getTurnNumber(),
                    "ai-nlp-search",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.conversational.error");
                log.error("Error processing conversational search for user: {}", userId, e);
                throw new RuntimeException("Conversational search failed", e);
            }
        });
    }

    /**
     * Recognize search intent
     */
    @PostMapping("/intent-recognition")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Recognize search intent",
        description = "Extracts and classifies user intent from natural language queries"
    )
    public CompletableFuture<ResponseEntity<IntentRecognitionResult>> recognizeIntent(
            @Valid @RequestBody IntentRecognitionRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Recognizing intent for query: {}", request.getQuery());

                IntentRecognitionResult intent = intentEngine.recognizeIntent(request);

                metricsService.recordCounter("ai.nlp.intent-recognition.success");

                return ResponseEntity.ok(intent);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.intent-recognition.error");
                log.error("Error recognizing intent for query: {}", request.getQuery(), e);
                throw new RuntimeException("Intent recognition failed", e);
            }
        });
    }

    /**
     * Semantic query analysis
     */
    @PostMapping("/semantic-analysis")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Semantic query analysis",
        description = "Analyzes query semantics and extracts meaningful entities and relationships"
    )
    public CompletableFuture<ResponseEntity<SemanticAnalysisResult>> analyzeQuerySemantics(
            @Valid @RequestBody SemanticAnalysisRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing query semantics: {}", request.getQuery());

                SemanticAnalysisResult semantics = semanticAnalyzer.analyzeSemantics(request);

                metricsService.recordCounter("ai.nlp.semantic-analysis.success");

                return ResponseEntity.ok(semantics);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.semantic-analysis.error");
                log.error("Error analyzing query semantics: {}", request.getQuery(), e);
                throw new RuntimeException("Semantic analysis failed", e);
            }
        });
    }

    /**
     * Generate search suggestions
     */
    @PostMapping("/suggestions/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Generate search suggestions",
        description = "Provides intelligent search suggestions and auto-completion"
    )
    public CompletableFuture<ResponseEntity<SearchSuggestions>> generateSuggestions(
            @PathVariable String userId,
            @Valid @RequestBody SuggestionRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating search suggestions for user: {}", userId);

                SearchSuggestions suggestions = suggestionEngine.generateSuggestions(userId, request);

                metricsService.recordCounter("ai.nlp.suggestions.success");

                return ResponseEntity.ok(suggestions);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.suggestions.error");
                log.error("Error generating suggestions for user: {}", userId, e);
                throw new RuntimeException("Suggestion generation failed", e);
            }
        });
    }

    /**
     * Process voice search
     */
    @PostMapping("/voice-search/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Process voice search",
        description = "Processes voice search queries and converts to text for property search"
    )
    public CompletableFuture<ResponseEntity<VoiceSearchResult>> processVoiceSearch(
            @PathVariable String userId,
            @Valid @RequestBody VoiceSearchRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Processing voice search for user: {}", userId);

                VoiceSearchResult voiceResult = voiceProcessor.processVoiceSearch(userId, request);

                metricsService.recordCounter("ai.nlp.voice-search.success");
                auditService.audit(
                    "VOICE_SEARCH_PROCESSED",
                    "userId=" + userId + ",duration=" + request.getAudioLength(),
                    "ai-nlp-search",
                    "success"
                );

                return ResponseEntity.ok(voiceResult);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.voice-search.error");
                log.error("Error processing voice search for user: {}", userId, e);
                throw new RuntimeException("Voice search processing failed", e);
            }
        });
    }

    /**
     * Multi-language search
     */
    @PostMapping("/multilingual-search/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Multi-language search",
        description = "Processes search queries in multiple languages"
    )
    public CompletableFuture<ResponseEntity<MultiLanguageSearchResult>> multiLanguageSearch(
            @PathVariable String userId,
            @Valid @RequestBody MultiLanguageSearchRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Processing multi-language search for user: {}, language: {}", userId, request.getLanguage());

                MultiLanguageSearchResult result = multiLanguageProcessor.processSearch(userId, request);

                metricsService.recordCounter("ai.nlp.multilingual-search.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.multilingual-search.error");
                log.error("Error processing multi-language search for user: {}", userId, e);
                throw new RuntimeException("Multi-language search failed", e);
            }
        });
    }

    /**
     * Fuzzy search with typo tolerance
     */
    @PostMapping("/fuzzy-search/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Fuzzy search",
        description = "Performs fuzzy search with typo tolerance and approximate matching"
    )
    public CompletableFuture<ResponseEntity<FuzzySearchResult>> fuzzySearch(
            @PathVariable String userId,
            @Valid @RequestBody FuzzySearchRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Performing fuzzy search for user: {}", userId);

                FuzzySearchResult result = nlpProcessor.performFuzzySearch(userId, request);

                metricsService.recordCounter("ai.nlp.fuzzy-search.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.fuzzy-search.error");
                log.error("Error performing fuzzy search for user: {}", userId, e);
                throw new RuntimeException("Fuzzy search failed", e);
            }
        });
    }

    /**
     * Context-aware search refinement
     */
    @PostMapping("/contextual-refinement/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Contextual search refinement",
        description = "Refines search results based on user context and previous interactions"
    )
    public CompletableFuture<ResponseEntity<ContextualRefinementResult>> contextualRefinement(
            @PathVariable String userId,
            @Valid @RequestBody ContextualRefinementRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Performing contextual refinement for user: {}", userId);

                ContextualRefinementResult result = contextProcessor.refineSearchResults(userId, request);

                metricsService.recordCounter("ai.nlp.contextual-refinement.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.contextual-refinement.error");
                log.error("Error performing contextual refinement for user: {}", userId, e);
                throw new RuntimeException("Contextual refinement failed", e);
            }
        });
    }

    /**
     * Query expansion and enhancement
     */
    @PostMapping("/query-expansion")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Query expansion",
        description = "Expands and enhances search queries with synonyms and related terms"
    )
    public CompletableFuture<ResponseEntity<QueryExpansionResult>> expandQuery(
            @Valid @RequestBody QueryExpansionRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Expanding query: {}", request.getOriginalQuery());

                QueryExpansionResult expansion = nlpProcessor.expandQuery(request);

                metricsService.recordCounter("ai.nlp.query-expansion.success");

                return ResponseEntity.ok(expansion);

            } catch (Exception e) {
                metricsService.recordCounter("ai.nlp.query-expansion.error");
                log.error("Error expanding query: {}", request.getOriginalQuery(), e);
                throw new RuntimeException("Query expansion failed", e);
            }
        });
    }

    // Helper Methods
    private NaturalLanguageSearchResult processNaturalLanguageSearch(String userId, NaturalLanguageSearchRequest request) {
        NaturalLanguageSearchResult result = new NaturalLanguageSearchResult();
        result.setUserId(userId);
        result.setSearchDate(LocalDateTime.now());
        result.setOriginalQuery(request.getQuery());
        result.setGeneratedBy("AI Natural Language Search Service");

        // Process natural language query
        ProcessedQuery processedQuery = nlpProcessor.processQuery(request.getQuery(), request);
        result.setProcessedQuery(processedQuery);

        // Recognize intent
        IntentRecognitionResult intent = intentEngine.recognizeIntent(
            new IntentRecognitionRequest(request.getQuery()));
        result.setRecognizedIntent(intent);

        // Analyze semantics
        SemanticAnalysisResult semantics = semanticAnalyzer.analyzeSemantics(
            new SemanticAnalysisRequest(request.getQuery()));
        result.setSemanticAnalysis(semantics);

        // Execute search
        List<PropertySearchResult> properties = executePropertySearch(processedQuery, intent, semantics);
        result.setProperties(properties);

        // Generate search insights
        result.setSearchInsights(generateSearchInsights(result));

        return result;
    }

    private List<PropertySearchResult> executePropertySearch(ProcessedQuery query, IntentRecognitionResult intent, SemanticAnalysisResult semantics) {
        // Simulate property search based on processed query
        return List.of(
            createSearchResult("prop-001", "Modern Downtown Apartment", 0.95),
            createSearchResult("prop-002", "Spacious Suburban House", 0.87),
            createSearchResult("prop-003", "Luxury Penthouse Suite", 0.82)
        );
    }

    private PropertySearchResult createSearchResult(String id, String title, double score) {
        PropertySearchResult result = new PropertySearchResult();
        result.setPropertyId(id);
        result.setPropertyTitle(title);
        result.setRelevanceScore(score);
        return result;
    }

    private List<String> generateSearchInsights(NaturalLanguageSearchResult result) {
        return List.of(
            "Primary intent detected: " + result.getRecognizedIntent().getPrimaryIntent(),
            "Key entities extracted: " + result.getSemanticAnalysis().getEntities().keySet(),
            "Search scope: " + result.getProcessedQuery().getSearchScope(),
            "Found " + result.getProperties().size() + " matching properties"
        );
    }
}

// Data Transfer Objects and Models

class NaturalLanguageSearchRequest {
    private String query;
    private String searchContext;
    private List<String> previousQueries;
    private Map<String, Object> searchPreferences;
    private String sessionId;

    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getSearchContext() { return searchContext; }
    public void setSearchContext(String searchContext) { this.searchContext = searchContext; }
    public List<String> getPreviousQueries() { return previousQueries; }
    public void setPreviousQueries(List<String> previousQueries) { this.previousQueries = previousQueries; }
    public Map<String, Object> getSearchPreferences() { return searchPreferences; }
    public void setSearchPreferences(Map<String, Object> searchPreferences) { this.searchPreferences = searchPreferences; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}

class NaturalLanguageSearchResult {
    private String userId;
    private LocalDateTime searchDate;
    private String generatedBy;
    private String originalQuery;
    private ProcessedQuery processedQuery;
    private IntentRecognitionResult recognizedIntent;
    private SemanticAnalysisResult semanticAnalysis;
    private List<PropertySearchResult> properties;
    private List<String> searchInsights;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getSearchDate() { return searchDate; }
    public void setSearchDate(LocalDateTime searchDate) { this.searchDate = searchDate; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
    public ProcessedQuery getProcessedQuery() { return processedQuery; }
    public void setProcessedQuery(ProcessedQuery processedQuery) { this.processedQuery = processedQuery; }
    public IntentRecognitionResult getRecognizedIntent() { return recognizedIntent; }
    public void setRecognizedIntent(IntentRecognitionResult recognizedIntent) { this.recognizedIntent = recognizedIntent; }
    public SemanticAnalysisResult getSemanticAnalysis() { return semanticAnalysis; }
    public void setSemanticAnalysis(SemanticAnalysisResult semanticAnalysis) { this.semanticAnalysis = semanticAnalysis; }
    public List<PropertySearchResult> getProperties() { return properties; }
    public void setProperties(List<PropertySearchResult> properties) { this.properties = properties; }
    public List<String> getSearchInsights() { return searchInsights; }
    public void setSearchInsights(List<String> searchInsights) { this.searchInsights = searchInsights; }
}

class ProcessedQuery {
    private String cleanedQuery;
    private Map<String, String> extractedCriteria;
    private String searchScope;
    private List<String> searchTerms;
    private Map<String, Object> queryParameters;

    // Getters and setters
    public String getCleanedQuery() { return cleanedQuery; }
    public void setCleanedQuery(String cleanedQuery) { this.cleanedQuery = cleanedQuery; }
    public Map<String, String> getExtractedCriteria() { return extractedCriteria; }
    public void setExtractedCriteria(Map<String, String> extractedCriteria) { this.extractedCriteria = extractedCriteria; }
    public String getSearchScope() { return searchScope; }
    public void setSearchScope(String searchScope) { this.searchScope = searchScope; }
    public List<String> getSearchTerms() { return searchTerms; }
    public void setSearchTerms(List<String> searchTerms) { this.searchTerms = searchTerms; }
    public Map<String, Object> getQueryParameters() { return queryParameters; }
    public void setQueryParameters(Map<String, Object> queryParameters) { this.queryParameters = queryParameters; }
}

class ConversationalSearchRequest {
    private String userMessage;
    private String sessionId;
    private int turnNumber;
    private List<ConversationTurn> conversationHistory;
    private Map<String, Object> context;

    // Getters and setters
    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public List<ConversationTurn> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<ConversationTurn> conversationHistory) { this.conversationHistory = conversationHistory; }
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}

class ConversationalSearchResult {
    private String assistantResponse;
    private List<PropertySearchResult> suggestedProperties;
    private List<String> clarifyingQuestions;
    private boolean conversationComplete;
    private Map<String, Object> updatedContext;

    // Getters and setters
    public String getAssistantResponse() { return assistantResponse; }
    public void setAssistantResponse(String assistantResponse) { this.assistantResponse = assistantResponse; }
    public List<PropertySearchResult> getSuggestedProperties() { return suggestedProperties; }
    public void setSuggestedProperties(List<PropertySearchResult> suggestedProperties) { this.suggestedProperties = suggestedProperties; }
    public List<String> getClarifyingQuestions() { return clarifyingQuestions; }
    public void setClarifyingQuestions(List<String> clarifyingQuestions) { this.clarifyingQuestions = clarifyingQuestions; }
    public boolean isConversationComplete() { return conversationComplete; }
    public void setConversationComplete(boolean conversationComplete) { this.conversationComplete = conversationComplete; }
    public Map<String, Object> getUpdatedContext() { return updatedContext; }
    public void setUpdatedContext(Map<String, Object> updatedContext) { this.updatedContext = updatedContext; }
}

class ConversationTurn {
    private String speaker; // user, assistant
    private String message;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;

    // Getters and setters
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

class IntentRecognitionRequest {
    private String query;
    private String searchContext;
    private List<String> possibleIntents;

    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getSearchContext() { return searchContext; }
    public void setSearchContext(String searchContext) { this.searchContext = searchContext; }
    public List<String> getPossibleIntents() { return possibleIntents; }
    public void setPossibleIntents(List<String> possibleIntents) { this.possibleIntents = possibleIntents; }
}

class IntentRecognitionResult {
    private String primaryIntent;
    private double confidenceScore;
    private Map<String, Double> allIntents;
    private List<String> extractedEntities;
    private Map<String, String> entityTypes;

    // Getters and setters
    public String getPrimaryIntent() { return primaryIntent; }
    public void setPrimaryIntent(String primaryIntent) { this.primaryIntent = primaryIntent; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public Map<String, Double> getAllIntents() { return allIntents; }
    public void setAllIntents(Map<String, Double> allIntents) { this.allIntents = allIntents; }
    public List<String> getExtractedEntities() { return extractedEntities; }
    public void setExtractedEntities(List<String> extractedEntities) { this.extractedEntities = extractedEntities; }
    public Map<String, String> getEntityTypes() { return entityTypes; }
    public void setEntityTypes(Map<String, String> entityTypes) { this.entityTypes = entityTypes; }
}

class SemanticAnalysisRequest {
    private String query;
    private boolean includeSentiment = true;
    private boolean includeEmotions = false;

    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public boolean isIncludeSentiment() { return includeSentiment; }
    public void setIncludeSentiment(boolean includeSentiment) { this.includeSentiment = includeSentiment; }
    public boolean isIncludeEmotions() { return includeEmotions; }
    public void setIncludeEmotions(boolean includeEmotions) { this.includeEmotions = includeEmotions; }
}

class SemanticAnalysisResult {
    private Map<String, String> entities;
    private Map<String, List<String>> relationships;
    private String sentiment;
    private double sentimentScore;
    private List<String> keyPhrases;
    private String queryType;

    // Getters and setters
    public Map<String, String> getEntities() { return entities; }
    public void setEntities(Map<String, String> entities) { this.entities = entities; }
    public Map<String, List<String>> getRelationships() { return relationships; }
    public void setRelationships(Map<String, List<String>> relationships) { this.relationships = relationships; }
    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    public double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }
    public List<String> getKeyPhrases() { return keyPhrases; }
    public void setKeyPhrases(List<String> keyPhrases) { this.keyPhrases = keyPhrases; }
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
}

class SuggestionRequest {
    private String partialQuery;
    private int maxSuggestions = 10;
    private String suggestionType; // auto-complete, related, popular

    // Getters and setters
    public String getPartialQuery() { return partialQuery; }
    public void setPartialQuery(String partialQuery) { this.partialQuery = partialQuery; }
    public int getMaxSuggestions() { return maxSuggestions; }
    public void setMaxSuggestions(int maxSuggestions) { this.maxSuggestions = maxSuggestions; }
    public String getSuggestionType() { return suggestionType; }
    public void setSuggestionType(String suggestionType) { this.suggestionType = suggestionType; }
}

class SearchSuggestions {
    private List<String> suggestions;
    private List<String> popularQueries;
    private List<String> relatedQueries;

    // Getters and setters
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public List<String> getPopularQueries() { return popularQueries; }
    public void setPopularQueries(List<String> popularQueries) { this.popularQueries = popularQueries; }
    public List<String> getRelatedQueries() { return relatedQueries; }
    public void setRelatedQueries(List<String> relatedQueries) { this.relatedQueries = relatedQueries; }
}

class VoiceSearchRequest {
    private String audioData;
    private String audioFormat;
    private int audioLength;
    private String language = "en-US";

    // Getters and setters
    public String getAudioData() { return audioData; }
    public void setAudioData(String audioData) { this.audioData = audioData; }
    public String getAudioFormat() { return audioFormat; }
    public void setAudioFormat(String audioFormat) { this.audioFormat = audioFormat; }
    public int getAudioLength() { return audioLength; }
    public void setAudioLength(int audioLength) { this.audioLength = audioLength; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}

class VoiceSearchResult {
    private String transcribedText;
    private double confidenceScore;
    private NaturalLanguageSearchResult searchResult;
    private List<String> alternativeTranscriptions;

    // Getters and setters
    public String getTranscribedText() { return transcribedText; }
    public void setTranscribedText(String transcribedText) { this.transcribedText = transcribedText; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public NaturalLanguageSearchResult getSearchResult() { return searchResult; }
    public void setSearchResult(NaturalLanguageSearchResult searchResult) { this.searchResult = searchResult; }
    public List<String> getAlternativeTranscriptions() { return alternativeTranscriptions; }
    public void setAlternativeTranscriptions(List<String> alternativeTranscriptions) { this.alternativeTranscriptions = alternativeTranscriptions; }
}

class MultiLanguageSearchRequest {
    private String query;
    private String language;
    private boolean translateToEnglish = true;

    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean isTranslateToEnglish() { return translateToEnglish; }
    public void setTranslateToEnglish(boolean translateToEnglish) { this.translateToEnglish = translateToEnglish; }
}

class MultiLanguageSearchResult {
    private String translatedQuery;
    private NaturalLanguageSearchResult searchResult;
    private List<String> supportedLanguages;

    // Getters and setters
    public String getTranslatedQuery() { return translatedQuery; }
    public void setTranslatedQuery(String translatedQuery) { this.translatedQuery = translatedQuery; }
    public NaturalLanguageSearchResult getSearchResult() { return searchResult; }
    public void setSearchResult(NaturalLanguageSearchResult searchResult) { this.searchResult = searchResult; }
    public List<String> getSupportedLanguages() { return supportedLanguages; }
    public void setSupportedLanguages(List<String> supportedLanguages) { this.supportedLanguages = supportedLanguages; }
}

class FuzzySearchRequest {
    private String query;
    private double fuzzinessThreshold = 0.8;
    private int maxCorrections = 2;

    // Getters and setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public double getFuzzinessThreshold() { return fuzzinessThreshold; }
    public void setFuzzinessThreshold(double fuzzinessThreshold) { this.fuzzinessThreshold = fuzzinessThreshold; }
    public int getMaxCorrections() { return maxCorrections; }
    public void setMaxCorrections(int maxCorrections) { this.maxCorrections = maxCorrections; }
}

class FuzzySearchResult {
    private String correctedQuery;
    private List<String> suggestedCorrections;
    private NaturalLanguageSearchResult searchResult;
    private Map<String, Double> correctionScores;

    // Getters and setters
    public String getCorrectedQuery() { return correctedQuery; }
    public void setCorrectedQuery(String correctedQuery) { this.correctedQuery = correctedQuery; }
    public List<String> getSuggestedCorrections() { return suggestedCorrections; }
    public void setSuggestedCorrections(List<String> suggestedCorrections) { this.suggestedCorrections = suggestedCorrections; }
    public NaturalLanguageSearchResult getSearchResult() { return searchResult; }
    public void setSearchResult(NaturalLanguageSearchResult searchResult) { this.searchResult = searchResult; }
    public Map<String, Double> getCorrectionScores() { return correctionScores; }
    public void setCorrectionScores(Map<String, Double> correctionScores) { this.correctionScores = correctionScores; }
}

class ContextualRefinementRequest {
    private String originalQuery;
    private List<String> userFeedback;
    private List<PropertySearchResult> previousResults;
    private Map<String, Object> userContext;

    // Getters and setters
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
    public List<String> getUserFeedback() { return userFeedback; }
    public void setUserFeedback(List<String> userFeedback) { this.userFeedback = userFeedback; }
    public List<PropertySearchResult> getPreviousResults() { return previousResults; }
    public void setPreviousResults(List<PropertySearchResult> previousResults) { this.previousResults = previousResults; }
    public Map<String, Object> getUserContext() { return userContext; }
    public void setUserContext(Map<String, Object> userContext) { this.userContext = userContext; }
}

class ContextualRefinementResult {
    private String refinedQuery;
    private List<PropertySearchResult> refinedResults;
    private List<String> refinementInsights;
    private double improvementScore;

    // Getters and setters
    public String getRefinedQuery() { return refinedQuery; }
    public void setRefinedQuery(String refinedQuery) { this.refinedQuery = refinedQuery; }
    public List<PropertySearchResult> getRefinedResults() { return refinedResults; }
    public void setRefinedResults(List<PropertySearchResult> refinedResults) { this.refinedResults = refinedResults; }
    public List<String> getRefinementInsights() { return refinementInsights; }
    public void setRefinementInsights(List<String> refinementInsights) { this.refinementInsights = refinementInsights; }
    public double getImprovementScore() { return improvementScore; }
    public void setImprovementScore(double improvementScore) { this.improvementScore = improvementScore; }
}

class QueryExpansionRequest {
    private String originalQuery;
    private List<String> expansionTypes; // synonyms, related, broader, narrower

    // Getters and setters
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
    public List<String> getExpansionTypes() { return expansionTypes; }
    public void setExpansionTypes(List<String> expansionTypes) { this.expansionTypes = expansionTypes; }
}

class QueryExpansionResult {
    private List<String> expandedQueries;
    private Map<String, List<String>> expansionsByType;
    private List<String> suggestedSynonyms;

    // Getters and setters
    public List<String> getExpandedQueries() { return expandedQueries; }
    public void setExpandedQueries(List<String> expandedQueries) { this.expandedQueries = expandedQueries; }
    public Map<String, List<String>> getExpansionsByType() { return expansionsByType; }
    public void setExpansionsByType(Map<String, List<String>> expansionsByType) { this.expansionsByType = expansionsByType; }
    public List<String> getSuggestedSynonyms() { return suggestedSynonyms; }
    public void setSuggestedSynonyms(List<String> suggestedSynonyms) { this.suggestedSynonyms = suggestedSynonyms; }
}

class PropertySearchResult {
    private String propertyId;
    private String propertyTitle;
    private double relevanceScore;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }
    public double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
}

// AI Service Interfaces (to be implemented)
interface NaturalLanguageProcessor {
    ProcessedQuery processQuery(String query, NaturalLanguageSearchRequest request);
    FuzzySearchResult performFuzzySearch(String userId, FuzzySearchRequest request);
    QueryExpansionResult expandQuery(QueryExpansionRequest request);
}

interface IntentRecognitionEngine {
    IntentRecognitionResult recognizeIntent(IntentRecognitionRequest request);
}

interface SemanticQueryAnalyzer {
    SemanticAnalysisResult analyzeSemantics(SemanticAnalysisRequest request);
}

interface ConversationalSearchEngine {
    ConversationalSearchResult processConversation(String userId, ConversationalSearchRequest request);
}

interface ContextAwareSearchProcessor {
    ContextualRefinementResult refineSearchResults(String userId, ContextualRefinementRequest request);
}

interface VoiceSearchProcessor {
    VoiceSearchResult processVoiceSearch(String userId, VoiceSearchRequest request);
}

interface MultiLanguageProcessor {
    MultiLanguageSearchResult processSearch(String userId, MultiLanguageSearchRequest request);
}

interface QuerySuggestionEngine {
    SearchSuggestions generateSuggestions(String userId, SuggestionRequest request);
}