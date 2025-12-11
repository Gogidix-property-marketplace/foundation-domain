#!/bin/bash

# ⚡ QUICK STATUS CHECK - AI Services Fix Progress
echo "⚡ AI SERVICES FIX PROGRESS STATUS"
echo "==============================="
echo "📅 Time: $(date)"
echo ""

cd "java-services"

# Count services with POM files (indicates fixed)
services_with_pom=0
services_with_app_yml=0
services_with_dockerfile=0
services_with_readme=0
total_services=0

echo "🔍 Checking fix progress..."
echo ""

for service in ai-*-service/; do
    if [ -d "$service" ]; then
        ((total_services++))
        service_name=$(basename "$service")

        # Check for configuration files
        has_pom=false
        has_yml=false
        has_docker=false
        has_readme=false

        [ -f "$service/pom.xml" ] && has_pom=true && ((services_with_pom++))
        [ -f "$service/application.yml" ] && has_yml=true && ((services_with_app_yml++))
        [ -f "$service/Dockerfile" ] && has_docker=true && ((services_with_dockerfile++))
        [ -f "$service/README.md" ] && has_readme=true && ((services_with_readme++))

        # Check package declarations
        fixed_package=false
        if [ -f "$service/domain/AIServices/${service_name%-service}/Application.java" ]; then
            package_line=$(grep "package " "$service/domain/AIServices/${service_name%-service}/Application.java" | head -1)
            if [[ "$package_line" == *"com.gogidix.ai."* ]]; then
                fixed_package=true
            fi
        fi

        # Status indicator
        if $has_pom && $has_yml && $has_docker && $has_readme && $fixed_package; then
            status="✅ COMPLETE"
        elif $has_pom || $has_yml; then
            status="🔄 IN PROGRESS"
        else
            status="❌ PENDING"
        fi

        echo "📁 $service_name: $status"
    fi
done

echo ""
echo "📊 PROGRESS SUMMARY:"
echo "===================="
echo "📈 Total Services: $total_services"
echo "📄 Services with POM: $services_with_pom/$total_services"
echo "📄 Services with application.yml: $services_with_app_yml/$total_services"
echo "📄 Services with Dockerfile: $services_with_dockerfile/$total_services"
echo "📄 Services with README: $services_with_readme/$total_services"

# Calculate completion percentage
completed=0
for service in ai-*-service/; do
    if [ -d "$service" ] && [ -f "$service/pom.xml" ] && [ -f "$service/application.yml" ] && [ -f "$service/Dockerfile" ] && [ -f "$service/README.md" ]; then
        ((completed++))
    fi
done

completion_percentage=$((completed * 100 / total_services))

echo ""
echo "🏆 OVERALL COMPLETION: $completion_percentage% ($completed/$total_services services fully fixed)"

if [ $completion_percentage -eq 100 ]; then
    echo "🎉 ALL SERVICES FIXED - Ready for compilation testing!"
elif [ $completion_percentage -ge 75 ]; then
    echo "⚡ Nearly complete - Almost ready!"
elif [ $completion_percentage -ge 50 ]; then
    echo "🔄 Good progress - More than halfway there!"
elif [ $completion_percentage -ge 25 ]; then
    echo "📈 Progress made - Keep going!"
else
    echo "🚀 Just getting started - Stay tuned!"
fi