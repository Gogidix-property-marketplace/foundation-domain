#!/bin/bash

# 📊 MASS GENERATION: CENTRALIZED DASHBOARD SERVICES
# Generate all 8 Dashboard Services for Gogidix Platform
# Enterprise Hexagonal Architecture Template

echo "📊 MASS GENERATION: CENTRALIZED DASHBOARD SERVICES"
echo "=================================================="
echo "🎯 Total Services: 8 Dashboard Services"
echo "🏗️  Template: Enterprise Hexagonal Architecture"
echo "⚡  Technology: Spring Boot 3.x + Java 21"
echo ""

# Array of all dashboard services with their configurations
declare -A dashboard_services=(
    ["agent-dashboard-service"]="8301:com.gogidix.dashboard.agent"
    ["alert-management-service"]="8302:com.gogidix.dashboard.alerts"
    ["analytics-service"]="8303:com.gogidix.dashboard.analytics"
    ["custom-dashboard-builder"]="8304:com.gogidix.dashboard.builder"
    ["executive-dashboard"]="8305:com.gogidix.dashboard.executive"
    ["metrics-service"]="8306:com.gogidix.dashboard.metrics"
    ["provider-dashboard-service"]="8307:com.gogidix.dashboard.provider"
    ["reporting-service"]="8308:com.gogidix.dashboard.reporting"
)

# Count total services
total_services=${#dashboard_services[@]}
echo "📊 Services to Generate: $total_services"
echo ""

# Counter for progress tracking
generated_count=0
failed_count=0

# Function to generate a single dashboard service
generate_dashboard_service() {
    local service_name="$1"
    local port_package="$2"
    local port="${port_package%%:*}"
    local package="${port_package#*:}"

    echo "⚡ Generating: $service_name (Port: $port)"

    # Change to CLI directory
    cd "C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-code-Generator-Factory/gogidix-java-cli"

    # Generate the service
    java -jar target/gogidix-java-cli-1.0.0.jar init "$service_name" \
        --domain Dashboard \
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
            echo "📁 Copying $service_name to dashboard domain..."
            mkdir -p "C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/centralized-dashboard/java-services/${service_name}"

            # Remove existing content (except README.md)
            find "C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/centralized-dashboard/java-services/${service_name}" -mindepth 1 -not -name "README.md" -exec rm -rf {} + 2>/dev/null

            # Copy new content
            cp -r "generated/${service_name}/"* "C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/centralized-dashboard/java-services/${service_name}/"
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
echo "🚀 STARTING MASS GENERATION (Dashboard Services)"
echo "================================================"

# Generate services in batches of 3 for better resource management
batch_size=3
current_batch=1

for service_name in "${!dashboard_services[@]}"; do
    port_package="${dashboard_services[$service_name]}"

    # Display progress
    echo "📈 Progress: $generated_count/$total_services services generated"
    echo "🎯 Current Batch: $current_batch (Every 3 services)"
    echo ""

    # Generate the service
    generate_dashboard_service "$service_name" "$port_package"

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
    echo "🎉 PERFECT SUCCESS! All $total_services dashboard services generated successfully!"
    echo ""
    echo "📍 Location: C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/centralized-dashboard/java-services/"
    echo ""
    echo "📋 Generated Dashboard Services:"
    for service_name in "${!dashboard_services[@]}"; do
        port_package="${dashboard_services[$service_name]}"
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
echo "2. Run production readiness test"
echo "3. Fix any package declaration issues"
echo "4. Add missing configuration files if needed"