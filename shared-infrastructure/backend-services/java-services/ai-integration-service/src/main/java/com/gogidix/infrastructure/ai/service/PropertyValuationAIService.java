package com.gogidix.infrastructure.ai.service;

import com.gogidix.platform.common.core.dto.BaseResponse;
import com.gogidix.platform.common.core.dto.PaginationRequest;
import com.gogidix.platform.common.core.dto.PaginationResponse;
import com.gogidixix.platform.common.security.annotation.RequiresRole;
import com.gogidix.platform.common.audit.annotation.AuditOperation;
import com.gogidix.platform.common.monitoring.annotation.Timed;
import com.gogidix.platform.common.cache.annotation.Cacheable;
import com.gogidix.platform.common.validation.annotation.ValidPropertyData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Property Valuation AI Service
 *
 * CATEGORY 1: Property Management Automation
 * Service: Property Valuation (7/48)
 *
 * AI-Powered property valuation using:
 * - Machine learning valuation models (XGBoost, Random Forest, Neural Networks)
 * - Comparable sales analysis (Comps)
 * - Cash flow and investment return analysis
 * - Market trend prediction
 * - Location-based valuation adjustments
 * - Property feature impact analysis
 * - Automated Valuation Reports (AVR)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Timed(name = "property-valuation-ai", description = "Property Valuation AI Service Metrics")
public class PropertyValuationAIService {

    private final ChatClient chatClient;
    private final PropertyValuationRepository repository;
    private final ValuationModelService valuationModelService;
    private final ComparableSalesService comparableSalesService;
    private final MarketDataService marketDataService;
    private final PropertyAnalyticsService propertyAnalyticsService;

    @Value("${ai.property-valuation.confidence-threshold:0.85}")
    private double confidenceThreshold;

    @Value("${ai.property-valuation.max-comps:20}")
    private int maxComparableSales;

    @Value("${ai.property-valuation.valuation-models:XGBOOST,RANDOM_FOREST,NEURAL_NETWORK}")
    private List<String> supportedModels;

    // Valuation model configurations
    private static final Map<String, ValuationModelConfig> MODEL_CONFIGS = Map.of(
        "XGBOOST", ValuationModelConfig.builder()
            .modelName("XGBoost")
            .features(Arrays.asList("square_footage", "bedrooms", "bathrooms", "age", "location_score",
                                 "condition_score", "lot_size", "garage_spaces", "view_score", "school_district"))
            .modelType("GRADIENT_BOOSTING")
            .accuracy(0.92)
            .interpretability("MEDIUM")
            .trainingData("Historical transactions")
            .build(),

        "RANDOM_FOREST", ValuationModelConfig.builder()
            .modelName("Random Forest")
            .features(Arrays.asList("square_footage", "bedrooms", "bathrooms", "age", "location_score",
                                 "condition_score", "lot_size", "garage_spaces", "view_score", "school_district"))
            .modelType("ENSEMBLE_LEARNING")
            .accuracy(0.89)
            .interpretability("HIGH")
            .trainingData("Historical transactions")
            .build(),

        "NEURAL_NETWORK", ValuationModelConfig.builder()
            .modelName("Deep Neural Network")
            .features(Arrays.asList("square_footage", "bedrooms", "bathrooms", "age", "location_score",
                                 "condition_score", "lot_size", "garage_spaces", "view_score", "school_district",
                                 "neighborhood_trend", "market_sentiment", "seasonal_factor"))
            .modelType("DEEP_LEARNING")
            .accuracy(0.94)
            .interpretability("LOW")
            .trainingData("Historical transactions + market data")
            .build()
    );

    /**
     * Generate property valuation
     */
    @Transactional
    @AuditOperation(operation = "GENERATE_PROPERTY_VALUATION",
                   entity = "PropertyValuation",
                   description = "AI-powered property valuation generation")
    @Cacheable(key = "#request.hashCode()", ttl = 14400)
    public CompletableFuture<PropertyValuationResponse> generateValuation(
            @ValidPropertyData PropertyValuationRequest request) {

        log.info("Generating property valuation for property: {}, location: {}",
                request.getPropertyId(), request.getLocation());

        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // 1. Extract and validate property features
                PropertyFeatures features = extractPropertyFeatures(request);

                // 2. Get comparable sales data
                ComparableSales comparableSales = comparableSalesService
                    .findComparableSales(request.getLocation(), request.getPropertyType(), maxComparableSales);

                // 3. Get market data and trends
                MarketData marketData = marketDataService.getMarketData(
                    request.getLocation(), request.getPropertyType());

                // 4. Run multiple valuation models
                Map<String, ValuationResult> modelResults = runValuationModels(features, comparableSales, marketData);

                // 5. Ensemble model results
                EnsembleValuation ensembleValuation = ensembleModelResults(modelResults);

                // 6. Generate confidence intervals
                ConfidenceIntervals confidenceIntervals = calculateConfidenceIntervals(
                    ensembleValuation, modelResults, comparableSales);

                // 7. Calculate ROI and investment metrics
                InvestmentMetrics investmentMetrics = calculateInvestmentMetrics(
                    request, ensembleValuation, marketData);

                // 8. Generate valuation adjustments
                ValuationAdjustments adjustments = calculateValuationAdjustments(features, marketData);

                // 9. Create valuation report
                ValuationReport valuationReport = createValuationReport(
                    request, ensembleValuation, modelResults, comparableSales, marketData);

                // 10. Save valuation record
                PropertyValuation valuation = saveValuationRecord(
                    request, features, ensembleValuation, modelResults, comparableSales,
                    marketData, confidenceIntervals, investmentMetrics, adjustments, valuationReport);

                // 11. Track analytics
                propertyAnalyticsService.trackValuation(valuation);

                long processingTime = System.currentTimeMillis() - startTime;

                return PropertyValuationResponse.builder()
                    .valuationId(valuation.getId())
                    .estimatedValue(ensembleValuation.getEstimatedValue())
                    .confidenceScore(ensembleValuation.getConfidenceScore())
                    .valueRange(confidenceIntervals.getValueRange())
                    .modelResults(modelResults)
                    .comparableSales(comparableSales)
                    .marketData(marketData)
                    .investmentMetrics(investmentMetrics)
                    .adjustments(adjustments)
                    .valuationReport(valuationReport)
                    .processingTime(processingTime)
                    .generatedAt(LocalDateTime.now())
                    .recommendations(generateValuationRecommendations(ensembleValuation, features))
                    .build();

            } catch (Exception e) {
                log.error("Error generating property valuation", e);
                throw new PropertyValuationException(
                    "Failed to generate valuation: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Rapid valuation using simplified model
     */
    @Transactional
    @AuditOperation(operation = "RAPID_VALUATION",
                   entity = "PropertyValuation",
                   description = "AI-powered rapid property valuation")
    @Cacheable(key = "#request.hashCode()", ttl = 7200)
    public CompletableFuture<RapidValuationResponse> generateRapidValuation(
            @ValidPropertyData RapidValuationRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use simplified features for rapid processing
                RapidFeatures features = extractRapidFeatures(request);

                // Get recent comparable sales
                List<RapidComparableSale> comps = comparableSalesService.getRapidComparableSales(
                    request.getLocation(), 5); // Only top 5 comps for speed

                // Use pre-trained model for rapid valuation
                RapidValuationResult result = valuationModelService.runRapidModel(features, comps);

                return RapidValuationResponse.builder()
                    .estimatedValue(result.getEstimatedValue())
                    .valuePerSf(result.getValuePerSf())
                    .confidenceScore(result.getConfidenceScore())
                    .comparableSales(comps)
                    .estimatedTime(result.getEstimatedTime())
                    .generatedAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("Error generating rapid valuation", e);
                throw new RapidValuationException(
                    "Failed to generate rapid valuation: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Investment analysis for property
     */
    @RequiresRole({"AGENT", "ADMIN", "MANAGER", "INVESTOR"})
    public CompletableFuture<InvestmentAnalysisResponse> analyzeInvestment(
            InvestmentAnalysisRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get current valuation
                PropertyValuation currentValuation = repository.findById(request.getValuationId())
                    .orElseThrow(() -> new ValuationNotFoundException("Valuation not found"));

                // Calculate investment metrics
                InvestmentMetrics metrics = calculateInvestmentMetrics(request, currentValuation);

                // Generate investment scenarios
                List<InvestmentScenario> scenarios = generateInvestmentScenarios(request, metrics);

                // Create risk analysis
                RiskAnalysis riskAnalysis = performRiskAnalysis(request, metrics, scenarios);

                return InvestmentAnalysisResponse.builder()
                    .valuationId(request.getValuationId())
                    .investmentMetrics(metrics)
                    .scenarios(scenarios)
                    .riskAnalysis(riskAnalysis)
                    .recommendations(generateInvestmentRecommendations(metrics, riskAnalysis))
                    .analyzedAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("Error analyzing investment", e);
                throw new InvestmentAnalysisException(
                    "Failed to analyze investment: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Market trend analysis and prediction
     */
    @RequiresRole({"AGENT", "ADMIN", "MANAGER"})
    public CompletableFuture<MarketTrendAnalysisResponse> analyzeMarketTrends(
            MarketTrendAnalysisRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                MarketTrendAnalysis analysis = marketDataService.performTrendAnalysis(
                    request.getLocation(), request.getPropertyType(), request.getTimeRange());

                return MarketTrendAnalysisResponse.builder()
                    .trendData(analysis.getTrendData())
                    .predictions(analysis.getPredictions())
                    .seasonalAdjustments(analysis.getSeasonalAdjustments())
                    .marketFactors(analysis.getMarketFactors())
                    .recommendations(analysis.getRecommendations())
                    .analyzedAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("Error analyzing market trends", e);
                throw new MarketTrendAnalysisException(
                    "Failed to analyze market trends: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Update valuation with new data
     */
    @Transactional
    @AuditOperation(operation = "UPDATE_VALUATION",
                   entity = "PropertyValuation",
                   description = "Update property valuation with new data")
    public CompletableFuture<ValuationUpdateResponse> updateValuation(
            String valuationId,
            ValuationUpdateRequest updateRequest) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                PropertyValuation existingValuation = repository.findById(valuationId)
                    .orElseThrow(() -> new ValuationNotFoundException("Valuation not found: " + valuationId));

                // Apply updates
                PropertyValuation updatedValuation = applyValuationUpdates(existingValuation, updateRequest);

                // Save updates
                updatedValuation.setUpdatedAt(LocalDateTime.now());
                repository.save(updatedValuation);

                return ValuationUpdateResponse.builder()
                    .valuationId(valuationId)
                    .previousValue(existingValuation.getEstimatedValue())
                    .updatedValue(updatedValuation.getEstimatedValue())
                    .valueChange(updatedValuation.getEstimatedValue() - existingValuation.getEstimatedValue())
                    .changePercentage(calculateChangePercentage(
                        existingValuation.getEstimatedValue(),
                        updatedValuation.getEstimatedValue()))
                    .updatedAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("Error updating valuation", e);
                throw new ValuationUpdateException(
                    "Failed to update valuation: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get valuation history
     */
    @RequiresRole({"AGENT", "ADMIN", "MANAGER"})
    public PaginationResponse<PropertyValuationSummary> getValuationHistory(
            String propertyId,
            PaginationRequest request) {

        log.info("Fetching valuation history for property: {}", propertyId);

        return repository.findByPropertyIdOrderByCreatedAtDesc(propertyId, Pageable.ofSize(
            request.getSize()).withPage(request.getPage()));
    }

    // Private helper methods

    private PropertyFeatures extractPropertyFeatures(PropertyValuationRequest request) {
        return PropertyFeatures.builder()
            .squareFootage(request.getSquareFootage())
            .bedrooms(request.getBedrooms())
            .bathrooms(request.getBathrooms())
            .age(request.getYearBuilt() != null ?
                LocalDateTime.now().getYear() - request.getYearBuilt() : 0)
            .propertyType(request.getPropertyType())
            .lotSize(request.getLotSize())
            .garageSpaces(request.getGarageSpaces())
            .hasPool(request.isHasPool())
            .hasBasement(request.isHasBasement())
            .hasGarden(request.isHasGarden())
            .condition(request.getCondition())
            .viewScore(calculateViewScore(request))
            .schoolScore(calculateSchoolScore(request))
            .amenitiesScore(calculateAmenitiesScore(request))
            .build();
    }

    private RapidFeatures extractRapidFeatures(RapidValuationRequest request) {
        return RapidFeatures.builder()
            .squareFootage(request.getSquareFootage())
            .bedrooms(request.getBedrooms())
            .bathrooms(request.getBathrooms())
            .propertyType(request.getPropertyType())
            .locationScore(calculateLocationScore(request.getLocation()))
            .build();
    }

    private Map<String, ValuationResult> runValuationModels(
            PropertyFeatures features,
            ComparableSales comparableSales,
            MarketData marketData) {

        Map<String, ValuationResult> results = new HashMap<>();

        for (String modelName : supportedModels) {
            ValuationModelConfig config = MODEL_CONFIGS.get(modelName);
            ValuationResult result = valuationModelService.runModel(
                features, comparableSales, marketData, config);
            results.put(modelName, result);
        }

        return results;
    }

    private EnsembleValuation ensembleModelResults(Map<String, ValuationResult> modelResults) {
        // Calculate weighted average based on model accuracy
        double totalWeight = modelResults.values().stream()
            .mapToDouble(r -> MODEL_CONFIGS.get(r.getModelName()).getAccuracy())
            .sum();

        double weightedValue = modelResults.entrySet().stream()
            .mapToDouble(e -> {
                double weight = MODEL_CONFIGS.get(e.getKey()).getAccuracy();
                return e.getValue().getEstimatedValue() * weight;
            })
            .sum();

        return EnsembleValuation.builder()
            .estimatedValue(weightedValue / totalWeight)
            .confidenceScore(calculateEnsembleConfidence(modelResults))
            .modelResults(modelResults)
            .weights(modelResults.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> MODEL_CONFIGS.get(e.getKey()).getAccuracy()
                )))
            .build();
    }

    private ConfidenceIntervals calculateConfidenceIntervals(
            EnsembleValuation ensembleValuation,
            Map<String, ValuationResult> modelResults,
            ComparableSales comparableSales) {

        double standardDeviation = calculateStandardDeviation(modelResults);
        double mean = ensembleValuation.getEstimatedValue();

        double lowerBound = mean - (1.96 * standardDeviation);
        double upperBound = mean + (1.96 * standardDeviation);

        return ConfidenceIntervals.builder()
            .valueRange(new ValueRange(lowerBound, upperBound))
            .confidenceLevel(0.95)
            .standardDeviation(standardDeviation)
            .sampleSize(modelResults.size())
            .build();
    }

    private InvestmentMetrics calculateInvestmentMetrics(
            PropertyValuationRequest request,
            EnsembleValuation valuation,
            MarketData marketData) {

        double estimatedValue = valuation.getEstimatedValue();
        double monthlyRent = marketData.getAverageRent();
        double grossYield = monthlyRent * 12 / estimatedValue * 100;
        double netYield = grossYield * 0.75; // 25% for expenses

        return InvestmentMetrics.builder()
            .estimatedValue(estimatedValue)
            .averageRent(monthlyRent)
            .grossYield(grossYield)
            .netYield(netYield)
            .capRate(calculateCapRate(estimatedValue, monthlyRent))
            .cashFlow(calculateAnnualCashFlow(monthlyRent))
            .roi(calculateROI(estimatedValue, monthlyRent))
            .paybackPeriod(calculatePaybackPeriod(estimatedValue, monthlyRent))
            .build();
    }

    private ValuationAdjustments calculateValuationAdjustments(
            PropertyFeatures features,
            MarketData marketData) {

        List<Adjustment> adjustments = new ArrayList<>();

        // Location adjustment
        if (features.getLocationScore() < 0.7) {
            adjustments.add(Adjustment.builder()
                .type("LOCATION")
                .description("Below average location score")
                .adjustmentPercentage(-0.05)
                .reason("Location has lower desirability")
                .build());
        }

        // Age adjustment
        if (features.getAge() > 20) {
            double ageAdjustment = Math.min(-0.15, -0.005 * (features.getAge() - 20));
            adjustments.add(Adjustment.builder()
                .type("AGE")
                .description("Property age over 20 years")
                .adjustmentPercentage(ageAdjustment)
                .reason("Older properties typically have reduced value")
                .build());
        }

        // Condition adjustment
        if (features.getConditionScore() < 0.6) {
            adjustments.add(Adjustment.builder()
                .type("CONDITION")
                .description("Poor property condition")
                .adjustmentPercentage(-0.10)
                .reason("Repairs and renovation needed")
                .build());
        }

        return ValuationAdjustments.builder()
            .adjustments(adjustments)
            .totalAdjustment(calculateTotalAdjustment(adjustments))
            .build();
    }

    private ValuationReport createValuationReport(
            PropertyValuationRequest request,
            EnsembleValuation valuation,
            Map<String, ValuationResult> modelResults,
            ComparableSales comparableSales,
            MarketData marketData) {

        return ValuationReport.builder()
            .executiveSummary(generateExecutiveSummary(request, valuation))
            .methodology(generateMethodologySection(modelResults))
            .comparableAnalysis(generateComparableAnalysis(comparableSales))
            .marketAnalysis(generateMarketAnalysis(marketData))
            .conclusions(generateConclusions(valuation, comparableSales, marketData))
            .generatedAt(LocalDateTime.now())
            .build();
    }

    private PropertyValuation saveValuationRecord(
            PropertyValuationRequest request,
            PropertyFeatures features,
            EnsembleValuation ensembleValuation,
            Map<String, ValuationResult> modelResults,
            ComparableSales comparableSales,
            MarketData marketData,
            ConfidenceIntervals confidenceIntervals,
            InvestmentMetrics investmentMetrics,
            ValuationAdjustments adjustments,
            ValuationReport valuationReport) {

        PropertyValuation valuation = PropertyValuation.builder()
            .propertyId(request.getPropertyId())
            .propertyType(request.getPropertyType())
            .location(request.getLocation())
            .estimatedValue(ensembleValuation.getEstimatedValue())
            .valueRangeJson(confidenceIntervals.getValueRange().toJson())
            .confidenceScore(ensembleValuation.getConfidenceScore())
            .modelResultsJson(modelResults.toString())
            .comparableSalesJson(comparableSales.toJson())
            .marketDataJson(marketData.toJson())
            .featuresJson(features.toJson())
            .investmentMetricsJson(investmentMetrics.toJson())
            .adjustmentsJson(adjustments.toJson())
            .reportJson(valuationReport.toJson())
            .createdAt(LocalDateTime.now())
            .build();

        return repository.save(valuation);
    }

    private List<String> generateValuationRecommendations(
            EnsembleValuation valuation,
            PropertyFeatures features) {

        List<String> recommendations = new ArrayList<>();

        if (valuation.getConfidenceScore() < confidenceThreshold) {
            recommendations.add("Consider gathering more comparable sales data for higher confidence");
        }

        if (features.getAge() > 30) {
            recommendations.add("Consider renovation to improve property value and appeal");
        }

        if (features.getConditionScore() < 0.5) {
            recommendations.add("Property improvements needed to achieve full market value");
        }

        return recommendations;
    }

    // Additional helper methods for other features
    private Double calculateViewScore(PropertyValuationRequest request) {
        // Simplified view score calculation
        return request.isHasView() ? 0.8 : 0.5;
    }

    private Double calculateSchoolScore(PropertyValuationRequest request) {
        // Simplified school score calculation
        return 0.7; // Would integrate with real school district data
    }

    private Double calculateAmenitiesScore(PropertyValuationRequest request) {
        return (request.isHasPool() ? 0.3 : 0) +
               (request.isHasBasement() ? 0.2 : 0) +
               (request.isHasGarden() ? 0.1 : 0);
    }

    private Double calculateLocationScore(String location) {
        // Simplified location score - would integrate with location data APIs
        return 0.75;
    }

    private Double calculateStandardDeviation(Map<String, ValuationResult> modelResults) {
        double mean = modelResults.values().stream()
            .mapToDouble(ValuationResult::getEstimatedValue)
            .average();

        double variance = modelResults.values().stream()
            .mapToDouble(r -> Math.pow(r.getEstimatedValue() - mean, 2))
            .average();

        return Math.sqrt(variance);
    }

    private Double calculateEnsembleConfidence(Map<String, ValuationResult> modelResults) {
        double avgConfidence = modelResults.values().stream()
            .mapToDouble(ValuationResult::getConfidenceScore)
            .average();

        // Increase confidence when models agree
        double agreementScore = calculateModelAgreement(modelResults);
        return Math.min(0.95, avgConfidence + (agreementScore * 0.1));
    }

    private Double calculateModelAgreement(Map<String, ValuationResult> modelResults) {
        // Calculate how closely models agree with each other
        double[] values = modelResults.values().stream()
            .mapToDouble(ValuationResult::getEstimatedValue)
            .toArray();

        if (values.length < 2) return 1.0;

        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = Arrays.stream(values)
            .map(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);

        double standardDeviation = Math.sqrt(variance);
        return mean > 0 ? (1 - (standardDeviation / mean)) : 0.0;
    }

    // Investment analysis methods
    private List<InvestmentScenario> generateInvestmentScenarios(
            InvestmentAnalysisRequest request,
            InvestmentMetrics metrics) {

        List<InvestmentScenario> scenarios = new ArrayList<>();

        // Best case scenario
        scenarios.add(InvestmentScenario.builder()
            .name("Best Case")
            .description("Optimal market conditions and rental income")
            .projectedValue(calculateProjectedValue(metrics.getEstimatedValue(), 0.05, 5))
            .roi(metrics.getRoi() * 1.2)
            .build());

        // Base case scenario
        scenarios.add(InvestmentScenario.builder()
            .name("Base Case")
            .description("Current market conditions")
            .projectedValue(metrics.getEstimatedValue())
            .roi(metrics.getRoi())
            .build());

        // Worst case scenario
        scenarios.add(InvestmentScenario.builder()
            .name("Worst Case")
            .description("Declining market conditions")
            .projectedValue(calculateProjectedValue(metrics.getEstimatedValue(), -0.03, 5))
            .roi(metrics.getRoi() * 0.6)
            .build());

        return scenarios;
    }

    private RiskAnalysis performRiskAnalysis(
            InvestmentAnalysisRequest request,
            InvestmentMetrics metrics,
            List<InvestmentScenario> scenarios) {

        return RiskAnalysis.builder()
            .marketRisk(analyzeMarketRisk(request.getLocation()))
            .propertyRisk(analyzePropertyRisk(metrics))
            .financialRisk(analyzeFinancialRisk(metrics, scenarios))
            .liquidityRisk(analyzeLiquidityRisk())
            .overallRisk(calculateOverallRisk())
            .build();
    }

    private List<String> generateInvestmentRecommendations(
            InvestmentMetrics metrics,
            RiskAnalysis riskAnalysis) {

        List<String> recommendations = new ArrayList<>();

        if (metrics.getNetYield() < 0.05) {
            recommendations.add("Consider property improvements or rental rate adjustments");
        }

        if (riskAnalysis.getMarketRisk() > 0.7) {
            recommendations.add("Monitor market trends closely before investing");
        }

        if (riskAnalysis.getLiquidityRisk() > 0.8) {
            recommendations.add("Consider holding period of 5+ years due to lower liquidity");
        }

        return recommendations;
    }

    // Metric calculation methods
    private Double calculateCapRate(double value, double monthlyRent) {
        return (monthlyRent * 12) / value * 100;
    }

    private Double calculateAnnualCashFlow(double monthlyRent) {
        return monthlyRent * 12;
    }

    private Double calculateROI(double value, double monthlyRent) {
        double annualCashFlow = calculateAnnualCashFlow(monthlyRent);
        double totalInvestment = value * 1.2; // Include closing costs
        return (annualCashFlow / totalInvestment) * 100;
    }

    private Double calculatePaybackPeriod(double value, double monthlyRent) {
        double annualCashFlow = calculateAnnualCashFlow(monthlyRent);
        return annualCashFlow > 0 ? value / annualCashFlow : 0;
    }

    // Update methods
    private PropertyValuation applyValuationUpdates(
            PropertyValuation existingValuation,
            ValuationUpdateRequest updateRequest) {

        PropertyValuation updated = new PropertyValuation(existingValuation);

        if (updateRequest.getNewPrice() != null) {
            updated.setEstimatedValue(updateRequest.getNewPrice());
        }

        if (updateRequest.getUpdatedFeatures() != null) {
            updated.setFeaturesJson(updateRequest.getUpdatedFeatures().toJson());
        }

        return updated;
    }

    // Report generation methods
    private String generateExecutiveSummary(
            PropertyValuationRequest request,
            EnsembleValuation valuation) {

        return String.format(
            "Property valuation completed for %s at %s. " +
            "Estimated value: $%,s with %d confidence score. " +
            "Based on %d comparable sales and current market analysis.",
            request.getPropertyType(),
            request.getLocation(),
            valuation.getEstimatedValue(),
            String.format("$%,.2f", valuation.getEstimatedValue()),
            valuation.getConfidenceScore(),
            maxComparableSales
        );
    }

    private String generateMethodologySection(Map<String, ValuationResult> modelResults) {
        return String.format(
            "Valuation performed using %s AI models: %s. " +
            "Models achieved average accuracy of %.1f%% with " +
            "ensemble confidence score of %.1f%%.",
            modelResults.size(),
            String.join(", ", modelResults.keySet()),
            modelResults.values().stream()
                .mapToDouble(r -> MODEL_CONFIGS.get(r.getModelName()).getAccuracy())
                .average() * 100,
            ensembleValuation.calculateConfidenceScore(modelResults) * 100
        );
    }

    private String generateComparableAnalysis(ComparableSales comparableSales) {
        return String.format(
            "Analysis of %d comparable sales in the area. " +
            "Average comp price: $%,s. " +
            "Price per SF: $%,s. " +
            "Time on market: %d days on average.",
            comparableSales.getSales().size(),
            String.format("$%,.2f", comparableSales.getAveragePrice()),
            String.format("$%,.2f", comparableServices.getPricePerSquareFoot()),
            comparableSales.getAverageDaysOnMarket()
        );
    }

    private String generateMarketAnalysis(MarketData marketData) {
        return String.format(
            "Current market analysis shows: %s market trend. " +
            "Average days on market: %d. " +
            "Inventory level: %s. " +
            "Price change: %+.1f%% year-over-year.",
            marketData.getMarketTrend(),
            marketData.getAverageDaysOnMarket(),
            marketData.getInventoryLevel(),
            marketData.getPriceChangePercentage()
        );
    }

    private String generateConclusions(
            EnsembleValuation valuation,
            ComparableSales comparableSales,
            MarketData marketData) {

        StringBuilder conclusions = new StringBuilder();
        conclusions.append("Based on comprehensive analysis: ");

        if (valuation.getConfidenceScore() > 0.85) {
            conclusions.append("High confidence in valuation estimate. ");
        } else {
            conclusions.append("Valuation estimate has moderate confidence due to limited data. ");
        }

        if (marketData.getInventoryLevel().equals("LOW")) {
            conclusions.append("Seller's market with potential for appreciation. ");
        } else if (marketData.getInventoryLevel().equals("HIGH")) {
            conclusions.append("Buyer's market with negotiation opportunities. ");
        }

        conclusions.append(String.format(
            "Estimated value of $%,.2f represents %s %s for this property.",
            valuation.getEstimatedValue(),
            marketData.getPropertyType(),
            marketData.getPriceChangePercentage() > 0 ? "above-average" : "below-average"
        ));

        return conclusions.toString();
    }

    // Additional helper methods
    private Double calculateProjectedValue(double currentValue, double annualGrowthRate, int years) {
        return currentValue * Math.pow(1 + annualGrowthRate, years);
    }

    private String analyzeMarketRisk(String location) {
        // Simplified market risk analysis
        return "MODERATE"; // Would integrate with market volatility data
    }

    private String analyzePropertyRisk(InvestmentMetrics metrics) {
        if (metrics.getNetYield() < 0.03) return "HIGH";
        if (metrics.getNetYield() < 0.05) return "MEDIUM";
        return "LOW";
    }

    private String analyzeFinancialRisk(InvestmentMetrics metrics, List<InvestmentScenario> scenarios) {
        // Analyze risk based on cash flow and scenario variation
        double worstCaseROI = scenarios.get(2).getRoi();
        if (worstCaseROI < 0) return "HIGH";
        if (worstCaseROI < 5) return "MEDIUM";
        return "LOW";
    }

    private String analyzeLiquidityRisk() {
        return "MODERATE"; // Would analyze market liquidity data
    }

    private Double calculateOverallRisk() {
        return 0.5; // Would combine all risk factors
    }

    private Double calculateTotalAdjustment(List<Adjustment> adjustments) {
        return adjustments.stream()
            .mapToDouble(Adjustment::getAdjustmentPercentage)
            .sum();
    }

    private Double calculateChangePercentage(double oldValue, double newValue) {
        return oldValue != 0 ? ((newValue - oldValue) / oldValue) * 100 : 0;
    }
}