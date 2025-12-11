# Ethical AI Service Dockerfile
# Multi-stage build with security best practices

# Build stage
FROM python:3.9-slim as builder

# Set build arguments
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# Set environment variables for build
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    git \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Create and activate virtual environment
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Copy requirements
COPY requirements.txt requirements-ethical-ai.txt ./

# Install Python dependencies
RUN pip install --upgrade pip setuptools wheel && \
    pip install -r requirements-ethical-ai.txt

# Production stage
FROM python:3.9-slim

# Set labels
LABEL maintainer="AI Services Team <ai-team@gogidix.com>" \
      org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/opt/venv/bin:$PATH" \
    PYTHONPATH="/app" \
    ENVIRONMENT=production

# Create non-root user
RUN groupadd -r ethicalai && useradd -r -g ethicalai ethicalai

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    libpq5 \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Copy virtual environment from builder
COPY --from=builder /opt/venv /opt/venv

# Create application directories
WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/logs /app/ethical_reports /app/uploads && \
    chown -R ethicalai:ethicalai /app

# Copy application code
COPY --chown=ethicalai:ethicalai src/ ./src/
COPY --chown=ethicalai:ethicalai scripts/ ./scripts/

# Install the application
RUN pip install -e .

# Switch to non-root user
USER ethicalai

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/ethical-ai/health || exit 1

# Expose port
EXPOSE 8000

# Default command
CMD ["python", "-m", "src.gogidix_ai.ethical_ai.main"]