# Gogidix Centralized Dashboard - Docker Makefile
# Provides convenient commands for managing the dockerized services

.PHONY: help build up down logs clean test dev prod status health

# Default target
help: ## Show this help message
	@echo "Gogidix Centralized Dashboard - Docker Commands"
	@echo "=============================================="
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

# Build commands
build: ## Build all Docker images
	@echo "Building all Docker images..."
	docker-compose build

build-java: ## Build only Java services
	@echo "Building Java services..."
	docker-compose build agent-dashboard-service alert-management-service analytics-service centralized-dashboard custom-dashboard-builder executive-dashboard metrics-service provider-dashboard-service reporting-service

build-nodejs: ## Build only Node.js services
	@echo "Building Node.js services..."
	docker-compose build alert-center-web analytics-dashboard-web custom-report-builder dashboard-web executive-dashboard-web real-time-dashboard shared-components-web visualization-web

# Service management commands
up: ## Start all services in production mode
	@echo "Starting all services..."
	docker-compose up -d

up-dev: ## Start all services in development mode
	@echo "Starting all services in development mode..."
	docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d

up-java: ## Start only Java services
	@echo "Starting Java services..."
	docker-compose up -d agent-dashboard-service alert-management-service analytics-service centralized-dashboard custom-dashboard-builder executive-dashboard metrics-service provider-dashboard-service reporting-service

up-nodejs: ## Start only Node.js services
	@echo "Starting Node.js services..."
	docker-compose up -d alert-center-web analytics-dashboard-web custom-report-builder dashboard-web executive-dashboard-web real-time-dashboard shared-components-web visualization-web

up-databases: ## Start only database services
	@echo "Starting database services..."
	docker-compose up -d postgres-centralized redis-cache

up-monitoring: ## Start monitoring services
	@echo "Starting monitoring services..."
	docker-compose up -d prometheus grafana

down: ## Stop all services
	@echo "Stopping all services..."
	docker-compose down

down-clean: ## Stop services and remove volumes
	@echo "Stopping services and removing volumes..."
	docker-compose down -v

# Logging commands
logs: ## Show logs for all services
	docker-compose logs -f

logs-java: ## Show logs for Java services
	docker-compose logs -f agent-dashboard-service alert-management-service analytics-service centralized-dashboard custom-dashboard-builder executive-dashboard metrics-service provider-dashboard-service reporting-service

logs-nodejs: ## Show logs for Node.js services
	docker-compose logs -f alert-center-web analytics-dashboard-web custom-report-builder dashboard-web executive-dashboard-web real-time-dashboard shared-components-web visualization-web

logs-databases: ## Show logs for databases
	docker-compose logs -f postgres-centralized redis-cache

logs-monitoring: ## Show logs for monitoring
	docker-compose logs -f prometheus grafana

# Service specific logs
logs-%: ## Show logs for a specific service (e.g., make logs-agent-dashboard-service)
	docker-compose logs -f $*

# Status and health commands
status: ## Show status of all services
	@echo "Service Status:"
	@docker-compose ps

health: ## Check health of all services
	@echo "Checking service health..."
	@docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
	@echo ""
	@echo "Service Health Checks:"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Testing commands
test: ## Run integration tests
	@echo "Running integration tests..."
	docker-compose exec centralized-dashboard mvn test

test-integration: ## Run domain integration tests
	@echo "Running domain integration tests..."
	docker-compose exec centralized-dashboard mvn test -Dtest=DomainIntegrationTest

# Database commands
db-migrate: ## Run database migrations
	@echo "Running database migrations..."
	docker-compose exec postgres-centralized psql -U gogidix_user -d centralized_dashboard -c "SELECT version();"

db-connect: ## Connect to PostgreSQL database
	@echo "Connecting to PostgreSQL database..."
	docker-compose exec postgres-centralized psql -U gogidix_user -d centralized_dashboard

redis-connect: ## Connect to Redis
	@echo "Connecting to Redis..."
	docker-compose exec redis-cache redis-cli

# Development commands
dev: ## Start development environment with hot reload
	@echo "Starting development environment..."
	make up-dev
	@echo "Development environment started!"
	@echo "Access services:"
	@echo "  - Main Dashboard: http://localhost:8013"
	@echo "  - Grafana: http://localhost:3000 (admin/gogidix_admin)"
	@echo "  - Prometheus: http://localhost:9090"

prod: ## Start production environment
	@echo "Starting production environment..."
	make up
	@echo "Production environment started!"
	@echo "Access services:"
	@echo "  - Main Dashboard: http://localhost:80"
	@echo "  - Grafana: http://localhost:3000 (admin/gogidix_admin)"
	@echo "  - Prometheus: http://localhost:9090"

# Cleanup commands
clean: ## Clean up unused Docker resources
	@echo "Cleaning up Docker resources..."
	docker system prune -f
	docker volume prune -f

clean-all: ## Complete cleanup including all images
	@echo "Performing complete cleanup..."
	docker-compose down -v --rmi all
	docker system prune -af
	docker volume prune -f

# Monitoring commands
monitor: ## Open monitoring dashboards
	@echo "Opening monitoring dashboards..."
	@if command -v xdg-open > /dev/null; then \
		xdg-open http://localhost:3000; \
		xdg-open http://localhost:9090; \
	elif command -v open > /dev/null; then \
		open http://localhost:3000; \
		open http://localhost:9090; \
	else \
		echo "Please open these URLs manually:"; \
		echo "  Grafana: http://localhost:3000 (admin/gogidix_admin)"; \
		echo "  Prometheus: http://localhost:9090"; \
	fi

# Backup commands
backup: ## Backup all data volumes
	@echo "Creating backups..."
	mkdir -p ./backups
	docker-compose exec postgres-centralized pg_dump -U gogidix_user centralized_dashboard > ./backups/postgres_backup_$(shell date +%Y%m%d_%H%M%S).sql
	docker-compose exec redis-cache redis-cli BGSAVE
	@echo "Backup completed in ./backups/"

# Utility commands
shell-java: ## Open shell in a Java service
	@read -p "Enter service name (agent-dashboard-service, alert-management-service, etc.): " service; \
	docker-compose exec $$service bash

shell-nodejs: ## Open shell in a Node.js service
	@read -p "Enter service name (alert-center-web, dashboard-web, etc.): " service; \
	docker-compose exec $$service sh

reload: ## Reload configuration without downtime
	@echo "Reloading services..."
	docker-compose restart
	@echo "Services reloaded!"

# Quick start commands
quick-start: ## Quick start for development
	@echo "Quick starting development environment..."
	make build
	make up-databases
	sleep 10
	make up-java
	make up-nodejs
	make up-monitoring
	@echo "Quick start complete!"
	@echo "Access main dashboard at: http://localhost:8013"

quick-start-prod: ## Quick start for production
	@echo "Quick starting production environment..."
	make build
	make up
	@echo "Quick start complete!"
	@echo "Access main dashboard at: http://localhost:80"