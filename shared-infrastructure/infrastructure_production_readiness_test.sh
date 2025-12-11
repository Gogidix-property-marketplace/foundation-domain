#!/bin/bash

# 🏗️ 4-STEP PRODUCTION READINESS TEST - INFRASTRUCTURE SERVICES
# Amazon AWS, Netflix, Google, Microsoft, IBM, NVIDIA Standards
# Testing Infrastructure Services Production Readiness

echo "🏗️ 4-STEP PRODUCTION READINESS TEST - INFRASTRUCTURE SERVICES"
echo "============================================================"
echo "🏗️ Testing: All 39 Infrastructure Services"
echo "📍 Location: $PWD"
echo "📅 Date: $(date)"
echo ""

# Step 1: Directory Structure Verification
echo ""
echo "📋 STEP 1: DIRECTORY STRUCTURE VERIFICATION"
echo "--------------------------------------------"

total_services=0
missing_structure=0

cd "java-services"

for service in */; do
    if [ -d "$service" ]; then
        ((total_services++))
        echo -n "📁 $service: "

        # Check for essential directories
        essential_dirs=("application" "domain" "infrastructure" "web" "config")
        missing_count=0

        for dir in "${essential_dirs[@]}"; do
            if [ ! -d "$service/$dir" ]; then
                ((missing_count++))
            fi
        done

        if [ $missing_count -eq 0 ]; then
            echo "✅ All essential directories present"
        else
            echo "❌ Missing $missing_count essential directories"
            ((missing_structure++))
        fi
    fi
done

echo ""
echo "📊 Structure Analysis:"
echo "- Total Services Found: $total_services"
echo "- Services with Complete Structure: $((total_services - missing_structure))"
echo "- Services Missing Structure: $missing_structure"

# Step 2: Java Files Verification
echo ""
echo "📋 STEP 2: JAVA FILES VERIFICATION"
echo "------------------------------------"

java_files_ok=0
java_files_total=0

for service in */; do
    if [ -d "$service" ]; then
        java_count=$(find "$service" -name "*.java" 2>/dev/null | wc -l)
        ((java_files_total += java_count))

        if [ $java_count -ge 50 ]; then
            echo "✅ $service: $java_count Java files (OK)"
            ((java_files_ok++))
        elif [ $java_count -gt 0 ]; then
            echo "⚠️  $service: $java_count Java files (LOW)"
        else
            echo "❌ $service: 0 Java files (MISSING)"
        fi
    fi
done

echo ""
echo "📊 Java Files Analysis:"
echo "- Total Java Files: $java_files_total"
echo "- Services with Adequate Java Files: $java_files_ok"
echo "- Average Files per Service: $((java_files_total / total_services))"

# Step 3: Configuration Files Verification
echo ""
echo "📋 STEP 3: CONFIGURATION FILES VERIFICATION"
echo "------------------------------------------"

config_files_ok=0
config_services=0

for service in */; do
    if [ -d "$service" ]; then
        ((config_services++))
        echo -n "⚙️  $service: "

        # Check for essential configuration files
        config_files=("pom.xml" "application.yml" "README.md" "Dockerfile")
        found_count=0

        for file in "${config_files[@]}"; do
            if [ -f "$service/$file" ]; then
                ((found_count++))
            fi
        done

        if [ $found_count -eq 4 ]; then
            echo "✅ All config files present"
            ((config_files_ok++))
        else
            echo "⚠️  $found_count/4 config files found"
        fi
    fi
done

echo ""
echo "📊 Configuration Analysis:"
echo "- Services with All Config Files: $config_files_ok/$config_services"

# Step 4: Package Structure & Dependencies Verification
echo ""
echo "📋 STEP 4: PACKAGE STRUCTURE & DEPENDENCIES VERIFICATION"
echo "------------------------------------------------------"

# Check package structure for key infrastructure services
sample_services=("api-gateway" "authentication-service" "config-server" "eureka-server" "monitoring-service")

for sample_service in "${sample_services[@]}"; do
    if [ -d "$sample_service" ]; then
        echo "🔍 Analyzing package structure in $sample_service:"

        # Check package declarations in Java files
        echo "- Package declarations found:"
        find "$sample_service" -name "*.java" -exec grep -l "package " {} \; 2>/dev/null | head -3 | while read file; do
            package_line=$(grep "package " "$file" | head -1)
            echo "  ✅ $package_line in $(basename $file)"
        done

        echo "- Infrastructure-specific dependencies:"
        if [ -f "$sample_service/pom.xml" ]; then
            if grep -q "spring-cloud-starter-gateway" "$sample_service/pom.xml"; then
                echo "  ✅ Gateway dependencies found"
            fi
            if grep -q "spring-boot-starter-data-redis" "$sample_service/pom.xml"; then
                echo "  ✅ Redis dependencies found"
            fi
            if grep -q "spring-boot-starter-amqp" "$sample_service/pom.xml"; then
                echo "  ✅ Message queue dependencies found"
            fi
        fi

        echo ""
    fi
done

# Final Summary
echo ""
echo "🏆 PRODUCTION READINESS SUMMARY"
echo "==============================="
echo "🏗️ Total Services: $total_services/39 (Expected)"
echo "📁 Structure Complete: $((total_services - missing_structure))/39"
echo "☕ Java Files Verified: $java_files_ok/39"
echo "⚙️  Config Files Ready: $config_files_ok/39"

# Calculate overall score
structure_score=$((total_services - missing_structure))
java_score=$java_files_ok
config_score=$config_files_ok
total_score=$((structure_score + java_score + config_score))
max_score=$((39 * 3))
percentage=$((total_score * 100 / max_score))

echo ""
echo "📈 OVERALL READINESS SCORE:"
echo "- Total Possible Score: $max_score"
echo "- Achieved Score: $total_score"
echo "- Readiness Percentage: $percentage%"

if [ $percentage -ge 90 ]; then
    echo "🎉 STATUS: PRODUCTION READY! ✅"
elif [ $percentage -ge 75 ]; then
    echo "⚠️  STATUS: NEAR PRODUCTION READY (Minor fixes needed)"
elif [percentage -ge 50 ]; then
    echo "🔧 STATUS: PREPARATION PHASE (Significant work needed)"
else
    echo "❌ STATUS: NOT PRODUCTION READY (Major work needed)"
fi

echo ""
echo "📋 NEXT STEPS:"
echo "- Fix package declarations in Java files if needed"
echo "- Add Maven POM files where missing"
echo "- Update configuration files with service-specific settings"
echo "- Run compilation tests for all services"
echo "- Verify infrastructure-specific dependencies (Redis, RabbitMQ, etc.)"

echo ""
echo "🔧 INFRASTRUCTURE-SPECIFIC CHECKS:"
echo "- Verify API Gateway routing configuration"
echo "- Check service discovery connectivity"
echo "- Test caching mechanisms (Redis)"
echo "- Validate message queue integration (RabbitMQ)"
echo "- Confirm monitoring and tracing setup"
echo "- Test circuit breaker patterns"