# Agent A - Shared Libraries Integration Guide

## Overview

This guide provides comprehensive instructions for integrating the shared libraries created by Agent A (Platform Architect) with all domain services and infrastructure components.

## Shared Libraries Architecture

The shared libraries follow enterprise-grade patterns and include:

1. **gogidix-common-core** - Core utilities, response patterns, exception handling
2. **gogidix-common-security** - Security, authentication, authorization
3. **gogidix-common-messaging** - Event-driven architecture messaging
4. **gogidix-common-api-client** - Type-safe API clients for infrastructure services

## Quick Start

### 1. Add Dependencies

Add the shared library dependency to your service's pom.xml:

```xml
<dependencies>
    <!-- Core Library -->
    <dependency>
        <groupId>com.gogidix.platform</groupId>
        <artifactId>gogidix-common-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- Security Library -->
    <dependency>
        <groupId>com.gogidix.platform</groupId>
        <artifactId>gogidix-common-security</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- Messaging Library -->
    <dependency>
        <groupId>com.gogidix.platform</groupId>
        <artifactId>gogidix-common-messaging</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- API Client Library -->
    <dependency>
        <groupId>com.gogidix.platform</groupId>
        <artifactId>gogidix-common-api-client</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 2. Enable Shared Libraries

Add the @EnableSharedLibraries annotation to your main application class:

```java
package com.gogidix.yourservice;

import com.gogidix.platform.core.EnableSharedLibraries;
import com.gogidix.platform.security.EnableSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableSharedLibraries
@EnableSecurity
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

## Core Library Features

### 1. Standard Response Pattern

```java
@RestController
public class YourController {

    @GetMapping("/resource")
    public ApiResponse<YourDTO> getResource() {
        YourDTO data = service.getData();
        return ApiResponse.success(data);
    }

    @PostMapping("/resource")
    public ApiResponse<YourDTO> createResource(@RequestBody CreateRequest request) {
        try {
            YourDTO created = service.create(request);
            return ApiResponse.created(created);
        } catch (ValidationException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }
}
```

### 2. Exception Handling

```java
@Service
public class YourService {

    public YourDTO processData(Long id) {
        YourEntity entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Resource not found with id: " + id
            ));

        // Process and return
        return convertToDTO(entity);
    }
}
```

### 3. Validation

```java
public class CreateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @Valid
    private Address address;

    // Custom validation
    @ValidPhone
    private String phoneNumber;
}
```

## Security Library Features

### 1. JWT Authentication

```java
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('USER')")
public class SecureController {

    @GetMapping("/profile")
    public UserProfile getProfile(
            @AuthenticationPrincipal UserDetails user) {
        return profileService.getProfile(user.getUsername());
    }

    @PostMapping("/admin/action")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> adminAction() {
        // Admin-only operation
        return ApiResponse.success();
    }
}
```

### 2. API Key Authentication

```java
@Service
public class ExternalApiClient {

    @SecureApiClient
    public void callExternalService() {
        // Method secured with API key
    }
}
```

### 3. Rate Limiting

```java
@RestController
@RequestMapping("/api/v1")
@RateLimited(value = "10/m", headers = "X-API-Key")
public class RateLimitedController {

    @GetMapping("/data")
    public ApiResponse<Data> getData() {
        // Rate limited to 10 requests per minute
    }
}
```

## Messaging Library Features

### 1. Event Publishing

```java
@Service
public class YourService {

    @Autowired
    private DomainEventPublisher eventPublisher;

    public YourDTO createResource(CreateRequest request) {
        YourEntity entity = new YourEntity(request);
        entity = repository.save(entity);

        // Publish domain event
        eventPublisher.publish(new ResourceCreatedEvent(
            entity.getId(),
            entity.getCreatedBy(),
            LocalDateTime.now()
        ));

        return convertToDTO(entity);
    }
}
```

### 2. Event Handling

```java
@Component
public class ResourceEventHandler {

    @EventHandler
    public void handle(ResourceCreatedEvent event) {
        // Handle resource creation event
        log.info("Resource created: {}", event.getResourceId());

        // Update search index
        searchService.indexResource(event.getResourceId());

        // Send notification
        notificationService.notify(event);
    }
}
```

### 3. Saga Orchestration

```java
@Service
public class OrderSaga {

    @SagaOrchestration(
        sagaType = "ORDER_PROCESSING",
        timeout = Duration.ofMinutes(30)
    )
    public SagaOrchestrationResult processOrder(OrderCommand command) {
        return SagaOrchestration.start()
            .step("validate-order")
                .invoke(() -> orderService.validate(command))
                .compensateWith(() -> orderService.cancel(command.getOrderId()))

            .step("reserve-inventory")
                .invoke(() -> inventoryService.reserve(command))
                .compensateWith(() -> inventoryService.release(command))

            .step("process-payment")
                .invoke(() -> paymentService.process(command))
                .compensateWith(() -> paymentService.refund(command))

            .step("confirm-order")
                .invoke(() -> orderService.confirm(command))

            .execute();
    }
}
```

## API Client Library Features

### 1. Config Server Integration

```java
@Service
public class ConfigurationService {

    @Autowired
    private ConfigServerClient configClient;

    public Map<String, Object> getServiceConfig(String serviceName) {
        try {
            return configClient.getConfiguration(
                serviceName,
                "production",
                "main"
            );
        } catch (ConfigServerException e) {
            log.error("Failed to fetch config for service: {}", serviceName, e);
            return getDefaultConfig();
        }
    }

    @Scheduled(fixedRate = 60000)
    public void refreshConfiguration() {
        configClient.refreshAllServices();
    }
}
```

### 2. Keycloak Integration

```java
@Service
public class UserManagementService {

    @Autowired
    private KeycloakClient keycloakClient;

    private String adminToken;

    @PostConstruct
    private void init() {
        // Get admin token on startup
        TokenResponse token = keycloakClient.getAdminToken(
            new TokenRequest("admin", "admin123")
        );
        this.adminToken = token.getAccessToken();
    }

    public User createAppUser(RegistrationRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        User created = keycloakClient.createUser("gogidix-property", user, adminToken);

        // Set password
        Credentials credentials = new Credentials(
            request.getPassword(),
            "password"
        );
        keycloakClient.setUserPassword("gogidix-property", created.getId(), credentials, adminToken);

        // Assign role
        Role userRole = keycloakClient.getRealmRoles("gogidix-property", adminToken)
            .stream()
            .filter(r -> "user".equals(r.getName()))
            .findFirst()
            .orElseThrow();

        keycloakClient.assignRolesToUser(
            "gogidix-property",
            created.getId(),
            List.of(userRole),
            adminToken
        );

        return created;
    }
}
```

### 3. Vault Integration

```java
@Service
public class SecretManagementService {

    @Autowired
    private VaultClient vaultClient;

    @Value("${vault.token}")
    private String vaultToken;

    @Cacheable(value = "secrets", key = "#path")
    public Map<String, String> getSecret(String path) {
        SecretData secretData = vaultClient.readSecret(path, vaultToken);
        return secretData.getData();
    }

    public String generateDatabaseCredentials(String role) {
        Map<String, Object> credentials = vaultClient.generateDatabaseCredentials(
            role,
            vaultToken
        );

        return String.format(
            "jdbc:postgresql://%s:5432/%s?user=%s&password=%s",
            credentials.get("host"),
            credentials.get("db_name"),
            credentials.get("username"),
            credentials.get("password")
        );
    }

    public String encryptData(String keyName, String plaintext) {
        EncryptRequest request = new EncryptRequest(
            plaintext,
            "base64"
        );

        Map<String, Object> response = vaultClient.encryptData(
            keyName,
            request,
            vaultToken
        );

        return (String) response.get("ciphertext");
    }
}
```

## Configuration

### Application Properties

```properties
# Shared Libraries Configuration
gogidix:
  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key}
      expiration: 86400
    api-key:
      header-name: X-API-Key
      validation-endpoint: /api/v1/validate-key

  messaging:
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      consumer:
        group-id: ${spring.application.name}
      producer:
        retries: 3

  api:
    client:
      config-server:
        url: ${CONFIG_SERVER_URL:http://localhost:8007}
        username: config-admin
        password: config-secret
      keycloak:
        url: ${KEYCLOAK_URL:http://localhost:8080}
        realm: gogidix-property
      vault:
        url: ${VAULT_URL:http://localhost:8200}
        token: ${VAULT_TOKEN:your-vault-token}
```

## Best Practices

### 1. Error Handling

```java
@ControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
```

### 2. Transaction Management

```java
@Service
@Transactional
public class TransactionalService {

    public void complexOperation(Command command) {
        // All operations are in a single transaction
        entity1 = repository1.save(entity1);
        entity2 = repository2.save(entity2);

        // Publish event after commit
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publish(new OperationCompletedEvent(command));
                }
            }
        );
    }
}
```

### 3. Caching

```java
@Service
public class CachedService {

    @Cacheable(value = "resources", key = "#id")
    public Resource getResource(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    @CacheEvict(value = "resources", key = "#id")
    public void deleteResource(Long id) {
        repository.deleteById(id);
    }
}
```

## Testing

### 1. Unit Testing with Test Utilities

```java
@SpringBootTest
class YourServiceTest {

    @Autowired
    private YourService service;

    @Test
    void shouldCreateResource() {
        // Given
        CreateRequest request = TestDataFactory.createRequest();

        // When
        YourDTO result = service.createResource(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(request.getName());

        // Verify event was published
        await().atMost(Duration.ofSeconds(1))
            .untilAsserted(() ->
                verify(eventPublisher).publish(any(ResourceCreatedEvent.class))
            );
    }
}
```

### 2. Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateResourceViaApi() {
        // Given
        CreateRequest request = TestDataFactory.createRequest();

        // When
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/resources",
            request,
            ApiResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
```

## Migration Guide

### From Legacy Code

1. **Replace Response Wrappers**:
   ```java
   // Old
   @GetMapping
   public ResponseEntity<ResponseEntity> get() {
       return ResponseEntity.ok(new ResponseEntity(data));
   }

   // New
   @GetMapping
   public ApiResponse<YourDTO> get() {
       return ApiResponse.success(data);
   }
   ```

2. **Use Shared Exception Classes**:
   ```java
   // Old
   throw new RuntimeException("Not found");

   // New
   throw new ResourceNotFoundException("Not found");
   ```

3. **Use Event Publishing**:
   ```java
   // Old
   otherService.notify(event);

   // New
   eventPublisher.publish(event);
   ```

## Monitoring and Observability

### 1. Metrics

```java
@Component
public class CustomMetrics {

    private final Counter requestCounter;
    private final Timer requestTimer;

    public CustomMetrics(MeterRegistry meterRegistry) {
        this.requestCounter = Counter.builder("api_requests")
            .tag("service", "your-service")
            .register(meterRegistry);

        this.requestTimer = Timer.builder("api_request_duration")
            .tag("service", "your-service")
            .register(meterRegistry);
    }

    public void recordRequest(String endpoint, Duration duration) {
        requestCounter.increment(Tags.of("endpoint", endpoint));
        requestTimer.record(duration);
    }
}
```

### 2. Health Checks

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Check external dependencies
        boolean isHealthy = checkDependencies();

        if (isHealthy) {
            return Health.up()
                .withDetail("dependencies", "All systems operational")
                .build();
        } else {
            return Health.down()
                .withDetail("dependencies", "Some services unavailable")
                .build();
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **JWT Token Validation Failed**
   - Check clock skew between services
   - Verify shared secret is correct
   - Ensure token is not expired

2. **Circuit Breaker Open**
   - Check downstream service health
   - Review failure threshold configuration
   - Check timeout settings

3. **Event Not Published**
   - Verify Kafka connectivity
   - Check serializer configuration
   - Ensure topic exists

### Debug Configuration

```properties
# Enable debug logging for shared libraries
logging:
  level:
    com.gogidix: DEBUG
    org.springframework.cloud.openfeign: DEBUG
    io.github.resilience4j: DEBUG
```

## Support

For issues and questions regarding the shared libraries:

- Documentation: https://docs.gogidix.com/shared-libraries
- API Reference: https://api-docs.gogidix.com/shared-libraries
- Examples: https://github.com/gogidix/shared-library-examples
- Support: shared-libraries@gogidix.com

## Version History

- **1.0.0-SNAPSHOT**: Initial release with core libraries
- **1.1.0-SNAPSHOT**: Added API client library
- **1.2.0-SNAPSHOT**: Enhanced security features
- **1.3.0-SNAPSHOT**: Added saga orchestration support

## Roadmap

Upcoming features in shared libraries:

1. **gogidix-common-data** - Repository patterns and data access
2. **gogidix-common-monitoring** - Enhanced metrics and tracing
3. **gogidix-common-test** - Shared test utilities and fixtures
4. **gogidix-common-validation** - Advanced validation rules
5. **gogidix-common-graphql** - GraphQL client and server support