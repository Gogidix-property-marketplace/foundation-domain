# Start Gogidix Property Marketplace Local Development Environment
# This script starts all services locally using Docker Compose (PowerShell version)

# Colors for output
$Colors = @{
    Red = "Red"
    Green = "Green"
    Yellow = "Yellow"
    Blue = "Blue"
}

Write-Host "========================================" -ForegroundColor $Colors.Blue
Write-Host "🚀 Gogidix Property Marketplace" -ForegroundColor $Colors.Blue
Write-Host "    Local Development Environment" -ForegroundColor $Colors.Blue
Write-Host "========================================" -ForegroundColor $Colors.Blue

# Check prerequisites
function Check-Prerequisites {
    Write-Host "Checking prerequisites..." -ForegroundColor $Colors.Yellow

    # Check Docker
    try {
        $null = Get-Command docker -ErrorAction Stop
        $dockerVersion = docker --version
        Write-Host "✓ Docker found: $dockerVersion"
    } catch {
        Write-Host "Error: Docker is not installed" -ForegroundColor $Colors.Red
        Write-Host "Please install Docker from https://docs.docker.com/get-docker/"
        exit 1
    }

    # Check Docker Compose
    try {
        $null = Get-Command docker-compose -ErrorAction Stop
        $composeVersion = docker-compose --version
        Write-Host "✓ Docker Compose found: $composeVersion"
    } catch {
        Write-Host "Error: Docker Compose is not installed" -ForegroundColor $Colors.Red
        Write-Host "Please install Docker Compose from https://docs.docker.com/compose/install/"
        exit 1
    }

    # Check Docker daemon is running
    try {
        $null = docker info 2>&1
        Write-Host "✓ Docker daemon is running"
    } catch {
        Write-Host "Error: Docker daemon is not running" -ForegroundColor $Colors.Red
        Write-Host "Please start Docker Desktop"
        exit 1
    }

    # Check available disk space
    $disk = Get-WmiObject -Class Win32_LogicalDisk | Where-Object { $_.DeviceID -eq "C:" }
    $freeSpaceGB = [math]::Round($disk.FreeSpace / 1GB, 2)
    if ($freeSpaceGB -lt 10) {
        Write-Host "Warning: Low disk space ($freeSpaceGB GB available). Recommend at least 10GB." -ForegroundColor $Colors.Red
    }

    # Check available memory
    $totalMemoryGB = [math]::Round((Get-WmiObject -Class Win32_ComputerSystem).TotalPhysicalMemory / 1GB, 2)
    if ($totalMemoryGB -lt 8) {
        Write-Host "Warning: Low memory ($totalMemoryGB GB total). Recommend at least 8GB for optimal performance." -ForegroundColor $Colors.Red
    }

    Write-Host "✓ Prerequisites check passed" -ForegroundColor $Colors.Green
}

# Create necessary directories
function Create-Directories {
    Write-Host "Creating necessary directories..." -ForegroundColor $Colors.Yellow

    $directories = @(
        "logs\api-gateway",
        "logs\property-service",
        "logs\user-service",
        "logs\payment-service",
        "logs\rate-limiting-service",
        "logs\circuit-breaker-service",
        "monitoring\prometheus",
        "monitoring\grafana\provisioning\datasources",
        "monitoring\grafana\provisioning\dashboards",
        "nginx\ssl",
        "sql",
        "vault\config"
    )

    foreach ($dir in $directories) {
        $null = New-Item -Path $dir -ItemType Directory -Force
    }

    # Create Prometheus configuration
    $prometheusConfig = @"
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
"@

    if (!(Test-Path "monitoring\prometheus\prometheus.yml")) {
        $prometheusConfig | Out-File -FilePath "monitoring\prometheus\prometheus.yml" -Encoding utf8
    }

    # Create Grafana datasources
    $grafanaDatasources = @"
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
"@

    if (!(Test-Path "monitoring\grafana\provisioning\datasources\prometheus.yml")) {
        $grafanaDatasources | Out-File -FilePath "monitoring\grafana\provisioning\datasources\prometheus.yml" -Encoding utf8
    }

    Write-Host "✓ Directories created" -ForegroundColor $Colors.Green
}

# Create database initialization script
function Create-DatabaseInit {
    if (!(Test-Path "sql\init.sql")) {
        $sqlInit = @"
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
"@

        $sqlInit | Out-File -FilePath "sql\init.sql" -Encoding utf8
    }
}

# Generate SSL certificates for local development
function Generate-SSLCerts {
    Write-Host "Generating SSL certificates..." -ForegroundColor $Colors.Yellow

    if (!(Test-Path "nginx\ssl\gogidix.local.crt")) {
        # Create self-signed certificate using OpenSSL (if available)
        try {
            $opensslCmd = @"
req -x509 -nodes -days 365 -newkey rsa:2048 -keyout nginx/ssl/gogidix.local.key -out nginx/ssl/gogidix.local.crt -subj "/C=US/ST=CA/L=San Francisco/O=Gogidix/CN=localhost" -addext "subjectAltName=DNS:localhost,DNS:*.gogidix.local"
"@
            openssl $opensslCmd.Split(" ")
            Write-Host "✓ SSL certificates generated" -ForegroundColor $Colors.Green
        } catch {
            Write-Host "Warning: Could not generate SSL certificates. SSL will be disabled." -ForegroundColor $Colors.Yellow
            # Disable SSL in nginx config
            $nginxConfig = Get-Content "nginx\nginx.conf" -Raw
            $nginxConfig = $nginxConfig -replace "listen 443 ssl;", "listen 443 ssl; # SSL disabled"
            $nginxConfig | Out-File -FilePath "nginx\nginx.conf" -Encoding utf8
        }
    }
}

# Start services
function Start-Services {
    Write-Host "Starting services with Docker Compose..." -ForegroundColor $Colors.Yellow

    # Pull all images first
    Write-Host "Pulling Docker images..." -ForegroundColor $Colors.Blue
    docker-compose pull

    # Start infrastructure services first
    Write-Host "Starting infrastructure services..." -ForegroundColor $Colors.Blue
    docker-compose up -d zookeeper kafka postgres redis elasticsearch

    # Wait for infrastructure to be ready
    Write-Host "Waiting for services to be ready..." -ForegroundColor $Colors.Yellow
    Start-Sleep -Seconds 30

    # Start monitoring services
    Write-Host "Starting monitoring services..." -ForegroundColor $Colors.Blue
    docker-compose up -d prometheus grafana kibana jaeger

    # Start application services
    Write-Host "Starting application services..." -ForegroundColor $Colors.Blue
    docker-compose up -d api-gateway rate-limiting-service property-service user-service payment-service circuit-breaker-service

    # Start additional services
    Write-Host "Starting additional services..." -ForegroundColor $Colors.Blue
    docker-compose up -d keycloak vault consul rabbitmq minio

    # Wait for all services to be healthy
    Write-Host "Waiting for all services to be healthy..." -ForegroundColor $Colors.Yellow
    Start-Sleep -Seconds 60
}

# Check service health
function Check-Health {
    Write-Host "Checking service health..." -ForegroundColor $Colors.Yellow

    $services = @(
        @{Name="API Gateway"; Port=8080; Health="/actuator/health"},
        @{Name="Property Service"; Port=8082; Health="/actuator/health"},
        @{Name="User Service"; Port=8083; Health="/actuator/health"},
        @{Name="Payment Service"; Port=8084; Health="/actuator/health"},
        @{Name="Rate Limiting Service"; Port=8085; Health="/actuator/health"},
        @{Name="Circuit Breaker Service"; Port=8086; Health="/actuator/health"}
    )

    $allHealthy = $true

    foreach ($service in $services) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$($service.Port)$($service.Health)" -TimeoutSec 5
            Write-Host "✓ $($service.Name) is healthy" -ForegroundColor $Colors.Green
        } catch {
            Write-Host "✗ $($service.Name) is not healthy" -ForegroundColor $Colors.Red
            $allHealthy = $false
        }
    }

    if ($allHealthy) {
        Write-Host "✅ All services are healthy!" -ForegroundColor $Colors.Green
    } else {
        Write-Host "❌ Some services are not healthy. Check logs for details." -ForegroundColor $Colors.Red
    }
}

# Show URLs
function Show-URLs {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor $Colors.Blue
    Write-Host "🎉 Services are running locally!" -ForegroundColor $Colors.Green
    Write-Host "========================================" -ForegroundColor $Colors.Blue
    Write-Host ""

    Write-Host "Application URLs:" -ForegroundColor $Colors.Yellow
    Write-Host "  • API Gateway:        http://localhost/api/"
    Write-Host "  • Property Service:    http://localhost:8082/actuator/health"
    Write-Host "  • User Service:        http://localhost:8083/actuator/health"
    Write-Host "  • Payment Service:     http://localhost:8084/actuator/health"
    Write-Host "  • Rate Limiting Service: http://localhost:8085/actuator/health"

    Write-Host ""
    Write-Host "Monitoring URLs:" -ForegroundColor $Colors.Yellow
    Write-Host "  • Grafana Dashboard:  http://localhost:3000"
    Write-Host "    Username: admin, Password: gogidix123"
    Write-Host "  • Kibana Logs:        http://localhost:5601"
    Write-Host "  • Prometheus:         http://localhost:9090"
    Write-Host "  • Jaeger Tracing:      http://localhost:16686"

    Write-Host ""
    Write-Host "Infrastructure URLs:" -ForegroundColor $Colors.Yellow
    Write-Host "  • Keycloak:           http://localhost:8080"
    Write-Host "    Username: admin, Password: gogidix123"
    Write-Host "  • MinIO Console:       http://localhost:9001"
    Write-Host "    Username: minioadmin, Password: minioadmin123"
    Write-Host "  • Vault:              http://localhost:8200"
    Write-Host "  • Consul UI:           http://localhost:8500"
    Write-Host "  • RabbitMQ Management: http://localhost:15672"
    Write-Host "    Username: gogidix, Password: gogidix123"

    Write-Host ""
    Write-Host "Databases:" -ForegroundColor $Colors.Yellow
    Write-Host "  • PostgreSQL:          localhost:5432"
    Write-Host "  • Redis:               localhost:6379"
    Write-Host "  • Kafka:               localhost:9092"
    Write-Host "  • Elasticsearch:        localhost:9200"

    Write-Host ""
    Write-Host "Useful Commands:" -ForegroundColor $Colors.Yellow
    Write-Host "  • View logs:           docker-compose logs -f [service-name]"
    Write-Host "  • Stop all services:    docker-compose down"
    Write-Host "  • Restart service:     docker-compose restart [service-name]"
    Write-Host "  • Scale service:        docker-compose up -d --scale [service-name]=3"
}

# Main execution
try {
    Check-Prerequisites
    Create-Directories
    Create-DatabaseInit
    Generate-SSLCerts
    Start-Services
    Check-Health
    Show-URLs

    Write-Host ""
    Write-Host "✅ Local development environment is ready!" -ForegroundColor $Colors.Green
    Write-Host "To stop all services, run: docker-compose down" -ForegroundColor $Colors.Yellow
    Write-Host ""
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor $Colors.Red
    exit 1
}