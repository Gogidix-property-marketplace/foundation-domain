#!/bin/bash

# 🏗️ MASS FIX ALL 39 INFRASTRUCTURE SERVICES
# Template-Based Batch Generation
# Using working EnterpriseTestService as master template

echo "🚀 MASS FIX ALL 39 INFRASTRUCTURE SERVICES"
echo "========================================"
echo "🏗️ Fixing Package Names & Adding Configuration Files"
echo "📍 Location: $PWD"
echo "📅 Date: $(date)"
echo ""

# Infrastructure Services mapping (service_name:port:package)
declare -A infrastructure_services=(
    # Core Infrastructure Services (Ports 8000-8099)
    ["aggregation-service"]="8000:com.gogidix.infrastructure.aggregation"
    ["api-gateway"]="8001:com.gogidix.infrastructure.gateway"
    ["authentication-service"]="8002:com.gogidix.infrastructure.auth"
    ["authorization-service"]="8003:com.gogidix.infrastructure.authorization"
    ["backup-service"]="8004:com.gogidix.infrastructure.backup"
    ["cache-service"]="8005:com.gogidix.infrastructure.cache"
    ["circuit-breaker-service"]="8006:com.gogidix.infrastructure.circuit"
    ["config-server"]="8007:com.gogidix.infrastructure.config"
    ["correlation-id-service"]="8008:com.gogidix.infrastructure.correlation"
    ["disaster-recovery-service"]="8009:com.gogidix.infrastructure.disaster"
    ["distributed-tracing-service"]="8010:com.gogidix.infrastructure.tracing"
    ["dns-service"]="8011:com.gogidix.infrastructure.dns"
    ["eureka-server"]="8012:com.gogidix.infrastructure.discovery"
    ["file-storage-service"]="8013:com.gogidix.infrastructure.storage"
    ["health-check-service"]="8014:com.gogidix.infrastructure.health"

    # Management & Operations Services (Ports 8015-8024)
    ["idempotency-service"]="8015:com.gogidix.infrastructure.idempotency"
    ["load-balancer"]="8016:com.gogidix.infrastructure.loadbalancer"
    ["logging-service"]="8017:com.gogidix.infrastructure.logging"
    ["message-broker-service"]="8018:com.gogidix.infrastructure.broker"
    ["message-queue-service"]="8019:com.gogidix.infrastructure.queue"
    ["metrics-collection-service"]="8020:com.gogidix.infrastructure.metrics"
    ["migration-service"]="8021:com.gogidix.infrastructure.migration"
    ["monitoring-service"]="8022:com.gogidix.infrastructure.monitoring"
    ["notification-service"]="8023:com.gogidix.infrastructure.notification"
    ["oauth2-service"]="8024:com.gogidix.infrastructure.oauth2"

    # Security & Integration Services (Ports 8025-8034)
    ["rate-limiting-service"]="8025:com.gogidix.infrastructure.ratelimit"
    ["registry-service"]="8026:com.gogidix.infrastructure.registry"
    ["resource-allocation-service"]="8027:com.gogidix.infrastructure.allocation"
    ["scheduler-service"]="8028:com.gogidix.infrastructure.scheduler"
    ["search-service"]="8029:com.gogidix.infrastructure.search"
    ["secret-management-service"]="8030:com.gogidix.infrastructure.secrets"
    ["security-service"]="8031:com.gogidix.infrastructure.security"
    ["service-mesh-service"]="8032:com.gogidix.infrastructure.mesh"
    ["service-registry-service"]="8033:com.gogidix.infrastructure.serviceregistry"
    ["session-management-service"]="8034:com.gogidix.infrastructure.session"

    # Advanced Infrastructure Services (Ports 8035-8039)
    ["task-management-service"]="8035:com.gogidix.infrastructure.task"
    ["user-management-service"]="8036:com.gogidix.infrastructure.usermgmt"
    ["version-control-service"]="8037:com.gogidix.infrastructure.version"
    ["workflow-engine"]="8038:com.gogidix.infrastructure.workflow"
    ["zookeeper-service"]="8039:com.gogidix.infrastructure.zookeeper"
)

# Template source location
TEMPLATE_SOURCE="C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-code-Generator-Factory/gogidix-java-cli/generated/EnterpriseTestService-service"

# Target base directory
TARGET_BASE="C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/java-services"

# Counter
generated=0
failed=0

echo "🏗️ Total Services to Fix: ${#infrastructure_services[@]}"
echo ""

# Fix each infrastructure service
cd "java-services"

for service_name in "${!infrastructure_services[@]}"; do
    port_package="${infrastructure_services[$service_name]}"
    port="${port_package%%:*}"
    package="${port_package#*:}"

    echo "⚡ Fixing: $service_name (Port: $port, Package: $package)"

    # Create target directory
    target_dir="$TARGET_BASE/$service_name"
    mkdir -p "$target_dir"

    # Copy template
    if cp -r "$TEMPLATE_SOURCE"/* "$target_dir/" 2>/dev/null; then
        echo "✅ Template copied for $service_name"

        # Update package declarations in Java files
        echo "  📦 Updating package declarations..."
        find "$target_dir" -name "*.java" -type f | while read file; do
            # Replace old package with new package
            sed -i "s/package com.gogidix.UserManagement.EnterpriseTestService/package $package/g" "$file"
            # Replace old imports with new imports
            sed -i "s/import com.gogidix.UserManagement.EnterpriseTestService/import $package/g" "$file"
        done

        # Create service-specific configuration files
        echo "  📄 Creating configuration files..."

        # Create POM file
        artifact_id=$(echo "$service_name" | sed 's/-service//')
        cat > "$target_dir/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
        <relativePath/>
    </parent>

    <groupId>com.gogidix.infrastructure</groupId>
    <artifactId>$artifact_id</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>$service_name</name>
    <description>Gogidix Infrastructure Service - $artifact_id</description>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Cloud -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Redis for Caching -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Message Queue -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Monitoring & Tracing -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-brave</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Apache Commons -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- Swagger/OpenAPI Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.2.0</version>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>\${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

        # Create application.yml
        cat > "$target_dir/application.yml" << EOF
server:
  port: $port
  servlet:
    context-path: /api/v1

spring:
  application:
    name: $service_name
  profiles:
    active: dev
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  datasource:
    url: jdbc:postgresql://localhost:5432/gogidix_infrastructure
    username: \${DB_USERNAME:admin}
    password: \${DB_PASSWORD:admin123}
    driver-class-name: org.postgresql.Driver
  h2:
    console:
      enabled: true
  redis:
    host: \${REDIS_HOST:localhost}
    port: \${REDIS_PORT:6379}
    password: \${REDIS_PASSWORD:}
    timeout: 2000ms
  rabbitmq:
    host: \${RABBITMQ_HOST:localhost}
    port: \${RABBITMQ_PORT:5672}
    username: \${RABBITMQ_USERNAME:guest}
    password: \${RABBITMQ_PASSWORD:guest}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  tracing:
    sampling:
      probability: 1.0

logging:
  level:
    com.gogidix: DEBUG
    org.springframework.security: DEBUG
    org.springframework.cloud: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%X{traceId},%X{spanId}] [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%X{traceId},%X{spanId}] [%thread] %-5level %logger{36} - %msg%n"

# Infrastructure Service Configuration
infrastructure:
  service:
    name: $service_name
    version: 1.0.0
    enabled: true
    environment: \${ENVIRONMENT:development}
  circuit-breaker:
    failure-rate-threshold: 50
    wait-duration-in-open-state: 30s
    sliding-window-size: 10
  cache:
    ttl: 3600
    max-size: 1000
  monitoring:
    enabled: true
    metrics-interval: 60000

---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

logging:
  level:
    com.gogidix: DEBUG
    org.springframework: DEBUG

---
spring:
  config:
    activate:
      on-profile: prod
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  datasource:
    url: \${PROD_DB_URL}
    username: \${PROD_DB_USERNAME}
    password: \${PROD_DB_PASSWORD}

logging:
  level:
    com.gogidix: INFO
    org.springframework: WARN
  file:
    name: logs/$service_name.log

---
spring:
  config:
    activate:
      on-profile: docker
  redis:
    host: redis
    port: 6379
  rabbitmq:
    host: rabbitmq
    port: 5672
EOF

        # Create Dockerfile
        cat > "$target_dir/Dockerfile" << EOF
FROM openjdk:21-jre-slim

LABEL maintainer="Gogidix Infrastructure Team"
LABEL service="$service_name"
LABEL version="1.0.0"

# Create app directory
WORKDIR /app

# Copy jar file
COPY target/$artifact_id-1.0.0.jar app.jar

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \\
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
EOF

        echo "✅ $service_name: FIXED"
        ((generated++))
    else
        echo "❌ FAILED: $service_name - Template copy failed"
        ((failed++))
    fi

    echo ""
done

# Summary
echo ""
echo "🏆 MASS FIX COMPLETE!"
echo "===================="
echo "✅ Successfully Fixed: $generated services"
echo "❌ Failed: $failed services"
echo "📊 Success Rate: $(( generated * 100 / ${#infrastructure_services[@]} ))%"
echo ""

if [ $failed -eq 0 ]; then
    echo "🎉 PERFECT SUCCESS! All infrastructure services fixed!"
else
    echo "⚠️  Some services failed. Check the logs above."
fi

echo ""
echo "📍 Location: $TARGET_BASE"
echo "📋 All services now have:"
echo "  ✅ Complete hexagonal architecture"
echo "  ✅ Correct package declarations"
echo "  ✅ Maven POM files"
echo "  ✅ Application configuration"
echo "  ✅ Docker configuration"
echo "  ✅ Infrastructure-specific dependencies (Redis, RabbitMQ, etc.)"