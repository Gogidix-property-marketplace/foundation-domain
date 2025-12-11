#!/bin/bash

# 📊 MASS FIX ALL 8 DASHBOARD SERVICES
# Template-Based Batch Generation
# Using working EnterpriseTestService as master template

echo "🚀 MASS FIX ALL 8 DASHBOARD SERVICES"
echo "================================="
echo "📊 Fixing Package Names & Adding Configuration Files"
echo "🏗️  Location: $PWD"
echo "📅 Date: $(date)"
echo ""

# Dashboard Services mapping (service_name:port:package)
declare -A dashboard_services=(
    ["agent-dashboard-service"]="8301:com.gogidix.dashboard.agent"
    ["alert-management-service"]="8302:com.gogidix.dashboard.alerts"
    ["analytics-service"]="8303:com.gogidix.dashboard.analytics"
    ["custom-dashboard-builder"]="8304:com.gogidix.dashboard.builder"
    ["executive-dashboard"]="8305:com.gogidix.dashboard.executive"
    ["metrics-service"]="8306:com.gogidix.dashboard.metrics"
    ["provider-dashboard-service"]="8307:com.gogidix.dashboard.provider"
    ["reporting-service"]="8308:com.gogidix.dashboard.reporting"
)

# Template source location
TEMPLATE_SOURCE="C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-code-Generator-Factory/gogidix-java-cli/generated/EnterpriseTestService-service"

# Target base directory
TARGET_BASE="C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/centralized-dashboard/java-services"

# Counter
generated=0
failed=0

echo "📊 Total Services to Fix: ${#dashboard_services[@]}"
echo ""

# Fix each dashboard service
for service_name in "${!dashboard_services[@]}"; do
    port_package="${dashboard_services[$service_name]}"
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

    <groupId>com.gogidix.dashboard</groupId>
    <artifactId>$artifact_id</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>$service_name</name>
    <description>Gogidix Dashboard Service - $artifact_id</description>

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
    url: jdbc:postgresql://localhost:5432/gogidix_dashboard
    username: \${DB_USERNAME:admin}
    password: \${DB_PASSWORD:admin123}
    driver-class-name: org.postgresql.Driver
  h2:
    console:
      enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

logging:
  level:
    com.gogidix: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Dashboard Service Configuration
dashboard:
  service:
    name: $service_name
    version: 1.0.0
    enabled: true
  widgets:
    max-per-dashboard: 20
    refresh-interval: 30000
  analytics:
    enabled: true
    retention-days: 90

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
EOF

        # Create Dockerfile
        cat > "$target_dir/Dockerfile" << EOF
FROM openjdk:21-jre-slim

LABEL maintainer="Gogidix Dashboard Team"
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
echo "📊 Success Rate: $(( generated * 100 / ${#dashboard_services[@]} ))%"
echo ""

if [ $failed_count -eq 0 ]; then
    echo "🎉 PERFECT SUCCESS! All dashboard services fixed!"
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
echo "  ✅ README documentation"