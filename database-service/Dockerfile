# Dockerfile for Foundation Database Service
# Production-ready container with security and performance optimizations

ARG BUILD_VERSION=1.0.0
ARG BUILD_PROFILE=prod

# Use base Dockerfile
FROM gogidix/foundation-base:${BUILD_VERSION}

# Set specific labels
LABEL service="foundation-database-service" \
      version="${BUILD_VERSION}" \
      description="Foundation Database Service - PostgreSQL, MongoDB, Redis management"

# Set build arguments
ARG BUILD_VERSION
ARG BUILD_PROFILE

# Copy service-specific application configuration
COPY --chown=gogidix:gogidix docker/application-database.yml application-database.yml
COPY --chown=gogidix:gogidix docker/application-database-${BUILD_PROFILE}.yml application-database-${BUILD_PROFILE}.yml

# Override Java options for Database Service (more memory for connection pooling)
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=80.0 -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=logs/heapdump.hprof -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${BUILD_PROFILE} \
               -Ddb.connection.pool.maxSize=50 -Ddb.connection.pool.initialSize=10"

# Override Spring Boot options for Database Service
ENV SPRING_OPTS="--spring.config.location=classpath:/application.yml,/opt/gogidix/application.yml,/opt/gogidix/application-database.yml,/opt/gogidix/application-database-${BUILD_PROFILE}.yml \
                 --logging.config=/opt/gogidix/logback-spring.xml \
                 --management.endpoints.web.exposure.include=health,info,metrics,prometheus,db \
                 --management.endpoint.health.show-details=always \
                 --management.metrics.export.prometheus.enabled=true"

# Additional health check for database connectivity
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/db && \
        curl -f http://localhost:8080/actuator/health || exit 1

# Add service-specific volumes
VOLUME ["/opt/gogidix/logs", "/opt/gogidix/config", "/opt/gogidix/db-backups"]