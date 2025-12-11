#!/bin/bash

# Fix pom.xml files by removing the bucket4j dependency and fixing XML structure

cd "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\central-configuration\backend-services\java-services"

services=("AuditLoggingConfigService" "BackupConfigService" "DisasterRecoveryConfigService"
          "DynamicConfigService" "EnvironmentVarsService" "FeatureFlagsService"
          "PolicyManagementService" "RateLimitingService" "SecretsManagementService"
          "SecretsRotationService")

for service in "${services[@]}"; do
    echo "Processing $service..."
    cd "$service/$service"

    # Create a backup
    cp pom.xml pom.xml.backup

    # Fix the pom.xml using Python for better XML handling
    python -c "
import re

with open('pom.xml', 'r') as f:
    content = f.read()

# Remove the broken bucket4j dependency section
pattern = r'<!-- Rate Limiting -->\s*<dependency>\s*<groupId>com\.github\.vladimir-bukhtoyarov</groupId>.*?</dependency>\s*'
content = re.sub(pattern, '', content, flags=re.DOTALL)

# Ensure proper spacing before Service Discovery
content = re.sub(r'\s*<!-- Service Discovery -->', '\n        <!-- Service Discovery -->', content)

with open('pom.xml', 'w') as f:
    f.write(content)

print('Fixed pom.xml for $service')
"

    cd ../..
done

echo "All pom.xml files fixed!"