FROM openjdk:21-jre-slim

LABEL maintainer="Gogidix Dashboard Team"
LABEL service="alert-management-service"
LABEL version="1.0.0"

# Create app directory
WORKDIR /app

# Copy jar file
COPY target/alert-management-1.0.0.jar app.jar

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
