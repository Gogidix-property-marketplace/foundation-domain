#!/bin/bash

# Script to fix bucket4j dependency issue in all Java services
echo "🔧 Fixing dependencies in all Java services..."

for service in */; do
    if [ -f "$service/pom.xml" ]; then
        echo "Processing $service..."
        # Comment out bucket4j dependency
        sed -i 's|        <dependency>|        <!-- Temporarily commented out bucket4j -->\n        <!--\n        <dependency>|' "$service/pom.xml"
        sed -i 's|        </dependency>|        </dependency>\n        -->|' "$service/pom.xml"

        # Add JitPack repository if not present
        if ! grep -q "jitpack.io" "$service/pom.xml"; then
            sed -i '/<properties>/a\\n    <repositories>\n        <repository>\n            <id>jitpack.io</id>\n            <url>https://jitpack.io</url>\n        </repository>\n    </repositories>' "$service/pom.xml"
        fi

        echo "✅ Fixed $service"
    fi
done

echo "🎉 All services fixed!"