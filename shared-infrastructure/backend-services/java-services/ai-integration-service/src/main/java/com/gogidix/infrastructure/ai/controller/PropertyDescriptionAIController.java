package com.gogidix.infrastructure.ai.controller;

import com.gogidix.platform.common.core.dto.BaseResponse;
import com.gogidix.platform.common.core.dto.PaginationRequest;
import com.gogidix.platform.common.core.dto.PaginationResponse;
import com.gogidix.platform.common.security.annotation.RequiresRole;
import com.gogidix.platform.common.audit.annotation.AuditOperation;
import com.gogidix.platform.common.monitoring.annotation.Timed;
import com.gogidix.infrastructure.ai.dto.*;
import com.gogidix.infrastructure.ai.service.PropertyDescriptionAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Property Description AI Service
 *
 * CATEGORY 1: Property Management Automation
 * Service: Property Description Generation (1/48)
 *
 * API endpoints for AI-powered property description generation, optimization, and management
 */
@RestController
@RequestMapping("/api/v1/ai/property/description")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Property Description AI", description = "AI-powered property description generation and optimization")
@Timed(name = "property-description-controller", description = "Property Description AI Controller Metrics")
public class PropertyDescriptionAIController {

    private final PropertyDescriptionAIService propertyDescriptionService;

    /**
     * Generate AI-powered property description
     */
    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Generate AI property description",
        description = "Generate a compelling property description using AI with SEO optimization and quality analysis"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully generated property description",
            content = @Content(schema = @Schema(implementation = PropertyDescriptionResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @RequiresRole({"AGENT", "ADMIN", "MANAGER", "OWNER"})
    @AuditOperation(
        operation = "GENERATE_PROPERTY_DESCRIPTION",
        entity = "PropertyDescription",
        description = "AI-generated property description via REST API"
    )
    public CompletableFuture<ResponseEntity<BaseResponse<PropertyDescriptionResponse>>> generateDescription(
            @Valid @RequestBody PropertyDescriptionRequest request) {

        log.info("REST API: Generate property description for property ID: {}", request.getPropertyId());

        return propertyDescriptionService.generateDescription(request)
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success(response)))
            .exceptionally(throwable -> {
                log.error("Error generating property description", throwable);
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("Failed to generate property description: " + throwable.getMessage()));
            });
    }

    /**
     * Batch generate descriptions for multiple properties
     */
    @PostMapping(value = "/batch-generate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Batch generate property descriptions",
        description = "Generate descriptions for multiple properties in a single request"
    )
    @RequiresRole({"ADMIN", "MANAGER"})
    @AuditOperation(
        operation = "BATCH_GENERATE_DESCRIPTIONS",
        entity = "PropertyDescription",
        description = "Batch AI-generated property descriptions via REST API"
    )
    public CompletableFuture<ResponseEntity<BaseResponse<BatchDescriptionResponse>>> batchGenerateDescriptions(
            @Valid @RequestBody BatchDescriptionRequest request) {

        log.info("REST API: Batch generate descriptions for {} properties", request.getProperties().size());

        return propertyDescriptionService.generateBatchDescriptions(request.getProperties())
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success(response)))
            .exceptionally(throwable -> {
                log.error("Error batch generating descriptions", throwable);
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("Failed to batch generate descriptions: " + throwable.getMessage()));
            });
    }

    /**
     * Get description analytics and performance metrics
     */
    @GetMapping(value = "/{descriptionId}/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get description analytics",
        description = "Retrieve comprehensive analytics and performance metrics for a property description"
    )
    @RequiresRole({"AGENT", "ADMIN", "MANAGER"})
    public ResponseEntity<BaseResponse<DescriptionAnalyticsResponse>> getDescriptionAnalytics(
            @Parameter(description = "Description ID", required = true)
            @PathVariable String descriptionId,
            @Parameter(description = "Time range for analytics (e.g., 7d, 30d, 90d)")
            @RequestParam(defaultValue = "30d") String timeRange) {

        log.info("REST API: Get description analytics for ID: {}", descriptionId);

        try {
            DescriptionAnalyticsResponse analytics = propertyDescriptionService
                .getDescriptionAnalytics(descriptionId, timeRange);
            return ResponseEntity.ok(BaseResponse.success(analytics));
        } catch (Exception e) {
            log.error("Error getting description analytics", e);
            return ResponseEntity.badRequest()
                .body(BaseResponse.error("Failed to get description analytics: " + e.getMessage()));
        }
    }

    /**
     * Update and optimize existing description
     */
    @PutMapping(value = "/{descriptionId}/optimize", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Optimize property description",
        description = "AI-optimize an existing property description based on specified goals"
    )
    @RequiresRole({"AGENT", "ADMIN", "MANAGER", "OWNER"})
    @AuditOperation(
        operation = "OPTIMIZE_DESCRIPTION",
        entity = "PropertyDescription",
        description = "AI-optimized property description via REST API"
    )
    public CompletableFuture<ResponseEntity<BaseResponse<PropertyDescriptionResponse>>> optimizeDescription(
            @Parameter(description = "Description ID", required = true)
            @PathVariable String descriptionId,
            @Valid @RequestBody DescriptionOptimizationRequest optimizationRequest) {

        log.info("REST API: Optimize description for ID: {}", descriptionId);

        return propertyDescriptionService.optimizeDescription(descriptionId, optimizationRequest)
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success(response)))
            .exceptionally(throwable -> {
                log.error("Error optimizing description", throwable);
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("Failed to optimize description: " + throwable.getMessage()));
            });
    }

    /**
     * Get saved descriptions with pagination and filtering
     */
    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Search property descriptions",
        description = "Search and retrieve property descriptions with pagination and advanced filtering"
    )
    @RequiresRole({"AGENT", "ADMIN", "MANAGER"})
    public ResponseEntity<BaseResponse<PaginationResponse<PropertyDescriptionSummary>>> searchDescriptions(
            @Valid @RequestBody DescriptionSearchRequest searchRequest) {

        log.info("REST API: Search property descriptions with filters");

        try {
            PaginationResponse<PropertyDescriptionSummary> descriptions = propertyDescriptionService
                .getDescriptions(searchRequest.getPagination(), searchRequest.getFilter());
            return ResponseEntity.ok(BaseResponse.success(descriptions));
        } catch (Exception e) {
            log.error("Error searching descriptions", e);
            return ResponseEntity.badRequest()
                .body(BaseResponse.error("Failed to search descriptions: " + e.getMessage()));
        }
    }

    /**
     * Get description by ID
     */
    @GetMapping(value = "/{descriptionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get property description",
        description = "Retrieve a specific property description by ID"
    )
    @RequiresRole({"AGENT", "ADMIN", "MANAGER", "OWNER"})
    public ResponseEntity<BaseResponse<PropertyDescriptionDetails>> getDescription(
            @Parameter(description = "Description ID", required = true)
            @PathVariable String descriptionId) {

        log.info("REST API: Get description by ID: {}", descriptionId);

        try {
            // This would be implemented in the service layer
            PropertyDescriptionDetails description = propertyDescriptionService
                .getDescriptionById(descriptionId);
            return ResponseEntity.ok(BaseResponse.success(description));
        } catch (Exception e) {
            log.error("Error getting description", e);
            return ResponseEntity.badRequest()
                .body(BaseResponse.error("Failed to get description: " + e.getMessage()));
        }
    }

    /**
     * Delete a property description
     */
    @DeleteMapping(value = "/{descriptionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Delete property description",
        description = "Soft delete a property description by ID"
    )
    @RequiresRole({"ADMIN", "MANAGER", "OWNER"})
    @AuditOperation(
        operation = "DELETE_DESCRIPTION",
        entity = "PropertyDescription",
        description = "Deleted property description via REST API"
    )
    public ResponseEntity<BaseResponse<Void>> deleteDescription(
            @Parameter(description = "Description ID", required = true)
            @PathVariable String descriptionId) {

        log.info("REST API: Delete description ID: {}", descriptionId);

        try {
            propertyDescriptionService.deleteDescription(descriptionId);
            return ResponseEntity.ok(BaseResponse.success(null));
        } catch (Exception e) {
            log.error("Error deleting description", e);
            return ResponseEntity.badRequest()
                .body(BaseResponse.error("Failed to delete description: " + e.getMessage()));
        }
    }

    /**
     * Get template descriptions
     */
    @GetMapping(value = "/templates", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get description templates",
        description = "Retrieve property description templates for different property types and categories"
    )
    public ResponseEntity<BaseResponse<TemplateDescriptionsResponse>> getTemplates(
            @Parameter(description = "Property type filter")
            @RequestParam(required = false) String propertyType,
            @Parameter(description = "Template category filter")
            @RequestParam(required = false) String category) {

        log.info("REST API: Get description templates");

        try {
            TemplateDescriptionsResponse templates = propertyDescriptionService
                .getTemplates(propertyType, category);
            return ResponseEntity.ok(BaseResponse.success(templates));
        } catch (Exception e) {
            log.error("Error getting templates", e);
            return ResponseEntity.badRequest()
                .body(BaseResponse.error("Failed to get templates: " + e.getMessage()));
        }
    }

    /**
     * Generate description variations
     */
    @PostMapping(value = "/{descriptionId}/variations", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Generate description variations",
        description = "Generate AI-powered variations of an existing property description"
    )
    @RequiresRole({"AGENT", "ADMIN", "MANAGER", "OWNER"})
    @AuditOperation(
        operation = "GENERATE_VARIATIONS",
        entity = "PropertyDescription",
        description = "Generated description variations via REST API"
    )
    public CompletableFuture<ResponseEntity<BaseResponse<DescriptionVariationsResponse>>> generateVariations(
            @Parameter(description = "Description ID", required = true)
            @PathVariable String descriptionId,
            @Valid @RequestBody VariationRequest variationRequest) {

        log.info("REST API: Generate variations for description ID: {}", descriptionId);

        return propertyDescriptionService.generateVariations(descriptionId, variationRequest)
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success(response)))
            .exceptionally(throwable -> {
                log.error("Error generating variations", throwable);
                return ResponseEntity.badRequest()
                    .body(BaseResponse.error("Failed to generate variations: " + throwable.getMessage()));
            });
    }

    /**
     * Get service health and metrics
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Service health check",
        description = "Get service health status and performance metrics"
    )
    public ResponseEntity<BaseResponse<ServiceHealthResponse>> getHealth() {
        log.info("REST API: Health check for Property Description AI Service");

        try {
            ServiceHealthResponse health = propertyDescriptionService.getServiceHealth();
            return ResponseEntity.ok(BaseResponse.success(health));
        } catch (Exception e) {
            log.error("Error getting service health", e);
            return ResponseEntity.badRequest()
                .body(BaseResponse.error("Failed to get service health: " + e.getMessage()));
        }
    }

    /**
     * Get AI model information
     */
    @GetMapping(value = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get AI model information",
        description = "Get information about available AI models and their capabilities"
    )
    public ResponseEntity<BaseResponse<ModelInfoResponse>> getModelInfo() {
        log.info("REST API: Get AI model information");

        try {
            ModelInfoResponse modelInfo = propertyDescriptionService.getModelInfo();
            return ResponseEntity.ok(BaseResponse.success(modelInfo));
        } catch (Exception e) {
            log.error("Error getting model info", e);
            return ResponseEntity.badRequest()
                .body(BaseResponse.error("Failed to get model info: " + e.getMessage()));
        }
    }
}