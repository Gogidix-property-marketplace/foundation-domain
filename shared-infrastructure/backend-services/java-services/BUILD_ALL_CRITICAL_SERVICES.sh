#!/bin/bash

# Build All Critical Infrastructure Services for Production
# This script builds the most critical services in parallel

echo "======================================================"
echo "Building Critical Infrastructure Services"
echo "======================================================"

# Set base path
BASE_PATH="C:/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/backend-services/java-services"

# Critical services that must be built first
declare -a critical_services=(
    "api-gateway"
    "authentication-service"
    "authorization-service"
    "oauth2-service"
    "config-server"
    "eureka-server"
    "metrics-service"
    "logging-service"
)

# Function to build a service
build_service() {
    local service=$1
    local start_time=$(date +%s)

    echo ""
    echo "------------------------------------------------------"
    echo "Building $service"
    echo "------------------------------------------------------"

    cd "$BASE_PATH/$service"

    # Check if pom.xml exists
    if [ ! -f "pom.xml" ]; then
        echo "⚠️  No pom.xml found for $service - Skipping"
        return 1
    fi

    # Clean and compile
    echo "[1/3] Cleaning..."
    mvn clean -q
    if [ $? -ne 0 ]; then
        echo "❌ Clean failed for $service"
        return 1
    fi

    echo "[2/3] Compiling..."
    mvn compile -q
    if [ $? -ne 0 ]; then
        echo "❌ Compile failed for $service"
        return 1
    fi

    echo "[3/3] Packaging (without tests)..."
    mvn package -q -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ Package failed for $service"
        return 1
    fi

    # Check if JAR was created
    jar_count=$(find target -name "*.jar" -not -name "*-sources.jar" | wc -l)
    if [ $jar_count -gt 0 ]; then
        end_time=$(date +%s)
        duration=$((end_time - start_time))
        echo "✅ SUCCESS: $service (${duration}s) - JAR files created: $jar_count"
        return 0
    else
        echo "⚠️  WARNING: $service compiled but no JAR found"
        return 1
    fi
}

# Export function for parallel execution
export -f build_service
export BASE_PATH

# Build all services in parallel (max 4 at a time)
echo ""
echo "Starting parallel build of ${#critical_services[@]} critical services..."
echo "Maximum parallel jobs: 4"

# Use parallel execution if available, otherwise sequential
if command -v parallel &> /dev/null; then
    printf '%s\n' "${critical_services[@]}" | parallel -j 4 build_service
else
    # Fallback to sequential with background processes
    pids=()
    for service in "${critical_services[@]}"; do
        build_service "$service" &
        pids+=($!)
        # Limit to 4 concurrent jobs
        if [ ${#pids[@]} -ge 4 ]; then
            for pid in "${pids[@]}"; do
                wait $pid
            done
            pids=()
        fi
    done

    # Wait for remaining jobs
    for pid in "${pids[@]}"; do
        wait $pid
    done
fi

echo ""
echo "======================================================"
echo "Build Summary"
echo "======================================================"

# Count successful builds
success_count=0
total_services=${#critical_services[@]}

for service in "${critical_services[@]}"; do
    if [ -d "$BASE_PATH/$service/target" ]; then
        jar_count=$(find "$BASE_PATH/$service/target" -name "*.jar" -not -name "*-sources.jar" | wc -l)
        if [ $jar_count -gt 0 ]; then
            echo "✅ $service - JAR created"
            ((success_count++))
        else
            echo "❌ $service - No JAR created"
        fi
    else
        echo "❌ $service - Build failed"
    fi
done

echo ""
echo "Build Results:"
echo "- Successful: $success_count/$total_services services"
echo "- Failed: $((total_services - success_count))/$total_services services"

if [ $success_count -eq $total_services ]; then
    echo ""
    echo "🎉 All critical services built successfully!"
    echo "Next step: Run tests and generate JAR files for production"
else
    echo ""
    echo "⚠️  Some services failed to build. Check logs above for details."
fi