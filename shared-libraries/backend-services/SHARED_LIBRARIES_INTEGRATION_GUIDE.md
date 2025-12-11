# ✨ GOGIDIX PLATFORM - SHARED LIBRARIES INTEGRATION GUIDE ✨
# Complete Integration Manual for Domain Services
# Version: 1.0.0
# Last Updated: 2025-11-29

## 📋 Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Integration Steps](#integration-steps)
4. [Configuration Details](#configuration-details)
5. [Testing Integration](#testing-integration)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)
8. [Automation](#automation)

## 🎯 Overview

This guide provides comprehensive instructions for integrating Gogidix domain services with the shared libraries framework. The integration ensures:

- **Zero Configuration**: Services automatically inherit platform configurations
- **Enterprise Features**: JWT security, RBAC, monitoring, caching out of the box
- **Standardization**: Consistent patterns across all microservices
- **Developer Productivity**: Focus on business logic, not boilerplate

## 📦 Shared Libraries Components

### 1. Core Library (`gogidix-common-core`)
- Enterprise API response framework
- Global exception handling
- Validation annotations
- Utility classes and helpers
- Common DTOs and interfaces

### 2. Security Library (`gogidix-common-security`)
- JWT token service
- RBAC implementation
- OAuth2 integration
- Password policies with Argon2
- Audit logging

### 3. Messaging Library (`gogidix-common-messaging`)
- Kafka event publishing/consuming
- Domain event framework
- Message retry patterns
- Dead letter queue handling

### 4. Persistence Library (`gogidix-common-persistence`)
- JPA base entities with auditing
- Repository patterns
- Soft delete support
- Multi-tenancy foundation

### 5. Observability Library (`gogidix-common-observability`)
- Prometheus metrics
- Distributed tracing
- Structured logging
- Health checks

## ✅ Prerequisites

Before beginning integration, ensure:

1. **Shared Libraries Built**
   ```bash
   cd foundation-domain/shared-libraries/backend-services/gogidix-shared-libraries
   mvn clean install
   ```

2. **Java Version**
   - JDK 21 or higher installed
   - JAVA_HOME configured

3. **Maven Version**
   - Maven 3.9.0 or higher

4. **Required Services**
   - PostgreSQL (for production)
   - Redis (for caching)
   - Kafka (for messaging)
   - Config Server (optional)

## 🔧 Integration Steps

### Step 1: Update POM.xml

Update your service's POM to inherit from shared libraries parent:

```xml
<parent>
    <groupId>com.gogidix.platform</groupId>
    <artifactId>gogidix-shared-libraries</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../../../../foundation-domain/shared-libraries/backend-services/gogidix-shared-libraries/pom.xml</relativePath>
</parent>
```

Add shared library dependencies:

```xml
<dependencies>
    <!-- GOGIDIX SHARED LIBRARIES -->
    <dependency>
        <groupId>com.gogidix.platform</groupId>
        <artifactId>gogidix-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.gogidix.platform</groupId>
        <artifactId>gogidix-common-security</artifactId>
    </dependency>
    <dependency>
        <groupId>com.gogidix.platform</groupId>
        <artifactId>gogidix-common-messaging</artifactId>
    </dependency>
    <dependency>
        <groupId>com.gogidix.platform</groupId>
        <artifactId>gogidix-common-persistence</artifactId>
    </dependency>
</dependencies>
```

### Step 2: Update Application.java

Add shared library enablement annotations:

```java
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableKafka

// GOGIDIX SHARED LIBRARIES ENABLEMENT
@EnableGogidixCore
@EnableGogidixSecurity
@EnableGogidixMessaging
@EnableGogidixPersistence
@EnableGogidixObservability

public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

### Step 3: Configure application.yml

Import central configurations:

```yaml
spring:
  application:
    name: your-service-name
  config:
    import:
      - optional:classpath:platform-application-commons/application.yml
      - optional:classpath:platform-application-commons/security.yml
      - optional:classpath:platform-application-commons/monitoring.yml
      - optional:classpath:platform-application-commons/performance.yml
```

### Step 4: Create bootstrap.yml (Optional)

For Spring Cloud Config Server integration:

```yaml
spring:
  application:
    name: your-service-name
  cloud:
    config:
      uri: ${CONFIG_SERVER_URL:http://localhost:8888}
      fail-fast: true
```

### Step 5: Package Structure

Follow the standard package structure:

```
src/main/java/com/gogidix/platform/yourservice/
├── YourServiceApplication.java
├── config/
│   ├── SecurityConfig.java
│   └── KafkaConfig.java
├── domain/
│   ├── model/
│   ├── repository/
│   └── service/
├── infrastructure/
│   ├── persistence/
│   └── messaging/
└── web/
    ├── controller/
    └── dto/
```

## ⚙️ Configuration Details

### Environment Variables

All configurations support environment variable overrides:

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |
| `DB_HOST` | Database host | `localhost` |
| `REDIS_HOST` | Redis host | `localhost` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | `localhost:9092` |
| `GOGIDIX_JWT_SECRET` | JWT signing secret | Required in prod |

### Database Configuration

```yaml
spring:
  datasource:
    url: ${YOUR_SERVICE_DB_URL:jdbc:postgresql://localhost:5432/your_db}
    username: ${YOUR_SERVICE_DB_USERNAME:your_user}
    password: ${YOUR_SERVICE_DB_PASSWORD:}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### Security Configuration

```yaml
gogidix:
  platform:
    security:
      jwt:
        secret: ${GOGIDIX_JWT_SECRET}
        access-token-expiration: PT1H
      rbac:
        enabled: true
```

## 🧪 Testing Integration

### Unit Test Example

```java
@SpringBootTest
@ActiveProfiles("test")
class SharedLibrariesIntegrationTest {

    @Test
    void contextLoads() {
        // Verify context loads with shared libraries
    }

    @Test
    void testApiResponseFramework() {
        ApiResponse<String> response = ApiResponse.<String>builder()
            .status(ResponseStatus.SUCCESS)
            .data("Working!")
            .build();

        assertThat(response.getData()).isEqualTo("Working!");
    }
}
```

### Test Configuration

Create `src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
```

## 🔧 Troubleshooting

### Common Issues

1. **Parent POM Not Found**
   ```
   [ERROR] Non-resolvable parent POM
   ```
   - Ensure shared libraries are installed: `mvn clean install`
   - Check relative path in parent POM

2. **Missing Annotations**
   ```
   Cannot find symbol: EnableGogidixCore
   ```
   - Add shared library dependencies
   - Run `mvn clean compile`

3. **Configuration Not Loaded**
   - Check file paths in config import
   - Verify profile activation
   - Enable debug logging: `logging.level.org.springframework.boot=DEBUG`

4. **JWT Issues**
   - Ensure `GOGIDIX_JWT_SECRET` is set in production
   - Check token expiration settings

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.gogidix.platform: DEBUG
    org.springframework.boot: DEBUG
```

## 📏 Best Practices

### 1. Service Configuration
- Use environment-specific profiles
- Never hardcode secrets
- Leverage central configuration
- Document service-specific settings

### 2. Security
- Always use HTTPS in production
- Implement proper RBAC
- Enable audit logging
- Rotate JWT secrets regularly

### 3. Performance
- Configure appropriate connection pools
- Use Redis caching strategically
- Monitor with Prometheus metrics
- Set up proper JVM tuning

### 4. Messaging
- Use domain events for loose coupling
- Implement idempotent consumers
- Handle message failures gracefully
- Monitor queue depths

### 5. Testing
- Write integration tests for shared libraries
- Use Testcontainers for real dependencies
- Test all active profiles
- Mock external services

## 🤖 Automation

### Using the Integration Script

The provided Python script automates the entire integration:

```bash
# Integrate all services
python integrate_shared_libraries.py --base-path /path/to/project

# Dry run to preview changes
python integrate_shared_libraries.py --dry-run

# Integrate specific service
python integrate_shared_libraries.py --service-path path/to/service
```

### CI/CD Integration

Add to your GitHub Actions workflow:

```yaml
- name: Integrate Shared Libraries
  run: |
    python foundation-domain/shared-libraries/backend-services/integrate_shared_libraries.py

- name: Build Service
  run: |
    mvn clean compile test
```

## 📊 Integration Checklist

### Pre-Integration
- [ ] Shared libraries built and installed
- [ ] Service follows standard structure
- [ ] Database schema defined
- [ ] Environment variables documented

### POM Updates
- [ ] Parent POM updated to shared libraries
- [ ] Shared library dependencies added
- [ ] Spring Boot starter dependencies correct
- [ ] Test dependencies configured

### Application Configuration
- [ ] Application.java updated with annotations
- [ ] application.yml imports central config
- [ ] bootstrap.yml configured (if needed)
- [ ] Service-specific properties set

### Security Configuration
- [ ] JWT secret configured
- [ ] RBAC enabled
- [ ] Public endpoints marked
- [ ] CORS configured

### Database Configuration
- [ ] Flyway migrations created
- [ ] JPA entities extend base entities
- [ ] Auditing enabled
- [ ] Connection pool configured

### Testing
- [ ] Integration tests created
- [ ] Test configuration set up
- [ ] All profiles tested
- [ ] Coverage meets requirements

### Post-Integration
- [ ] Service starts successfully
- [ ] Health checks pass
- [ ] Metrics are exposed
- [ ] Logs are structured
- [ ] Security functions work
- [ ] Messaging events flow

## 📞 Support

For integration support:

1. Check this guide first
2. Review the integration script logs
3. Enable debug logging
4. Check shared library documentation
5. Contact the platform team

---

## 📝 Notes

- Always test in a development environment first
- Back up your service before integration
- Keep service-specific configurations minimal
- Leverage the platform features
- Follow the established patterns