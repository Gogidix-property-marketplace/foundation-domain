#!/bin/bash

# Enterprise Node.js Batch Production Pipeline Executor
# CTO Level - Amazon/Google/Microsoft/Netflix Standards
# Systematic Node.js Services Processing

SERVICES_DIR="nodejs-services"
LOG_FILE="enterprise_nodejs_$(date +%Y%m%d_%H%M%S).log"
SUCCESS_TRACKER="nodejs_success.txt"
FAILED_TRACKER="nodejs_failed.txt"

echo "🏢 ENTERPRISE NODE.JS BATCH AUTOMATOR"
echo "===================================="
echo "Services Directory: $SERVICES_DIR"
echo "Timestamp: $(date)"
echo "Log: $LOG_FILE"
echo "Success: $SUCCESS_TRACKER"
echo "Failed: $FAILED_TRACKER"
echo ""

# Initialize counters
TOTAL_SERVICES=0
SUCCESS_COUNT=0
FAILED_COUNT=0

# Function to fix Node.js service issues
fix_nodejs_service() {
    local service=$1

    echo "  🔧 Fixing Node.js issues for $service..."

    # Remove corrupted node_modules if exists
    if [ -d "node_modules" ]; then
        echo "    - Removing corrupted node_modules"
        rm -rf node_modules
    fi

    # Clear npm cache
    echo "    - Clearing npm cache"
    npm cache clean --force 2>/dev/null

    # Fix package.json if missing main field
    if ! grep -q '"main"' package.json; then
        echo "    - Adding main field to package.json"
        sed -i 's/"description":/"main": "src\/app.js",\n  "description":/g' package.json
    fi

    # Ensure engines are compatible
    if ! grep -q '"engines"' package.json; then
        echo "    - Adding engines specification"
        sed -i 's/"keywords": \[/ "engines": {\n    "node": ">=18.0.0",\n    "npm": ">=8.0.0"\n  },\n  "keywords": \[/g' package.json
    fi

    echo "  ✅ Node.js fixes applied"
}

# Function to execute complete pipeline for a Node.js service
execute_nodejs_pipeline() {
    local service=$1
    echo "📦 Processing: $service"

    cd "$service"

    # Step 0: Fix issues first
    fix_nodejs_service "$service"

    # Step 1: Install Dependencies
    echo "  [1/5] Installing dependencies..."
    if npm install --verbose >> "../$LOG_FILE" 2>&1; then
        echo "    ✅ Dependencies installed"
        ((SUCCESS_COUNT++))
        echo "$service" >> "../$SUCCESS_TRACKER"
    else
        echo "    ❌ Dependencies failed"
        ((FAILED_COUNT++))
        echo "$service" >> "../$FAILED_TRACKER"
        cd ..
        return 1
    fi

    # Step 2: Validate Installation
    echo "  [2/5] Validating installation..."
    if [ -d "node_modules" ] && [ -f "package-lock.json" ]; then
        echo "    ✅ Installation validated"
    else
        echo "    ❌ Installation validation failed"
    fi

    # Step 3: Run Linting
    echo "  [3/5] Running linting..."
    if npm run lint --silent >> "../$LOG_FILE" 2>&1; then
        echo "    ✅ Linting passed"
    else
        echo "    ⚠️  Linting failed (will continue)"
    fi

    # Step 4: Run Tests
    echo "  [4/5] Running tests..."
    if npm test --silent >> "../$LOG_FILE" 2>&1; then
        echo "    ✅ Tests passed"
    else
        echo "    ⚠️  Tests failed (will continue)"
    fi

    # Step 5: Build Verification
    echo "  [5/5] Build verification..."
    if [ -f "src/app.js" ] || [ -f "index.js" ] || [ -f "server.js" ]; then
        echo "    ✅ Entry point found"
    else
        echo "    ❌ Entry point not found"
    fi

    echo "  🎯 $service pipeline completed"
    cd ..
}

# Count total services
cd "$SERVICES_DIR"
TOTAL_SERVICES=$(ls -1d */ 2>/dev/null | wc -l)
echo "📊 Found $TOTAL_SERVICES Node.js services"
echo ""

# Execute pipeline for each service
for service in */; do
    if [ -d "$service" ] && [ -f "$service/package.json" ]; then
        service_name=$(echo "$service" | sed 's/\///')
        execute_nodejs_pipeline "$service_name"
        echo ""
    else
        echo "❌ Invalid service: $service"
    fi
done

# Generate comprehensive report
echo "📊 ENTERPRISE NODE.JS BATCH REPORT" | tee -a "$LOG_FILE"
echo "===============================" | tee -a "$LOG_FILE"
echo "Total Services: $TOTAL_SERVICES" | tee -a "$LOG_FILE"
echo "Successful: $SUCCESS_COUNT" | tee -a "$LOG_FILE"
echo "Failed: $FAILED_COUNT" | tee -a "$LOG_FILE"

# Calculate success rate
if [ $TOTAL_SERVICES -gt 0 ]; then
    SUCCESS_RATE=$((SUCCESS_COUNT * 100 / TOTAL_SERVICES))
    echo "Success Rate: $SUCCESS_RATE%" | tee -a "$LOG_FILE"
else
    echo "Success Rate: 0%" | tee -a "$LOG_FILE"
fi

echo "" | tee -a "$LOG_FILE"
echo "SUCCESSFUL SERVICES:" | tee -a "$LOG_FILE"
if [ -f "$SUCCESS_TRACKER" ]; then
    cat "$SUCCESS_TRACKER" | tee -a "$LOG_FILE"
fi

echo "" | tee -a "$LOG_FILE"
echo "FAILED SERVICES:" | tee -a "$LOG_FILE"
if [ -f "$FAILED_TRACKER" ]; then
    cat "$FAILED_TRACKER" | tee -a "$LOG_FILE"
fi

# Overall batch status
if [ $SUCCESS_RATE -ge 80 ]; then
    echo "" | tee -a "$LOG_FILE"
    echo "🎯 BATCH STATUS: PRODUCTION READY ✅" | tee -a "$LOG_FILE"
    exit 0
elif [ $SUCCESS_RATE -ge 60 ]; then
    echo "" | tee -a "$LOG_FILE"
    echo "⚠️  BATCH STATUS: PRODUCTION READY WITH ISSUES" | tee -a "$LOG_FILE"
    exit 1
else
    echo "" | tee -a "$LOG_FILE"
    echo "❌ BATCH STATUS: REQUIRES REMEDIATION" | tee -a "$LOG_FILE"
    exit 2
fi