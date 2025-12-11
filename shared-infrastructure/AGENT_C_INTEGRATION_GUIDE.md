# Agent C - AI Services Integration Guide

## Overview

This guide provides comprehensive instructions for integrating Agent C (AI Services) with the Gogidix Property Marketplace infrastructure, enabling intelligent analytics, predictive capabilities, and AI-powered automation.

## AI Services Available

The platform includes 25 AI services:

### Core AI Services
- **ai-gateway-service** - Central gateway for AI services
- **ai-inference-service** - Model inference engine
- **ai-model-management-service** - ML model lifecycle management

### Analytics & Prediction
- **ai-predictive-analytics-service** - Time-series forecasting
- **ai-forecasting-service** - Business metrics forecasting
- **ai-bi-analytics-service** - Business intelligence analytics
- **ai-anomaly-detection-service** - Anomaly detection in metrics

### ML & Data Processing
- **ai-data-quality-service** - Data quality assessment
- **ai-optimization-service** - Resource optimization
- **ai-matching-algorithm-service** - Property matching algorithms
- **ai-categorization-service** - Content categorization

### User Experience
- **ai-personalization-service** - Personalized recommendations
- **ai-recommendation-service** - Product recommendations
- **ai-chatbot-service** - Intelligent chatbot
- **ai-search-optimization-service** - Enhanced search

### Computer Vision & NLP
- **ai-computer-vision-service** - Image processing
- **ai-image-recognition-service** - Image recognition
- **ai-nlp-processing-service** - Natural language processing
- **ai-speech-recognition-service** - Speech to text
- **ai-sentiment-analysis-service** - Sentiment analysis

### Business Operations
- **ai-pricing-engine-service** - Dynamic pricing
- **ai-risk-assessment-service** - Risk assessment
- **ai-fraud-detection-service** - Fraud detection
- **ai-content-moderation-service** - Content moderation
- **ai-report-generation-service** - Automated reporting
- **ai-automated-tagging-service** - Automated tagging
- **ai-translation-service** - Language translation

## Quick Start

### 1. Deploy AI Integration Service

```bash
cd shared-infrastructure/java-services/ai-integration-service
mvn clean package -DskipTests
docker build -t gogidix/ai-integration:latest .
kubectl apply -f k8s/
```

### 2. Configure AI Services

Add to your `application.yml`:

```yaml
# AI Services Configuration
ai:
  services:
    predictive-analytics:
      url: http://ai-predictive-analytics:8081
      timeout: 30000
      retry-attempts: 3
    anomaly-detection:
      url: http://ai-anomaly-detection:8082
      confidence-threshold: 0.85
    recommendation:
      url: http://ai-recommendation:8083
      cache-ttl: 300
      max-recommendations: 10
    forecasting:
      url: http://ai-forecasting:8084
      model-update-frequency: 3600000  # 1 hour

  # Machine Learning Configuration
  ml:
    models:
      resource-prediction:
        type: "tensorflow"
        model-path: "/models/resource_prediction.pb"
        input-features: ["cpu_usage", "memory_usage", "request_rate"]
      traffic-prediction:
        type: "spark-ml"
        model-path: "/models/traffic_model"
      anomaly-detection:
        type: "h2o"
        algorithm: "IsolationForest"
        contamination: 0.1

  # TensorFlow Configuration
  tensorflow:
    enable-gpu: false
    num-threads: 4
    session-config:
      allow-growth: true
    model:
      save-interval: 3600000
      batch-size: 32

# Kafka Topics for AI Events
kafka:
  topics:
    ai-predictions: ai-predictions
    ai-anomalies: ai-anomalies
    ai-recommendations: ai-recommendations
    ml-model-updates: ml-model-updates

# Redis Configuration for AI Caching
spring:
  cache:
    type: redis
  redis:
    ai:
      ttl: 300  # 5 minutes
      max-entries: 10000
```

### 3. Add to Docker Compose

```yaml
ai-integration:
  image: gogidix/ai-integration:latest
  ports:
    - "8095:8095"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - AI_SERVICES_PREDICTIVE_ANALYTICS_URL=http://ai-predictive-analytics:8081
    - AI_SERVICES_ANOMALY_DETECTION_URL=http://ai-anomaly-detection:8082
    - AI_SERVICES_RECOMMENDATION_URL=http://ai-recommendation:8083
  depends_on:
    - ai-predictive-analytics
    - ai-anomaly-detection
    - ai-recommendation
    - kafka
    - redis
  networks:
    - gogidix-network
```

## Predictive Analytics Integration

### Resource Usage Prediction

```java
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    @Autowired
    private PredictiveAnalyticsService predictiveService;

    @PostMapping("/predict/resources")
    public CompletableFuture<ResourcePrediction> predictResourceUsage(
            @RequestBody ResourcePredictionRequest request) {

        Map<String, Object> features = Map.of(
            "cpu_usage", request.getCurrentCpu(),
            "memory_usage", request.getCurrentMemory(),
            "request_rate", request.getRequestRate(),
            "disk_io", request.getDiskIO(),
            "network_io", request.getNetworkIO()
        );

        return predictiveService.predictResourceUsage(
            request.getResourceType(),
            request.getPeriod(),
            features
        );
    }
}
```

### Traffic Pattern Prediction

```java
@Service
public class TrafficPredictionService {

    @Async
    public CompletableFuture<TrafficForecast> predictTraffic(
            String service,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        Map<String, Object> features = Map.of(
            "historical_traffic", getHistoricalTraffic(service),
            "day_of_week", startTime.getDayOfWeek().getValue(),
            "hour_of_day", startTime.getHour(),
            "season", getCurrentSeason(),
            "special_events", getSpecialEvents(startTime)
        );

        return predictiveService.predictTraffic(service, "24h", features)
            .thenApply(this::processTrafficForecast);
    }
}
```

### SLA Compliance Prediction

```java
@Scheduled(cron = "0 0 * * * *") // Every hour
public void predictSLACompliance() {
    List<String> services = getActiveServices();

    services.parallelStream().forEach(service -> {
        predictiveService.predictSLACompliance(
            service,
            LocalDateTime.now().plusDays(1)
        ).thenAccept(prediction -> {
            if (prediction.getComplianceProbability() < 0.95) {
                alertService.sendSlaRiskAlert(service, prediction);
            }
        });
    });
}
```

## Anomaly Detection Integration

### Real-time Anomaly Detection

```java
@Component
public class AnomalyDetectionService {

    @KafkaListener(topics = "metrics")
    public void processMetrics(MetricMessage message) {
        // Collect recent metrics for analysis
        List<Double> recentValues = metricsRepository.getRecentValues(
            message.getMetricName(),
            60 // Last 60 data points
        );

        Map<String, Object> context = Map.of(
            "service", message.getServiceName(),
            "timestamp", message.getTimestamp(),
            "threshold", message.getThreshold()
        );

        anomalyService.detectAnomalies(
            message.getMetricName(),
            recentValues,
            context
        ).thenAccept(result -> {
            if (result.isHasAnomalies()) {
                handleAnomaly(result);
            }
        });
    }

    private void handleAnomaly(AnomalyDetectionResult result) {
        // Create alert
        Alert alert = Alert.builder()
            .type("ANOMALY")
            .severity(calculateSeverity(result.getScore()))
            .message("Anomaly detected in " + result.getMetric())
            .details(result)
            .build();

        alertService.sendAlert(alert);

        // Send to dashboard via WebSocket
        webSocketHandler.broadcastAnomaly(result);
    }
}
```

### Custom Anomaly Detection Models

```java
@Service
public class CustomAnomalyModels {

    // Network traffic anomaly detection
    public void detectNetworkAnomalies(String service) {
        NetworkTrafficModel model = NetworkTrafficModel.builder()
            .protocol(TCP)
            .algorithm("LSTM")
            .sequenceLength(60)
            .build();

        List<Double> trafficData = getNetworkTraffic(service);
        AnomalyResult result = model.detect(trafficData);
    }

    // Database performance anomaly detection
    public void detectDatabaseAnomalies(String database) {
        DatabaseMetricsModel model = DatabaseMetricsModel.builder()
            .metrics(List.of("query_time", "connections", "deadlocks"))
            .algorithm("IsolationForest")
            .contamination(0.1)
            .build();

        Map<String, List<Double>> dbMetrics = getDatabaseMetrics(database);
        AnomalyResult result = model.detect(dbMetrics);
    }
}
```

## Recommendation Engine Integration

### Property Recommendations

```java
@Service
public class PropertyRecommendationService {

    public CompletableFuture<List<PropertyRecommendation>> getRecommendations(
            Long userId,
            RecommendationType type) {

        Map<String, Object> context = Map.of(
            "user_id", userId,
            "user_history", getUserHistory(userId),
            "preferences", getUserPreferences(userId),
            "location", getUserLocation(userId),
            "budget", getUserBudget(userId)
        );

        return recommendationService.generateRecommendations(
            type.toString(),
            context
        ).thenApply(this::processPropertyRecommendations);
    }

    private List<PropertyRecommendation> processPropertyRecommendations(
            Recommendations recommendations) {

        return recommendations.getItems().stream()
            .map(this::createPropertyRecommendation)
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(10)
            .collect(Collectors.toList());
    }
}
```

### Dynamic Pricing Recommendations

```java
@Service
public class PricingRecommendationService {

    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    public void updatePropertyPricing() {
        List<Long> propertyIds = propertyService.getAllPropertyIds();

        propertyIds.parallelStream().forEach(propertyId -> {
            Map<String, Object> context = Map.of(
                "property_id", propertyId,
                "market_data", getMarketData(propertyId),
                "competitor_pricing", getCompetitorPricing(propertyId),
                "demand_index", getDemandIndex(propertyId),
                "seasonality", getSeasonality()
            );

            aiPricingService.optimizePrice(context)
                .thenAccept(optimalPrice -> {
                    propertyService.updatePrice(propertyId, optimalPrice.getPrice());
                    logPriceUpdate(propertyId, optimalPrice);
                });
        });
    }
}
```

## Natural Language Processing Integration

### Automated Content Analysis

```java
@Service
public class ContentAnalysisService {

    public CompletableFuture<ContentAnalysis> analyzePropertyListing(
            Long propertyId,
            String description,
            List<String> features) {

        Map<String, Object> request = Map.of(
            "text", description,
            "features", features,
            "language", "en",
            "analysis_types", List.of("sentiment", "keywords", "categorization")
        );

        return nlpService.processText(request)
            .thenApply(analysis -> {
                ContentAnalysis result = new ContentAnalysis();
                result.setSentiment(analysis.get("sentiment"));
                result.setKeywords((List<String>) analysis.get("keywords"));
                result.setCategory((String) analysis.get("category"));
                result.setConfidence((Double) analysis.get("confidence"));
                return result;
            });
    }
}
```

### Sentiment Analysis for Reviews

```java
@Component
public class ReviewSentimentAnalyzer {

    @EventListener
    public void analyzeNewReview(ReviewCreatedEvent event) {
        analyzeSentiment(event.getReviewId(), event.getReviewText())
            .thenAccept(sentiment -> {
                reviewRepository.updateSentiment(event.getReviewId(), sentiment);
                updatePropertyRating(event.getPropertyId(), sentiment);
            });
    }

    private CompletableFuture<Sentiment> analyzeSentiment(Long reviewId, String text) {
        Map<String, Object> request = Map.of(
            "text", text,
            "model", "bert-based-sentiment",
            "granularity", "sentence"
        );

        return sentimentAnalysisService.analyze(request);
    }
}
```

## Computer Vision Integration

### Property Image Analysis

```java
@Service
public class ImageAnalysisService {

    public CompletableFuture<ImageAnalysis> analyzePropertyImage(
            Long propertyId,
            String imageUrl) {

        Map<String, Object> request = Map.of(
            "image_url", imageUrl,
            "analysis_types", List.of(
                "object_detection",
                "scene_recognition",
                "quality_assessment",
                "feature_extraction"
            )
        );

        return computerVisionService.analyzeImage(request)
            .thenApply(analysis -> {
                ImageAnalysis result = new ImageAnalysis();
                result.setDetectedObjects((List<Detection>) analysis.get("objects"));
                result.setSceneType((String) analysis.get("scene"));
                result.setQualityScore((Double) analysis.get("quality"));
                result.setFeatures((Map<String, Double>) analysis.get("features"));
                return result;
            });
    }
}
```

### Automated Image Tagging

```java
@Scheduled(fixedRate = 60000) // Every minute
public void processPendingImages() {
    List<Image> pendingImages = imageRepository.findPendingProcessing();

    pendingImages.parallelStream().forEach(image -> {
        taggingService.autoTagImage(image.getUrl())
            .thenAccept(tags -> {
                image.setTags(tags);
                image.setProcessed(true);
                imageRepository.save(image);

                // Update search index
                searchIndexService.updateImageTags(image.getId(), tags);
            });
    });
}
```

## Machine Learning Model Management

### Model Training and Deployment

```java
@Service
public class ModelManagementService {

    public void trainNewModel(ModelTrainingRequest request) {
        // 1. Prepare training data
        Dataset trainingData = dataPreparationService.prepare(
            request.getDataSource(),
            request.getFeatures()
        );

        // 2. Train model
        CompletableFuture<Model> trainingFuture = CompletableFuture.supplyAsync(() -> {
            return trainModel(trainingData, request.getAlgorithm());
        });

        // 3. Evaluate model
        trainingFuture.thenCompose(model -> {
            return evaluateModel(model, request.getTestData())
                .thenApply(metrics -> {
                    model.setMetrics(metrics);
                    return model;
                });
        });

        // 4. Deploy model if it meets criteria
        trainingFuture.thenAccept(model -> {
            if (model.getAccuracy() > request.getMinAccuracy()) {
                deployModel(model, request.getEnvironment());
            }
        });
    }
}
```

### Model Monitoring and Retraining

```java
@Component
public class ModelMonitoringService {

    @Scheduled(cron = "0 0 * * * *") // Daily
    public void monitorModelPerformance() {
        List<Model> deployedModels = modelRepository.findDeployedModels();

        deployedModels.forEach(model -> {
            ModelMetrics currentMetrics = evaluateModelPerformance(model);
            ModelMetrics trainingMetrics = model.getTrainingMetrics();

            // Calculate performance drift
            double drift = calculatePerformanceDrift(trainingMetrics, currentMetrics);

            if (drift > 0.1) { // 10% performance drop
                log.warn("Model performance drift detected for: {}", model.getName());

                // Trigger retraining
                triggerModelRetraining(model);
            }
        });
    }
}
```

## AI Integration in Dashboard

### AI-powered Dashboard Widgets

```java
@RestController
@RequestMapping("/api/v1/ai/dashboard")
public class AIDashboardController {

    @Autowired
    private PredictiveAnalyticsService predictiveService;

    @Autowired
    private AnomalyDetectionService anomalyService;

    @GetMapping("/predictions")
    public ApiResponse<List<WidgetData>> getPredictions(@RequestParam String type) {
        List<WidgetData> widgets = new ArrayList<>();

        switch (type) {
            case "traffic":
                predictiveService.predictTraffic("api-gateway", "24h", Map.of())
                    .thenAccept(prediction -> {
                        widgets.add(createTrafficWidget(prediction));
                    });
                break;

            case "resources":
                predictiveService.predictResourceUsage("cpu", "7d", Map.of())
                    .thenAccept(prediction -> {
                        widgets.add(createResourceWidget(prediction));
                    });
                break;

            case "anomalies":
                anomalyService.getRecentAnomalies()
                    .thenAccept(anomalies -> {
                        widgets.add(createAnomalyWidget(anomalies));
                    });
                break;
        }

        return ApiResponse.success(widgets);
    }
}
```

### Real-time AI Insights

```java
@KafkaListener(topics = "ai-insights")
public void processAIInsight(AIInsight insight) {
    // Format insight for dashboard
    Map<String, Object> dashboardMessage = Map.of(
        "type", "ai-insight",
        "category", insight.getCategory(),
        "title", insight.getTitle(),
        "description", insight.getDescription(),
        "confidence", insight.getConfidence(),
        "timestamp", Instant.now()
    );

    // Send to dashboard clients
    webSocketHandler.broadcastMessage(dashboardMessage);

    // Store insight for historical analysis
    insightRepository.save(insight);
}
```

## Configuration and Tuning

### AI Service Performance Tuning

```yaml
# AI Performance Configuration
ai:
  performance:
    batch-size: 32
    num-threads: 4
    gpu-acceleration: false
    model-cache-ttl: 3600

  model-management:
    auto-retrain: true
    performance-threshold: 0.8
    drift-threshold: 0.1
    retrain-interval: 86400000  # 24 hours
```

### Custom AI Service Implementation

```java
@Configuration
public class AIServiceConfiguration {

    @Bean
    @ConditionalOnProperty(name = "ai.service.custom.enabled", havingValue = "true")
    public CustomAIService customAIService() {
        return new CustomAIService();
    }
}
```

## Testing AI Integration

### Mock AI Services for Testing

```java
@TestConfiguration
public class AIServiceTestConfiguration {

    @Bean
    @Primary
    public PredictiveAnalyticsService mockPredictiveService() {
        PredictiveAnalyticsService mock = mock(PredictiveAnalyticsService.class);

        when(mock.predictResourceUsage(anyString(), anyString(), anyMap()))
            .thenReturn(CompletableFuture.completedFuture(
                ResourcePrediction.builder()
                    .resourceType("cpu")
                    .averageValue(BigDecimal.valueOf(75.5))
                    .peakValue(BigDecimal.valueOf(95.2))
                    .confidence(0.92)
                    .build()
            ));

        return mock;
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@TestPropertySource(properties = {
    "ai.services.predictive-analytics.url=http://localhost:8081",
    "ai.services.anomaly-detection.url=http://localhost:8082"
})
class AIIntegrationTest {

    @Autowired
    private AIIntegrationService aiService;

    @Test
    void shouldPredictResourceUsage() {
        Map<String, Object> features = Map.of(
            "cpu_usage", 65.5,
            "memory_usage", 78.2,
            "request_rate", 1250.0
        );

        CompletableFuture<ResourcePrediction> future = aiService
            .predictResourceUsage("cpu", "24h", features);

        ResourcePrediction prediction = future.join();

        assertNotNull(prediction);
        assertTrue(prediction.getConfidence() > 0.8);
        assertNotNull(prediction.getAverageValue());
    }
}
```

## Deployment Considerations

### Resource Requirements

```yaml
# AI Services Resource Requirements
ai:
  resources:
    predictive-analytics:
      cpu: "2000m"
      memory: "4Gi"
      gpu: "1"
    anomaly-detection:
      cpu: "1000m"
      memory: "2Gi"
    computer-vision:
      cpu: "4000m"
      memory: "8Gi"
      gpu: "2"
```

### Model Storage

```yaml
# Model Storage Configuration
storage:
  models:
    type: "s3"
    bucket: "gogidix-ml-models"
    path: "/models"
    backup:
      enabled: true
      retention: 90
```

## Best Practices

1. **Model Versioning**: Always version your models
2. **A/B Testing**: Test new models before full deployment
3. **Performance Monitoring**: Monitor model performance continuously
4. **Data Quality**: Ensure high-quality training data
5. **Privacy Protection**: Anonymize sensitive data before training

## Troubleshooting

### Common Issues

1. **Model Loading Failures**
   - Check model file permissions
   - Verify model format compatibility
   - Review TensorFlow version compatibility

2. **Memory Issues**
   - Increase JVM heap size
   - Optimize batch sizes
   - Use model quantization

3. **Performance Issues**
   - Enable GPU acceleration
   - Implement request batching
   - Use model caching

## Support

- AI Services Documentation: https://docs.gogidix.com/ai-services
- Model Registry: https://ml.gogidix.com
- Support: ai-services@gogidix.com

## Future Enhancements

1. **Deep Learning Models**: More sophisticated neural networks
2. **Edge AI**: Deploy models closer to data sources
3. **AutoML**: Automated machine learning pipeline
4. **Explainable AI**: Model interpretability
5. **Federated Learning**: Privacy-preserving ML

## Version History

- **1.0.0**: Initial release with basic AI services
- **1.1.0**: Added predictive analytics
- **1.2.0**: Enhanced anomaly detection
- **1.3.0**: Added computer vision capabilities
- **1.4.0**: Integrated NLP services

---

*This integration guide demonstrates how AI services can enhance the Gogidix Property Marketplace with intelligent capabilities, predictive analytics, and automated decision-making.*