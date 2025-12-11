# Multi-stage Dockerfile for Production
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY src ./src

# Install dependencies and build the application
RUN ./mvnw package -DskipTests -Pprod

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Create non-root user
RUN addgroup -g 1001 -S appuser &&     adduser -S appuser -u 1001

# Set working directory
WORKDIR /app

# Copy the jar file
COPY --from=builder /build/target/ConfigManagementService-1.0.0.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs &&     chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3     CMD curl -f http://localhost:8080/configmanagementservice/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
