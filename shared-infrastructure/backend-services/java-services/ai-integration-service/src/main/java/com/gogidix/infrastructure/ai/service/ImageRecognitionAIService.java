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
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI-powered Image Recognition Service
 *
 * This service provides advanced computer vision capabilities for property image analysis,
 * visual search, style recognition, and image-based property matching.
 *
 * Features:
 * - Visual property search using image queries
 * - Architectural style recognition
 * - Interior design style detection
 * - Property feature extraction
 * - Image quality assessment
 * - Room type classification
 * - Object detection in property images
 * - Visual similarity matching
 * - Color scheme analysis
 * - Property condition assessment
 */
@RestController
@RequestMapping("/ai/v1/image-recognition")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image Recognition AI Service", description = "AI-powered computer vision for property image analysis and visual search")
public class ImageRecognitionAIService {

    private final CacheService cacheService;
    private final MetricsService metricsService;
    private final AuditService auditService;
    private final SecurityService securityService;
    private final ValidationService validationService;

    // Image Recognition Models
    private final VisualSearchEngine visualSearchEngine;
    private final ArchitecturalStyleRecognizer styleRecognizer;
    private final InteriorDesignAnalyzer interiorAnalyzer;
    private final PropertyFeatureExtractor featureExtractor;
    private final ImageQualityAssessor qualityAssessor;
    private final RoomTypeClassifier roomClassifier;
    private final ObjectDetectionEngine objectDetectionEngine;
    private final VisualSimilarityMatcher similarityMatcher;
    private final ColorSchemeAnalyzer colorAnalyzer;
    private final PropertyConditionAssessor conditionAssessor;

    /**
     * Visual property search using image
     */
    @PostMapping("/visual-search/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Visual property search",
        description = "Searches for properties using image as query"
    )
    public CompletableFuture<ResponseEntity<VisualSearchResult>> visualSearch(
            @PathVariable String userId,
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute VisualSearchRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.image.visual-search");

            try {
                log.info("Performing visual search for user: {}", userId);

                // Validate request
                validationService.validate(request);
                securityService.validateUserAccess(userId);

                // Perform visual search
                VisualSearchResult result = visualSearchEngine.performVisualSearch(userId, image, request);

                // Cache results
                cacheService.set("visual-search:" + userId + ":" + image.hashCode(),
                               result, java.time.Duration.ofMinutes(30));

                // Record metrics
                metricsService.recordCounter("ai.image.visual-search.success");
                metricsService.recordTimer("ai.image.visual-search", stopwatch);

                // Audit
                auditService.audit(
                    "VISUAL_SEARCH_PERFORMED",
                    "userId=" + userId + ",results=" + result.getMatchingProperties().size(),
                    "ai-image-recognition",
                    "success"
                );

                log.info("Successfully completed visual search for user: {}, found {} matches",
                        userId, result.getMatchingProperties().size());
                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.visual-search.error");
                log.error("Error performing visual search for user: {}", userId, e);
                throw new RuntimeException("Visual search failed", e);
            }
        });
    }

    /**
     * Recognize architectural style
     */
    @PostMapping("/architectural-style")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Recognize architectural style",
        description = "Identifies architectural style from property images"
    )
    public CompletableFuture<ResponseEntity<ArchitecturalStyleResult>> recognizeArchitecturalStyle(
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute StyleRecognitionRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.image.style-recognition");

            try {
                log.info("Recognizing architectural style from image");

                ArchitecturalStyleResult result = styleRecognizer.recognizeStyle(image, request);

                metricsService.recordCounter("ai.image.style-recognition.success");
                metricsService.recordTimer("ai.image.style-recognition", stopwatch);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.style-recognition.error");
                log.error("Error recognizing architectural style", e);
                throw new RuntimeException("Style recognition failed", e);
            }
        });
    }

    /**
     * Analyze interior design
     */
    @PostMapping("/interior-design")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Analyze interior design",
        description = "Analyzes interior design style and features"
    )
    public CompletableFuture<ResponseEntity<InteriorDesignResult>> analyzeInteriorDesign(
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute InteriorDesignRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing interior design from image");

                InteriorDesignResult result = interiorAnalyzer.analyzeInterior(image, request);

                metricsService.recordCounter("ai.image.interior-design.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.interior-design.error");
                log.error("Error analyzing interior design", e);
                throw new RuntimeException("Interior design analysis failed", e);
            }
        });
    }

    /**
     * Extract property features
     */
    @PostMapping("/feature-extraction/{propertyId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Extract property features",
        description = "Extracts detailed features from property images"
    )
    public CompletableFuture<ResponseEntity<FeatureExtractionResult>> extractPropertyFeatures(
            @PathVariable String propertyId,
            @RequestParam("images") MultipartFile[] images,
            @Valid @ModelAttribute FeatureExtractionRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.image.feature-extraction");

            try {
                log.info("Extracting features for property: {}", propertyId);

                FeatureExtractionResult result = featureExtractor.extractFeatures(propertyId, images, request);

                metricsService.recordCounter("ai.image.feature-extraction.success");
                metricsService.recordTimer("ai.image.feature-extraction", stopwatch);

                auditService.audit(
                    "FEATURES_EXTRACTED",
                    "propertyId=" + propertyId + ",images=" + images.length,
                    "ai-image-recognition",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.feature-extraction.error");
                log.error("Error extracting features for property: {}", propertyId, e);
                throw new RuntimeException("Feature extraction failed", e);
            }
        });
    }

    /**
     * Assess image quality
     */
    @PostMapping("/quality-assessment")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Assess image quality",
        description = "Evaluates the quality of property images"
    )
    public CompletableFuture<ResponseEntity<ImageQualityResult>> assessImageQuality(
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute QualityAssessmentRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Assessing image quality");

                ImageQualityResult result = qualityAssessor.assessQuality(image, request);

                metricsService.recordCounter("ai.image.quality-assessment.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.quality-assessment.error");
                log.error("Error assessing image quality", e);
                throw new RuntimeException("Quality assessment failed", e);
            }
        });
    }

    /**
     * Classify room type
     */
    @PostMapping("/room-classification")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Classify room type",
        description = "Identifies the type of room from images"
    )
    public CompletableFuture<ResponseEntity<RoomClassificationResult>> classifyRoomType(
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute RoomClassificationRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Classifying room type from image");

                RoomClassificationResult result = roomClassifier.classifyRoom(image, request);

                metricsService.recordCounter("ai.image.room-classification.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.room-classification.error");
                log.error("Error classifying room type", e);
                throw new RuntimeException("Room classification failed", e);
            }
        });
    }

    /**
     * Detect objects in property image
     */
    @PostMapping("/object-detection")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Detect objects",
        description = "Detects and identifies objects in property images"
    )
    public CompletableFuture<ResponseEntity<ObjectDetectionResult>> detectObjects(
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute ObjectDetectionRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.image.object-detection");

            try {
                log.info("Performing object detection in image");

                ObjectDetectionResult result = objectDetectionEngine.detectObjects(image, request);

                metricsService.recordCounter("ai.image.object-detection.success");
                metricsService.recordTimer("ai.image.object-detection", stopwatch);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.object-detection.error");
                log.error("Error detecting objects", e);
                throw new RuntimeException("Object detection failed", e);
            }
        });
    }

    /**
     * Find visually similar properties
     */
    @PostMapping("/visual-similarity/{propertyId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Find visually similar properties",
        description = "Finds properties with similar visual characteristics"
    )
    public CompletableFuture<ResponseEntity<VisualSimilarityResult>> findVisuallySimilarProperties(
            @PathVariable String propertyId,
            @Valid @RequestBody VisualSimilarityRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Finding visually similar properties for: {}", propertyId);

                VisualSimilarityResult result = similarityMatcher.findSimilarProperties(propertyId, request);

                metricsService.recordCounter("ai.image.visual-similarity.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.visual-similarity.error");
                log.error("Error finding visually similar properties for: {}", propertyId, e);
                throw new RuntimeException("Visual similarity search failed", e);
            }
        });
    }

    /**
     * Analyze color scheme
     */
    @PostMapping("/color-analysis")
    @PreAuthorize("hasRole('ROLE_AI_USER')")
    @Operation(
        summary = "Analyze color scheme",
        description = "Analyzes color palette and scheme in property images"
    )
    public CompletableFuture<ResponseEntity<ColorAnalysisResult>> analyzeColorScheme(
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute ColorAnalysisRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing color scheme from image");

                ColorAnalysisResult result = colorAnalyzer.analyzeColors(image, request);

                metricsService.recordCounter("ai.image.color-analysis.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.color-analysis.error");
                log.error("Error analyzing color scheme", e);
                throw new RuntimeException("Color analysis failed", e);
            }
        });
    }

    /**
     * Assess property condition
     */
    @PostMapping("/condition-assessment/{propertyId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Assess property condition",
        description = "Assesses property condition from images"
    )
    public CompletableFuture<ResponseEntity<ConditionAssessmentResult>> assessPropertyCondition(
            @PathVariable String propertyId,
            @RequestParam("images") MultipartFile[] images,
            @Valid @ModelAttribute ConditionAssessmentRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.image.condition-assessment");

            try {
                log.info("Assessing condition for property: {}", propertyId);

                ConditionAssessmentResult result = conditionAssessor.assessCondition(propertyId, images, request);

                metricsService.recordCounter("ai.image.condition-assessment.success");
                metricsService.recordTimer("ai.image.condition-assessment", stopwatch);

                auditService.audit(
                    "CONDITION_ASSESSED",
                    "propertyId=" + propertyId + ",images=" + images.length,
                    "ai-image-recognition",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.condition-assessment.error");
                log.error("Error assessing condition for property: {}", propertyId, e);
                throw new RuntimeException("Condition assessment failed", e);
            }
        });
    }

    /**
     * Batch image processing
     */
    @PostMapping("/batch-process/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_AGENT')")
    @Operation(
        summary = "Batch process images",
        description = "Processes multiple images with selected analysis types"
    )
    public CompletableFuture<ResponseEntity<BatchProcessingResult>> batchProcessImages(
            @PathVariable String userId,
            @RequestParam("images") MultipartFile[] images,
            @Valid @ModelAttribute BatchProcessingRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.image.batch-processing");

            try {
                log.info("Batch processing {} images for user: {}", images.length, userId);

                BatchProcessingResult result = performBatchProcessing(userId, images, request);

                metricsService.recordCounter("ai.image.batch-processing.success");
                metricsService.recordTimer("ai.image.batch-processing", stopwatch);

                auditService.audit(
                    "BATCH_PROCESSING_COMPLETED",
                    "userId=" + userId + ",images=" + images.length + ",types=" + request.getProcessingTypes(),
                    "ai-image-recognition",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.image.batch-processing.error");
                log.error("Error batch processing images for user: {}", userId, e);
                throw new RuntimeException("Batch processing failed", e);
            }
        });
    }

    // Helper Methods
    private BatchProcessingResult performBatchProcessing(String userId, MultipartFile[] images, BatchProcessingRequest request) {
        BatchProcessingResult result = new BatchProcessingResult();
        result.setUserId(userId);
        result.setProcessingDate(LocalDateTime.now());
        result.setTotalImages(images.length);
        result.setProcessingTypes(request.getProcessingTypes());

        // Process each image according to requested types
        for (MultipartFile image : images) {
            ProcessedImageResult processedImage = new ProcessedImageResult();
            processedImage.setImageName(image.getOriginalFilename());
            processedImage.setProcessingDate(LocalDateTime.now());

            // Apply selected processing types
            if (request.getProcessingTypes().contains("feature-extraction")) {
                FeatureExtractionResult features = featureExtractor.extractFeatures("batch-" + image.hashCode(), new MultipartFile[]{image}, new FeatureExtractionRequest());
                processedImage.setFeatures(features);
            }

            if (request.getProcessingTypes().contains("quality-assessment")) {
                ImageQualityResult quality = qualityAssessor.assessQuality(image, new QualityAssessmentRequest());
                processedImage.setQualityAssessment(quality);
            }

            if (request.getProcessingTypes().contains("object-detection")) {
                ObjectDetectionResult objects = objectDetectionEngine.detectObjects(image, new ObjectDetectionRequest());
                processedImage.setDetectedObjects(objects);
            }

            result.getProcessedImages().add(processedImage);
        }

        // Calculate processing summary
        result.setSuccessRate(calculateSuccessRate(result.getProcessedImages()));
        result.setProcessingTimeSummary(generateProcessingTimeSummary(result.getProcessedImages()));

        return result;
    }

    private double calculateSuccessRate(List<ProcessedImageResult> processedImages) {
        long successful = processedImages.stream()
                .filter(img -> img.getFeatures() != null || img.getQualityAssessment() != null || img.getDetectedObjects() != null)
                .count();
        return Math.round((double) successful / processedImages.size() * 100.0) / 100.0;
    }

    private String generateProcessingTimeSummary(List<ProcessedImageResult> processedImages) {
        return String.format("Average processing time: %.2f seconds per image",
                processedImages.stream()
                        .mapToDouble(img -> 1.5) // Simulated processing time
                        .average()
                        .orElse(0.0));
    }
}

// Data Transfer Objects and Models

class VisualSearchRequest {
    private int maxResults = 20;
    private double similarityThreshold = 0.7;
    private List<String> searchFilters;

    // Getters and setters
    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public List<String> getSearchFilters() { return searchFilters; }
    public void setSearchFilters(List<String> searchFilters) { this.searchFilters = searchFilters; }
}

class VisualSearchResult {
    private String queryImageId;
    private List<VisualSearchMatch> matchingProperties;
    private double searchConfidence;
    private LocalDateTime searchTime;

    // Getters and setters
    public String getQueryImageId() { return queryImageId; }
    public void setQueryImageId(String queryImageId) { this.queryImageId = queryImageId; }
    public List<VisualSearchMatch> getMatchingProperties() { return matchingProperties; }
    public void setMatchingProperties(List<VisualSearchMatch> matchingProperties) { this.matchingProperties = matchingProperties; }
    public double getSearchConfidence() { return searchConfidence; }
    public void setSearchConfidence(double searchConfidence) { this.searchConfidence = searchConfidence; }
    public LocalDateTime getSearchTime() { return searchTime; }
    public void setSearchTime(LocalDateTime searchTime) { this.searchTime = searchTime; }
}

class VisualSearchMatch {
    private String propertyId;
    private String propertyTitle;
    private double similarityScore;
    private List<String> similarFeatures;
    private String matchedImageUrl;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }
    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
    public List<String> getSimilarFeatures() { return similarFeatures; }
    public void setSimilarFeatures(List<String> similarFeatures) { this.similarFeatures = similarFeatures; }
    public String getMatchedImageUrl() { return matchedImageUrl; }
    public void setMatchedImageUrl(String matchedImageUrl) { this.matchedImageUrl = matchedImageUrl; }
}

class StyleRecognitionRequest {
    private boolean includeDetailedAnalysis = true;
    private String propertyType;

    // Getters and setters
    public boolean isIncludeDetailedAnalysis() { return includeDetailedAnalysis; }
    public void setIncludeDetailedAnalysis(boolean includeDetailedAnalysis) { this.includeDetailedAnalysis = includeDetailedAnalysis; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
}

class ArchitecturalStyleResult {
    private String primaryStyle;
    private double confidenceScore;
    private List<String> secondaryStyles;
    private Map<String, Double> styleProbabilities;
    private List<String> identifiedFeatures;
    private String architecturalPeriod;

    // Getters and setters
    public String getPrimaryStyle() { return primaryStyle; }
    public void setPrimaryStyle(String primaryStyle) { this.primaryStyle = primaryStyle; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public List<String> getSecondaryStyles() { return secondaryStyles; }
    public void setSecondaryStyles(List<String> secondaryStyles) { this.secondaryStyles = secondaryStyles; }
    public Map<String, Double> getStyleProbabilities() { return styleProbabilities; }
    public void setStyleProbabilities(Map<String, Double> styleProbabilities) { this.styleProbabilities = styleProbabilities; }
    public List<String> getIdentifiedFeatures() { return identifiedFeatures; }
    public void setIdentifiedFeatures(List<String> identifiedFeatures) { this.identifiedFeatures = identifiedFeatures; }
    public String getArchitecturalPeriod() { return architecturalPeriod; }
    public void setArchitecturalPeriod(String architecturalPeriod) { this.architecturalPeriod = architecturalPeriod; }
}

class InteriorDesignRequest {
    private boolean includeFurnitureAnalysis = true;
    private boolean includeColorAnalysis = true;
    private boolean includeLightingAnalysis = true;

    // Getters and setters
    public boolean isIncludeFurnitureAnalysis() { return includeFurnitureAnalysis; }
    public void setIncludeFurnitureAnalysis(boolean includeFurnitureAnalysis) { this.includeFurnitureAnalysis = includeFurnitureAnalysis; }
    public boolean isIncludeColorAnalysis() { return includeColorAnalysis; }
    public void setIncludeColorAnalysis(boolean includeColorAnalysis) { this.includeColorAnalysis = includeColorAnalysis; }
    public boolean isIncludeLightingAnalysis() { return includeLightingAnalysis; }
    public void setIncludeLightingAnalysis(boolean includeLightingAnalysis) { this.includeLightingAnalysis = includeLightingAnalysis; }
}

class InteriorDesignResult {
    private String designStyle;
    private double styleConfidence;
    private List<String> furniturePieces;
    private Map<String, Double> colorPalette;
    private List<String> lightingFeatures;
    private String roomAmbiance;

    // Getters and setters
    public String getDesignStyle() { return designStyle; }
    public void setDesignStyle(String designStyle) { this.designStyle = designStyle; }
    public double getStyleConfidence() { return styleConfidence; }
    public void setStyleConfidence(double styleConfidence) { this.styleConfidence = styleConfidence; }
    public List<String> getFurniturePieces() { return furniturePieces; }
    public void setFurniturePieces(List<String> furniturePieces) { this.furniturePieces = furniturePieces; }
    public Map<String, Double> getColorPalette() { return colorPalette; }
    public void setColorPalette(Map<String, Double> colorPalette) { this.colorPalette = colorPalette; }
    public List<String> getLightingFeatures() { return lightingFeatures; }
    public void setLightingFeatures(List<String> lightingFeatures) { this.lightingFeatures = lightingFeatures; }
    public String getRoomAmbiance() { return roomAmbiance; }
    public void setRoomAmbiance(String roomAmbiance) { this.roomAmbiance = roomAmbiance; }
}

class FeatureExtractionRequest {
    private boolean extractArchitecturalFeatures = true;
    private boolean extractInteriorFeatures = true;
    private boolean extractOutdoorFeatures = true;
    private boolean extractAmenities = true;

    // Getters and setters
    public boolean isExtractArchitecturalFeatures() { return extractArchitecturalFeatures; }
    public void setExtractArchitecturalFeatures(boolean extractArchitecturalFeatures) { this.extractArchitecturalFeatures = extractArchitecturalFeatures; }
    public boolean isExtractInteriorFeatures() { return extractInteriorFeatures; }
    public void setExtractInteriorFeatures(boolean extractInteriorFeatures) { this.extractInteriorFeatures = extractInteriorFeatures; }
    public boolean isExtractOutdoorFeatures() { return extractOutdoorFeatures; }
    public void setExtractOutdoorFeatures(boolean extractOutdoorFeatures) { this.extractOutdoorFeatures = extractOutdoorFeatures; }
    public boolean isExtractAmenities() { return extractAmenities; }
    public void setExtractAmenities(boolean extractAmenities) { this.extractAmenities = extractAmenities; }
}

class FeatureExtractionResult {
    private String propertyId;
    private LocalDateTime extractionDate;
    private List<String> architecturalFeatures;
    private List<String> interiorFeatures;
    private List<String> outdoorFeatures;
    private List<String> amenities;
    private Map<String, Integer> roomCounts;
    private List<String> uniqueFeatures;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public LocalDateTime getExtractionDate() { return extractionDate; }
    public void setExtractionDate(LocalDateTime extractionDate) { this.extractionDate = extractionDate; }
    public List<String> getArchitecturalFeatures() { return architecturalFeatures; }
    public void setArchitecturalFeatures(List<String> architecturalFeatures) { this.architecturalFeatures = architecturalFeatures; }
    public List<String> getInteriorFeatures() { return interiorFeatures; }
    public void setInteriorFeatures(List<String> interiorFeatures) { this.interiorFeatures = interiorFeatures; }
    public List<String> getOutdoorFeatures() { return outdoorFeatures; }
    public void setOutdoorFeatures(List<String> outdoorFeatures) { this.outdoorFeatures = outdoorFeatures; }
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
    public Map<String, Integer> getRoomCounts() { return roomCounts; }
    public void setRoomCounts(Map<String, Integer> roomCounts) { this.roomCounts = roomCounts; }
    public List<String> getUniqueFeatures() { return uniqueFeatures; }
    public void setUniqueFeatures(List<String> uniqueFeatures) { this.uniqueFeatures = uniqueFeatures; }
}

class QualityAssessmentRequest {
    private boolean includeDetailedMetrics = true;
    private String qualityStandard = "real-estate";

    // Getters and setters
    public boolean isIncludeDetailedMetrics() { return includeDetailedMetrics; }
    public void setIncludeDetailedMetrics(boolean includeDetailedMetrics) { this.includeDetailedMetrics = includeDetailedMetrics; }
    public String getQualityStandard() { return qualityStandard; }
    public void setQualityStandard(String qualityStandard) { this.qualityStandard = qualityStandard; }
}

class ImageQualityResult {
    private double overallScore;
    private double resolutionScore;
    private double clarityScore;
    private double lightingScore;
    private double compositionScore;
    private List<String> qualityIssues;
    private List<String> improvementSuggestions;

    // Getters and setters
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
    public double getResolutionScore() { return resolutionScore; }
    public void setResolutionScore(double resolutionScore) { this.resolutionScore = resolutionScore; }
    public double getClarityScore() { return clarityScore; }
    public void setClarityScore(double clarityScore) { this.clarityScore = clarityScore; }
    public double getLightingScore() { return lightingScore; }
    public void setLightingScore(double lightingScore) { this.lightingScore = lightingScore; }
    public double getCompositionScore() { return compositionScore; }
    public void setCompositionScore(double compositionScore) { this.compositionScore = compositionScore; }
    public List<String> getQualityIssues() { return qualityIssues; }
    public void setQualityIssues(List<String> qualityIssues) { this.qualityIssues = qualityIssues; }
    public List<String> getImprovementSuggestions() { return improvementSuggestions; }
    public void setImprovementSuggestions(List<String> improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }
}

class RoomClassificationRequest {
    private boolean includeConfidenceScores = true;
    private List<String> possibleRoomTypes;

    // Getters and setters
    public boolean isIncludeConfidenceScores() { return includeConfidenceScores; }
    public void setIncludeConfidenceScores(boolean includeConfidenceScores) { this.includeConfidenceScores = includeConfidenceScores; }
    public List<String> getPossibleRoomTypes() { return possibleRoomTypes; }
    public void setPossibleRoomTypes(List<String> possibleRoomTypes) { this.possibleRoomTypes = possibleRoomTypes; }
}

class RoomClassificationResult {
    private String primaryRoomType;
    private double confidenceScore;
    private List<String> secondaryRoomTypes;
    private Map<String, Double> roomTypeProbabilities;
    private List<String> identifiedFeatures;
    private double estimatedSize;

    // Getters and setters
    public String getPrimaryRoomType() { return primaryRoomType; }
    public void setPrimaryRoomType(String primaryRoomType) { this.primaryRoomType = primaryRoomType; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public List<String> getSecondaryRoomTypes() { return secondaryRoomTypes; }
    public void setSecondaryRoomTypes(List<String> secondaryRoomTypes) { this.secondaryRoomTypes = secondaryRoomTypes; }
    public Map<String, Double> getRoomTypeProbabilities() { return roomTypeProbabilities; }
    public void setRoomTypeProbabilities(Map<String, Double> roomTypeProbabilities) { this.roomTypeProbabilities = roomTypeProbabilities; }
    public List<String> getIdentifiedFeatures() { return identifiedFeatures; }
    public void setIdentifiedFeatures(List<String> identifiedFeatures) { this.identifiedFeatures = identifiedFeatures; }
    public double getEstimatedSize() { return estimatedSize; }
    public void setEstimatedSize(double estimatedSize) { this.estimatedSize = estimatedSize; }
}

class ObjectDetectionRequest {
    private List<String> targetObjects;
    private double confidenceThreshold = 0.5;
    private boolean includeBoundingBoxes = true;

    // Getters and setters
    public List<String> getTargetObjects() { return targetObjects; }
    public void setTargetObjects(List<String> targetObjects) { this.targetObjects = targetObjects; }
    public double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
    public boolean isIncludeBoundingBoxes() { return includeBoundingBoxes; }
    public void setIncludeBoundingBoxes(boolean includeBoundingBoxes) { this.includeBoundingBoxes = includeBoundingBoxes; }
}

class ObjectDetectionResult {
    private List<DetectedObject> objects;
    private int totalObjects;
    private Map<String, Integer> objectCounts;

    // Getters and setters
    public List<DetectedObject> getObjects() { return objects; }
    public void setObjects(List<DetectedObject> objects) { this.objects = objects; }
    public int getTotalObjects() { return totalObjects; }
    public void setTotalObjects(int totalObjects) { this.totalObjects = totalObjects; }
    public Map<String, Integer> getObjectCounts() { return objectCounts; }
    public void setObjectCounts(Map<String, Integer> objectCounts) { this.objectCounts = objectCounts; }
}

class DetectedObject {
    private String objectClass;
    private double confidenceScore;
    private BoundingBox boundingBox;
    private Map<String, Object> attributes;

    // Getters and setters
    public String getObjectClass() { return objectClass; }
    public void setObjectClass(String objectClass) { this.objectClass = objectClass; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public BoundingBox getBoundingBox() { return boundingBox; }
    public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}

class BoundingBox {
    private int x;
    private int y;
    private int width;
    private int height;

    // Getters and setters
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
}

class VisualSimilarityRequest {
    private String propertyId;
    private List<String> imageIds;
    private double similarityThreshold = 0.7;
    private int maxResults = 10;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public List<String> getImageIds() { return imageIds; }
    public void setImageIds(List<String> imageIds) { this.imageIds = imageIds; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
}

class VisualSimilarityResult {
    private String sourcePropertyId;
    private List<VisualSimilarityMatch> similarProperties;
    private double averageSimilarityScore;

    // Getters and setters
    public String getSourcePropertyId() { return sourcePropertyId; }
    public void setSourcePropertyId(String sourcePropertyId) { this.sourcePropertyId = sourcePropertyId; }
    public List<VisualSimilarityMatch> getSimilarProperties() { return similarProperties; }
    public void setSimilarProperties(List<VisualSimilarityMatch> similarProperties) { this.similarProperties = similarProperties; }
    public double getAverageSimilarityScore() { return averageSimilarityScore; }
    public void setAverageSimilarityScore(double averageSimilarityScore) { this.averageSimilarityScore = averageSimilarityScore; }
}

class VisualSimilarityMatch {
    private String propertyId;
    private String propertyTitle;
    private double similarityScore;
    private List<String> similarFeatures;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }
    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
    public List<String> getSimilarFeatures() { return similarFeatures; }
    public void setSimilarFeatures(List<String> similarFeatures) { this.similarFeatures = similarFeatures; }
}

class ColorAnalysisRequest {
    private boolean includeColorPalette = true;
    private boolean includeDominantColors = true;
    private int maxColors = 10;

    // Getters and setters
    public boolean isIncludeColorPalette() { return includeColorPalette; }
    public void setIncludeColorPalette(boolean includeColorPalette) { this.includeColorPalette = includeColorPalette; }
    public boolean isIncludeDominantColors() { return includeDominantColors; }
    public void setIncludeDominantColors(boolean includeDominantColors) { this.includeDominantColors = includeDominantColors; }
    public int getMaxColors() { return maxColors; }
    public void setMaxColors(int maxColors) { this.maxColors = maxColors; }
}

class ColorAnalysisResult {
    private String dominantColor;
    private List<String> colorPalette;
    private Map<String, Double> colorDistribution;
    private String colorScheme;
    private List<String> colorHarmony;

    // Getters and setters
    public String getDominantColor() { return dominantColor; }
    public void setDominantColor(String dominantColor) { this.dominantColor = dominantColor; }
    public List<String> getColorPalette() { return colorPalette; }
    public void setColorPalette(List<String> colorPalette) { this.colorPalette = colorPalette; }
    public Map<String, Double> getColorDistribution() { return colorDistribution; }
    public void setColorDistribution(Map<String, Double> colorDistribution) { this.colorDistribution = colorDistribution; }
    public String getColorScheme() { return colorScheme; }
    public void setColorScheme(String colorScheme) { this.colorScheme = colorScheme; }
    public List<String> getColorHarmony() { return colorHarmony; }
    public void setColorHarmony(List<String> colorHarmony) { this.colorHarmony = colorHarmony; }
}

class ConditionAssessmentRequest {
    private boolean includeDetailedReport = true;
    private List<String> areasToAssess;

    // Getters and setters
    public boolean isIncludeDetailedReport() { return includeDetailedReport; }
    public void setIncludeDetailedReport(boolean includeDetailedReport) { this.includeDetailedReport = includeDetailedReport; }
    public List<String> getAreasToAssess() { return areasToAssess; }
    public void setAreasToAssess(List<String> areasToAssess) { this.areasToAssess = areasToAssess; }
}

class ConditionAssessmentResult {
    private String propertyId;
    private LocalDateTime assessmentDate;
    private double overallConditionScore;
    private Map<String, Double> areaScores;
    private List<String> maintenanceIssues;
    private List<String> improvementRecommendations;
    private String conditionGrade;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public LocalDateTime getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDateTime assessmentDate) { this.assessmentDate = assessmentDate; }
    public double getOverallConditionScore() { return overallConditionScore; }
    public void setOverallConditionScore(double overallConditionScore) { this.overallConditionScore = overallConditionScore; }
    public Map<String, Double> getAreaScores() { return areaScores; }
    public void setAreaScores(Map<String, Double> areaScores) { this.areaScores = areaScores; }
    public List<String> getMaintenanceIssues() { return maintenanceIssues; }
    public void setMaintenanceIssues(List<String> maintenanceIssues) { this.maintenanceIssues = maintenanceIssues; }
    public List<String> getImprovementRecommendations() { return improvementRecommendations; }
    public void setImprovementRecommendations(List<String> improvementRecommendations) { this.improvementRecommendations = improvementRecommendations; }
    public String getConditionGrade() { return conditionGrade; }
    public void setConditionGrade(String conditionGrade) { this.conditionGrade = conditionGrade; }
}

class BatchProcessingRequest {
    private List<String> processingTypes;
    private boolean generateReport = true;

    // Getters and setters
    public List<String> getProcessingTypes() { return processingTypes; }
    public void setProcessingTypes(List<String> processingTypes) { this.processingTypes = processingTypes; }
    public boolean isGenerateReport() { return generateReport; }
    public void setGenerateReport(boolean generateReport) { this.generateReport = generateReport; }
}

class BatchProcessingResult {
    private String userId;
    private LocalDateTime processingDate;
    private int totalImages;
    private List<String> processingTypes;
    private List<ProcessedImageResult> processedImages;
    private double successRate;
    private String processingTimeSummary;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getProcessingDate() { return processingDate; }
    public void setProcessingDate(LocalDateTime processingDate) { this.processingDate = processingDate; }
    public int getTotalImages() { return totalImages; }
    public void setTotalImages(int totalImages) { this.totalImages = totalImages; }
    public List<String> getProcessingTypes() { return processingTypes; }
    public void setProcessingTypes(List<String> processingTypes) { this.processingTypes = processingTypes; }
    public List<ProcessedImageResult> getProcessedImages() { return processedImages; }
    public void setProcessedImages(List<ProcessedImageResult> processedImages) { this.processedImages = processedImages; }
    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    public String getProcessingTimeSummary() { return processingTimeSummary; }
    public void setProcessingTimeSummary(String processingTimeSummary) { this.processingTimeSummary = processingTimeSummary; }
}

class ProcessedImageResult {
    private String imageName;
    private LocalDateTime processingDate;
    private FeatureExtractionResult features;
    private ImageQualityResult qualityAssessment;
    private ObjectDetectionResult detectedObjects;

    // Getters and setters
    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }
    public LocalDateTime getProcessingDate() { return processingDate; }
    public void setProcessingDate(LocalDateTime processingDate) { this.processingDate = processingDate; }
    public FeatureExtractionResult getFeatures() { return features; }
    public void setFeatures(FeatureExtractionResult features) { this.features = features; }
    public ImageQualityResult getQualityAssessment() { return qualityAssessment; }
    public void setQualityAssessment(ImageQualityResult qualityAssessment) { this.qualityAssessment = qualityAssessment; }
    public ObjectDetectionResult getDetectedObjects() { return detectedObjects; }
    public void setDetectedObjects(ObjectDetectionResult detectedObjects) { this.detectedObjects = detectedObjects; }
}

// AI Service Interfaces (to be implemented)
interface VisualSearchEngine {
    VisualSearchResult performVisualSearch(String userId, MultipartFile image, VisualSearchRequest request);
}

interface ArchitecturalStyleRecognizer {
    ArchitecturalStyleResult recognizeStyle(MultipartFile image, StyleRecognitionRequest request);
}

interface InteriorDesignAnalyzer {
    InteriorDesignResult analyzeInterior(MultipartFile image, InteriorDesignRequest request);
}

interface PropertyFeatureExtractor {
    FeatureExtractionResult extractFeatures(String propertyId, MultipartFile[] images, FeatureExtractionRequest request);
}

interface ImageQualityAssessor {
    ImageQualityResult assessQuality(MultipartFile image, QualityAssessmentRequest request);
}

interface RoomTypeClassifier {
    RoomClassificationResult classifyRoom(MultipartFile image, RoomClassificationRequest request);
}

interface ObjectDetectionEngine {
    ObjectDetectionResult detectObjects(MultipartFile image, ObjectDetectionRequest request);
}

interface VisualSimilarityMatcher {
    VisualSimilarityResult findSimilarProperties(String propertyId, VisualSimilarityRequest request);
}

interface ColorSchemeAnalyzer {
    ColorAnalysisResult analyzeColors(MultipartFile image, ColorAnalysisRequest request);
}

interface PropertyConditionAssessor {
    ConditionAssessmentResult assessCondition(String propertyId, MultipartFile[] images, ConditionAssessmentRequest request);
}