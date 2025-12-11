#!/bin/bash

# Generate Dockerfiles for all Node.js services

SERVICES_DIR="/c/Users/HP/Desktop/Gogidix-property-marketplace/Gogidix-Domain/foundation-domain/shared-infrastructure/backend-services/nodejs-services"
DOCKERFILE_TEMPLATE="$SERVICES_DIR/DOCKERFILE_TEMPLATE"

echo "Generating Dockerfiles for Node.js services..."

# Find all service directories
for service_dir in "$SERVICES_DIR"/*/; do
    if [ -d "$service_dir" ]; then
        service_name=$(basename "$service_dir")

        # Skip if not a service directory
        if [ "$service_name" == "node_modules" ]; then
            continue
        fi

        dockerfile_path="$service_dir/Dockerfile"

        echo "Creating Dockerfile for $service_name..."

        # Create service-specific Dockerfile
        cat > "$dockerfile_path" << EOF
# Dockerfile for $service_name
# Generated for production deployment

# Use the template but adjust for specific service
FROM node:18-alpine AS base

# Install security updates
RUN apk update && apk upgrade && \
    apk add --no-cache dumb-init curl

# Create app directory
WORKDIR /usr/src/app

# Install dependencies efficiently
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

# ---- Production Stage ----
FROM base AS production

# Set environment to production
ENV NODE_ENV=production
ENV PORT=3000

# Create non-root user for security
RUN addgroup -g 1001 -S nodejs && \\
    adduser -S nodejs -u 1001

# Copy source code
COPY --chown=nodejs:nodejs . .

# Switch to non-root user
USER nodejs

# Expose port
EXPOSE \$PORT

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\
  CMD curl -f http://localhost:\${PORT}/health || exit 1

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start application
CMD ["node", "src/server.js"]
EOF

        echo "✓ Created: $dockerfile_path"
    fi
done

echo ""
echo "Dockerfiles generated for all services!"
echo ""
echo "To build all services:"
echo "  docker-compose build"
echo ""
echo "To run all services:"
echo "  docker-compose up -d"
EOF