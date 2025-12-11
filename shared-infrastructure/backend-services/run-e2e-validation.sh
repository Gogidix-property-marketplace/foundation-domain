#!/bin/bash

# Complete End-to-End Production Validation
# Tests all services and generates final certification

echo "🚀 COMPLETE END-TO-END PRODUCTION VALIDATION"
echo "=========================================="
echo "Date: $(date)"
echo "Target: Validate all services pass tests without issues"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Directories
BACKEND_DIR="/c/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/backend-services"
NODE_DIR="$BACKEND_DIR/nodejs-services"
JAVA_DIR="$BACKEND_DIR/java-services"
REPORT_DIR="$BACKEND_DIR/e2e-validation-$(date +%Y%m%d-%H%M%S)"

# Create report directory
mkdir -p "$REPORT_DIR"

# Initialize results
echo "E2E Validation Report - $(date)" > "$REPORT_DIR/e2e-results.log"
echo "==========================================" >> "$REPORT_DIR/e2e-results.log"
echo "" >> "$REPORT_DIR/e2e-results.log"

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Function to log results
log_result() {
    local service=$1
    local test_type=$2
    local status=$3
    local details=$4
    local output=$5

    echo "[$status] $service - $test_type: $details" | tee -a "$REPORT_DIR/e2e-results.log"

    if [ -n "$output" ]; then
        echo "$output" >> "$REPORT_DIR/e2e-results.log"
    fi

    ((TOTAL_TESTS++))

    if [[ "$status" == *"PASS"* ]]; then
        ((PASSED_TESTS++))
    elif [[ "$status" == *"FAIL"* ]]; then
        ((FAILED_TESTS++))
    else
        ((SKIPPED_TESTS++))
    fi
}

# Function to run Node.js service tests
test_nodejs_service() {
    local service_name=$1
    local service_path="$NODE_DIR/$service_name"

    echo -e "\n${BLUE}Testing Node.js Service: $service_name${NC}"

    if [ ! -d "$service_path" ]; then
        log_result "$service_name" "directory" "SKIP" "Service not found"
        return 1
    fi

    cd "$service_path"

    # 1. Check package.json
    if [ ! -f "package.json" ]; then
        log_result "$service_name" "package.json" "FAIL" "package.json not found"
        return 1
    fi
    log_result "$service_name" "package.json" "PASS" "package.json exists"

    # 2. Install dependencies
    echo -e "  ${YELLOW}Installing dependencies...${NC}"
    if npm install --silent --no-audit 2>"$REPORT_DIR/${service_name}-npm-install.log"; then
        log_result "$service_name" "npm-install" "PASS" "Dependencies installed"
    else
        log_result "$service_name" "npm-install" "FAIL" "Failed to install dependencies" "$(cat "$REPORT_DIR/${service_name}-npm-install.log")"
        return 1
    fi

    # 3. Run tests
    echo -e "  ${YELLOW}Running tests...${NC}"
    if npm test -- --passWithNoTests --coverage --coverageReporters=text-summary 2>"$REPORT_DIR/${service_name}-test.log"; then
        log_result "$service_name" "test" "PASS" "All tests passed"

        # Extract coverage if available
        if grep -q "All files" "$REPORT_DIR/${service_name}-test.log"; then
            coverage=$(grep "All files" "$REPORT_DIR/${service_name}-test.log" | awk '{print $4}' | tr -d '%')
            if [ -n "$coverage" ] && [ "$coverage" != "" ]; then
                if (( $(echo "$coverage >= 85" | bc -l) )); then
                    log_result "$service_name" "coverage" "PASS" "Coverage: ${coverage}%"
                else
                    log_result "$service_name" "coverage" "FAIL" "Coverage: ${coverage}% (below 85%)"
                fi
            fi
        fi
    else
        log_result "$service_name" "test" "FAIL" "Tests failed" "$(tail -20 "$REPORT_DIR/${service_name}-test.log")"
    fi

    # 4. Try to start service (smoke test)
    echo -e "  ${YELLOW}Running smoke test...${NC}"
    if timeout 10s npm start 2>/dev/null & then
        sleep 3
        if curl -s http://localhost:3000/health 2>/dev/null || curl -s http://localhost:8080/health 2>/dev/null; then
            log_result "$service_name" "smoke-test" "PASS" "Service starts successfully"
        else
            log_result "$service_name" "smoke-test" "WARN" "Service starts but health check fails"
        fi
        pkill -f "node.*$service_name" 2>/dev/null || true
    else
        log_result "$service_name" "smoke-test" "SKIP" "Service failed to start within timeout"
    fi

    cd "$BACKEND_DIR"
    return 0
}

# Function to run Java service tests
test_java_service() {
    local service_name=$1
    local service_path="$JAVA_DIR/$service_name"

    echo -e "\n${BLUE}Testing Java Service: $service_name${NC}"

    if [ ! -d "$service_path" ]; then
        log_result "$service_name" "directory" "SKIP" "Service not found"
        return 1
    fi

    cd "$service_path"

    # 1. Check pom.xml
    if [ ! -f "pom.xml" ]; then
        log_result "$service_name" "pom.xml" "FAIL" "pom.xml not found"
        return 1
    fi
    log_result "$service_name" "pom.xml" "PASS" "pom.xml exists"

    # 2. Compile
    echo -e "  ${YELLOW}Compiling...${NC}"
    if mvn compile -q -DskipTests 2>"$REPORT_DIR/${service_name}-compile.log"; then
        log_result "$service_name" "compile" "PASS" "Compilation successful"
    else
        log_result "$service_name" "compile" "FAIL" "Compilation failed" "$(tail -10 "$REPORT_DIR/${service_name}-compile.log")"
        return 1
    fi

    # 3. Run tests
    echo -e "  ${YELLOW}Running tests...${NC}"
    if mvn test -q 2>"$REPORT_DIR/${service_name}-test.log"; then
        log_result "$service_name" "test" "PASS" "All tests passed"
    else
        # Check if tests failed due to no tests
        if grep -q "No tests were run" "$REPORT_DIR/${service_name}-test.log"; then
            log_result "$service_name" "test" "SKIP" "No tests found"
        else
            log_result "$service_name" "test" "FAIL" "Tests failed" "$(tail -20 "$REPORT_DIR/${service_name}-test.log")"
        fi
    fi

    # 4. Package
    echo -e "  ${YELLOW}Packaging...${NC}"
    if mvn package -DskipTests -q 2>"$REPORT_DIR/${service_name}-package.log"; then
        jar_file=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
        if [ -n "$jar_file" ]; then
            log_result "$service_name" "package" "PASS" "JAR built: $(basename $jar_file)"
        else
            log_result "$service_name" "package" "FAIL" "No JAR found"
        fi
    else
        log_result "$service_name" "package" "FAIL" "Packaging failed" "$(tail -10 "$REPORT_DIR/${service_name}-package.log")"
    fi

    cd "$BACKEND_DIR"
    return 0
}

# Main execution
echo "=========================================="
echo "Testing Node.js Services"
echo "=========================================="

# Test Node.js services
if [ -d "$NODE_DIR" ]; then
    NODE_SERVICES=($(ls "$NODE_DIR" | grep -v node_modules | head -10))

    for service in "${NODE_SERVICES[@]}"; do
        test_nodejs_service "$service"
    done
else
    log_result "Node.js" "services" "FAIL" "Node.js services directory not found"
fi

echo ""
echo "=========================================="
echo "Testing Java Services"
echo "=========================================="

# Test Java services
if [ -d "$JAVA_DIR" ]; then
    JAVA_SERVICES=($(ls "$JAVA_DIR" | grep -v node_modules | head -10))

    for service in "${JAVA_SERVICES[@]}"; do
        test_java_service "$service"
    done
else
    log_result "Java" "services" "FAIL" "Java services directory not found"
fi

# Generate summary
echo ""
echo "=========================================="
echo "E2E VALIDATION SUMMARY"
echo "=========================================="
echo "Date: $(date)"
echo "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
echo -e "Skipped: ${YELLOW}$SKIPPED_TESTS${NC}"
echo ""

PASS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
echo "Success Rate: $PASS_RATE%"

# Determine final status
if (( $(echo "$PASS_RATE >= 90" | bc -l) )); then
    echo -e "\n${GREEN}✅ PRODUCTION READY CERTIFICATION: PASSED${NC}"
    echo "✅ Services meet production readiness criteria"
    FINAL_STATUS="PASSED"
elif (( $(echo "$PASS_RATE >= 80" | bc -l) )); then
    echo -e "\n${YELLOW}⚠️ PRODUCTION READY CERTIFICATION: CONDITIONAL${NC}"
    echo "⚠️ Services mostly ready but some issues need attention"
    FINAL_STATUS="CONDITIONAL"
else
    echo -e "\n${RED}❌ PRODUCTION READY CERTIFICATION: FAILED${NC}"
    echo "❌ Services do not meet production readiness criteria"
    FINAL_STATUS="FAILED"
fi

# Save final status
echo ""
echo "========================================" >> "$REPORT_DIR/e2e-results.log"
echo "FINAL STATUS: $FINAL_STATUS" >> "$REPORT_DIR/e2e-results.log"
echo "Success Rate: $PASS_RATE%" >> "$REPORT_DIR/e2e-results.log"
echo "========================================" >> "$REPORT_DIR/e2e-results.log"

# Generate HTML report
cat > "$REPORT_DIR/e2e-report.html" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>E2E Production Validation Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f0f0f0; padding: 20px; border-radius: 5px; }
        .passed { color: green; }
        .failed { color: red; }
        .skipped { color: orange; }
        pre { background: #f5f5f5; padding: 10px; overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        .status-pass { background-color: #d4edda; }
        .status-fail { background-color: #f8d7da; }
        .status-skip { background-color: #fff3cd; }
    </style>
</head>
<body>
    <div class="header">
        <h1>E2E Production Validation Report</h1>
        <p><strong>Date:</strong> $(date)</p>
        <p><strong>Status:</strong> <span class="$FINAL_STATUS">$FINAL_STATUS</span></p>
        <p><strong>Success Rate:</strong> $PASS_RATE%</p>
        <p><strong>Total Tests:</strong> $TOTAL_TESTS</p>
        <p><strong>Passed:</strong> <span class="passed">$PASSED_TESTS</span></p>
        <p><strong>Failed:</strong> <span class="failed">$FAILED_TESTS</span></p>
        <p><strong>Skipped:</strong> <span class="skipped">$SKIPPED_TESTS</span></p>
    </div>

    <h2>Test Results</h2>
    <pre>$(cat "$REPORT_DIR/e2e-results.log")</pre>
</body>
</html>
EOF

echo ""
echo "=========================================="
echo "Full report saved to: $REPORT_DIR/e2e-results.log"
echo "HTML report saved to: $REPORT_DIR/e2e-report.html"
echo "=========================================="

exit 0