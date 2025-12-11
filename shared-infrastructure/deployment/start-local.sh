#!/bin/bash

# Start Gogidix Property Marketplace Local Development Environment
# This script starts all services locally using Docker Compose

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🚀 Gogidix Property Marketplace${NC}"
echo -e "${BLUE}    Local Development Environment${NC}"
echo -e "${BLUE}========================================${NC}"

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"

    # Check Docker
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Error: Docker is not installed${NC}"
        echo "Please install Docker from https://docs.docker.com/get-docker/"
        exit 1
    fi

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}Error: Docker Compose is not installed${NC}"
        echo "Please install Docker Compose from https://docs.docker.com/compose/install/"
        exit 1
    fi

    # Check Docker daemon is running
    if ! docker info &> /dev/null; then
        echo -e "${RED}Error: Docker daemon is not running${NC}"
        echo "Please start Docker daemon"
        exit 1
    fi

    # Check available disk space (at least 10GB)
    available_space=$(df -BG / | tail -1 | awk '{print $4}' | sed 's/G//')
    if [ "$available_space" -lt 10 ]; then
        echo -e "${RED}Warning: Low disk space (${available_space}GB available). Recommend at least 10GB.${NC}"
    fi

    # Check available memory (at least 8GB)
    available_memory=$(free -g | awk '/^Mem:/{print $7}' | sed 's/G//')
    if [ "$available_memory" -lt 8 ]; then
        echo -e "${RED}Warning: Low memory (${available_memory}GB available). Recommend at least 8GB for optimal performance.${NC}"
    fi

    echo -e "${GREEN}✓ Prerequisites check passed${NC}"
}

# Create necessary directories
create_directories() {
    echo -e "${YELLOW}Creating necessary directories...${NC}"

    mkdir -p logs/api-gateway
    mkdir -p logs/property-service
    mkdir -p logs/user-service
    mkdir -p logs/payment-service
    mkdir -p logs/rate-limiting-service
    mkdir -p logs/circuit-breaker-service

    mkdir -p monitoring/prometheus
    mkdir -p monitoring/grafana/provisioning/datasources
    mkdir -p monitoring/grafana/provisioning/dashboards

    mkdir -p nginx/ssl

    mkdir -p sql
    mkdir -p vault/config

    # Create Prometheus configuration
    if [ ! -f monitoring/prometheus/prometheus.yml ]; then
        cat > monitoring/prometheus/prometheus.yml <<'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "first_rules.yml"
  - "second_rules.yml"

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8081']

  - job_name: 'property-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['property-service:8082']

  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['user-service:8083']

  - job_name: 'payment-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['payment-service:8084']

  - job_name: 'rate-limiting-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['rate-limiting-service:8085']

  - job_name: 'circuit-breaker-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['circuit-breaker-service:8086']
EOF
    fi

    # Create Grafana datasources
    if [ ! -f monitoring/grafana/provisioning/datasources/prometheus.yml ]; then
        cat > monitoring/grafana/provisioning/datasources/prometheus.yml <<'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
EOF
    fi

    echo -e "${GREEN}✓ Directories created${NC}"
}

# Create database initialization script
create_db_init() {
    if [ ! -f sql/init.sql ]; then
        cat > sql/init.sql <<'EOF'
-- Initialize Gogidix Property Marketplace Database

-- Create Keycloak database
CREATE DATABASE keycloak;

-- Create Keycloak user
CREATE USER keycloak WITH PASSWORD 'keycloak123';
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;

-- Create application schemas
\c gogidix_property;

-- Create tables
CREATE TABLE IF NOT EXISTS properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    property_type VARCHAR(50),
    bedrooms INTEGER,
    bathrooms INTEGER,
    square_feet INTEGER,
    year_built INTEGER,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    owner_id UUID
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_properties_city ON properties(city);
CREATE INDEX IF NOT EXISTS idx_properties_status ON properties(status);
CREATE INDEX IF NOT EXISTS idx_properties_owner ON properties(owner_id);
CREATE INDEX IF NOT EXISTS idx_properties_price ON properties(price);
CREATE INDEX IF NOT EXISTS idx_properties_type ON properties(property_type);

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'user',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO properties (title, description, price, city, state, property_type, bedrooms, bathrooms) VALUES
('Modern Apartment in Downtown', 'Beautiful 2BR apartment with city view', 250000.00, 'New York', 'NY', 'apartment', 2, 2),
('Spacious House with Garden', '3BR house with large backyard', 450000.00, 'San Francisco', 'CA', 'house', 3, 2),
('Cozy Studio Near Beach', 'Studio apartment walking distance to beach', 150000.00, 'Miami', 'FL', 'studio', 1, 1);

-- Insert sample users
INSERT INTO users (email, first_name, last_name, phone, role) VALUES
('john.doe@example.com', 'John', 'Doe', '555-0123', 'user'),
('jane.smith@example.com', 'Jane', 'Smith', '555-0456', 'agent'),
('admin@gogidix.com', 'Admin', 'User', '555-0789', 'admin');
EOF
    fi
}

# Generate SSL certificates for local development
generate_ssl_certs() {
    echo -e "${YELLOW}Generating SSL certificates...${NC}"

    if [ ! -f nginx/ssl/gogidix.local.crt ]; then
        # Create self-signed certificate
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout nginx/ssl/gogidix.local.key \
            -out nginx/ssl/gogidix.local.crt \
            -subj "/C=US/ST=CA/L=San Francisco/O=Gogidix/CN=localhost" \
            -addext "subjectAltName=DNS:localhost,DNS:*.gogidix.local"

        echo -e "${GREEN}✓ SSL certificates generated${NC}"
    fi
}

# Create Nginx configuration
create_nginx_config() {
    if [ ! -f nginx/nginx.conf ]; then
        cat > nginx/nginx.conf <<'EOF'
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    # Logging
    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;
    error_log   /var/log/nginx/error.log   warn;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml+rss
        application/atom+xml
        image/svg+xml;

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=one:10m rate=10r/s;

    # Upstream servers
    upstream api_gateway {
        server api-gateway:8080;
    }

    upstream grafana {
        server grafana:3000;
    }

    upstream kibana {
        server kibana:5601;
    }

    # Main server block
    server {
        listen 80;
        listen 443 ssl;
        server_name localhost gogidix.local;

        # SSL configuration
        ssl_certificate     /etc/nginx/ssl/gogidixlocal.crt;
        ssl_certificate_key /etc/nginx/ssl/gogidixlocal.key;

        # Security headers
        add_header X-Frame-Options DENY;
        add_header X-Content-Type-Options nosniff;
        add_header X-XSS-Protection "1; mode=block";
        add_header Referrer-Policy "strict-origin-when-cross-origin";

        # API Gateway
        location /api/ {
            limit_req zone=one burst=20 nodelay;
            proxy_pass http://api_gateway;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Grafana
        location /grafana/ {
            proxy_pass http://grafana;
            proxy_set_header Host $host;
        }

        # Kibana
        location /kibana/ {
            proxy_pass http://kibana;
            proxy_set_header Host $host;
        }

        # Static files
        location / {
            root /usr/share/nginx/html;
            index index.html;
        }
    }
}
EOF
    fi
}

# Start services
start_services() {
    echo -e "${YELLOW}Starting services with Docker Compose...${NC}"

    # Pull all images first
    echo -e "${BLUE}Pulling Docker images...${NC}"
    docker-compose pull

    # Start infrastructure services first
    echo -e "${BLUE}Starting infrastructure services...${NC}"
    docker-compose up -d zookeeper kafka postgres redis elasticsearch

    # Wait for infrastructure to be ready
    echo -e "${YELLOW}Waiting for services to be ready...${NC}"
    sleep 30

    # Start monitoring services
    echo -e "${BLUE}Starting monitoring services...${NC}"
    docker-compose up -d prometheus grafana kibana jaeger

    # Start application services
    echo -e "${BLUE}Starting application services...${NC}"
    docker-compose up -d api-gateway rate-limiting-service property-service user-service payment-service circuit-breaker-service

    # Start additional services
    echo -e "${BLUE}Starting additional services...${NC}"
    docker-compose up -d keycloak vault consul rabbitmq minio

    # Wait for all services to be healthy
    echo -e "${YELLOW}Waiting for all services to be healthy...${NC}"
    sleep 60
}

# Check service health
check_health() {
    echo -e "${YELLOW}Checking service health...${NC}"

    services=("api-gateway:8080" "property-service:8082" "user-service:8083" "payment-service:8084" "rate-limiting-service:8085" "circuit-breaker-service:8086")

    all_healthy=true

    for service in "${services[@]}"; do
        service_name=$(echo $service | cut -d: -f1)
        port=$(echo $service | cut -d: -f2)

        if curl -f http://localhost:$port/actuator/health &> /dev/null; then
            echo -e "${GREEN}✓ $service_name is healthy${NC}"
        else
            echo -e "${RED}✗ $service_name is not healthy${NC}"
            all_healthy=false
        fi
    done

    if [ "$all_healthy" = true ]; then
        echo -e "${GREEN}✅ All services are healthy!${NC}"
    else
        echo -e "${RED}❌ Some services are not healthy. Check logs for details.${NC}"
    fi
}

# Show URLs
show_urls() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${GREEN}🎉 Services are running locally!${NC}"
    echo -e "${BLUE}========================================${NC}\n"

    echo -e "${YELLOW}Application URLs:${NC}"
    echo -e "  • API Gateway:        ${GREEN}http://localhost/api/${NC}"
    echo -e "  • Property Service:    ${GREEN}http://localhost:8082/actuator/health${NC}"
    echo -e "  • User Service:        ${GREEN}http://localhost:8083/actuator/health${NC}"
    echo -e "  • Payment Service:     ${GREEN}http://localhost:8084/actuator/health${NC}"
    echo -e "  • Rate Limiting Service: ${GREEN}http://localhost:8085/actuator/health${NC}"

    echo -e "\n${YELLOW}Monitoring URLs:${NC}"
    echo -e "  • Grafana Dashboard:  ${GREEN}http://localhost:3000${NC}"
    echo -e "    Username: ${BLUE}admin${NC}, Password: ${BLUE}gogidix123${NC}"
    echo -e "  • Kibana Logs:        ${GREEN}http://localhost:5601${NC}"
    echo -e "  • Prometheus:         ${GREEN}http://localhost:9090${NC}"
    echo -e "  • Jaeger Tracing:      ${GREEN}http://localhost:16686${NC}"

    echo -e "\n${YELLOW}Infrastructure URLs:${NC}"
    echo -e "  • Keycloak:           ${GREEN}http://localhost:8080${NC}"
    echo -e "    Username: ${BLUE}admin${NC}, Password: ${BLUE}gogidix123${NC}"
    echo -e "  • MinIO Console:       ${GREEN}http://localhost:9001${NC}"
    echo -e "    Username: ${BLUE}minioadmin${NC}, Password: ${BLUE}minioadmin123${NC}"
    echo -e "  • Vault:              ${GREEN}http://localhost:8200${NC}"
    echo -e "  • Consul UI:           ${GREEN}http://localhost:8500${NC}"
    echo -e "  • RabbitMQ Management: ${GREEN}http://localhost:15672${NC}"
    echo -e "    Username: ${BLUE}gogidix${NC}, Password: ${BLUE}gogidix123${NC}"

    echo -e "\n${YELLOW}Databases:${NC}"
    echo -e "  • PostgreSQL:          localhost:5432"
    echo -e "  • Redis:               localhost:6379"
    echo -e "  • Kafka:               localhost:9092"
    echo -e "  • Elasticsearch:        localhost:9200"

    echo -e "\n${YELLOW}Useful Commands:${NC}"
    echo -e "  • View logs:           ${BLUE}docker-compose logs -f [service-name]${NC}"
    echo -e "  • Stop all services:    ${BLUE}docker-compose down${NC}"
    echo -e "  • Restart service:     ${BLUE}docker-compose restart [service-name]${NC}"
    echo -e "  • Scale service:        ${BLUE}docker-compose up -d --scale [service-name]=3${NC}"
}

# Main execution
main() {
    check_prerequisites
    create_directories
    create_db_init
    generate_ssl_certs
    create_nginx_config
    start_services
    check_health
    show_urls

    echo -e "\n${GREEN}✅ Local development environment is ready!${NC}"
    echo -e "${YELLOW}To stop all services, run: ${BLUE}docker-compose down${NC}\n"
}

# Handle script interruption
trap 'echo -e "\n${RED}Script interrupted. Stopping services...${NC}; docker-compose down; exit 1' INT

# Run main function
main "$@"