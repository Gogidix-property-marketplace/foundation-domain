#!/bin/bash

# Local Development Environment Setup Script
# This script sets up the Gogidix AI Services for local development

set -e  # Exit on any error

echo "🚀 Setting up Gogidix AI Services Local Development Environment"
echo "=============================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_step() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check prerequisites
echo ""
echo "Checking prerequisites..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker first."
    exit 1
fi
print_step "Docker found: $(docker --version)"

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi
print_step "Docker Compose found: $(docker-compose --version)"

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    print_error "Python 3 is not installed. Please install Python 3.9 or higher."
    exit 1
fi
print_step "Python found: $(python3 --version)"

# Check if pip is installed
if ! command -v pip &> /dev/null; then
    print_error "pip is not installed. Please install pip first."
    exit 1
fi

# Check GPU availability (optional)
if command -v nvidia-smi &> /dev/null; then
    print_step "NVIDIA GPU detected"
    GPU_SUPPORT=true
else
    print_warning "No NVIDIA GPU detected. CPU-only mode will be used."
    GPU_SUPPORT=false
fi

# Create necessary directories
echo ""
echo "Creating directories..."

mkdir -p data
mkdir -p models/property_valuation_v1
mkdir -p logs
mkdir -p monitoring/grafana/dashboards
mkdir -p monitoring/grafana/datasources
mkdir -p nginx/ssl
mkdir -p scripts

print_step "Directories created"

# Create environment file
echo ""
echo "Creating environment file..."

if [ ! -f .env ]; then
    cat > .env << EOF
# Database Configuration
DATABASE_URL=postgresql://gogidix:gogidix123@localhost:5432/gogidix_ai
REDIS_URL=redis://localhost:6379

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Elasticsearch Configuration
ELASTICSEARCH_URL=http://localhost:9200

# MLflow Configuration
MLFLOW_TRACKING_URI=http://localhost:5000

# Service Configuration
ENVIRONMENT=development
DEBUG=true
LOG_LEVEL=INFO

# Authentication
API_KEY=sk-gogidix-local-1234567890abcdef
JWT_SECRET=your-super-secret-jwt-key-change-in-production

# GPU Support
GPU_ENABLED=${GPU_SUPPORT}

# OpenAI API (Optional - for enhanced NLP features)
OPENAI_API_KEY=your-openai-api-key-here

# Hugging Face Token (Optional - for custom models)
HUGGINGFACE_TOKEN=your-huggingface-token-here

# MinIO/S3 Configuration (Optional)
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=gogidix-ai-data
EOF
    print_step ".env file created"
else
    print_warning ".env file already exists. Skipping creation."
fi

# Build Docker image
echo ""
echo "Building Docker image..."

# Check if base image exists
if docker images | grep -q "gogidix/ai-services"; then
    print_warning "Docker image already exists. Rebuilding..."
    docker-compose -f docker-compose.local.yml build --no-cache
else
    docker-compose -f docker-compose.local.yml build
fi

print_step "Docker image built successfully"

# Create monitoring configuration
echo ""
echo "Setting up monitoring..."

# Prometheus configuration
mkdir -p monitoring
cat > monitoring/prometheus.yml << EOF
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'ai-gateway'
    static_configs:
      - targets: ['ai-gateway:8000']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'property-intelligence'
    static_configs:
      - targets: ['property-intelligence:8001']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'conversational-ai'
    static_configs:
      - targets: ['conversational-ai:8002']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'analytics'
    static_configs:
      - targets: ['analytics:8003']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'ml-platform'
    static_configs:
      - targets: ['ml-platform:8004']
    metrics_path: /metrics
    scrape_interval: 10s

  - job_name: 'ethical-ai'
    static_configs:
      - targets: ['ethical-ai:8005']
    metrics_path: /metrics
    scrape_interval: 10s
EOF

# Grafana datasource configuration
mkdir -p monitoring/grafana/datasources
cat > monitoring/grafana/datasources/prometheus.yml << EOF
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
EOF

print_step "Monitoring configuration created"

# Create Nginx configuration
echo ""
echo "Setting up Nginx..."

mkdir -p nginx
cat > nginx/nginx.conf << EOF
events {
    worker_connections 1024;
}

http {
    upstream ai_gateway {
        server ai-gateway:8000;
    }

    upstream property_intelligence {
        server property-intelligence:8001;
    }

    upstream conversational_ai {
        server conversational-ai:8002;
    }

    upstream analytics {
        server analytics:8003;
    }

    upstream ml_platform {
        server ml-platform:8004;
    }

    upstream ethical_ai {
        server ethical-ai:8005;
    }

    server {
        listen 80;
        server_name localhost;

        # API Gateway (main entry point)
        location /api/v1/ {
            proxy_pass http://ai_gateway;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
        }

        # Property Intelligence (direct access)
        location /property-intelligence/ {
            proxy_pass http://property_intelligence/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
        }

        # Conversational AI (direct access)
        location /chat/ {
            proxy_pass http://conversational_ai/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
        }

        # Analytics (direct access)
        location /analytics/ {
            proxy_pass http://analytics/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
        }

        # ML Platform (direct access)
        location /ml/ {
            proxy_pass http://ml_platform/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
        }

        # Ethical AI (direct access)
        location /ethical-ai/ {
            proxy_pass http://ethical_ai/;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
        }

        # Health checks
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
EOF

print_step "Nginx configuration created"

# Create database initialization script
echo ""
echo "Setting up database..."

cat > scripts/init-db.sql << EOF
-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS ai_services;
CREATE SCHEMA IF NOT EXISTS mlflow;

-- Grant permissions
GRANT ALL ON SCHEMA ai_services TO gogidix;
GRANT ALL ON SCHEMA mlflow TO gogidix;
EOF

print_step "Database initialization script created"

# Create local startup script
echo ""
echo "Creating startup script..."

cat > start_local.sh << 'EOF'
#!/bin/bash

# Start Gogidix AI Services Locally

echo "🚀 Starting Gogidix AI Services..."

# Start infrastructure services
echo "Starting infrastructure..."
docker-compose -f docker-compose.local.yml up -d postgres redis kafka zookeeper elasticsearch

# Wait for infrastructure
echo "Waiting for services to be ready..."
sleep 10

# Start MLflow
echo "Starting MLflow..."
docker-compose -f docker-compose.local.yml up -d mlflow

# Start monitoring
echo "Starting monitoring..."
docker-compose -f docker-compose.local.yml up -d prometheus grafana

# Start core services
echo "Starting core AI services..."
docker-compose -f docker-compose.local.yml up -d ai-gateway

# Wait for gateway
echo "Waiting for AI Gateway..."
sleep 5

# Start all services
echo "Starting all AI services..."
docker-compose -f docker-compose.local.yml up -d property-intelligence conversational-ai analytics ml-platform ethical-ai

# Generate synthetic data
echo "Generating synthetic data..."
docker-compose -f docker-compose.local.yml up data-processor

echo ""
echo "✅ All services started!"
echo ""
echo "📊 Service URLs:"
echo "  • AI Gateway:         http://localhost:8000"
echo "  • Property Intel:     http://localhost:8001"
echo "  • Conversational AI:  http://localhost:8002"
echo "  • Analytics:          http://localhost:8003"
echo "  • ML Platform:        http://localhost:8004"
echo "  • Ethical AI:         http://localhost:8005"
echo "  • MLflow UI:          http://localhost:5000"
echo "  • Grafana Dashboard:  http://localhost:3000 (admin/admin)"
echo "  • Prometheus:         http://localhost:9090"
echo "  • Jupyter Lab:        http://localhost:8888"
echo ""
echo "🗄️  Database URLs:"
echo "  • PostgreSQL:         postgresql://gogidix:gogidix123@localhost:5432/gogidix_ai"
echo "  • Redis:              redis://localhost:6379"
echo "  • Kafka:              localhost:9092"
echo "  • Elasticsearch:      http://localhost:9200"
echo ""
echo "📝 API Documentation:"
echo "  • Swagger UI:         http://localhost:8000/docs"
echo "  • ReDoc:              http://localhost:8000/redoc"
echo ""
echo "To train models:"
echo "  docker-compose -f docker-compose.local.yml up --build model-trainer"
echo ""
echo "To stop all services:"
echo "  docker-compose -f docker-compose.local.yml down"
echo ""
EOF

chmod +x start_local.sh
print_step "Startup script created"

# Create development script
echo ""
echo "Creating development helper scripts..."

cat > dev_shell.sh << 'EOF'
#!/bin/bash

# Development Shell - Access the AI Services container

docker exec -it gogidix-ai-gateway bash
EOF

chmod +x dev_shell.sh

cat > train_models.sh << 'EOF'
#!/bin/bash

# Train Models Locally

echo "🤖 Training property valuation model..."

docker-compose -f docker-compose.local.yml up --build model-trainer

echo "✅ Model training complete!"
echo "Check the models/ directory for trained models."
EOF

chmod +x train_models.sh

cat > test_api.sh << 'EOF'
#!/bin/bash

# Test API Endpoints

echo "🧪 Testing API endpoints..."

# Test health endpoint
echo "Testing health endpoint..."
curl -s http://localhost:8000/health | jq .

# Test property valuation
echo -e "\nTesting property valuation..."
curl -X POST http://localhost:8000/api/v1/property-valuation \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk-gogidix-local-1234567890abcdef" \
  -d '{
    "property_type": "apartment",
    "bedrooms": 2,
    "bathrooms": 2,
    "square_feet": 1200,
    "year_built": 2010,
    "city": "New York",
    "state": "NY"
  }' | jq .

# Test chat
echo -e "\nTesting conversational AI..."
curl -X POST http://localhost:8000/api/v1/chat \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk-gogidix-local-1234567890abcdef" \
  -d '{
    "message": "What are the best neighborhoods in NYC for families?",
    "conversation_id": "test-123"
  }' | jq .

echo -e "\n✅ API tests complete!"
EOF

chmod +x test_api.sh

print_step "Development scripts created"

# Final steps
echo ""
echo "🎉 Setup complete!"
echo ""
echo "Next steps:"
echo "1. Start the services:      ./start_local.sh"
echo "2. Train the models:        ./train_models.sh"
echo "3. Test the APIs:          ./test_api.sh"
echo "4. Access Jupyter Lab:     http://localhost:8888"
echo "5. View Grafana:           http://localhost:3000"
echo ""
print_warning "Note: The first start may take a few minutes as Docker images are downloaded."

echo ""
echo "📚 Useful commands:"
echo "  • View logs:          docker-compose -f docker-compose.local.yml logs -f [service-name]"
echo "  • Access shell:       ./dev_shell.sh"
echo "  • Stop services:      docker-compose -f docker-compose.local.yml down"
echo "  • Rebuild services:   docker-compose -f docker-compose.local.yml up --build"
echo ""
echo "🔧 Configuration:"
echo "  • Edit .env file to change settings"
echo "  • Add OPENAI_API_KEY for enhanced NLP features"
echo "  • Set GPU_ENABLED=true if you have NVIDIA GPU and Docker GPU support"
echo ""

print_step "Local development environment is ready! 🚀"