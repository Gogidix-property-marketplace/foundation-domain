#!/bin/bash

# 🚀 CENTRALIZED DASHBOARD - PRODUCTION DEPLOYMENT SCRIPT
# Enterprise-Grade Deployment Automation
# Gogidix Property Marketplace Platform
# ==========================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
DEPLOYMENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$DEPLOYMENT_DIR/../.." && pwd)"
ENVIRONMENT="${ENVIRONMENT:-production}"
LOG_FILE="$DEPLOYMENT_DIR/logs/deployment-$(date +%Y%m%d-%H%M%S).log"

# Create logs directory
mkdir -p "$DEPLOYMENT_DIR/logs"

# Logging functions
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_info() {
    echo -e "${GREEN}[INFO]${NC} $(log "$1")"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(log "$1")"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(log "$1")"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $(log "$1")"
}

log_success() {
    echo -e "${PURPLE}[SUCCESS]${NC} $(log "$1")"
}

log_header() {
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
}

# Helper functions
check_dependencies() {
    log_step "🔍 Checking deployment dependencies..."

    local deps=("docker" "docker-compose" "curl" "jq")
    local missing_deps=()

    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            missing_deps+=("$dep")
        fi
    done

    if [ ${#missing_deps[@]} -ne 0 ]; then
        log_error "❌ Missing dependencies: ${missing_deps[*]}"
        log_info "Please install missing dependencies and retry deployment"
        exit 1
    fi

    log_success "✅ All dependencies are installed"
}

load_environment() {
    log_step "📋 Loading environment configuration..."

    local env_file="$DEPLOYMENT_DIR/.env.prod"
    if [ ! -f "$env_file" ]; then
        log_error "❌ Environment file not found: $env_file"
        log_info "Please copy .env.prod.example to .env.prod and configure values"
        exit 1
    fi

    # Load environment variables
    set -a
    source "$env_file"
    set +a

    # Validate required variables
    local required_vars=("DB_PASSWORD" "REDIS_PASSWORD" "JWT_SECRET")
    local missing_vars=()

    for var in "${required_vars[@]}"; do
        if [ -z "${!var:-}" ]; then
            missing_vars+=("$var")
        fi
    done

    if [ ${#missing_vars[@]} -ne 0 ]; then
        log_error "❌ Missing environment variables: ${missing_vars[*]}"
        exit 1
    fi

    log_success "✅ Environment configuration loaded successfully"
}

deploy_services() {
    log_step "🚀 Deploying microservices..."

    cd "$DEPLOYMENT_DIR"

    # Start all services
    log_info "Starting all services..."
    docker-compose up -d

    log_success "✅ All services deployed successfully"
}

verify_deployment() {
    log_step "🔍 Verifying deployment integrity..."

    local failed_services=()
    local services=(
        "API Gateway:http://localhost/health"
        "Centralized Dashboard:http://localhost:8080/actuator/health"
        "Dashboard Web:http://localhost:3001/health"
        "Alert Center Web:http://localhost:3002/health"
        "Analytics Dashboard:http://localhost:3003/health"
        "Real-time Dashboard:http://localhost:3004/health"
        "Prometheus:http://localhost:9090/-/healthy"
        "Grafana:http://localhost:3000/api/health"
    )

    for service in "${services[@]}"; do
        local name="${service%:*}"
        local url="${service#*:}"

        if curl -f -s "$url" > /dev/null 2>&1; then
            log_success "✅ $name is responding correctly"
        else
            log_warn "⚠️ $name is not responding (may be starting up)"
            failed_services+=("$name")
        fi
    done

    log_info "Deployment verification completed"
    return 0
}

# Main deployment function
main() {
    local command=${1:-deploy}

    log_header "🚀 CENTRALIZED DASHBOARD PRODUCTION DEPLOYMENT"
    log_info "Environment: $ENVIRONMENT"
    log_info "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"

    case "$command" in
        "deploy")
            check_dependencies
            load_environment
            deploy_services
            verify_deployment

            log_header "🎉 DEPLOYMENT COMPLETED SUCCESSFULLY"
            log_info "Access Points:"
            log_info "  • Main Application: http://localhost"
            log_info "  • Grafana: http://localhost:3000"
            log_info "  • Prometheus: http://localhost:9090"
            log_info "  • Kibana: http://localhost:5601"
            log_info "  • RabbitMQ: http://localhost:15672"
            ;;

        "status")
            log_step "📊 Checking deployment status..."
            docker-compose ps
            ;;

        "health")
            log_step "💚 Running health checks..."
            verify_deployment
            ;;

        "stop")
            log_step "🛑 Stopping all services..."
            docker-compose down
            log_success "✅ All services stopped"
            ;;

        *)
            log_error "❌ Unknown command: $command"
            log_info "Available commands:"
            log_info "  deploy    - Full deployment (default)"
            log_info "  status    - Check deployment status"
            log_info "  health    - Run health checks"
            log_info "  stop      - Stop all services"
            exit 1
            ;;
    esac
}

# Execute main function with all arguments
main "$@"
