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
 * AI-powered Property Discovery Service
 *
 * This service helps users discover properties they might not find through traditional search methods
 * using AI-driven discovery algorithms, pattern recognition, and hidden gem identification.
 *
 * Features:
 * - Hidden property discovery algorithms
 * - Serendipitous property recommendations
 * - Trend-based property discovery
 * - Off-market property identification
 * - Emerging neighborhood discovery
 * - Price anomaly detection
 * - Unique property feature discovery
 * - Up-and-coming area identification
 * - Investment opportunity discovery
 * - Lifestyle-based property matching
 */
@RestController
@RequestMapping("/ai/v1/property-discovery")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Property Discovery AI Service", description = "AI-powered property discovery and hidden gem identification")
public class PropertyDiscoveryAIService {

    private final CacheService cacheService;
    private final MetricsService metricsService;
    private final AuditService auditService;
    private final SecurityService securityService;
    private final ValidationService validationService;

    // Property Discovery Models
    private final HiddenGemDiscoveryEngine hiddenGemEngine;
    private final SerendipityEngine serendipityEngine;
    private final TrendDiscoveryEngine trendEngine;
    private final OffMarketDiscoveryEngine offMarketEngine;
    private final NeighborhoodDiscoveryEngine neighborhoodEngine;
    private final PriceAnomalyDetector priceAnomalyDetector;
    private final UniqueFeatureDiscoveryEngine uniqueFeatureEngine;
    private final EmergingAreaDetector emergingAreaDetector;
    private final InvestmentOpportunityEngine investmentEngine;
    private final LifestyleMatchingEngine lifestyleEngine;

    /**
     * Discover hidden gem properties
     */
    @PostMapping("/hidden-gems/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Discover hidden gem properties",
        description = "Identifies undervalued or overlooked properties with high potential"
    )
    public CompletableFuture<ResponseEntity<HiddenGemDiscoveryResult>> discoverHiddenGems(
            @PathVariable String userId,
            @Valid @RequestBody HiddenGemDiscoveryRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.property.discovery.hidden-gems");

            try {
                log.info("Discovering hidden gem properties for user: {}", userId);

                // Validate request
                validationService.validate(request);
                securityService.validateUserAccess(userId);

                // Discover hidden gems
                HiddenGemDiscoveryResult result = hiddenGemEngine.discoverHiddenGems(userId, request);

                // Cache results
                cacheService.set("hidden-gems:" + userId + ":" + request.getDiscoveryType(),
                               result, java.time.Duration.ofHours(2));

                // Record metrics
                metricsService.recordCounter("ai.property.discovery.hidden-gems.success");
                metricsService.recordTimer("ai.property.discovery.hidden-gems", stopwatch);

                // Audit
                auditService.audit(
                    "HIDDEN_GEMS_DISCOVERED",
                    "userId=" + userId + ",type=" + request.getDiscoveryType() + ",count=" + result.getProperties().size(),
                    "ai-property-discovery",
                    "success"
                );

                log.info("Successfully discovered {} hidden gems for user: {}", result.getProperties().size(), userId);
                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.hidden-gems.error");
                log.error("Error discovering hidden gems for user: {}", userId, e);
                throw new RuntimeException("Hidden gem discovery failed", e);
            }
        });
    }

    /**
     * Serendipitous property discovery
     */
    @PostMapping("/serendipity/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Serendipitous property discovery",
        description = "Provides unexpected property discoveries based on AI serendipity algorithms"
    )
    public CompletableFuture<ResponseEntity<SerendipityDiscoveryResult>> discoverSerendipitously(
            @PathVariable String userId,
            @Valid @RequestBody SerendipityDiscoveryRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating serendipitous discoveries for user: {}", userId);

                SerendipityDiscoveryResult result = serendipityEngine.discoverSerendipitously(userId, request);

                metricsService.recordCounter("ai.property.discovery.serendipity.success");
                auditService.audit(
                    "SERENDIPITOUS_DISCOVERIES",
                    "userId=" + userId + ",count=" + result.getProperties().size(),
                    "ai-property-discovery",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.serendipity.error");
                log.error("Error generating serendipitous discoveries for user: {}", userId, e);
                throw new RuntimeException("Serendipity discovery failed", e);
            }
        });
    }

    /**
     * Trend-based property discovery
     */
    @PostMapping("/trending/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Trending property discovery",
        description = "Discovers properties based on emerging market trends and patterns"
    )
    public CompletableFuture<ResponseEntity<TrendDiscoveryResult>> discoverTrendingProperties(
            @PathVariable String userId,
            @Valid @RequestBody TrendDiscoveryRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Discovering trending properties for user: {}", userId);

                TrendDiscoveryResult result = trendEngine.discoverTrendingProperties(userId, request);

                metricsService.recordCounter("ai.property.discovery.trending.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.trending.error");
                log.error("Error discovering trending properties for user: {}", userId, e);
                throw new RuntimeException("Trend discovery failed", e);
            }
        });
    }

    /**
     * Off-market property discovery
     */
    @PostMapping("/off-market/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Off-market property discovery",
        description = "Identifies off-market properties and pocket listings"
    )
    public CompletableFuture<ResponseEntity<OffMarketDiscoveryResult>> discoverOffMarketProperties(
            @PathVariable String userId,
            @Valid @RequestBody OffMarketDiscoveryRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.property.discovery.off-market");

            try {
                log.info("Discovering off-market properties for user: {}", userId);

                OffMarketDiscoveryResult result = offMarketEngine.discoverOffMarketProperties(userId, request);

                metricsService.recordCounter("ai.property.discovery.off-market.success");
                metricsService.recordTimer("ai.property.discovery.off-market", stopwatch);

                auditService.audit(
                    "OFF_MARKET_DISCOVERIES",
                    "userId=" + userId + ",count=" + result.getProperties().size(),
                    "ai-property-discovery",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.off-market.error");
                log.error("Error discovering off-market properties for user: {}", userId, e);
                throw new RuntimeException("Off-market discovery failed", e);
            }
        });
    }

    /**
     * Emerging neighborhood discovery
     */
    @PostMapping("/emerging-areas/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Emerging neighborhood discovery",
        description = "Identifies up-and-coming neighborhoods with growth potential"
    )
    public CompletableFuture<ResponseEntity<EmergingAreaDiscoveryResult>> discoverEmergingAreas(
            @PathVariable String userId,
            @Valid @RequestBody EmergingAreaDiscoveryRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Discovering emerging areas for user: {}", userId);

                EmergingAreaDiscoveryResult result = emergingAreaDetector.discoverEmergingAreas(userId, request);

                metricsService.recordCounter("ai.property.discovery.emerging-areas.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.emerging-areas.error");
                log.error("Error discovering emerging areas for user: {}", userId, e);
                throw new RuntimeException("Emerging area discovery failed", e);
            }
        });
    }

    /**
     * Price anomaly discovery
     */
    @PostMapping("/price-anomalies/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Price anomaly discovery",
        description = "Identifies properties with pricing anomalies and opportunities"
    )
    public CompletableFuture<ResponseEntity<PriceAnomalyDiscoveryResult>> discoverPriceAnomalies(
            @PathVariable String userId,
            @Valid @RequestBody PriceAnomalyDiscoveryRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Discovering price anomalies for user: {}", userId);

                PriceAnomalyDiscoveryResult result = priceAnomalyDetector.discoverPriceAnomalies(userId, request);

                metricsService.recordCounter("ai.property.discovery.price-anomalies.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.price-anomalies.error");
                log.error("Error discovering price anomalies for user: {}", userId, e);
                throw new RuntimeException("Price anomaly discovery failed", e);
            }
        });
    }

    /**
     * Unique feature discovery
     */
    @PostMapping("/unique-features/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Unique feature discovery",
        description = "Discovers properties with unique and distinctive features"
    )
    public CompletableFuture<ResponseEntity<UniqueFeatureDiscoveryResult>> discoverUniqueFeatures(
            @PathVariable String userId,
            @Valid @RequestBody UniqueFeatureDiscoveryRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Discovering properties with unique features for user: {}", userId);

                UniqueFeatureDiscoveryResult result = uniqueFeatureEngine.discoverUniqueFeatures(userId, request);

                metricsService.recordCounter("ai.property.discovery.unique-features.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.unique-features.error");
                log.error("Error discovering unique features for user: {}", userId, e);
                throw new RuntimeException("Unique feature discovery failed", e);
            }
        });
    }

    /**
     * Investment opportunity discovery
     */
    @PostMapping("/investment-opportunities/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_INVESTOR')")
    @Operation(
        summary = "Investment opportunity discovery",
        description = "Identifies high-potential investment opportunities"
    )
    public CompletableFuture<ResponseEntity<InvestmentOpportunityResult>> discoverInvestmentOpportunities(
            @PathVariable String userId,
            @Valid @RequestBody InvestmentOpportunityRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.property.discovery.investment");

            try {
                log.info("Discovering investment opportunities for user: {}", userId);

                InvestmentOpportunityResult result = investmentEngine.discoverInvestmentOpportunities(userId, request);

                metricsService.recordCounter("ai.property.discovery.investment.success");
                metricsService.recordTimer("ai.property.discovery.investment", stopwatch);

                auditService.audit(
                    "INVESTMENT_OPPORTUNITIES_DISCOVERED",
                    "userId=" + userId + ",count=" + result.getOpportunities().size(),
                    "ai-property-discovery",
                    "success"
                );

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.investment.error");
                log.error("Error discovering investment opportunities for user: {}", userId, e);
                throw new RuntimeException("Investment opportunity discovery failed", e);
            }
        });
    }

    /**
     * Lifestyle-based property discovery
     */
    @PostMapping("/lifestyle/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Lifestyle-based property discovery",
        description = "Discovers properties matching specific lifestyle preferences"
    )
    public CompletableFuture<ResponseEntity<LifestyleDiscoveryResult>> discoverLifestyleProperties(
            @PathVariable String userId,
            @Valid @RequestBody LifestyleDiscoveryRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Discovering lifestyle properties for user: {}", userId);

                LifestyleDiscoveryResult result = lifestyleEngine.discoverLifestyleProperties(userId, request);

                metricsService.recordCounter("ai.property.discovery.lifestyle.success");

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.lifestyle.error");
                log.error("Error discovering lifestyle properties for user: {}", userId, e);
                throw new RuntimeException("Lifestyle discovery failed", e);
            }
        });
    }

    /**
     * Comprehensive property discovery
     */
    @PostMapping("/comprehensive/{userId}")
    @PreAuthorize("hasRole('ROLE_AI_USER') or hasRole('ROLE_PROPERTY_SEEKER')")
    @Operation(
        summary = "Comprehensive property discovery",
        description = "Provides comprehensive discovery using all available algorithms"
    )
    public CompletableFuture<ResponseEntity<ComprehensiveDiscoveryResult>> comprehensiveDiscovery(
            @PathVariable String userId,
            @Valid @RequestBody ComprehensiveDiscoveryRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var stopwatch = metricsService.startTimer("ai.property.discovery.comprehensive");

            try {
                log.info("Performing comprehensive property discovery for user: {}", userId);

                // Validate request
                validationService.validate(request);
                securityService.validateUserAccess(userId);

                // Perform comprehensive discovery
                ComprehensiveDiscoveryResult result = performComprehensiveDiscovery(userId, request);

                // Cache results
                cacheService.set("comprehensive-discovery:" + userId,
                               result, java.time.Duration.ofHours(1));

                // Record metrics
                metricsService.recordCounter("ai.property.discovery.comprehensive.success");
                metricsService.recordTimer("ai.property.discovery.comprehensive", stopwatch);

                // Audit
                auditService.audit(
                    "COMPREHENSIVE_DISCOVERY",
                    "userId=" + userId + ",totalProperties=" + result.getTotalPropertiesDiscovered(),
                    "ai-property-discovery",
                    "success"
                );

                log.info("Successfully completed comprehensive discovery for user: {}, found {} properties",
                        userId, result.getTotalPropertiesDiscovered());
                return ResponseEntity.ok(result);

            } catch (Exception e) {
                metricsService.recordCounter("ai.property.discovery.comprehensive.error");
                log.error("Error performing comprehensive discovery for user: {}", userId, e);
                throw new RuntimeException("Comprehensive discovery failed", e);
            }
        });
    }

    // Helper Methods
    private ComprehensiveDiscoveryResult performComprehensiveDiscovery(String userId, ComprehensiveDiscoveryRequest request) {
        ComprehensiveDiscoveryResult result = new ComprehensiveDiscoveryResult();
        result.setUserId(userId);
        result.setDiscoveryDate(LocalDateTime.now());
        result.setGeneratedBy("AI Property Discovery Service");

        // Hidden gems discovery
        HiddenGemDiscoveryRequest hiddenGemRequest = new HiddenGemDiscoveryRequest();
        hiddenGemRequest.setDiscoveryType("comprehensive");
        HiddenGemDiscoveryResult hiddenGems = hiddenGemEngine.discoverHiddenGems(userId, hiddenGemRequest);
        result.setHiddenGems(hiddenGems.getProperties());

        // Serendipitous discovery
        SerendipityDiscoveryRequest serendipityRequest = new SerendipityDiscoveryRequest();
        SerendipityDiscoveryResult serendipity = serendipityEngine.discoverSerendipitously(userId, serendipityRequest);
        result.setSerendipitousProperties(serendipity.getProperties());

        // Trending properties
        TrendDiscoveryRequest trendRequest = new TrendDiscoveryRequest();
        TrendDiscoveryResult trending = trendEngine.discoverTrendingProperties(userId, trendRequest);
        result.setTrendingProperties(trending.getProperties());

        // Off-market properties
        OffMarketDiscoveryRequest offMarketRequest = new OffMarketDiscoveryRequest();
        OffMarketDiscoveryResult offMarket = offMarketEngine.discoverOffMarketProperties(userId, offMarketRequest);
        result.setOffMarketProperties(offMarket.getProperties());

        // Calculate total discovered properties
        result.setTotalPropertiesDiscovered(
            result.getHiddenGems().size() +
            result.getSerendipitousProperties().size() +
            result.getTrendingProperties().size() +
            result.getOffMarketProperties().size()
        );

        // Generate discovery insights
        result.setDiscoveryInsights(generateDiscoveryInsights(result));

        return result;
    }

    private List<String> generateDiscoveryInsights(ComprehensiveDiscoveryResult result) {
        return List.of(
            "Hidden gems found: " + result.getHiddenGems().size() + " undervalued properties",
            "Serendipitous discoveries: " + result.getSerendipitousProperties().size() + " unexpected matches",
            "Trending properties: " + result.getTrendingProperties().size() + " in emerging areas",
            "Off-market opportunities: " + result.getOffMarketProperties().size() + " exclusive listings",
            "Discovery diversity score: " + calculateDiscoveryDiversity(result)
        );
    }

    private double calculateDiscoveryDiversity(ComprehensiveDiscoveryResult result) {
        int totalTypes = 0;
        if (!result.getHiddenGems().isEmpty()) totalTypes++;
        if (!result.getSerendipitousProperties().isEmpty()) totalTypes++;
        if (!result.getTrendingProperties().isEmpty()) totalTypes++;
        if (!result.getOffMarketProperties().isEmpty()) totalTypes++;

        return Math.round((double) totalTypes / 4.0 * 100.0) / 100.0;
    }
}

// Data Transfer Objects and Models

class HiddenGemDiscoveryRequest {
    private String discoveryType; // value, potential, overlooked
    private List<String> preferredAreas;
    private double maxBudget;
    private String investmentHorizon;

    // Getters and setters
    public String getDiscoveryType() { return discoveryType; }
    public void setDiscoveryType(String discoveryType) { this.discoveryType = discoveryType; }
    public List<String> getPreferredAreas() { return preferredAreas; }
    public void setPreferredAreas(List<String> preferredAreas) { this.preferredAreas = preferredAreas; }
    public double getMaxBudget() { return maxBudget; }
    public void setMaxBudget(double maxBudget) { this.maxBudget = maxBudget; }
    public String getInvestmentHorizon() { return investmentHorizon; }
    public void setInvestmentHorizon(String investmentHorizon) { this.investmentHorizon = investmentHorizon; }
}

class HiddenGemDiscoveryResult {
    private List<DiscoveredProperty> properties;
    private List<String> gemReasons;
    private double averageUndervaluationScore;

    // Getters and setters
    public List<DiscoveredProperty> getProperties() { return properties; }
    public void setProperties(List<DiscoveredProperty> properties) { this.properties = properties; }
    public List<String> getGemReasons() { return gemReasons; }
    public void setGemReasons(List<String> gemReasons) { this.gemReasons = gemReasons; }
    public double getAverageUndervaluationScore() { return averageUndervaluationScore; }
    public void setAverageUndervaluationScore(double averageUndervaluationScore) { this.averageUndervaluationScore = averageUndervaluationScore; }
}

class SerendipityDiscoveryRequest {
    private String serendipityLevel; // low, medium, high
    private List<String> userInterests;
    private boolean includeUnexpectedMatches = true;

    // Getters and setters
    public String getSerendipityLevel() { return serendipityLevel; }
    public void setSerendipityLevel(String serendipityLevel) { this.serendipityLevel = serendipityLevel; }
    public List<String> getUserInterests() { return userInterests; }
    public void setUserInterests(List<String> userInterests) { this.userInterests = userInterests; }
    public boolean isIncludeUnexpectedMatches() { return includeUnexpectedMatches; }
    public void setIncludeUnexpectedMatches(boolean includeUnexpectedMatches) { this.includeUnexpectedMatches = includeUnexpectedMatches; }
}

class SerendipityDiscoveryResult {
    private List<DiscoveredProperty> properties;
    private List<String> serendipityFactors;
    private double surpriseScore;

    // Getters and setters
    public List<DiscoveredProperty> getProperties() { return properties; }
    public void setProperties(List<DiscoveredProperty> properties) { this.properties = properties; }
    public List<String> getSerendipityFactors() { return serendipityFactors; }
    public void setSerendipityFactors(List<String> serendipityFactors) { this.serendipityFactors = serendipityFactors; }
    public double getSurpriseScore() { return surpriseScore; }
    public void setSurpriseScore(double surpriseScore) { this.surpriseScore = surpriseScore; }
}

class TrendDiscoveryRequest {
    private String trendType; // market, neighborhood, feature
    private List<String> areasOfInterest;
    private String timeFrame = "6months";

    // Getters and setters
    public String getTrendType() { return trendType; }
    public void setTrendType(String trendType) { this.trendType = trendType; }
    public List<String> getAreasOfInterest() { return areasOfInterest; }
    public void setAreasOfInterest(List<String> areasOfInterest) { this.areasOfInterest = areasOfInterest; }
    public String getTimeFrame() { return timeFrame; }
    public void setTimeFrame(String timeFrame) { this.timeFrame = timeFrame; }
}

class TrendDiscoveryResult {
    private List<DiscoveredProperty> properties;
    private List<String> identifiedTrends;
    private Map<String, Double> trendScores;

    // Getters and setters
    public List<DiscoveredProperty> getProperties() { return properties; }
    public void setProperties(List<DiscoveredProperty> properties) { this.properties = properties; }
    public List<String> getIdentifiedTrends() { return identifiedTrends; }
    public void setIdentifiedTrends(List<String> identifiedTrends) { this.identifiedTrends = identifiedTrends; }
    public Map<String, Double> getTrendScores() { return trendScores; }
    public void setTrendScores(Map<String, Double> trendScores) { this.trendScores = trendScores; }
}

class OffMarketDiscoveryRequest {
    private String propertyType;
    private List<String> targetNeighborhoods;
    private boolean includePocketListings = true;

    // Getters and setters
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public List<String> getTargetNeighborhoods() { return targetNeighborhoods; }
    public void setTargetNeighborhoods(List<String> targetNeighborhoods) { this.targetNeighborhoods = targetNeighborhoods; }
    public boolean isIncludePocketListings() { return includePocketListings; }
    public void setIncludePocketListings(boolean includePocketListings) { this.includePocketListings = includePocketListings; }
}

class OffMarketDiscoveryResult {
    private List<DiscoveredProperty> properties;
    private List<String> offMarketSources;
    private double exclusivityScore;

    // Getters and setters
    public List<DiscoveredProperty> getProperties() { return properties; }
    public void setProperties(List<DiscoveredProperty> properties) { this.properties = properties; }
    public List<String> getOffMarketSources() { return offMarketSources; }
    public void setOffMarketSources(List<String> offMarketSources) { this.offMarketSources = offMarketSources; }
    public double getExclusivityScore() { return exclusivityScore; }
    public void setExclusivityScore(double exclusivityScore) { this.exclusivityScore = exclusivityScore; }
}

class EmergingAreaDiscoveryRequest {
    private String investmentFocus; // residential, commercial, mixed
    private List<String> targetRegions;
    private double growthPotentialThreshold = 0.15;

    // Getters and setters
    public String getInvestmentFocus() { return investmentFocus; }
    public void setInvestmentFocus(String investmentFocus) { this.investmentFocus = investmentFocus; }
    public List<String> getTargetRegions() { return targetRegions; }
    public void setTargetRegions(List<String> targetRegions) { this.targetRegions = targetRegions; }
    public double getGrowthPotentialThreshold() { return growthPotentialThreshold; }
    public void setGrowthPotentialThreshold(double growthPotentialThreshold) { this.growthPotentialThreshold = growthPotentialThreshold; }
}

class EmergingAreaDiscoveryResult {
    private List<DiscoveredProperty> properties;
    private List<String> emergingNeighborhoods;
    private Map<String, Double> growthProjections;

    // Getters and setters
    public List<DiscoveredProperty> getProperties() { return properties; }
    public void setProperties(List<DiscoveredProperty> properties) { this.properties = properties; }
    public List<String> getEmergingNeighborhoods() { return emergingNeighborhoods; }
    public void setEmergingNeighborhoods(List<String> emergingNeighborhoods) { this.emergingNeighborhoods = emergingNeighborhoods; }
    public Map<String, Double> getGrowthProjections() { return growthProjections; }
    public void setGrowthProjections(Map<String, Double> growthProjections) { this.growthProjections = growthProjections; }
}

class PriceAnomalyDiscoveryRequest {
    private String anomalyType; // undervalued, overvalued, unusual
    private List<String> propertyCategories;
    private double anomalyThreshold = 0.20;

    // Getters and setters
    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
    public List<String> getPropertyCategories() { return propertyCategories; }
    public void setPropertyCategories(List<String> propertyCategories) { this.propertyCategories = propertyCategories; }
    public double getAnomalyThreshold() { return anomalyThreshold; }
    public void setAnomalyThreshold(double anomalyThreshold) { this.anomalyThreshold = anomalyThreshold; }
}

class PriceAnomalyDiscoveryResult {
    private List<DiscoveredProperty> properties;
    private List<String> anomalyReasons;
    private Map<String, Double> anomalyScores;

    // Getters and setters
    public List<DiscoveredProperty> getProperties() { return properties; }
    public void setProperties(List<DiscoveredProperty> properties) { this.properties = properties; }
    public List<String> getAnomalyReasons() { return anomalyReasons; }
    public void setAnomalyReasons(List<String> anomalyReasons) { this.anomalyReasons = anomalyReasons; }
    public Map<String, Double> getAnomalyScores() { return anomalyScores; }
    public void setAnomalyScores(Map<String, Double> anomalyScores) { this.anomalyScores = anomalyScores; }
}

class UniqueFeatureDiscoveryRequest {
    private List<String> desiredFeatures;
    private String uniquenessLevel; // rare, unique, one-of-a-kind
    private boolean includeHistoricalProperties = true;

    // Getters and setters
    public List<String> getDesiredFeatures() { return desiredFeatures; }
    public void setDesiredFeatures(List<String> desiredFeatures) { this.desiredFeatures = desiredFeatures; }
    public String getUniquenessLevel() { return uniquenessLevel; }
    public void setUniquenessLevel(String uniquenessLevel) { this.uniquenessLevel = uniquenessLevel; }
    public boolean isIncludeHistoricalProperties() { return includeHistoricalProperties; }
    public void setIncludeHistoricalProperties(boolean includeHistoricalProperties) { this.includeHistoricalProperties = includeHistoricalProperties; }
}

class UniqueFeatureDiscoveryResult {
    private List<DiscoveredProperty> properties;
    private List<String> uniqueFeatures;
    private Map<String, List<String>> propertyUniqueFeatures;

    // Getters and setters
    public List<DiscoveredProperty> getProperties() { return properties; }
    public void setProperties(List<DiscoveredProperty> properties) { this.properties = properties; }
    public List<String> getUniqueFeatures() { return uniqueFeatures; }
    public void setUniqueFeatures(List<String> uniqueFeatures) { this.uniqueFeatures = uniqueFeatures; }
    public Map<String, List<String>> getPropertyUniqueFeatures() { return propertyUniqueFeatures; }
    public void setPropertyUniqueFeatures(Map<String, List<String>> propertyUniqueFeatures) { this.propertyUniqueFeatures = propertyUniqueFeatures; }
}

class InvestmentOpportunityRequest {
    private String investmentStrategy; // flip, rental, development
    private List<String> targetMarkets;
    private double minROITarget = 0.15;
    private String riskLevel; // low, medium, high

    // Getters and setters
    public String getInvestmentStrategy() { return investmentStrategy; }
    public void setInvestmentStrategy(String investmentStrategy) { this.investmentStrategy = investmentStrategy; }
    public List<String> getTargetMarkets() { return targetMarkets; }
    public void setTargetMarkets(List<String> targetMarkets) { this.targetMarkets = targetMarkets; }
    public double getMinROITarget() { return minROITarget; }
    public void setMinROITarget(double minROITarget) { this.minROITarget = minROITarget; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
}

class InvestmentOpportunityResult {
    private List<InvestmentProperty> opportunities;
    private List<String> investmentInsights;
    private double averageROI;

    // Getters and setters
    public List<InvestmentProperty> getOpportunities() { return opportunities; }
    public void setOpportunities(List<InvestmentProperty> opportunities) { this.opportunities = opportunities; }
    public List<String> getInvestmentInsights() { return investmentInsights; }
    public void setInvestmentInsights(List<String> investmentInsights) { this.investmentInsights = investmentInsights; }
    public double getAverageROI() { return averageROI; }
    public void setAverageROI(double averageROI) { this.averageROI = averageROI; }
}

class LifestyleDiscoveryRequest {
    private List<String> lifestylePreferences;
    private List<String> activityPreferences;
    private String communityType; // urban, suburban, rural

    // Getters and setters
    public List<String> getLifestylePreferences() { return lifestylePreferences; }
    public void setLifestylePreferences(List<String> lifestylePreferences) { this.lifestylePreferences = lifestylePreferences; }
    public List<String> getActivityPreferences() { return activityPreferences; }
    public void setActivityPreferences(List<String> activityPreferences) { this.activityPreferences = activityPreferences; }
    public String getCommunityType() { return communityType; }
    public void setCommunityType(String communityType) { this.communityType = communityType; }
}

class LifestyleDiscoveryResult {
    private List<DiscoveredProperty> properties;
    private List<String> lifestyleMatches;
    private Map<String, Double> lifestyleScores;

    // Getters and setters
    public List<DiscoveredProperty> getProperties() { return properties; }
    public void setProperties(List<DiscoveredProperty> properties) { this.properties = properties; }
    public List<String> getLifestyleMatches() { return lifestyleMatches; }
    public void setLifestyleMatches(List<String> lifestyleMatches) { this.lifestyleMatches = lifestyleMatches; }
    public Map<String, Double> getLifestyleScores() { return lifestyleScores; }
    public void setLifestyleScores(Map<String, Double> lifestyleScores) { this.lifestyleScores = lifestyleScores; }
}

class ComprehensiveDiscoveryRequest {
    private boolean includeAllTypes = true;
    private List<String> focusAreas;
    private String discoveryScope; // local, regional, national
    private int maxResultsPerType = 10;

    // Getters and setters
    public boolean isIncludeAllTypes() { return includeAllTypes; }
    public void setIncludeAllTypes(boolean includeAllTypes) { this.includeAllTypes = includeAllTypes; }
    public List<String> getFocusAreas() { return focusAreas; }
    public void setFocusAreas(List<String> focusAreas) { this.focusAreas = focusAreas; }
    public String getDiscoveryScope() { return discoveryScope; }
    public void setDiscoveryScope(String discoveryScope) { this.discoveryScope = discoveryScope; }
    public int getMaxResultsPerType() { return maxResultsPerType; }
    public void setMaxResultsPerType(int maxResultsPerType) { this.maxResultsPerType = maxResultsPerType; }
}

class ComprehensiveDiscoveryResult {
    private String userId;
    private LocalDateTime discoveryDate;
    private String generatedBy;
    private List<DiscoveredProperty> hiddenGems;
    private List<DiscoveredProperty> serendipitousProperties;
    private List<DiscoveredProperty> trendingProperties;
    private List<DiscoveredProperty> offMarketProperties;
    private int totalPropertiesDiscovered;
    private List<String> discoveryInsights;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getDiscoveryDate() { return discoveryDate; }
    public void setDiscoveryDate(LocalDateTime discoveryDate) { this.discoveryDate = discoveryDate; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public List<DiscoveredProperty> getHiddenGems() { return hiddenGems; }
    public void setHiddenGems(List<DiscoveredProperty> hiddenGems) { this.hiddenGems = hiddenGems; }
    public List<DiscoveredProperty> getSerendipitousProperties() { return serendipitousProperties; }
    public void setSerendipitousProperties(List<DiscoveredProperty> serendipitousProperties) { this.serendipitousProperties = serendipitousProperties; }
    public List<DiscoveredProperty> getTrendingProperties() { return trendingProperties; }
    public void setTrendingProperties(List<DiscoveredProperty> trendingProperties) { this.trendingProperties = trendingProperties; }
    public List<DiscoveredProperty> getOffMarketProperties() { return offMarketProperties; }
    public void setOffMarketProperties(List<DiscoveredProperty> offMarketProperties) { this.offMarketProperties = offMarketProperties; }
    public int getTotalPropertiesDiscovered() { return totalPropertiesDiscovered; }
    public void setTotalPropertiesDiscovered(int totalPropertiesDiscovered) { this.totalPropertiesDiscovered = totalPropertiesDiscovered; }
    public List<String> getDiscoveryInsights() { return discoveryInsights; }
    public void setDiscoveryInsights(List<String> discoveryInsights) { this.discoveryInsights = discoveryInsights; }
}

// Supporting classes
class DiscoveredProperty {
    private String propertyId;
    private String propertyTitle;
    private String discoveryType;
    private double discoveryScore;
    private List<String> discoveryReasons;
    private Map<String, String> propertyFeatures;

    // Getters and setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }
    public String getDiscoveryType() { return discoveryType; }
    public void setDiscoveryType(String discoveryType) { this.discoveryType = discoveryType; }
    public double getDiscoveryScore() { return discoveryScore; }
    public void setDiscoveryScore(double discoveryScore) { this.discoveryScore = discoveryScore; }
    public List<String> getDiscoveryReasons() { return discoveryReasons; }
    public void setDiscoveryReasons(List<String> discoveryReasons) { this.discoveryReasons = discoveryReasons; }
    public Map<String, String> getPropertyFeatures() { return propertyFeatures; }
    public void setPropertyFeatures(Map<String, String> propertyFeatures) { this.propertyFeatures = propertyFeatures; }
}

class InvestmentProperty extends DiscoveredProperty {
    private double projectedROI;
    private String investmentStrategy;
    private double riskScore;
    private Map<String, Double> investmentMetrics;

    // Getters and setters
    public double getProjectedROI() { return projectedROI; }
    public void setProjectedROI(double projectedROI) { this.projectedROI = projectedROI; }
    public String getInvestmentStrategy() { return investmentStrategy; }
    public void setInvestmentStrategy(String investmentStrategy) { this.investmentStrategy = investmentStrategy; }
    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
    public Map<String, Double> getInvestmentMetrics() { return investmentMetrics; }
    public void setInvestmentMetrics(Map<String, Double> investmentMetrics) { this.investmentMetrics = investmentMetrics; }
}

// AI Service Interfaces (to be implemented)
interface HiddenGemDiscoveryEngine {
    HiddenGemDiscoveryResult discoverHiddenGems(String userId, HiddenGemDiscoveryRequest request);
}

interface SerendipityEngine {
    SerendipityDiscoveryResult discoverSerendipitously(String userId, SerendipityDiscoveryRequest request);
}

interface TrendDiscoveryEngine {
    TrendDiscoveryResult discoverTrendingProperties(String userId, TrendDiscoveryRequest request);
}

interface OffMarketDiscoveryEngine {
    OffMarketDiscoveryResult discoverOffMarketProperties(String userId, OffMarketDiscoveryRequest request);
}

interface NeighborhoodDiscoveryEngine {
    EmergingAreaDiscoveryResult discoverEmergingAreas(String userId, EmergingAreaDiscoveryRequest request);
}

interface PriceAnomalyDetector {
    PriceAnomalyDiscoveryResult discoverPriceAnomalies(String userId, PriceAnomalyDiscoveryRequest request);
}

interface UniqueFeatureDiscoveryEngine {
    UniqueFeatureDiscoveryResult discoverUniqueFeatures(String userId, UniqueFeatureDiscoveryRequest request);
}

interface EmergingAreaDetector {
    EmergingAreaDiscoveryResult discoverEmergingAreas(String userId, EmergingAreaDiscoveryRequest request);
}

interface InvestmentOpportunityEngine {
    InvestmentOpportunityResult discoverInvestmentOpportunities(String userId, InvestmentOpportunityRequest request);
}

interface LifestyleMatchingEngine {
    LifestyleDiscoveryResult discoverLifestyleProperties(String userId, LifestyleDiscoveryRequest request);
}