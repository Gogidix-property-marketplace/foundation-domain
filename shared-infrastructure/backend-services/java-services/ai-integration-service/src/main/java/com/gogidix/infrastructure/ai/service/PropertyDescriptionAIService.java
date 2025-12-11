package com.gogidix.infrastructure.ai.service;

import com.gogidix.platform.common.core.dto.BaseResponse;
import com.gogidix.platform.common.core.dto.PaginationRequest;
import com.gogidix.platform.common.core.dto.PaginationResponse;
import com.gogidix.platform.common.security.annotation.RequiresRole;
import com.gogidix.platform.common.audit.annotation.AuditOperation;
import com.gogidix.platform.common.monitoring.annotation.Timed;
import com.gogidix.platform.common.cache.annotation.Cacheable;
import com.gogidix.platform.common.validation.annotation.ValidPropertyData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Property Description AI Service
 *
 * CATEGORY 1: Property Management Automation
 * Service: Property Description Generation (9/48)
 *
 * AI-Powered property description generation using:
 * - GPT-4 for natural language generation
 * - Property feature analysis and optimization
 * - Market-specific content customization
 * - SEO-optimized descriptions
 * - Multi-language support
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Timed(name = "property-description-ai", description = "Property Description AI Service Metrics")
public class PropertyDescriptionAIService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final PropertyDescriptionRepository repository;
    private final PropertyAnalyticsService analyticsService;

    @Value("${ai.property-description.model:gpt-4}")
    private String aiModel;

    @Value("${ai.property-description.max-tokens:1000}")
    private int maxTokens;

    @Value("${ai.property-description.temperature:0.7}")
    private double temperature;

    // Property description templates for different property types
    private static final Map<String, String> DESCRIPTION_TEMPLATES = Map.of(
        "RESIDENTIAL", """
            Generate a compelling property description for a residential property with these features:
            - Property Type: {propertyType}
            - Bedrooms: {bedrooms}
            - Bathrooms: {bathrooms}
            - Square Footage: {squareFootage}
            - Location: {location}
            - Key Features: {keyFeatures}
            - Unique Selling Points: {uniqueSellingPoints}

            Requirements:
            - Start with an engaging opening line
            - Highlight lifestyle benefits
            - Include neighborhood amenities
            - Use persuasive, descriptive language
            - Keep under 300 words
            - Include SEO keywords
            - End with a strong call-to-action
            """,

        "COMMERCIAL", """
            Create a professional property description for a commercial property:
            - Property Type: {propertyType}
            - Total Area: {totalArea}
            - Location: {location}
            - Zoning: {zoning}
            - Accessibility: {accessibility}
            - Business Advantages: {businessAdvantages}

            Requirements:
            - Focus on business value proposition
            - Include investment potential
            - Highlight infrastructure and connectivity
            - Mention competitive advantages
            - Professional and concise tone
            - Include compliance information
            """,

        "LUXURY", """
            Craft an exclusive description for a luxury property:
            - Property Type: {propertyType}
            - Premium Features: {premiumFeatures}
            - Location: {location}
            - Exclusive Amenities: {exclusiveAmenities}
            - Investment Value: {investmentValue}
            - Lifestyle: {lifestyle}

            Requirements:
            - Sophisticated and elegant tone
            - Emphasize exclusivity and prestige
            - Highlight unique architectural elements
            - Include premium materials and finishes
            - Focus on luxury lifestyle benefits
            - Use evocative, premium language
            """
    );

    /**
     * Generate AI-powered property description
     */
    @Transactional
    @AuditOperation(operation = "GENERATE_PROPERTY_DESCRIPTION",
                   entity = "PropertyDescription",
                   description = "AI-generated property description")
    @Cacheable(key = "#propertyData.hashCode()", ttl = 3600)
    public CompletableFuture<PropertyDescriptionResponse> generateDescription(
            @ValidPropertyData PropertyDescriptionRequest propertyData) {

        log.info("Generating AI property description for property type: {}",
                propertyData.getPropertyType());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Select appropriate template
                String template = selectTemplate(propertyData);

                // 2. Create prompt with property data
                PromptTemplate promptTemplate = new PromptTemplate(template,
                    Map.of(
                        "propertyType", propertyData.getPropertyType(),
                        "bedrooms", propertyData.getBedrooms(),
                        "bathrooms", propertyData.getBathrooms(),
                        "squareFootage", propertyData.getSquareFootage(),
                        "location", propertyData.getLocation(),
                        "keyFeatures", String.join(", ", propertyData.getFeatures()),
                        "uniqueSellingPoints", String.join(", ", propertyData.getUniqueSellingPoints())
                    )
                );

                // 3. Generate description using AI
                Prompt prompt = promptTemplate.create();
                String aiResponse = chatClient.call(prompt).getResult().getOutput().getContent();

                // 4. Enhance with SEO keywords
                String seoOptimized = enhanceWithSEO(aiResponse, propertyData);

                // 5. Analyze quality and metrics
                DescriptionQuality quality = analyzeDescriptionQuality(seoOptimized);

                // 6. Save to database
                PropertyDescription savedDescription = saveDescription(
                    propertyData, seoOptimized, quality);

                // 7. Track analytics
                analyticsService.trackDescriptionGeneration(savedDescription);

                // 8. Generate response variations
                List<String> variations = generateVariations(seoOptimized, 2);

                return PropertyDescriptionResponse.builder()
                    .descriptionId(savedDescription.getId())
                    .primaryDescription(seoOptimized)
                    .variations(variations)
                    .qualityScore(quality.getOverallScore())
                    .seoScore(quality.getSeoScore())
                    .readabilityScore(quality.getReadabilityScore())
                    .engagementScore(quality.getEngagementScore())
                    .suggestedImprovements(quality.getImprovements())
                    .generatedAt(LocalDateTime.now())
                    .modelUsed(aiModel)
                    .wordCount(countWords(seoOptimized))
                    .build();

            } catch (Exception e) {
                log.error("Error generating property description", e);
                throw new PropertyDescriptionGenerationException(
                    "Failed to generate property description: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Batch generate descriptions for multiple properties
     */
    @Transactional
    @AuditOperation(operation = "BATCH_GENERATE_DESCRIPTIONS",
                   entity = "PropertyDescription",
                   description = "Batch AI-generated property descriptions")
    public CompletableFuture<BatchDescriptionResponse> generateBatchDescriptions(
            List<PropertyDescriptionRequest> properties) {

        log.info("Starting batch description generation for {} properties", properties.size());

        List<CompletableFuture<PropertyDescriptionResponse>> futures = properties.stream()
            .map(this::generateDescription)
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<PropertyDescriptionResponse> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

                long successful = results.stream()
                    .mapToLong(r -> r.getDescriptionId() != null ? 1 : 0)
                    .sum();

                return BatchDescriptionResponse.builder()
                    .totalCount(properties.size())
                    .successfulCount((int) successful)
                    .failedCount(properties.size() - (int) successful)
                    .results(results)
                    .processedAt(LocalDateTime.now())
                    .build();
            });
    }

    /**
     * Get description analytics and performance metrics
     */
    @RequiresRole({"AGENT", "ADMIN", "MANAGER"})
    public DescriptionAnalyticsResponse getDescriptionAnalytics(
            String descriptionId,
            String timeRange) {

        log.info("Fetching description analytics for ID: {}", descriptionId);

        PropertyDescription description = repository.findById(descriptionId)
            .orElseThrow(() -> new DescriptionNotFoundException(
                "Description not found: " + descriptionId));

        return analyticsService.getDescriptionMetrics(description, timeRange);
    }

    /**
     * Update and optimize existing description
     */
    @Transactional
    @AuditOperation(operation = "OPTIMIZE_DESCRIPTION",
                   entity = "PropertyDescription",
                   description = "AI-optimized property description")
    @Cacheable(key = "#descriptionId + '_' + #optimizationRequest.hashCode()", ttl = 1800)
    public CompletableFuture<PropertyDescriptionResponse> optimizeDescription(
            String descriptionId,
            DescriptionOptimizationRequest optimizationRequest) {

        return CompletableFuture.supplyAsync(() -> {
            PropertyDescription existing = repository.findById(descriptionId)
                .orElseThrow(() -> new DescriptionNotFoundException(
                    "Description not found: " + descriptionId));

            // Analyze current description
            DescriptionQuality currentQuality = analyzeDescriptionQuality(
                existing.getPrimaryDescription());

            // Generate optimized version
            String optimized = generateOptimizedVersion(
                existing.getPrimaryDescription(),
                optimizationRequest.getOptimizationGoals());

            // Compare and validate improvements
            DescriptionQuality optimizedQuality = analyzeDescriptionQuality(optimized);

            if (optimizedQuality.getOverallScore() > currentQuality.getOverallScore()) {
                // Save optimized version
                existing.setOptimizedDescription(optimized);
                existing.setOptimizedAt(LocalDateTime.now());
                existing.setOptimizationScore(optimizedQuality.getOverallScore());
                repository.save(existing);

                return PropertyDescriptionResponse.builder()
                    .descriptionId(existing.getId())
                    .primaryDescription(existing.getPrimaryDescription())
                    .optimizedDescription(optimized)
                    .originalQuality(currentQuality.getOverallScore())
                    .optimizedQuality(optimizedQuality.getOverallScore())
                    .improvementPercentage(calculateImprovement(
                        currentQuality.getOverallScore(),
                        optimizedQuality.getOverallScore()))
                    .optimizationsApplied(optimizationRequest.getOptimizationGoals())
                    .optimizedAt(LocalDateTime.now())
                    .build();
            } else {
                throw new OptimizationNotBeneficialException(
                    "Optimization would not improve description quality");
            }
        });
    }

    /**
     * Get saved descriptions with pagination
     */
    @RequiresRole({"AGENT", "ADMIN", "MANAGER"})
    public PaginationResponse<PropertyDescriptionSummary> getDescriptions(
            PaginationRequest request,
            PropertyDescriptionFilter filter) {

        log.info("Fetching property descriptions with pagination: page={}, size={}",
                request.getPage(), request.getSize());

        Pageable pageable = Pageable.ofSize(request.getSize())
            .withPage(request.getPage());

        return repository.findDescriptionsWithFilter(filter, pageable);
    }

    // Private helper methods

    private String selectTemplate(PropertyDescriptionRequest data) {
        String category = data.getPropertyCategory().toUpperCase();
        return DESCRIPTION_TEMPLATES.getOrDefault(category,
            DESCRIPTION_TEMPLATES.get("RESIDENTIAL"));
    }

    private String enhanceWithSEO(String description, PropertyDescriptionRequest data) {
        // Add location-based keywords
        String locationKeywords = extractLocationKeywords(data.getLocation());

        // Add property type keywords
        String typeKeywords = getPropertyTypeKeywords(data.getPropertyType());

        // Add feature keywords
        String featureKeywords = extractFeatureKeywords(data.getFeatures());

        return description + "\n\nKeywords: " +
            String.join(", ", locationKeywords, typeKeywords, featureKeywords);
    }

    private DescriptionQuality analyzeDescriptionQuality(String description) {
        return DescriptionQuality.builder()
            .overallScore(calculateOverallScore(description))
            .seoScore(calculateSEOScore(description))
            .readabilityScore(calculateReadabilityScore(description))
            .engagementScore(calculateEngagementScore(description))
            .improvements(generateImprovementSuggestions(description))
            .build();
    }

    private List<String> generateVariations(String baseDescription, int count) {
        // Generate variations using AI with different prompts
        List<String> variations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String variationPrompt = String.format(
                "Rewrite this property description with a different tone: %s",
                baseDescription);

            String variation = chatClient.call(
                new Prompt(new PromptTemplate(variationPrompt).create()))
                .getResult().getOutput().getContent();
            variations.add(variation);
        }
        return variations;
    }

    private PropertyDescription saveDescription(
            PropertyDescriptionRequest request,
            String description,
            DescriptionQuality quality) {

        PropertyDescription entity = PropertyDescription.builder()
            .propertyId(request.getPropertyId())
            .propertyType(request.getPropertyType())
            .location(request.getLocation())
            .primaryDescription(description)
            .qualityScore(quality.getOverallScore())
            .createdAt(LocalDateTime.now())
            .modelUsed(aiModel)
            .build();

        return repository.save(entity);
    }

    // Additional helper methods for metrics and analysis
    private int countWords(String text) {
        return text.split("\\s+").length;
    }

    private double calculateImprovement(double original, double optimized) {
        return ((optimized - original) / original) * 100;
    }

    private String generateOptimizedVersion(String original, List<String> goals) {
        String optimizationPrompt = String.format(
            "Optimize this property description focusing on: %s\n\nOriginal: %s",
            String.join(", ", goals), original);

        return chatClient.call(new Prompt(optimizationPrompt))
            .getResult().getOutput().getContent();
    }

    // Additional private methods for keyword extraction and scoring
    private List<String> extractLocationKeywords(String location) {
        // Extract and normalize location-based SEO keywords
        return Arrays.asList(location.toLowerCase().split("[,\\s]+"));
    }

    private List<String> getPropertyTypeKeywords(String propertyType) {
        // Generate property type specific keywords
        return Arrays.asList(propertyType.toLowerCase().split("_"));
    }

    private List<String> extractFeatureKeywords(List<String> features) {
        // Extract and normalize feature keywords
        return features.stream()
            .flatMap(f -> Arrays.stream(f.toLowerCase().split("[\\s-]+")))
            .distinct()
            .collect(Collectors.toList());
    }

    private double calculateOverallScore(String description) {
        // Calculate overall quality score based on multiple factors
        double seoWeight = 0.3;
        double readabilityWeight = 0.4;
        double engagementWeight = 0.3;

        return (calculateSEOScore(description) * seoWeight) +
               (calculateReadabilityScore(description) * readabilityWeight) +
               (calculateEngagementScore(description) * engagementWeight);
    }

    private double calculateSEOScore(String description) {
        // Score based on keyword density, length, and SEO best practices
        double keywordScore = calculateKeywordDensity(description);
        double lengthScore = calculateLengthScore(description);
        double structureScore = calculateStructureScore(description);

        return (keywordScore + lengthScore + structureScore) / 3;
    }

    private double calculateReadabilityScore(String description) {
        // Calculate readability score using Flesch-Kincaid or similar
        int sentences = description.split("[.!?]+").length;
        int words = description.split("\\s+").length;
        int syllables = countSyllables(description);

        // Simplified readability calculation
        return Math.max(0, Math.min(100, 206.835 - (1.015 * (words / sentences)) - (84.6 * (syllables / words))));
    }

    private double calculateEngagementScore(String description) {
        // Score based on emotional words, call-to-actions, and persuasive language
        double emotionalScore = calculateEmotionalWords(description);
        double ctaScore = calculateCallToActions(description);
        double persuasiveScore = calculatePersuasiveLanguage(description);

        return (emotionalScore + ctaScore + persuasiveScore) / 3;
    }

    private List<String> generateImprovementSuggestions(String description) {
        List<String> suggestions = new ArrayList<>();

        if (description.length() < 100) {
            suggestions.add("Consider adding more detail to the description");
        }

        if (description.length() > 500) {
            suggestions.add("Description might be too long - consider condensing");
        }

        if (!description.toLowerCase().contains("contact")) {
            suggestions.add("Add a clear call-to-action");
        }

        return suggestions;
    }

    // Additional metric calculation helpers
    private double calculateKeywordDensity(String description) {
        // Calculate keyword density for SEO
        String[] words = description.toLowerCase().split("\\s+");
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        return (double) uniqueWords.size() / words.length * 100;
    }

    private double calculateLengthScore(String description) {
        // Optimal length is 200-300 words
        int wordCount = countWords(description);
        if (wordCount >= 200 && wordCount <= 300) {
            return 100;
        } else if (wordCount < 200) {
            return (wordCount / 200.0) * 100;
        } else {
            return Math.max(0, 100 - ((wordCount - 300) / 10));
        }
    }

    private double calculateStructureScore(String description) {
        // Check for proper structure: intro, body, conclusion
        boolean hasIntro = description.length() > 0;
        boolean hasBody = description.length() > 50;
        boolean hasConclusion = description.contains("contact") ||
                               description.contains("schedule") ||
                               description.contains("visit");

        return (hasIntro ? 33.3 : 0) + (hasBody ? 33.3 : 0) + (hasConclusion ? 33.4 : 0);
    }

    private int countSyllables(String text) {
        // Simplified syllable counting
        return text.replaceAll("[^aeiouAEIOU]", "").length();
    }

    private double calculateEmotionalWords(String description) {
        String[] emotionalWords = {"beautiful", "stunning", "amazing", "perfect", "dream", "luxury"};
        long count = Arrays.stream(emotionalWords)
            .filter(word -> description.toLowerCase().contains(word))
            .count();
        return (count * 100) / emotionalWords.length;
    }

    private double calculateCallToActions(String description) {
        String[] ctaPhrases = {"contact us", "schedule a", "visit today", "call now", "learn more"};
        long count = Arrays.stream(ctaPhrases)
            .filter(phrase -> description.toLowerCase().contains(phrase))
            .count();
        return (count * 100) / ctaPhrases.length;
    }

    private double calculatePersuasiveLanguage(String description) {
        String[] persuasiveWords = {"exclusive", "prime", "premium", "unique", "rare", "limited"};
        long count = Arrays.stream(persuasiveWords)
            .filter(word -> description.toLowerCase().contains(word))
            .count();
        return (count * 100) / persuasiveWords.length;
    }
}