FROM python:3.9-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    git \
    curl \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create directories for models and experiments
RUN mkdir -p /app/models /app/experiments /app/logs

# Expose port
EXPOSE 9045

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:9045/health || exit 1

# Set environment variables
ENV PYTHONPATH=/app
ENV MODEL_DIR=/app/models
ENV EXPERIMENT_DIR=/app/experiments

# Run the application
CMD ["python", "main.py"]