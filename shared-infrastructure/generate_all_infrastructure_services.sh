#!/bin/bash

# 🏗️ MASS GENERATION: SHARED INFRASTRUCTURE SERVICES
# Generate all 39 Infrastructure Services for Gogidix Platform
# Enterprise Hexagonal Architecture Template

echo "🏗️ MASS GENERATION: SHARED INFRASTRUCTURE SERVICES"
echo "================================================="
echo "🎯 Total Services: 39 Infrastructure Services"
echo "🏗️  Template: Enterprise Hexagonal Architecture"
echo "⚡  Technology: Spring Boot 3.x + Java 21"
echo ""

# Array of all infrastructure services with their configurations
declare -A infrastructure_services=(
    # Core Infrastructure Services (Ports 8000-8099)
    ["aggregation-service"]="8000:com.gogidix.infrastructure.aggregation"
    ["api-gateway"]="8001:com.gogidix.infrastructure.gateway"
    ["authentication-service"]="8002:com.gogidix.infrastructure.auth"
    ["authorization-service"]="8003:com.gogidix.infrastructure.authorization"
    ["backup-service"]="8004:com.gogidix.infrastructure.backup"
    ["cache-service"]="8005:com.gogidix.infrastructure.cache"
    ["circuit-breaker-service"]="8006:com.gogidix.infrastructure.circuit"
    ["config-server"]="8007:com.gogidix.infrastructure.config"
    ["correlation-id-service"]="8008:com.gogidix.infrastructure.correlation"
    ["disaster-recovery-service"]="8009:com.gogidix.infrastructure.disaster"
    ["distributed-tracing-service"]="8010:com.gogidix.infrastructure.tracing"
    ["dns-service"]="8011:com.gogidix.infrastructure.dns"
    ["eureka-server"]="8012:com.gogidix.infrastructure.discovery"
    ["file-storage-service"]="8013:com.gogidix.infrastructure.storage"
    ["health-check-service"]="8014:com.gogidix.infrastructure.health"

    # Management & Operations Services (Ports 8015-8024)
    ["idempotency-service"]="8015:com.gogidix.infrastructure.idempotency"
    ["load-balancer"]="8016:com.gogidix.infrastructure.loadbalancer"
    ["logging-service"]="8017:com.gogidix.infrastructure.logging"
    ["message-broker-service"]="8018:com.gogidix.infrastructure.broker"
    ["message-queue-service"]="8019:com.gogidix.infrastructure.queue"
    ["metrics-collection-service"]="8020:com.gogidix.infrastructure.metrics"
    ["migration-service"]="8021:com.gogidix.infrastructure.migration"
    ["monitoring-service"]="8022:com.gogidix.infrastructure.monitoring"
    ["notification-service"]="8023:com.gogidix.infrastructure.notification"
    ["oauth2-service"]="8024:com.gogidix.infrastructure.oauth2"

    # Security & Integration Services (Ports 8025-8034)
    ["rate-limiting-service"]="8025:com.gogidix.infrastructure.ratelimit"
    ["registry-service"]="8026:com.gogidix.infrastructure.registry"
    ["resource-allocation-service"]="8027:com.gogidix.infrastructure.allocation"
    ["scheduler-service"]="8028:com.gogidix.infrastructure.scheduler"
    ["search-service"]="8029:com.gogidix.infrastructure.search"
    ["secret-management-service"]="8030:com.gogidix.infrastructure.secrets"
    ["security-service"]="8031:com.gogidix.infrastructure.security"
    ["service-mesh-service"]="8032:com.gogidix.infrastructure.mesh"
    ["service-registry-service"]="8033:com.gogidix.infrastructure.serviceregistry"
    ["session-management-service"]="8034:com.gogidix.infrastructure.session"

    # Advanced Infrastructure Services (Ports 8035-8039)
    ["task-management-service"]="8035:com.gogidix.infrastructure.task"
    ["user-management-service"]="8036:com.gogidix.infrastructure.usermgmt"
    ["version-control-service"]="8037:com.gogidix.infrastructure.version"
    ["workflow-engine"]="8038:com.gogidix.infrastructure.workflow"
    ["zookeeper-service"]="8039:com.gogidix.infrastructure.zookeeper"
)

# Count total services
total_services=${#infrastructure_services[@]}
echo "🏗️ Services to Generate: $total_services"
echo ""

# Counter for progress tracking
generated_count=0
failed_count=0

# Function to generate a single infrastructure service
generate_infrastructure_service() {
    local service_name="$1"
    local port_package="$2"
    local port="${port_package%%:*}"
    local package="${port_package#*:}"

    echo "⚡ Generating: $service_name (Port: $port)"

    # Change to CLI directory
    cd "C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-code-Generator-Factory/gogidix-java-cli"

    # Generate the service
    java -jar target/gogidix-java-cli-1.0.0.jar init "$service_name" \
        --domain Infrastructure \
        --package "$package" \
        --template enterprise \
        --port "$port" \
        --verbose > "logs/${service_name}.log" 2>&1

    local exit_code=$?

    if [ $exit_code -eq 0 ]; then
        echo "✅ SUCCESS: $service_name generated"
        ((generated_count++))

        # Copy to correct location
        if [ -d "generated/${service_name}" ]; then
            echo "📁 Copying $service_name to infrastructure domain..."
            mkdir -p "C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/java-services/${service_name}"

            # Remove existing content (except README.md)
            find "C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/java-services/${service_name}" -mindepth 1 -not -name "README.md" -exec rm -rf {} + 2>/dev/null

            # Copy new content
            cp -r "generated/${service_name}/"* "C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/java-services/${service_name}/"
            echo "✅ $service_name copied successfully"
        fi
    else
        echo "❌ FAILED: $service_name (Exit Code: $exit_code)"
        ((failed_count++))
        echo "📄 Check logs/${service_name}.log for details"
    fi

    echo ""
}

# Create logs directory
mkdir -p "C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-code-Generator-Factory/gogidix-java-cli/logs"

# Start mass generation
echo "🚀 STARTING MASS GENERATION (Infrastructure Services)"
echo "================================================="

# Generate services in batches of 5 for better resource management
batch_size=5
current_batch=1

for service_name in "${!infrastructure_services[@]}"; do
    port_package="${infrastructure_services[$service_name]}"

    # Display progress
    echo "📈 Progress: $generated_count/$total_services services generated"
    echo "🎯 Current Batch: $current_batch (Every 5 services)"
    echo ""

    # Generate the service
    generate_infrastructure_service "$service_name" "$port_package"

    # Update batch counter
    if ((generated_count % batch_size == 0)) && [ $generated_count -gt 0 ]; then
        ((current_batch++))
        echo "🔄 Batch $((current_batch-1)) completed. Short pause..."
        sleep 2
    fi
done

# Final Summary
echo ""
echo "🏆 MASS GENERATION COMPLETE!"
echo "=========================="
echo "✅ Successfully Generated: $generated_count services"
echo "❌ Failed: $failed_count services"
echo "📊 Success Rate: $(( generated_count * 100 / total_services ))%"
echo ""

if [ $failed_count -eq 0 ]; then
    echo "🎉 PERFECT SUCCESS! All $total_services infrastructure services generated successfully!"
    echo ""
    echo "📍 Location: C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/java-services/"
    echo ""
    echo "🏗️ Generated Infrastructure Services:"
    for service_name in "${!infrastructure_services[@]}"; do
        port_package="${infrastructure_services[$service_name]}"
        port="${port_package%%:*}"
        package="${port_package#*:}"
        echo "  ✅ $service_name (Port: $port, Package: $package)"
    done
else
    echo "⚠️  Some services failed. Check logs/ directory for details."
fi

echo ""
echo "📋 Next Steps:"
echo "1. Verify all services are in the correct location"
echo "2. Run infrastructure mass fix script for package declarations"
echo "3. Run production readiness test"
echo "4. Fix any remaining configuration issues"