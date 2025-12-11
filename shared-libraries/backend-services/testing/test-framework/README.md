# ✨ GOGIDIX PLATFORM - COMPREHENSIVE TESTING FRAMEWORK ✨
# Enterprise Testing Suite for All Platform Services

## 📋 Overview

This testing framework provides comprehensive testing capabilities for the Gogidix Platform, covering unit tests, integration tests, performance tests, and security assessments.

## 🏗️ Test Structure

```
testing/
├── test-framework/
│   ├── unit-tests/          # JUnit 5 unit tests
│   ├── integration-tests/   # Spring Boot integration tests
│   ├── contract-tests/      # Consumer-driven contract tests
│   ├── performance-tests/  # JMH & Gatling performance tests
│   ├── security-tests/      # OWASP ZAP security scans
│   └── e2e-tests/          # Selenium & Playwright E2E tests
├── test-data/             # Test data fixtures
├── test-containers/       # TestContainers configurations
└── test-reports/          # Generated test reports
```

## 🧪 Test Types

### 1. Unit Tests
- **Purpose**: Test individual components in isolation
- **Framework**: JUnit 5 + Mockito
- **Coverage Target**: > 90%
- **Execution**: Every build

### 2. Integration Tests
- **Purpose**: Test service interactions
- **Framework**: Spring Boot Test + TestContainers
- **Coverage**: Database, Kafka, Redis, External APIs
- **Execution**: Pre-commit + CI

### 3. Contract Tests
- **Purpose**: API contract validation
- **Framework**: Pact
- **Coverage**: All public APIs
- **Execution**: CI pipeline

### 4. Performance Tests
- **Purpose**: Load and stress testing
- **Framework**: JMH, Gatling, K6
- **Metrics**: Throughput, Latency, Resource Usage
- **Execution**: Nightly builds

### 5. Security Tests
- **Purpose**: Vulnerability scanning
- **Tools**: OWASP ZAP, SonarQube, Dependency Check
- **Coverage**: SAST, DAST, SCA
- **Execution:**
  - SonarQube: Every PR
  - OWASP ZAP: Staging environment
  - Dependency Check: Every build

### 6. End-to-End Tests
- **Purpose**: User journey validation
- **Framework**: Playwright
- **Coverage**: Critical user paths
- **Execution**: Pre-production

## 📊 Test Configuration

### Maven Surefire Configuration (Unit Tests)
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
            <include>**/*Spec.java</include>
        </includes>
        <excludes>
            <exclude>**/*IT.java</exclude>
            <exclude>**/*IntegrationTest.java</exclude>
            <exclude>**/*E2ETest.java</exclude>
        </excludes>
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

### Maven Failsafe Configuration (Integration Tests)
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*IT.java</include>
            <include>**/*IntegrationTest.java</include>
        </includes>
        <systemPropertyVariables>
            <spring.profiles.active>integration-test</spring.profiles.active>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

## 🏃 Running Tests

### Local Testing
```bash
# Run all tests
mvn clean verify

# Run unit tests only
mvn test

# Run integration tests only
mvn verify -DskipITs=false -DskipTests=false

# Run with specific profile
mvn test -Dspring.profiles.active=test
```

### Docker-based Testing
```bash
# Run tests in Docker container
docker run --rm \
  -v $(pwd):/app \
  -v ~/.m2:/root/.m2 \
  maven:3.9.5-eclipse-temurin-21 \
  bash -c "cd /app && mvn verify"
```

### TestContainers Configuration
```yaml
# testcontainers.properties file
testcontainers.reuse.enable=true
testcontainers.db.driver=org.postgresql.Driver
testcontainers.db.image=postgres:15-alpine
testcontainers.kafka.image=confluentinc/cp-kafka:7.4.0
testcontainers.redis.image=redis:7-alpine
```

## 📈 Performance Testing

### JMH Microbenchmarks
```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class InvoiceServiceBenchmark {

    @Benchmark
    public Invoice createInvoiceBenchmark(Blackhole bh) {
        // Benchmark invoice creation
        return invoiceService.create(createInvoiceRequest());
    }
}
```

### Gatling Load Tests
```scala
class BillingApiSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("http://api.gogidix.com")
    .acceptHeader("application/json")

  val scn = scenario("Billing API Load Test")
    .exec(http("Create Invoice")
      .post("/api/invoices")
      .body(StringBody("""{"amount": 100}"""))
      .check(status.is(201)))

  setUp(scn.inject(
    rampUsersPerSec(10) to 100 during (5 minutes)
  )).protocols(httpProtocol)
}
```

## 🔐 Security Testing

### OWASP ZAP Configuration
```yaml
# zap-config.yml
contexts:
  - name: gogidix-api
    urls:
      - "https://api.gogidix.com"

spiders:
  - context: gogidix-api
    maxDepth: 5

scans:
  - context: gogidix-api
    policy: "OWASP Top 10"
```

### Security Test Example
```java
@Test
void testSqlInjectionPrevention() {
    // Attempt SQL injection
    String maliciousInput = "'; DROP TABLE users; --";

    ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
        "/api/users?search=" + maliciousInput,
        ApiResponse.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}

@Test
void testXssPrevention() {
    String xssPayload = "<script>alert('xss')</script>";

    ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
        "/api/comments",
        new CommentRequest(xssPayload),
        ApiResponse.class
    );

    Comment comment = (Comment) response.getBody().getData();
    assertThat(comment.getContent()).doesNotContain("<script>");
}
```

## 📋 Test Data Management

### Test Fixtures
```java
@Component
public class TestDataFactory {

    public static Invoice createTestInvoice() {
        return Invoice.builder()
            .id(UUID.randomUUID())
            .amount(new BigDecimal("100.00"))
            .dueDate(LocalDate.now().plusDays(30))
            .status(InvoiceStatus.DRAFT)
            .build();
    }

    public static User createTestUser() {
        return User.builder()
            .email("test@gogidix.com")
            .firstName("Test")
            .lastName("User")
            .role(Role.USER)
            .build();
    }
}
```

### Database Cleanup
```java
@AfterEach
void cleanupDatabase() {
    // Clean up test data
    jdbcTemplate.update("DELETE FROM invoice_items");
    jdbcTemplate.update("DELETE FROM invoices");
    jdbcTemplate.update("DELETE FROM users");
}
```

## 📊 Test Reports

### JaCoCo Coverage Report
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.90</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### Test Results Dashboard
- Allure Report: `target/site/allure-report/index.html`
- JaCoCo Report: `target/site/jacoco/index.html`
- Surefire Report: `target/surefire-reports`
- Failsafe Report: `target/failsafe-reports`

## 🔧 CI/CD Integration

### GitHub Actions Test Workflow
```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

    - name: Run tests
      run: mvn clean verify

    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: target/surefire-reports/*.xml
        reporter: java-junit
```

## 📚 Best Practices

### Test Naming Conventions
- Unit tests: `[MethodName]_[Scenario]_[ExpectedResult]`
- Integration tests: `[ServiceName]_[Feature]_[Integration]Test`
- E2E tests: `[UserStory]_[E2E]Test`

### Test Organization
- One assertion per test
- Descriptive test names
- Arrange-Act-Assert pattern
- Test independence

### Mocking Guidelines
- Mock external dependencies
- Use real implementations for internal services
- Verify mock interactions
- Avoid over-mocking

### Performance Test Guidelines
- Baseline measurements first
- Test realistic scenarios
- Monitor resource usage
- Document results

## 🔍 Troubleshooting

### Common Issues
1. **TestContainer startup failures**
   - Check Docker daemon
   - Verify image availability
   - Increase timeout values

2. **Flaky tests**
   - Add proper synchronization
   - Use TestContainers reuse
   - Check for race conditions

3. **Memory issues in tests**
   - Use @DirtiesContext
   - Limit test data
   - Profile memory usage

## 📞 Support

For testing framework issues:
1. Check the test logs
2. Review configuration
3. Consult documentation
4. Contact QA team

---

**Remember**: Good tests are not just about coverage, but about confidence in the system!