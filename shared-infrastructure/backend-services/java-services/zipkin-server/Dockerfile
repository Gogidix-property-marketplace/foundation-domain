FROM openjdk:21-jdk-slim

LABEL maintainer="gogidix-infra@gogidix.com"
LABEL version="1.0.0"
LABEL description="Zipkin Distributed Tracing Server"

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    netcat \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy jar
COPY target/zipkin-server-*.jar app.jar

# Create health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9411/health || exit 1

# Expose port
EXPOSE 9411

# Add user
RUN groupadd -r zipkin && useradd -r -g zipkin zipkin
USER zipkin

# Run the application
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]