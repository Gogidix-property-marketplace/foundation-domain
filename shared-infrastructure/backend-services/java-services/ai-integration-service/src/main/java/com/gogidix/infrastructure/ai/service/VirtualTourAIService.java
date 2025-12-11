package com.gogidix.infrastructure.ai.service;

import com.gogidix.platform.common.core.dto.BaseResponse;
import com.gogidix.platform.common.core.dto.PaginationRequest;
import com.gogidix.platform.common.core.dto.PaginationResponse;
import com.gogidix.platform.common.security.annotation.RequiresRole;
import com.gogidix.platform.common.audit.annotation.AuditOperation;
import com.gogidix.platform.common.monitoring.annotation.Timed;
import com.gogidix.platform.common.cache.annotation.Cacheable;
import com.gogidix.platform.common.validation.annotation.ValidImageData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Virtual Tour AI Service
 *
 * CATEGORY 1: Property Management Automation
 * Service: Virtual Tour (4/48)
 *
 * AI-Powered virtual property tours using:
 * - 3D reconstruction from 2D images
 * - Matterport-style immersive tours
 * - Interactive hotspots and information points
 * - AI-guided tour narration
 * - Multi-device compatibility (VR, AR, Web)
 * - Real-time rendering optimization
 * - Tour analytics and user behavior tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Timed(name = "virtual-tour-ai", description = "Virtual Tour AI Service Metrics")
public class VirtualTourAIService {

    private final VirtualTourRepository repository;
    private final Tour3DRenderingService tour3DRenderingService;
    private final TourNarrationService tourNarrationService;
    private final TourAnalyticsService tourAnalyticsService;
    private final PropertyAnalyticsService propertyAnalyticsService;

    @Value("${ai.virtual-tour.max-rooms:20}")
    private int maxRooms;

    @Value("${ai.virtual-tour.supported-formats:MATTERPORT,360,VIDEO,INTERACTIVE_3D,VR}")
    private List<String> supportedFormats;

    @Value("${ai.virtual-tour.default-quality:HIGH}")
    private String defaultQuality;

    @Value("${ai.virtual-tour.max-tour-duration:600}")
    private int maxTourDurationSeconds;

    // Tour templates for different property types
    private static final Map<String, TourTemplate> TOUR_TEMPLATES = Map.of(
        "APARTMENT", TourTemplate.builder()
            .propertyType("APARTMENT")
            .recommendedRooms(Arrays.asList("living_room", "bedroom_master", "kitchen", "bathroom", "balcony"))
            .tourDurationMinutes(3)
            .narrationStyle("MODERN_FRIENDLY")
            .hotspots(Arrays.asList("appliances", "storage", "views", "amenities"))
            .interactiveFeatures(Arrays.asList("measurements", "furniture_placement", "day_night_view"))
            .build(),

        "HOUSE", TourTemplate.builder()
            .propertyType("HOUSE")
            .recommendedRooms(Arrays.asList("entrance", "living_room", "kitchen", "dining_room",
                                           "bedroom_master", "bedroom_secondary", "bathroom_master",
                                           "bathroom_secondary", "backyard", "garage"))
            .tourDurationMinutes(5)
            .narrationStyle("WELCOMING_DETAILED")
            .hotspots(Arrays.asList("appliances", "storage", "views", "amenities", "outdoor_spaces"))
            .interactiveFeatures(Arrays.asList("measurements", "furniture_placement", "day_night_view",
                                            "seasonal_changes", "renovation_potentials"))
            .build(),

        "VILLA", TourTemplate.builder()
            .propertyType("VILLA")
            .recommendedRooms(Arrays.asList("entrance", "foyer", "living_room", "formal_dining",
                                           "gourmet_kitchen", "family_room", "bedroom_master_suite",
                                           "bedrooms_secondary", "bathrooms_multiple", "home_office",
                                           "home_theater", "wine_cellar", "pool_area", "garden",
                                           "guest_house", "garage_multiple"))
            .tourDurationMinutes(7)
            .narrationStyle("LUXURY_ELEGANT")
            .hotspots(Arrays.asList("luxury_features", "smart_home", "views", "amenities",
                                   "outdoor_living", "custom_details"))
            .interactiveFeatures(Arrays.asList("measurements", "furniture_placement", "day_night_view",
                                            "seasonal_changes", "renovation_potentials",
                                            "virtual_staging_options"))
            .build(),

        "COMMERCIAL", TourTemplate.builder()
            .propertyType("COMMERCIAL")
            .recommendedRooms(Arrays.asList("entrance", "reception", "main_office", "conference_rooms",
                                           "break_room", "storage", "parking"))
            .tourDurationMinutes(4)
            .narrationStyle("PROFESSIONAL_INFORMATIVE")
            .hotspots(Arrays.asList("infrastructure", "accessibility", "capacity", "tech_features"))
            .interactiveFeatures(Arrays.asList("measurements", "layout_options", "tenant_customization"))
            .build()
    );

    /**
     * Create immersive virtual tour from property images
     */
    @Transactional
    @AuditOperation(operation = "CREATE_VIRTUAL_TOUR",
                   entity = "VirtualTour",
                   description = "AI-powered immersive virtual tour creation")
    @Cacheable(key = "#request.hashCode()", ttl = 28800)
    public CompletableFuture<VirtualTourResponse> createVirtualTour(
            @ValidImageData VirtualTourRequest request) {

        log.info("Creating virtual tour for property: {}, format: {}, quality: {}",
                request.getPropertyId(), request.getTourFormat(), request.getQuality());

        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // 1. Validate and preprocess images
                List<BufferedImage> processedImages = validateAndPreprocessImages(request);

                // 2. Select tour template
                TourTemplate template = selectTourTemplate(request.getPropertyType(), request.getTourStyle());

                // 3. Create 3D model reconstruction
                Model3D model3D = tour3DRenderingService.create3DModel(processedImages, request);

                // 4. Generate tour path and navigation
                TourPath tourPath = generateTourPath(model3D, template);

                // 5. Create interactive hotspots
                List<TourHotspot> hotspots = createInteractiveHotspots(model3D, request);

                // 6. Generate AI narration
                TourNarration narration = tourNarrationService.generateNarration(
                    model3D, tourPath, template.getNarrationStyle(), request.getLanguage());

                // 7. Render virtual tour
                VirtualTourResult tourResult = tour3DRenderingService.renderVirtualTour(
                    model3D, tourPath, hotspots, narration, request);

                // 8. Generate alternative views
                List<TourAlternative> alternatives = generateTourAlternatives(
                    model3D, request.getAlternativeViews());

                // 9. Calculate tour metrics
                TourMetrics metrics = calculateTourMetrics(tourResult, model3D);

                // 10. Save tour record
                VirtualTour tour = saveTourRecord(
                    request, tourResult, model3D, tourPath, narration, metrics);

                // 11. Track analytics
                tourAnalyticsService.trackTourCreation(tour);

                long processingTime = System.currentTimeMillis() - startTime;

                return VirtualTourResponse.builder()
                    .tourId(tour.getId())
                    .tourUrl(tourResult.getTourUrl())
                    .embedCode(tourResult.getEmbedCode())
                    .tourDuration(tourResult.getDuration())
                    .roomCount(tourResult.getRoomCount())
                    .tourFormat(request.getTourFormat())
                    .quality(request.getQuality())
                    .viewingUrl(tourResult.getViewingUrl())
                    .downloadUrl(tourResult.getDownloadUrl())
                    .alternatives(alternatives)
                    .hotspots(hotspots)
                    .narration(narration)
                    .metrics(metrics)
                    .processingTime(processingTime)
                    .generatedAt(LocalDateTime.now())
                    .recommendations(generateTourRecommendations(metrics))
                    .build();

            } catch (Exception e) {
                log.error("Error creating virtual tour", e);
                throw new VirtualTourException(
                    "Failed to create virtual tour: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Create guided AI tour with narration
     */
    @Transactional
    @AuditOperation(operation = "CREATE_GUIDED_TOUR",
                   entity = "VirtualTour",
                   description = "AI-guided virtual tour with narration")
    public CompletableFuture<GuidedTourResponse> createGuidedTour(
            @ValidImageData GuidedTourRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create base virtual tour
                VirtualTourRequest baseRequest = createBaseTourRequest(request);
                VirtualTourResponse baseTour = createVirtualTour(baseRequest).get();

                // Add AI-guided features
                GuidedTourFeatures guidedFeatures = createGuidedFeatures(request);

                // Generate AI narration script
                NarrationScript narrationScript = tourNarrationService.generateDetailedNarration(
                    request.getPropertyDetails(), guidedFeatures, request.getLanguage());

                // Create interactive checkpoints
                List<TourCheckpoint> checkpoints = createTourCheckpoints(baseTour, narrationScript);

                // Save guided tour
                VirtualTour guidedTour = saveGuidedTourRecord(
                    request, baseTour, guidedFeatures, narrationScript, checkpoints);

                return GuidedTourResponse.builder()
                    .tourId(guidedTour.getId())
                    .baseTour(baseTour)
                    .narrationScript(narrationScript)
                    .checkpoints(checkpoints)
                    .guidedFeatures(guidedFeatures)
                    .estimatedViewTime(calculateEstimatedViewTime(checkpoints))
                    .generatedAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("Error creating guided tour", e);
                throw new GuidedTourException(
                    "Failed to create guided tour: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Create VR/AR compatible tour
     */
    @Transactional
    @AuditOperation(operation = "CREATE_VR_AR_TOUR",
                   entity = "VirtualTour",
                   description = "AI-powered VR/AR virtual tour")
    public CompletableFuture<VRARTourResponse> createVRARTour(
            @ValidImageData VRARTourRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create base tour
                VirtualTourRequest baseRequest = createBaseTourRequest(request);
                VirtualTourResponse baseTour = createVirtualTour(baseRequest).get();

                // Enhance for VR/AR
                VRAREnhancements enhancements = tour3DRenderingService.enhanceForVRAR(baseTour, request);

                // Generate device-specific formats
                Map<String, String> deviceFormats = generateDeviceFormats(enhancements, request);

                // Create AR overlays
                List<AROverlay> arOverlays = createAROverlays(request);

                // Save VR/AR tour
                VirtualTour vrArTour = saveVRARTourRecord(request, baseTour, enhancements, deviceFormats);

                return VRARTourResponse.builder()
                    .tourId(vrArTour.getId())
                    .baseTour(baseTour)
                    .enhancements(enhancements)
                    .deviceFormats(deviceFormats)
                    .arOverlays(arOverlays)
                    .vrCompatibility(checkVRCompatibility(request))
                    .arCompatibility(checkARCompatibility(request))
                    .generatedAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("Error creating VR/AR tour", e);
                throw new VRARTourException(
                    "Failed to create VR/AR tour: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Update existing tour with new features
     */
    @Transactional
    @AuditOperation(operation = "UPDATE_VIRTUAL_TOUR",
                   entity = "VirtualTour",
                   description = "Update virtual tour with new features")
    public CompletableFuture<VirtualTourUpdateResponse> updateVirtualTour(
            String tourId,
            VirtualTourUpdateRequest updateRequest) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                VirtualTour existingTour = repository.findById(tourId)
                    .orElseThrow(() -> new VirtualTourNotFoundException("Tour not found: " + tourId));

                // Apply updates
                VirtualTourResult updatedResult = applyTourUpdates(existingTour, updateRequest);

                // Save updates
                existingTour.setUpdatedAt(LocalDateTime.now());
                existingTour.setTourData(updatedResult.getTourDataJson());
                existingTour.setLastModifiedBy(updateRequest.getModifiedBy());
                repository.save(existingTour);

                return VirtualTourUpdateResponse.builder()
                    .tourId(tourId)
                    .updatedTour(updatedResult)
                    .updateAppliedAt(LocalDateTime.now())
                    .changesApplied(updateRequest.getChanges())
                    .build();

            } catch (Exception e) {
                log.error("Error updating virtual tour", e);
                throw new VirtualTourUpdateException(
                    "Failed to update tour: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get tour analytics and insights
     */
    @RequiresRole({"AGENT", "ADMIN", "MANAGER"})
    public TourAnalyticsResponse getTourAnalytics(
            String tourId,
            String timeRange) {

        log.info("Fetching tour analytics for tour ID: {}", tourId);

        VirtualTour tour = repository.findById(tourId)
            .orElseThrow(() -> new VirtualTourNotFoundException("Tour not found: " + tourId));

        return tourAnalyticsService.getTourAnalytics(tour, timeRange);
    }

    /**
     * Get tour history and performance
     */
    @RequiresRole({"AGENT", "ADMIN", "MANAGER"})
    public PaginationResponse<VirtualTourSummary> getTourHistory(
            String propertyId,
            PaginationRequest request) {

        log.info("Fetching virtual tour history for property: {}", propertyId);

        return repository.findByPropertyIdOrderByCreatedAtDesc(propertyId, Pageable.ofSize(
            request.getSize()).withPage(request.getPage()));
    }

    // Private helper methods

    private List<BufferedImage> validateAndPreprocessImages(VirtualTourRequest request) {
        try {
            // Validate image count and format
            if (request.getImages().size() < 5) {
                throw new InsufficientImagesException(
                    "Minimum 5 images required for virtual tour");
            }

            // Preprocess each image
            return request.getImages().stream()
                .map(imageData -> {
                    BufferedImage image = tour3DRenderingService.bytesToBufferedImage(imageData);
                    return tour3DRenderingService.preprocessFor3D(image);
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            throw new ImageValidationException("Invalid image data for virtual tour", e);
        }
    }

    private TourTemplate selectTourTemplate(String propertyType, String tourStyle) {
        String templateKey = propertyType.toUpperCase();
        TourTemplate baseTemplate = TOUR_TEMPLATES.getOrDefault(templateKey,
            TOUR_TEMPLATES.get("HOUSE"));

        // Apply style modifications
        return applyTourStyleModifications(baseTemplate, tourStyle);
    }

    private TourTemplate applyTourStyleModifications(TourTemplate template, String tourStyle) {
        switch (tourStyle.toUpperCase()) {
            case "CINEMATIC":
                return template.toBuilder()
                    .narrationStyle("DRAMATIC_MOVIE")
                    .interactiveFeatures(Arrays.asList("cinematic_angles", "smooth_transitions", "music_accompaniment"))
                    .build();
            case "MINIMAL":
                return template.toBuilder()
                    .narrationStyle("QUICK_CONCISE")
                    .tourDurationMinutes(template.getTourDurationMinutes() / 2)
                    .build();
            case "DETAILED":
                return template.toBuilder()
                    .narrationStyle("COMPREHENSIVE_EDUCATIONAL")
                    .tourDurationMinutes(template.getTourDurationMinutes() * 2)
                    .interactiveFeatures(Arrays.asList("detailed_measurements", "material_specs", "maintenance_info"))
                    .build();
            default:
                return template;
        }
    }

    private TourPath generateTourPath(Model3D model3D, TourTemplate template) {
        return tour3DRenderingService.generateOptimalPath(model3D, template);
    }

    private List<TourHotspot> createInteractiveHotspots(Model3D model3D, VirtualTourRequest request) {
        List<TourHotspot> hotspots = new ArrayList<>();

        // AI-detected points of interest
        hotspots.addAll(tour3DRenderingService.detectPointsOfInterest(model3D));

        // User-specified hotspots
        if (request.getCustomHotspots() != null) {
            hotspots.addAll(request.getCustomHotspots());
        }

        // Property features
        hotspots.addAll(createPropertyFeatureHotspots(request.getPropertyFeatures()));

        return hotspots;
    }

    private List<TourHotspot> createPropertyFeatureHotspots(List<String> propertyFeatures) {
        return propertyFeatures.stream()
            .map(feature -> TourHotspot.builder()
                .type("FEATURE")
                .title(feature.replace("_", " ").toUpperCase())
                .description("Highlight: " + feature)
                .interactive(true)
                .build())
            .collect(Collectors.toList());
    }

    private List<TourAlternative> generateTourAlternatives(Model3D model3D, List<String> alternativeViews) {
        List<TourAlternative> alternatives = new ArrayList<>();

        if (alternativeViews != null) {
            for (String view : alternativeViews) {
                TourAlternative alternative = tour3DRenderingService.createAlternativeView(model3D, view);
                alternatives.add(alternative);
            }
        }

        // Add standard alternatives
        alternatives.add(createNightViewAlternative(model3D));
        alternatives.add(createSeasonalAlternative(model3D, "SUMMER"));

        return alternatives;
    }

    private TourAlternative createNightViewAlternative(Model3D model3D) {
        return tour3DRenderingService.createLightingAlternative(model3D, "NIGHT");
    }

    private TourAlternative createSeasonalAlternative(Model3D model3D, String season) {
        return tour3DRenderingService.createSeasonalAlternative(model3D, season);
    }

    private TourMetrics calculateTourMetrics(VirtualTourResult tourResult, Model3D model3D) {
        return TourMetrics.builder()
            .tourDuration(tourResult.getDuration())
            .roomCount(tourResult.getRoomCount())
            .hotspotCount(tourResult.getHotspotCount())
            .renderingQuality(calculateRenderingQuality(tourResult))
            .fileSize(tourResult.getFileSize())
            .loadingTime(calculateLoadingTime(tourResult))
            .interactivityScore(calculateInteractivityScore(tourResult))
            .mobileCompatibility(calculateMobileCompatibility(tourResult))
            .build();
    }

    private VirtualTour saveTourRecord(
            VirtualTourRequest request,
            VirtualTourResult tourResult,
            Model3D model3D,
            TourPath tourPath,
            TourNarration narration,
            TourMetrics metrics) {

        VirtualTour tour = VirtualTour.builder()
            .propertyId(request.getPropertyId())
            .propertyType(request.getPropertyType())
            .tourFormat(request.getTourFormat())
            .quality(request.getQuality())
            .tourUrl(tourResult.getTourUrl())
            .embedCode(tourResult.getEmbedCode())
            .tourDataJson(tourResult.getTourDataJson())
            .model3DDataJson(model3D.toJson())
            .tourPathJson(tourPath.toJson())
            .narrationJson(narration.toJson())
            .duration(metrics.getTourDuration())
            .roomCount(metrics.getRoomCount())
            .hotspotCount(metrics.getHotspotCount())
            .viewCount(0L)
            .shareCount(0L)
            .createdAt(LocalDateTime.now())
            .build();

        return repository.save(tour);
    }

    private List<String> generateTourRecommendations(TourMetrics metrics) {
        List<String> recommendations = new ArrayList<>();

        if (metrics.getRenderingQuality() < 0.8) {
            recommendations.add("Consider increasing quality settings for better user experience");
        }

        if (metrics.getLoadingTime() > 5000) {
            recommendations.add("Tour loading time is high - consider optimization");
        }

        if (metrics.getMobileCompatibility() < 0.9) {
            recommendations.add("Optimize for better mobile device compatibility");
        }

        if (metrics.getInteractivityScore() < 0.7) {
            recommendations.add("Add more interactive elements to engage viewers");
        }

        return recommendations;
    }

    // Additional helper methods for specialized tours

    private VirtualTourRequest createBaseTourRequest(Object request) {
        // Convert specialized request to base request
        // Implementation depends on request type
        return VirtualTourRequest.builder().build(); // Simplified
    }

    private GuidedTourFeatures createGuidedFeatures(GuidedTourRequest request) {
        return GuidedTourFeatures.builder()
            .aiGuidance(request.isAiGuidance())
            .interactiveQuestions(request.isInteractiveQuestions())
            .personalizedContent(request.isPersonalizedContent())
            .voiceControl(request.isVoiceControl())
            .build();
    }

    private List<TourCheckpoint> createTourCheckpoints(VirtualTourResponse baseTour, NarrationScript narration) {
        // Create checkpoints based on narration and tour structure
        return narration.getSegments().stream()
            .map(segment -> TourCheckpoint.builder()
                .segmentId(segment.getId())
                .timestamp(segment.getTimestamp())
                .title(segment.getTitle())
                .description(segment.getDescription())
                .interactiveElements(segment.getInteractiveElements())
                .build())
            .collect(Collectors.toList());
    }

    private VirtualTour saveGuidedTourRecord(
            GuidedTourRequest request,
            VirtualTourResponse baseTour,
            GuidedTourFeatures guidedFeatures,
            NarrationScript narrationScript,
            List<TourCheckpoint> checkpoints) {

        VirtualTour guidedTour = VirtualTour.builder()
            .propertyId(request.getPropertyId())
            .tourType("GUIDED_AI")
            .baseTourId(baseTour.getTourId())
            .guidedFeaturesJson(guidedFeatures.toJson())
            .narrationScriptJson(narrationScript.toJson())
            .checkpointsJson(checkpoints.stream()
                .map(TourCheckpoint::toJson)
                .collect(Collectors.joining(",")))
            .createdAt(LocalDateTime.now())
            .build();

        return repository.save(guidedTour);
    }

    private long calculateEstimatedViewTime(List<TourCheckpoint> checkpoints) {
        // Calculate total viewing time based on checkpoints
        return checkpoints.stream()
            .mapToLong(cp -> cp.getEstimatedDuration() != null ? cp.getEstimatedDuration() : 30)
            .sum();
    }

    // VR/AR specific methods
    private VRAREnhancements applyVRAREnhancements(VirtualTourResponse baseTour, VRARTourRequest request) {
        return tour3DRenderingService.enhanceForVRAR(baseTour, request);
    }

    private Map<String, String> generateDeviceFormats(VRAREnhancements enhancements, VRARTourRequest request) {
        Map<String, String> formats = new HashMap<>();

        formats.put("OCULUS_QUEST", enhancements.getOculusFormat());
        formats.put("HTC_VIVE", enhancements.getViveFormat());
        formats.put("APPLE_VISION_PRO", enhancements.getVisionProFormat());
        formats.put("GOOGLE_AR", enhancements.getGoogleArFormat());
        formats.put("UNITY_WEBGL", enhancements.getUnityWebglFormat());

        return formats;
    }

    private List<AROverlay> createAROverlays(VRARTourRequest request) {
        return request.getArOverlays() != null ? request.getArOverlays() :
               Arrays.asList(
                   AROverlay.builder()
                       .type("DIMENSIONS")
                       .title("Room Dimensions")
                       .build(),
                   AROverlay.builder()
                       .type("FURNITURE")
                       .title("Furniture Options")
                       .build()
               );
    }

    private VirtualTour saveVRARTourRecord(
            VRARTourRequest request,
            VirtualTourResponse baseTour,
            VRAREnhancements enhancements,
            Map<String, String> deviceFormats) {

        VirtualTour vrArTour = VirtualTour.builder()
            .propertyId(request.getPropertyId())
            .tourType("VR_AR")
            .baseTourId(baseTour.getTourId())
            .vrArEnhancementsJson(enhancements.toJson())
            .deviceFormatsJson(deviceFormats.toString())
            .createdAt(LocalDateTime.now())
            .build();

        return repository.save(vrArTour);
    }

    private boolean checkVRCompatibility(VRARTourRequest request) {
        return request.getTargetDevices().stream()
            .anyMatch(device -> device.toUpperCase().contains("VR"));
    }

    private boolean checkARCompatibility(VRARTourRequest request) {
        return request.getTargetDevices().stream()
            .anyMatch(device -> device.toUpperCase().contains("AR"));
    }

    // Update methods
    private VirtualTourResult applyTourUpdates(VirtualTour existingTour, VirtualTourUpdateRequest updateRequest) {
        // Apply requested updates to existing tour
        return tour3DRenderingService.updateTour(existingTour, updateRequest);
    }

    // Metric calculation methods
    private Double calculateRenderingQuality(VirtualTourResult result) {
        return tour3DRenderingService.calculateQuality(result);
    }

    private Long calculateLoadingTime(VirtualTourResult result) {
        return tour3DRenderingService.calculateLoadingTime(result);
    }

    private Double calculateInteractivityScore(VirtualTourResult result) {
        return tour3DRenderingService.calculateInteractivity(result);
    }

    private Double calculateMobileCompatibility(VirtualTourResult result) {
        return tour3DRenderingService.calculateMobileCompatibility(result);
    }
}