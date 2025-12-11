#!/bin/bash

echo "🚀 Generating all Foundation Domain Central Configuration services..."
echo ""

# Change to the java-services directory
cd "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\central-configuration\backend-services\java-services"

# Array of all services to generate
declare -A services=(
    ["ConfigManagementService"]="com.gogidix.foundation.config:8888"
    ["DynamicConfigService"]="com.gogidix.foundation.config:8889"
    ["SecretsManagementService"]="com.gogidix.foundation.security:8890"
    ["SecretsRotationService"]="com.gogidix.foundation.security:8891"
    ["FeatureFlagsService"]="com.gogidix.foundation.config:8892"
    ["RateLimitingService"]="com.gogidix.foundation.config:8893"
    ["AuditLoggingConfigService"]="com.gogidix.foundation.audit:8894"
    ["BackupConfigService"]="com.gogidix.foundation.backup:8895"
    ["DisasterRecoveryConfigService"]="com.gogidix.foundation.disaster:8896"
    ["EnvironmentVarsService"]="com.gogidix.foundation.config:8897"
    ["PolicyManagementService"]="com.gogidix.foundation.policy:8898"
)

# Path to CLI JAR
CLI_JAR="C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-code-Generator-Factory\gogidix-java-cli\target\gogidix-java-cli-1.0.0.jar"

# Generate each service
for service in "${!services[@]}"; do
    IFS=':' read -r package port <<< "${services[$service]}"

    echo "📦 Generating $service..."
    echo "   Package: $package"
    echo "   Port: $port"

    # Create directory
    mkdir -p "$service"
    cd "$service"

    # Initialize service
    java -jar "$CLI_JAR" init "$service" -t microservice -p "$package" -d foundation --port "$port"

    cd ..
    echo "✅ $service generated successfully!"
    echo ""
done

echo "🎉 All services generated successfully!"
echo ""
echo "📍 Location: C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\central-configuration\backend-services\java-services"
echo ""
echo "📋 Generated services:"
for service in "${!services[@]}"; do
    echo "   • $service"
done