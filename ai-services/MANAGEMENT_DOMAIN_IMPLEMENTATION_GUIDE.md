# Management Domain Implementation Guide

**Date**: December 1, 2024
**Domain**: Management Domain
**Purpose**: Complete implementation guide for all Management Domain services
**Target**: Agents and Developers working on Management Domain

---

## 🎯 **EXECUTIVE SUMMARY**

The Management Domain provides core business management capabilities for the Gogidix platform, including financial reporting, project management, resource allocation, and business intelligence. This guide provides complete implementation patterns and standards to achieve production-ready services.

---

## 📋 **DOMAIN OVERVIEW**

### **Purpose**
Manage business operations, financial data, projects, resources, and provide insights for decision-making.

### **Key Services to Implement**

#### **1. Financial Services (5 services)**
- **financial-reporting-service** (Port 9000) - Financial statements and reports
- **budget-management-service** (Port 9001) - Budget planning and tracking
- **expense-management-service** (Port 9002) - Expense tracking and approvals
- **revenue-tracking-service** (Port 9003) - Revenue analytics and reporting
- **cost-allocation-service** (Port 9004) - Cost distribution across departments

#### **2. Project Management Services (6 services)**
- **project-management-service** (Port 9010) - Project lifecycle management
- **task-management-service** (Port 9011) - Task assignment and tracking
- **milestone-service** (Port 9012) - Milestone tracking and reporting
- **resource-allocation-service** (Port 9013) - Resource assignment and optimization
- **time-tracking-service** (Port 9014) - Time logging and analysis
- **project-analytics-service** (Port 9015) - Project performance metrics

#### **3. Resource Management Services (5 services)**
- **human-resources-service** (Port 9020) - Employee management
- **asset-management-service** (Port 9021) - Asset tracking and lifecycle
- **inventory-management-service** (Port 9022) - Stock and inventory control
- **capacity-planning-service** (Port 9023) - Resource capacity analysis
- **vendor-management-service** (Port 9024) - Vendor and supplier management

#### **4. Reporting & Analytics Services (4 services)**
- **business-intelligence-service** (Port 9030) - BI dashboards and reports
- **data-analytics-service** (Port 9031) - Data analysis and insights
- **performance-metrics-service** (Port 9032) - KPI tracking and reporting
- **custom-report-service** (Port 9033) - Custom report generation

#### **5. Workflow & Process Services (5 services)**
- **workflow-engine-service** (Port 9040) - Business process automation
- **approval-service** (Port 9041) - Multi-level approval workflows
- **notification-service** (Port 9042) - System notifications and alerts
- **document-management-service** (Port 9043) - Document storage and versioning
- **audit-service** (Port 9044) - Audit trail and compliance

---

## 🏗️ **IMPLEMENTATION STANDARDS**

### **Technology Stack**
- **Framework**: Spring Boot 3.x
- **Language**: Java 21 LTS
- **Database**: PostgreSQL (Production), H2 (Testing)
- **Cache**: Redis
- **Message Queue**: RabbitMQ/Kafka
- **Search**: Elasticsearch
- **Authentication**: JWT + OAuth2

### **Architecture Pattern**
```
┌─────────────────────────────────────────┐
│             API Gateway                │
├─────────────────────────────────────────┤
│          Management Services             │
├─────────────────────────────────────────┤
│      Service Registry (Eureka)           │
├─────────────────────────────────────────┤
│      Config Server (Centralized)         │
├─────────────────────────────────────────┤
│     Databases (PostgreSQL/Redis)        │
└─────────────────────────────────────────┘
```

---

## 🔧 **IMPLEMENTATION CHECKLIST**

### **For Each Service:**

#### **1. Project Structure**
```
service-name/
├── src/main/java/com/gogidix/management/
│   ├── controller/
│   │   └── [Service]Controller.java
│   ├── service/
│   │   ├── [Service]Service.java
│   │   └── impl/
│   │       └── [Service]ServiceImpl.java
│   ├── repository/
│   │   └── [Entity]Repository.java
│   ├── entity/
│   │   └── [Entity].java
│   ├── dto/
│   │   ├── [Entity]DTO.java
│   │   ├── RequestDTO.java
│   │   └── ResponseDTO.java
│   └── config/
│       └── [Service]Config.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__Create_[Entity]_Table.sql
└── pom.xml
```

#### **2. Required Components**

##### **Controller Layer**
```java
@RestController
@RequestMapping("/api/[service]")
@Validated
@Tag(name = "[Service] API", description = "API for [Service] management")
public class [Service]Controller {

    @GetMapping
    @Operation(summary = "Get all [entities]")
    public ResponseEntity<Page<[Entity]DTO>> getAll[Entities](
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Implementation
    }

    @PostMapping
    @Operation(summary = "Create [entity]")
    public ResponseEntity<[Entity]DTO> create[Entity](
            @Valid @RequestBody [Entity]DTO dto) {
        // Implementation
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get [entity] by ID")
    public ResponseEntity<[Entity]DTO> get[Entity](@PathVariable Long id) {
        // Implementation
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update [entity]")
    public ResponseEntity<[Entity]DTO> update[Entity](
            @PathVariable Long id,
            @Valid @RequestBody [Entity]DTO dto) {
        // Implementation
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete [entity]")
    public ResponseEntity<Void> delete[Entity](@PathVariable Long id) {
        // Implementation
    }
}
```

##### **Entity Layer**
```java
@Entity
@Table(name = "[table_name]")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class [Entity] {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private [Entity]Status status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(nullable = false, updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 100)
    private String updatedBy;

    // Version for optimistic locking
    @Version
    private Long version;

    // Soft delete
    @Column(nullable = false)
    private Boolean deleted = false;

    // Business specific fields
    // Add fields based on service requirements
}
```

##### **Service Layer**
```java
@Service
@Transactional
@Slf4j
public class [Service]Service {

    private final [Entity]Repository repository;

    public [Service]Service([Entity]Repository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<[Entity]DTO> getAll[Entities](Pageable pageable) {
        return repository.findAllByDeletedFalse(pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public [Entity]DTO create[Entity]([Entity]DTO dto, String username) {
        [Entity] entity = convertToEntity(dto);
        entity.setCreatedBy(username);
        entity = repository.save(entity);
        log.info("Created {}: {}", "[entity]", entity.getId());
        return convertToDTO(entity);
    }

    // Additional business methods
    public List<[Entity]DTO> getBy[Critieria](String [criteria]) {
        return repository.findBy[Critieria]Containing([criteria])
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Private conversion methods
    private [Entity]DTO convertToDTO([Entity] entity) {
        // Use ModelMapper or manual conversion
    }

    private [Entity] convertToEntity([Entity]DTO dto) {
        // Use ModelMapper or manual conversion
    }
}
```

##### **Repository Layer**
```java
@Repository
public interface [Entity]Repository extends JpaRepository<[Entity], Long>, JpaSpecificationExecutor<[Entity]> {

    // Custom queries
    Page<[Entity]> findAllByDeletedFalse(Pageable pageable);

    List<[Entity]> findBy[Critieria]Containing(String [criteria]);

    @Query("SELECT e FROM [Entity] e WHERE e.status = :status AND e.deleted = false")
    List<[Entity]> findByStatusAndNotDeleted(@Param("status") [Entity]Status status);

    @Modifying
    @Query("UPDATE [Entity] e SET e.deleted = true, e.updatedBy = :username WHERE e.id = :id")
    int softDelete(@Param("id") Long id, @Param("username") String username);
}
```

#### **3. Database Migration (Flyway)**
```sql
-- V1__Create_[table_name]_Table.sql
CREATE TABLE [table_name] (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes
CREATE INDEX idx_[table]_status ON [table_name](status);
CREATE INDEX idx_[table]_deleted ON [table_name](deleted);
CREATE INDEX idx_[table]_created_at ON [table_name](created_at);

-- Add comments
COMMENT ON TABLE [table_name] IS '[Entity] table for [service]';
```

#### **4. Application Configuration (application.yml)**
```yaml
server:
  port: 9xxx
  servlet:
    context-path: /[service]

spring:
  application:
    name: [service-name]

  datasource:
    url: jdbc:postgresql://localhost:5432/[service_name]_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    timeout: 2000ms

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OAUTH2_ISSUER_URI}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER:http://localhost:8761/eureka}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.gogidix.management: DEBUG
    org.springframework.security: DEBUG
```

#### **5. Testing**
```java
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class [Service]ServiceTest {

    @Mock
    private [Entity]Repository repository;

    @InjectMocks
    private [Service]Service service;

    @Test
    @Order(1)
    void testCreate[Entity]() {
        // Test implementation
    }

    @Test
    @Order(2)
    void testGetAll[Entities]() {
        // Test implementation
    }

    @Test
    @Order(3)
    void testUpdate[Entity]() {
        // Test implementation
    }

    @Test
    @Order(4)
    void testDelete[Entity]() {
        // Test implementation
    }
}
```

---

## 🔒 **SECURITY REQUIREMENTS**

### **Authentication & Authorization**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/[service]/**").hasAnyRole("ADMIN", "MANAGER", "USER")
                .requestMatchers(HttpMethod.POST, "/api/[service]/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/[service]/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/[service]/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }
}
```

---

## 📊 **INTEGRATION REQUIREMENTS**

### **With Foundation Domain**
- Use Config Server for configuration
- Register with Eureka for service discovery
- Implement audit logging via AuditLoggingConfigService
- Use SecretsManagementService for sensitive data

### **With Infrastructure Domain**
- Authenticate through API Gateway
- Use distributed tracing
- Log to centralized logging service
- Report metrics to monitoring service

### **Inter-Service Communication**
```java
@FeignClient(name = "[dependency-service]")
public interface [Dependency]Client {

    @GetMapping("/api/[resource]")
    List<[Resource]DTO> getAll[Resources]();

    @PostMapping("/api/[resource]")
    [Resource]DTO create[Resource](@RequestBody [Resource]DTO dto);
}
```

---

## 🚀 **DEPLOYMENT CHECKLIST**

### **Pre-Deployment**
- [ ] All unit tests pass (>80% coverage)
- [ ] Integration tests pass
- [ ] Security scan passes
- [ ] Performance tests meet criteria
- [ ] Documentation complete

### **Deployment**
- [ ] Environment variables configured
- [ ] Database migrations applied
- [ ] Service registered with Eureka
- [ ] Health check endpoint accessible
- [ ] Metrics endpoint accessible

### **Post-Deployment**
- [ ] Service monitoring active
- [ ] Log aggregation working
- [ ] Alerting rules configured
- [ ] Backup procedures tested
- [ ] Disaster recovery tested

---

## 📋 **PRIORITY IMPLEMENTATION ORDER**

### **Phase 1 (Week 1) - Core Services**
1. **financial-reporting-service** - Critical for business operations
2. **project-management-service** - Core project tracking
3. **user-management-service** - User and role management
4. **approval-service** - Workflow engine for approvals

### **Phase 2 (Week 2) - Supporting Services**
5. **resource-allocation-service** - Resource management
6. **time-tracking-service** - Time and attendance
7. **notification-service** - System notifications
8. **document-management-service** - Document handling

### **Phase 3 (Week 3) - Analytics & Reporting**
9. **business-intelligence-service** - BI dashboards
10. **data-analytics-service** - Data insights
11. **performance-metrics-service** - KPI tracking
12. **audit-service** - Compliance and audit

---

## 🔗 **USEFUL LINKS AND REFERENCES**

### **Documentation**
- API Documentation: `http://localhost:9xxx/[service]/swagger-ui.html`
- Health Check: `http://localhost:9xxx/[service]/actuator/health`
- Metrics: `http://localhost:9xxx/[service]/actuator/metrics`

### **Dependencies**
- Config Server: `http://localhost:8888`
- Eureka Server: `http://localhost:8761`
- API Gateway: `http://localhost:8080`

### **Related Services**
- Foundation Domain services for configuration
- Infrastructure Domain for monitoring and logging
- Business Domain for financial transactions

---

## ✅ **SUCCESS CRITERIA**

A Management Domain service is production-ready when:

1. **Functional Requirements Met**
   - [ ] All business logic implemented
   - [ ] CRUD operations working
   - [ ] Business rules enforced
   - [ ] Data validation complete

2. **Non-Functional Requirements Met**
   - [ ] Response time < 200ms (95th percentile)
   - [ ] Security compliance achieved
   - [ ] Monitoring and logging active
   - [ ] Error handling comprehensive

3. **Operational Requirements Met**
   - [ ] Health checks passing
   - [ ] Metrics being collected
   - [ ] Documentation complete
   - [ ] Testing coverage >80%

---

## 📞 **SUPPORT**

For any questions or blocking issues:
1. Check the Foundation Domain documentation
2. Review Infrastructure Domain patterns
3. Consult the implementation examples above
4. Connect with other agents for cross-domain integration

---

**Get this document shared with all agents working on the Management Domain to ensure consistent implementation standards and fast delivery!** 🚀