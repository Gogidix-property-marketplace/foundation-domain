# Agent B - Centralized Dashboard Integration Guide

## Overview

This guide provides comprehensive instructions for integrating Agent B (Centralized Dashboard) with all infrastructure and monitoring systems in the Gogidix Property Marketplace.

## Architecture

The centralized dashboard integrates with:

1. **Prometheus** - Metrics collection and time-series data
2. **Grafana** - Visualization and alerting
3. **ELK Stack** - Centralized logging and analysis
4. **Jaeger** - Distributed tracing
5. **WebSocket** - Real-time updates
6. **Custom Integration Service** - Aggregation and data processing

## Components

### 1. Dashboard Integration Service

A dedicated Spring Boot service that:
- Aggregates metrics from Prometheus
- Provides WebSocket endpoints for real-time updates
- Caches frequently accessed data
- Processes and transforms metrics data

### 2. Existing Dashboard Services

- **centralized-dashboard** - Main dashboard service
- **analytics-service** - Analytics and reporting
- **metrics-service** - Custom metrics collection
- **alert-management-service** - Alert handling
- **executive-dashboard** - Executive view
- **agent-dashboard-service** - Agent-specific dashboard
- **provider-dashboard-service** - Provider-specific dashboard

## Quick Start

### 1. Deploy the Dashboard Integration Service

```bash
cd shared-infrastructure/java-services/dashboard-integration-service
mvn clean package -DskipTests
docker build -t gogidix/dashboard-integration:latest .
kubectl apply -f k8s/
```

### 2. Configure Connection to Monitoring Systems

Update `application.yml`:

```yaml
server:
  port: 8090

spring:
  application:
    name: dashboard-integration

# Prometheus Configuration
prometheus:
  url: http://prometheus:9090
  queries:
    health: "up{job=~\".+\"}"
    request-rate: "rate(http_requests_total{job=~\".+\"}[5m])"
    error-rate: "rate(http_requests_total{job=~\".+\",status=~\"5..\"}[5m])"
    response-time: "histogram_quantile(0.95, http_request_duration_seconds)"

# Grafana Configuration
grafana:
  url: http://grafana:3000
  username: admin
  password: gogidix123

# WebSocket Configuration
websocket:
  allowed-origins: "*"
  heartbeat-interval: 30s

# Caching Configuration
cache:
  metrics-ttl: 30s
  system-overview-ttl: 10s

# Kafka Configuration for Events
kafka:
  bootstrap-servers: kafka:9092
  topics:
    metrics-updates: dashboard-metrics
    alerts: dashboard-alerts
```

### 3. Add to Docker Compose

```yaml
dashboard-integration:
  image: gogidix/dashboard-integration:latest
  ports:
    - "8090:8090"
  environment:
    - PROMETHEUS_URL=http://prometheus:9090
    - GRAFANA_URL=http://grafana:3000
    - GRAFANA_USERNAME=admin
    - GRAFANA_PASSWORD=gogidix123
  depends_on:
    - prometheus
    - grafana
  networks:
    - gogidix-network
```

## WebSocket API

### Connect to Dashboard

```javascript
const socket = new SockJS('/ws/dashboard');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // Subscribe to real-time metrics
    stompClient.subscribe('/topic/metrics', function(message) {
        const metrics = JSON.parse(message.body);
        updateDashboard(metrics);
    });

    // Subscribe to alerts
    stompClient.subscribe('/topic/alerts', function(message) {
        const alert = JSON.parse(message.body);
        showAlert(alert);
    });
});
```

### WebSocket Message Format

Metrics Update:
```json
{
    "type": "metrics",
    "query": "up{job=~\".+\"}",
    "data": {
        "metrics": [
            {
                "metric": {"job": "api-gateway", "instance": "localhost:8080"},
                "value": 1.0,
                "timestamp": 1640995200000
            }
        ]
    },
    "timestamp": "2023-12-31T23:59:59.999Z"
}
```

Alert Message:
```json
{
    "type": "alert",
    "level": "critical",
    "service": "api-gateway",
    "message": "High error rate detected",
    "value": "15%",
    "threshold": "5%",
    "timestamp": "2023-12-31T23:59:59.999Z"
}
```

## REST API Endpoints

### Get System Overview

```bash
GET /api/v1/system/overview

Response:
{
    "totalServices": 15,
    "totalRequestRate": 1250.5,
    "averageResponseTime": 145.2,
    "errorRate": 0.8,
    "cpuUsage": 65.2,
    "memoryUsage": 78.5,
    "diskUsage": 45.3,
    "timestamp": "2023-12-31T23:59:59.999Z"
}
```

### Get Service Metrics

```bash
GET /api/v1/services/{serviceName}/metrics

Response:
{
    "serviceName": "api-gateway",
    "healthy": true,
    "requestRate": 450.2,
    "errorRate": 0.5,
    "averageResponseTime": 120.5,
    "cpuUsage": 45.2,
    "memoryUsage": 65.8,
    "customMetrics": {
        "activeSessions": 1250,
        "cacheHitRate": 85.5
    }
}
```

### Get Time Series Data

```bash
GET /api/v1/metrics/range?query=rate(http_requests_total[5m])&start=2023-12-31T00:00:00Z&end=2023-12-31T23:59:59Z&step=1m

Response:
{
    "timeSeries": [
        {
            "metric": {"job": "api-gateway"},
            "dataPoints": [
                {"timestamp": "2023-12-31T00:00:00", "value": 425.5},
                {"timestamp": "2023-12-31T00:01:00", "value": 432.1}
            ]
        }
    ]
}
```

## Dashboard Views

### 1. Executive Dashboard

```java
@RestController
@RequestMapping("/api/v1/executive")
public class ExecutiveDashboardController {

    @Autowired
    private ExecutiveDashboardService executiveService;

    @GetMapping("/kpi")
    public ApiResponse<ExecutiveKPI> getKPIs() {
        ExecutiveKPI kpis = executiveService.calculateKPIs();
        return ApiResponse.success(kpis);
    }

    @GetMapping("/sla")
    public ApiResponse<SLAMetrics> getSLAMetrics() {
        SLAMetrics sla = executiveService.getSLAMetrics();
        return ApiResponse.success(sla);
    }
}
```

### 2. Service Health Dashboard

```java
@RestController
@RequestMapping("/api/v1/health")
public class ServiceHealthController {

    @GetMapping("/services")
    public ApiResponse<List<ServiceHealth>> getAllServiceHealth() {
        List<ServiceHealth> services = healthService.getAllServices();
        return ApiResponse.success(services);
    }

    @GetMapping("/services/{serviceName}")
    public ApiResponse<ServiceHealthDetail> getServiceHealth(@PathVariable String serviceName) {
        ServiceHealthDetail health = healthService.getServiceDetail(serviceName);
        return ApiResponse.success(health);
    }
}
```

### 3. Analytics Dashboard

```java
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    @GetMapping("/traffic")
    public ApiResponse<TrafficAnalytics> getTrafficAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        TrafficAnalytics analytics = analyticsService.getTrafficAnalytics(start, end);
        return ApiResponse.success(analytics);
    }

    @GetMapping("/performance")
    public ApiResponse<PerformanceAnalytics> getPerformanceAnalytics(
            @RequestParam String timeRange) {
        PerformanceAnalytics analytics = analyticsService.getPerformanceAnalytics(timeRange);
        return ApiResponse.success(analytics);
    }
}
```

## Integration with Central Configuration

### Dashboard Configuration from Config Server

```yaml
# dashboard.yml in Config Server
dashboard:
  widgets:
    - type: metric-chart
      title: Request Rate
      query: rate(http_requests_total[5m])
      refresh: 5s

    - type: gauge
      title: CPU Usage
      query: avg(100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100))
      max: 100

    - type: table
      title: Service Health
      query: up{job=~".+"}
      columns:
        - name: Service
          field: job
        - name: Status
          field: value
          mapping:
            "1": "UP"
            "0": "DOWN"
```

### Load Configuration

```java
@Service
@RefreshScope
public class DashboardConfigurationService {

    @Value("${dashboard.widgets}")
    private List<WidgetConfig> widgets;

    public List<WidgetConfig> getWidgets() {
        return widgets;
    }
}
```

## Advanced Features

### 1. Custom Metrics Registration

```java
@Component
public class CustomMetricsCollector {

    private final MeterRegistry meterRegistry;

    public CustomMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        registerMetrics();
    }

    private void registerMetrics() {
        // Custom gauge for business metrics
        Gauge.builder("properties.listed")
            .description("Number of properties listed")
            .register(meterRegistry, this, CustomMetricsCollector::getPropertiesCount);

        // Custom counter
        Counter.builder("bookings.completed")
            .description("Completed bookings")
            .tag("type", "property")
            .register(meterRegistry);
    }

    private double getPropertiesCount() {
        // Return current properties count
        return propertyService.getCount();
    }
}
```

### 2. Alert Integration

```java
@Service
public class AlertService {

    @KafkaListener(topics = "dashboard-alerts")
    public void handleAlert(AlertMessage alert) {
        // Process alert
        processAlert(alert);

        // Send to WebSocket clients
        Map<String, Object> message = Map.of(
            "type", "alert",
            "data", alert,
            "timestamp", Instant.now()
        );
        webSocketHandler.broadcastMessage(message);

        // Create incident in system
        if (alert.getLevel().equals("critical")) {
            incidentService.create(alert);
        }
    }
}
```

### 3. Performance Optimization

```java
@Configuration
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(1000));
        return cacheManager;
    }

    @Bean
    public RestTemplate prometheusRestTemplate() {
        RestTemplate template = new RestTemplate();
        template.setMessageConverters(List.of(
            new StringHttpMessageConverter(),
            new MappingJackson2HttpMessageConverter()
        ));
        return template;
    }
}
```

## Testing

### Unit Test Example

```java
@SpringBootTest
@TestPropertySource(properties = {
    "prometheus.url=http://localhost:9090"
})
class PrometheusIntegrationServiceTest {

    @Autowired
    private PrometheusIntegrationService prometheusService;

    @MockBean
    private PrometheusClient prometheusClient;

    @Test
    void shouldGetSystemOverview() {
        // Given
        PrometheusResponse response = createMockResponse();
        when(prometheusClient.query(anyString())).thenReturn(response);

        // When
        CompletableFuture<SystemOverviewMetrics> future = prometheusService.getSystemOverview();

        // Then
        SystemOverviewMetrics metrics = future.join();
        assertThat(metrics.getTotalServices()).isGreaterThan(0);
    }
}
```

### Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DashboardIntegrationIntegrationTest {

    @Container
    static GenericContainer<?> prometheus = new GenericContainer<>("prom/prometheus:latest")
            .withExposedPorts(9090);

    @Test
    void shouldConnectToPrometheus() {
        // Test real connection to Prometheus
        // Verify metrics are being fetched
    }
}
```

## Deployment

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dashboard-integration
spec:
  replicas: 3
  selector:
    matchLabels:
      app: dashboard-integration
  template:
    spec:
      containers:
      - name: dashboard-integration
        image: gogidix/dashboard-integration:latest
        ports:
        - containerPort: 8090
        env:
        - name: PROMETHEUS_URL
          value: "http://prometheus.monitoring.svc.cluster.local:9090"
        - name: GRAFANA_URL
          value: "http://grafana.monitoring.svc.cluster.local:3000"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8090
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8090
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: dashboard-integration
spec:
  selector:
    app: dashboard-integration
  ports:
  - port: 8090
    targetPort: 8090
  type: LoadBalancer
```

## Monitoring the Dashboard

### Dashboard Metrics

```yaml
# Add to prometheus.yml
- job_name: 'dashboard-integration'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['dashboard-integration:8090']
```

### Grafana Dashboard

Import the pre-configured dashboard:
```bash
curl -X POST \
  http://admin:gogidix123@localhost:3000/api/dashboards/db \
  -H 'Content-Type: application/json' \
  -d @dashboard-dashboard.json
```

## Troubleshooting

### Common Issues

1. **WebSocket Connection Fails**
   - Check if WebSocket is enabled in configuration
   - Verify firewall allows WebSocket connections
   - Check SockJS configuration

2. **Metrics Not Updating**
   - Verify Prometheus connection
   - Check query syntax
   - Review cache configuration

3. **High Memory Usage**
   - Optimize cache TTL values
   - Limit time range for historical data
   - Implement pagination for large datasets

### Debug Configuration

```properties
# Enable debug logging
logging:
  level:
    com.ogidix.infrastructure.dashboard: DEBUG
    org.springframework.web.socket: DEBUG
    feign: DEBUG
```

## Best Practices

1. **Query Optimization**
   - Use efficient Prometheus queries
   - Cache frequently accessed metrics
   - Limit query time ranges

2. **Real-time Updates**
   - Use WebSocket for live data
   - Implement rate limiting for updates
   - Aggregate data before sending

3. **Security**
   - Secure WebSocket connections
   - Implement role-based access
   - Validate all input data

4. **Performance**
   - Use asynchronous operations
   - Implement circuit breakers
   - Monitor dashboard performance

## Integration with Central Configuration Services

The dashboard integrates with central configuration services:

```java
@Service
public class ConfigIntegrationService {

    @Autowired
    private ConfigServerClient configClient;

    @Scheduled(fixedRate = 60000)
    public void refreshDashboardConfig() {
        Map<String, Object> config = configClient.getConfiguration(
            "dashboard",
            "production"
        );
        updateDashboardConfiguration(config);
    }
}
```

## Support

For issues and questions:

- Documentation: https://docs.gogidix.com/dashboard
- API Reference: https://api-docs.gogidix.com/dashboard
- Support: dashboard@gogidix.com

## Version History

- **1.0.0**: Initial release with Prometheus integration
- **1.1.0**: Added WebSocket support
- **1.2.0**: Enhanced caching and performance
- **1.3.0**: Added custom metrics support

## Roadmap

Future enhancements:

1. Machine learning for anomaly detection
2. Predictive analytics
3. Mobile dashboard support
4. Advanced alerting capabilities
5. Integration with more monitoring tools