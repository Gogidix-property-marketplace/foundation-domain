# Central Configuration Integration Guide

This guide explains how to integrate domain services with the central configuration system (Config Server, Eureka, and Zipkin).

## Overview

The central configuration system provides:
- **Spring Cloud Config Server**: Centralized configuration management
- **Eureka Server**: Service discovery and registration
- **Zipkin Server**: Distributed tracing

## Prerequisites

1. Add required dependencies to your service's `pom.xml`
2. Configure bootstrap configuration
3. Enable service discovery
4. Configure distributed tracing

## Dependencies

Add these dependencies to your microservice:

```xml
<dependencies>
    <!-- Spring Cloud Config Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>

    <!-- Eureka Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <!-- Spring Cloud Bus for Config Refresh -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-bus-amqp</artifactId>
    </dependency>

    <!-- Distributed Tracing -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-sleuth</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-sleuth-zipkin</artifactId>
    </dependency>

    <!-- Actuator for Management Endpoints -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2022.0.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Configuration

### 1. Create bootstrap.yml

Create `src/main/resources/bootstrap.yml` in your service:

```yaml
spring:
  application:
    name: your-service-name  # This must match the config file name

  cloud:
    config:
      uri: http://config-admin:config-secret@config-server:8007/config
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 6
        max-interval: 2000
      health:
        enabled: true

# Service Discovery
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka
    register-with-eureka: true
    fetch-registry: true
    registry-fetch-interval-seconds: 30
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
    metadata-map:
      version: ${project.version:1.0.0}
      environment: ${ENVIRONMENT:dev}

# Tracing
spring:
  sleuth:
    sampler:
      probability: ${SLEUTH_SAMPLER_PROBABILITY:0.1}
    zipkin:
      base-url: http://zipkin-server:9411
```

### 2. Create/Update application.yml

```yaml
server:
  port: ${SERVER_PORT:0}  # 0 for random port in local development

spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

# Management Endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,refresh,bus-refresh
  endpoint:
    health:
      show-details: always
    refresh:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

# Logging
logging:
  level:
    org.springframework.cloud.config: DEBUG
    com.netflix.eureka: DEBUG
    org.springframework.cloud.sleuth: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%X{traceId},%X{spanId}] [%thread] %-5level %logger{36} - %msg%n"
```

## Service Configuration Files

Create configuration files in the Config Server's config repository:

### your-service-name.yml
```yaml
# Your Service Configuration
server:
  port: 0

spring:
  # Database
  datasource:
    url: jdbc:postgresql://postgres:5432/gogidix_property
    username: gogidix_admin
    password: gogidix123
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000

  # Redis
  redis:
    host: redis
    port: 6379
    password: gogidix123
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8

  # Kafka
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
    producer:
      acks: all
      retries: 3

# Business Logic Configuration
your-service:
  config:
    property1: value1
    property2: value2
    feature-flags:
      new-feature: false
      experimental-feature: true
```

### your-service-name-prod.yml
```yaml
# Production-specific overrides
spring:
  datasource:
    url: ${PROD_DB_URL}
    username: ${PROD_DB_USERNAME}
    password: ${PROD_DB_PASSWORD}
    hikari:
      maximum-pool-size: 50

your-service:
  config:
    feature-flags:
      new-feature: true
```

## Enable Service Discovery

Add `@EnableDiscoveryClient` annotation to your main application class:

```java
package com.gogidix.yourservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

## Using External Configuration

### 1. Configuration Properties

Create a configuration properties class:

```java
@ConfigurationProperties(prefix = "your-service.config")
@RefreshScope
@Component
@Data
public class YourServiceProperties {
    private String property1;
    private String property2;
    private FeatureFlags featureFlags = new FeatureFlags();

    @Data
    public static class FeatureFlags {
        private boolean newFeature = false;
        private boolean experimentalFeature = false;
    }
}
```

### 2. Enable Configuration Properties

Add to your main application class:
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(YourServiceProperties.class)
public class YourServiceApplication {
    // ...
}
```

### 3. Refresh Configuration

Use Spring Cloud Bus to refresh configurations without restarting:

```bash
# Refresh all services
curl -X POST http://localhost:8080/actuator/bus-refresh

# Refresh specific service
curl -X POST http://localhost:8080/actuator/refresh
```

## Service-to-Service Communication

### 1. Using Discovery Client

```java
@Service
public class ExternalServiceClient {
    @Autowired
    private DiscoveryClient discoveryClient;

    public String getServiceUrl(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            throw new ServiceUnavailableException(serviceName + " is not available");
        }
        return instances.get(0).getUri().toString();
    }
}
```

### 2. Using Load Balancer

```java
@Configuration
public class AppConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class ServiceClient {
    @Autowired
    @LoadBalanced
    private RestTemplate restTemplate;

    public String callOtherService() {
        return restTemplate.getForObject(
            "http://other-service/api/endpoint",
            String.class
        );
    }
}
```

## Environment Variables

Set these environment variables when running your service:

```bash
# Configuration Server
CONFIG_SERVER_URI=http://config-admin:config-secret@config-server:8007/config
CONFIG_SERVER_USER=config-admin
CONFIG_SERVER_PASSWORD=config-secret

# Service Discovery
EUREKA_SERVER_URL=http://eureka-server:8761/eureka

# Tracing
SLEUTH_SAMPLER_PROBABILITY=0.1
ZIPKIN_BASE_URL=http://zipkin-server:9411

# Application Specific
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
ENVIRONMENT=development
```

## Docker Integration

Add to your service's Dockerfile:

```dockerfile
FROM openjdk:21-jdk-slim

# Add your application jar
COPY target/your-service-*.jar app.jar

# Environment variables
ENV CONFIG_SERVER_URI=http://config-admin:config-secret@config-server:8007/config
ENV EUREKA_SERVER_URL=http://eureka-server:8761/eureka
ENV ZIPKIN_BASE_URL=http://zipkin-server:9411

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Kubernetes Integration

Use ConfigMaps for environment-specific configuration:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: your-service-config
data:
  SPRING_PROFILES_ACTIVE: "production"
  EUREKA_SERVER_URL: "http://eureka-server:8761/eureka"
  ZIPKIN_BASE_URL: "http://zipkin-server:9411"
  SLEUTH_SAMPLER_PROBABILITY: "0.1"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: your-service
spec:
  template:
    spec:
      containers:
      - name: your-service
        image: your-service:latest
        envFrom:
        - configMapRef:
            name: your-service-config
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

## Testing Configuration

### 1. Unit Tests

```java
@SpringBootTest
@ActiveProfiles("test")
class YourServiceConfigurationTest {
    @Autowired
    private YourServiceProperties properties;

    @Test
    void testConfigurationLoading() {
        assertNotNull(properties);
        assertEquals("expected-value", properties.getProperty1());
    }
}
```

### 2. Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "CONFIG_SERVER_URI=http://localhost:8888",
    "EUREKA_SERVER_URL=http://localhost:8761"
})
class ServiceIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testServiceHealth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/health",
            String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

## Troubleshooting

### Common Issues

1. **Config Server Connection Failed**
   - Check if Config Server is running
   - Verify credentials in bootstrap.yml
   - Check network connectivity

2. **Service Not Registering with Eureka**
   - Verify Eureka server URL
   - Check application name matches
   - Ensure eureka.client.enabled=true

3. **Tracing Not Working**
   - Check Zipkin server is running
   - Verify sampler probability
   - Check RabbitMQ connection for Sleuth

### Debug Commands

```bash
# Check Eureka registered services
curl http://localhost:8761/eureka/apps

# Check Config Server health
curl http://localhost:8007/actuator/health

# Get service configuration
curl http://localhost:8007/config/your-service-name/dev

# Refresh configuration
curl -X POST http://localhost:8080/actuator/refresh
```

## Best Practices

1. **Version Control**: Version your configuration files in Git
2. **Environment Separation**: Use profiles for different environments
3. **Sensitive Data**: Use Vault for secrets, not Config Server
4. **Health Checks**: Always implement health checks
5. **Graceful Shutdown**: Handle configuration refresh properly
6. **Circuit Breaker**: Use with service discovery calls
7. **Retry Logic**: Implement retry for config server failures

## Migration Steps

1. Add dependencies to existing services
2. Create bootstrap.yml
3. Update application.yml
4. Create configuration files in Config Server
5. Test with dev profile
6. Migrate to prod profile
7. Update deployment scripts

## Monitoring

Monitor these metrics:
- Config server health
- Eureka registry status
- Service registration/deregistration
- Configuration refresh events
- Zipkin trace collection

## Security

1. Secure Config Server with authentication
2. Use HTTPS for all communication
3. Encrypt sensitive configuration values
4. Limit who can refresh configurations
5. Audit configuration changes