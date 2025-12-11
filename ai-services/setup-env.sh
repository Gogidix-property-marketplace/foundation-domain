#!/bin/bash

# Gogidix AI Services - Environment Setup Script
# This script helps set up all required API keys and tokens

echo "🚀 Gogidix AI Services - Environment Setup"
echo "=========================================="
echo ""

# Create .env file template
cat > .env.template << 'EOF
# 🤖 AI/ML Service Providers
# =====================================

# OpenAI (Primary - Recommended)
# Get from: https://platform.openai.com/api-keys
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
OPENAI_ORG_ID=org-xxxxxxxxxxxxxxxxxxxxxx

# Anthropic Claude (Alternative to OpenAI)
# Get from: https://console.anthropic.com/
ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Hugging Face (Free models and datasets)
# Get from: https://huggingface.co/settings/tokens
HF_TOKEN=hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Google Cloud Platform
# 1. Create project: https://console.cloud.google.com/
# 2. Enable APIs: aiplatform.googleapis.com, translate.googleapis.com
# 3. Create service account and download JSON key
GOOGLE_APPLICATION_CREDENTIALS=./google-credentials.json
GOOGLE_PROJECT_ID=your-gcp-project-id

# Amazon Web Services
# 1. Create IAM policy for Bedrock
# 2. Enable Bedrock models in AWS Console
# 3. Create access keys
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_REGION=us-east-1

# 🗄️ Database Services
# =====================

# MongoDB Atlas (Recommended Vector DB)
# Get from: https://cloud.mongodb.com/
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/ai-services

# Pinecone (Alternative Vector DB)
# Get from: https://app.pinecone.io/
PINECONE_API_KEY=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx
PINECONE_ENVIRONMENT=us-west1-gcp-free

# Redis (Caching and Session Storage)
# Get from: https://redis.com/try-free/
REDIS_URL=redis://username:password@host:port

# PostgreSQL (Alternative DB)
# Get from: AWS RDS, Google Cloud SQL, or Railway
POSTGRES_URL=postgresql://username:password@host:port/database

# 📧 Communication Services
# ======================

# Twilio (SMS and Voice notifications)
# Get from: https://www.twilio.com/try-twilio
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=+1234567890

# SendGrid (Email notifications)
# Get from: https://signup.sendgrid.com/
SENDGRID_API_KEY=SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Slack (Team notifications)
# Get from: https://api.slack.com/apps/
SLACK_BOT_TOKEN=xoxb-xxxxxxxxxxxx-xxxxxxxxxxxx-xxxxx

# 🔐 Security & Authentication
# ==========================

# Auth0 (Recommended)
# Get from: https://auth0.com/
AUTH0_DOMAIN=your-domain.auth0.com
AUTH0_CLIENT_ID=your_client_id
AUTH0_CLIENT_SECRET=your_client_secret
AUTH0_AUDIENCE=your_api_identifier

# Okta (Alternative)
# Get from: https://okta.com/
OKTA_DOMAIN=your-domain.okta.com
OKTA_CLIENT_ID=your_client_id
OKTA_CLIENT_SECRET=your_client_secret

# 📊 Monitoring & Analytics
# ========================

# Sentry (Error tracking)
# Get from: https://sentry.io/
SENTRY_DSN=https://xxxxxxx.ingest.sentry.io/xxxxxxx

# Datadog (Monitoring - Optional)
# Get from: https://www.datadoghq.com/
DATADOG_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# New Relic (APM - Optional)
# Get from: https://newrelic.com/
NEW_RELIC_LICENSE_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# ☁️ Cloud Provider Specific
# ===========================

# Azure Cognitive Services (Optional)
AZURE_COGNITIVE_SERVICES_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
AZURE_COGNITIVE_SERVICES_REGION=eastus

# Cloudflare (Optional)
CLOUDFLARE_API_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
CLOUDFLARE_ZONE_ID=xxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# 🎯 Custom Configuration
# =======================

# API Gateway Configuration
API_GATEWAY_PORT=3002
API_GATEWAY_HOST=localhost

# WebSocket Configuration
WS_PORT=3003

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ai_services

# Cache Configuration
CACHE_HOST=localhost
CACHE_PORT=6379
CACHE_DB=0

# Logging Configuration
LOG_LEVEL=info
LOG_FILE=./logs/ai-services.log

# Rate Limiting
RATE_LIMIT_REQUESTS=100
RATE_LIMIT_WINDOW=60000

# Security
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
CORS_ORIGIN=http://localhost:3000

# Environment
NODE_ENV=development
API_VERSION=v1

# Feature Flags
ENABLE_WEBSOCKETS=true
ENABLE_SERVICE_MESH=false
ENABLE_ADVANCED_ANALYTICS=false
ENABLE_MULTI_TENANT=false

# Performance
MAX_FILE_SIZE=10485760  # 10MB
REQUEST_TIMEOUT=30000
CONCURRENT_UPLOADS=5

# AI Model Configuration
DEFAULT_LLM_MODEL=gpt-4-turbo
DEFAULT_EMBEDDINGS_MODEL=text-embedding-ada-002
DEFAULT_VISION_MODEL=gpt-4-vision-preview
MAX_TOKENS=4096
TEMPERATURE=0.7

# Business Configuration
ORGANIZATION_NAME=Gogidix
SUPPORT_EMAIL=support@gogidix.com
CONTACT_PHONE=+1-555-123-4567
TIMEZONE=UTC

# Custom Provider Configurations
CUSTOM_MODELS_PATH=./models
CUSTOM_PROMPTS_PATH=./prompts
CUSTOM_INTEGRATIONS_PATH=./integrations
EOF

echo "✅ Created .env.template file"
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "📝 Creating .env file from template..."
    cp .env.template .env
    echo ""
    echo "⚠️  IMPORTANT: Please edit .env file and add your actual API keys!"
    echo ""
    echo "📋 Required minimum setup:"
    echo "   1. OPENAI_API_KEY - From https://platform.openai.com/api-keys"
    echo "   2. MONGODB_URI - From https://cloud.mongodb.com/"
    echo "   3. JWT_SECRET - Generate a secure random string"
    echo ""
    echo "💡 Next steps:"
    echo "   1. Edit .env with your actual API keys"
    echo "   2. Run: source .env"
    echo "   3. Start the services"
    echo ""
else
    echo "✅ .env file already exists"
    echo ""
    echo "⚡ Quick check for required environment variables:"

    required_vars=("OPENAI_API_KEY" "MONGODB_URI" "JWT_SECRET")
    missing_vars=()

    for var in "${required_vars[@]}"; do
        if ! grep -q "^${var}=" .env; then
            missing_vars+=("$var")
        fi
    done

    if [ ${#missing_vars[@]} -eq 0 ]; then
        echo "✅ All required environment variables are set!"
    else
        echo "❌ Missing required variables:"
        for var in "${missing_vars[@]}"; do
            echo "   - $var"
        done
        echo ""
        echo "Please add these to your .env file"
    fi
fi

echo ""
echo "🔗 Useful Links:"
echo "   OpenAI Platform: https://platform.openai.com/api-keys"
echo "   MongoDB Atlas: https://cloud.mongodb.com/"
echo "   Google Cloud: https://console.cloud.google.com/"
echo "   Auth0: https://auth0.com/"
echo "   Hugging Face: https://huggingface.co/"
echo "   Anthropic: https://console.anthropic.com/"
echo "   Twilio: https://www.twilio.com/try-twilio"
echo ""

echo "💡 Pro Tips:"
echo "   • Use different AI providers to avoid vendor lock-in"
echo "   • Monitor usage to control costs"
echo "   • Use environment variables for all secrets"
echo "   • Never commit .env file to version control"
echo "   • Use .env.example for team sharing"
echo ""

echo "🎯 Setup Complete! The platform is ready to configure with your API keys."
echo ""

# Create .env.example for sharing
if [ ! -f .env.example ]; then
    echo "📄 Creating .env.example for team sharing..."
    cp .env.template .env.example

    # Replace sensitive values with examples
    sed -i 's/sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/g' .env.example
    sed -i 's/hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/g' .env.example
    sed -i 's/ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/g' .env.example
    sed -i 's/your-super-secret-jwt-key-change-this-in-production/your-super-secret-jwt-key-change-this-in-production/g' .env.example

    echo "✅ Created .env.example (safe for version control)"
fi

echo ""
echo "🎉 Setup script completed successfully!"
echo "   Next: Edit .env with your API keys and run the services"
EOF

chmod +x setup-env.sh