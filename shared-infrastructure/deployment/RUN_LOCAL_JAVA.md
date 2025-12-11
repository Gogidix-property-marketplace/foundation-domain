# Running Services Locally (Java Processes)

## Quick Start - No Docker Required

If Docker Desktop is not available or you prefer running services directly, you can run the Java services locally.

## Prerequisites

1. **Java 21** installed
2. **Maven 3.9+** installed
3. **PostgreSQL** (optional, can use H2 in-memory database for testing)
4. **Redis** (optional, can run without caching for basic testing)

## Step 1: Infrastructure Services (Essential)

### Option A: Using Docker for Infrastructure Only
```powershell
# Start only databases and message queues
cd C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\local-deployment
docker-compose up -d postgres redis elasticsearch

# Or use individual services
docker run -d --name postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:15
docker run -d --name redis -p 6379:6379 redis:7
```

### Option B: No Docker - Using In-Memory Options
The application will fallback to:
- H2 database for PostgreSQL
- In-memory caching for Redis
- No external dependencies needed

## Step 2: Start Core Services

### 1. Config Server
```powershell
cd ..\java-services\config-server
mvn spring-boot:run
```
Access: http://localhost:8888

### 2. Eureka Discovery Server
```powershell
cd ..\java-services\eureka-server
mvn spring-boot:run
```
Access: http://localhost:8761

### 3. API Gateway
```powershell
cd ..\java-services\api-gateway
mvn spring-boot:run
```
Access: http://localhost:8080

### 4. Rate Limiting Service
```powershell
cd ..\java-services\rate-limiting-service
mvn spring-boot:run
```
Access: http://localhost:8081

## Step 3: Start Application Services

### Dashboard Integration Service
```powershell
cd ..\java-services\dashboard-integration-service
mvn spring-boot:run
```
Access: http://localhost:8082

### AI Integration Service
```powershell
cd ..\java-services\ai-integration-service
mvn spring-boot:run
```
Access: http://localhost:8083

## Step 4: Verify Services

### Health Check All Services
```powershell
# Config Server
curl http://localhost:8888/actuator/health

# Eureka
curl http://localhost:8761/actuator/health

# API Gateway
curl http://localhost:8080/actuator/health

# Rate Limiting
curl http://localhost:8081/actuator/health
```

### Test API Gateway
```powershell
# Test basic routing
curl http://localhost:8080/api/v1/test

# Test rate limiting
curl -H "X-API-Key: test-key" http://localhost:8080/api/v1/rate-limited
```

## Configuration for Local Development

### application-local.yml

Create this file in each service's `src/main/resources` directory:

```yaml
spring:
  profiles:
    active: local
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  redis:
    host: 127.0.0.1
    port: 6379
    # Remove if Redis is not available
  cache:
    type: simple  # Use in-memory caching if Redis is not available

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    register-with-eureka: true
    fetch-registry: true

server:
  port: 8080  # Change per service

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
    com.gogidix: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

## Running Multiple Services

### Option 1: Multiple Terminal Windows
Open a separate PowerShell/Command Prompt for each service.

### Option 2: Background Processes (Windows)
```powershell
# Start service in background
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd config-server; mvn spring-boot:run"

# Or using Windows Terminal tabs
wt new-tab --profile "PowerShell" --startingDirectory "C:\path\to\config-server" cmd /k "mvn spring-boot:run"
```

### Option 3: Maven Multi-Module
Create a parent pom.xml to run multiple modules:
```xml
<modules>
    <module>../java-services/config-server</module>
    <module>../java-services/eureka-server</module>
    <module>../java-services/api-gateway</module>
</modules>
```
Then run: `mvn spring-boot:run -pl <module-name>`

## Testing Without Full Infrastructure

### Minimal Setup
1. Start only Config Server and Eureka
2. Run API Gateway with in-memory database
3. Test basic routing functionality

### Script for Quick Start
Save as `run-services.ps1`:
```powershell
$services = @(
    @{name="Config Server"; path="..\java-services\config-server"; port=8888},
    @{name="Eureka"; path="..\java-services\eureka-server"; port=8761},
    @{name="API Gateway"; path="..\java-services\api-gateway"; port=8080},
    @{name="Rate Limiting"; path="..\java-services\rate-limiting-service"; port=8081}
)

foreach ($service in $services) {
    Write-Host "Starting $($service.name) on port $($service.port)..." -ForegroundColor Green
    Start-Job -ScriptBlock {
        param($path, $name)
        Set-Location $path
        mvn spring-boot:run
    } -ArgumentList $service.path, $service.name
    Start-Sleep 5  # Give each service time to start
}

Write-Host "All services starting..." -ForegroundColor Yellow
Write-Host "Check http://localhost:8761 for Eureka dashboard"
```

## Port Allocation

Make sure each service uses a unique port:
- Config Server: 8888
- Eureka: 8761
- API Gateway: 8080
- Rate Limiting: 8081
- Dashboard Integration: 8082
- AI Integration: 8083
- Zipkin: 9411
- Prometheus: 9090

## Troubleshooting

### Port Already in Use
```powershell
# Find process using port
netstat -ano | findstr :8080

# Kill process
taskkill /PID <PID> /F
```

### Maven Dependencies Issues
```powershell
# Clean and rebuild
mvn clean install -DskipTests

# Or clear Maven cache
mvn dependency:purge-local-repository
```

### Memory Issues
Increase JVM memory:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g -Xms1g"
```

## Next Steps

1. Verify all services are registered in Eureka
2. Test API Gateway routing
3. Check service health endpoints
4. Run integration tests
5. Add monitoring if needed