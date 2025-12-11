#!/bin/bash

# Foundation Domain Deployment Script
# Deploys one domain at a time to GitHub

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GITHUB_ORG="Gogidix-property-marketplace"
REPO_NAME="foundation-domain"
GITHUB_TOKEN="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"

# Domains to deploy
DOMAINS=(
    "shared-libraries"
    "shared-infrastructure"
    "central-configuration"
    "ai-services"
    "centralized-dashboard"
)

echo -e "${BLUE}🚀 Foundation Domain Deployment Script${NC}"
echo -e "${BLUE}======================================${NC}\n"

# Function to create GitHub repository
create_github_repo() {
    echo -e "${YELLOW}📋 Creating GitHub repository...${NC}"

    # Check if repo already exists
    if curl -k -s -H "Authorization: token $GITHUB_TOKEN" \
           "https://api.github.com/repos/$GITHUB_ORG/$REPO_NAME" > /dev/null; then
        echo -e "${YELLOW}⚠️  Repository already exists${NC}"
    else
        # Create repository
        curl -k -s -X POST \
             -H "Authorization: token $GITHUB_TOKEN" \
             -H "Accept: application/vnd.github.v3+json" \
             "https://api.github.com/orgs/$GITHUB_ORG/repos" \
             -d "{\"name\":\"$REPO_NAME\",\"description\":\"Gogidix Foundation Domain - Core Infrastructure Services\",\"private\":false}" \
             > /dev/null

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Repository created successfully${NC}"
        else
            echo -e "${RED}❌ Failed to create repository${NC}"
            exit 1
        fi
    fi
}

# Function to initialize git and push domain
deploy_domain() {
    local domain=$1

    echo -e "\n${YELLOW}📦 Deploying domain: $domain${NC}"
    echo -e "${YELLOW}----------------------------${NC}"

    # Check if domain directory exists
    if [ ! -d "$domain" ]; then
        echo -e "${RED}❌ Domain directory not found: $domain${NC}"
        return 1
    fi

    # Initialize git if not already initialized
    if [ ! -d ".git" ]; then
        git init
        git config user.name "Gogidix DevOps"
        git config user.email "devops@gogidix.com"
    fi

    # Create a temporary branch for this domain
    local branch_name="deploy-$domain-$(date +%Y%m%d%H%M%S)"
    git checkout -b "$branch_name" 2>/dev/null || git checkout -b "$branch_name" main

    # Add all files
    git add .

    # Commit
    git commit -m "feat: Deploy $domain domain

- Add $domain services and configurations
- Includes all microservices and infrastructure
- Ready for CI/CD pipeline

🤖 Generated with Gogidix DevOps Automation" 2>/dev/null || true

    # Add remote if not exists
    if ! git remote get-url origin > /dev/null 2>&1; then
        git remote add origin "https://github.com/$GITHUB_ORG/$REPO_NAME.git"
    fi

    # Push to GitHub
    echo -e "${BLUE}📤 Pushing to GitHub...${NC}"
    git push -u origin "$branch_name" --force

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Successfully deployed $domain${NC}"
    else
        echo -e "${RED}❌ Failed to push $domain${NC}"
        return 1
    fi

    # Return to main branch
    git checkout main 2>/dev/null || git checkout master
}

# Function to create pull request
create_pull_request() {
    local domain=$1
    local branch_name="deploy-$domain-$(date +%Y%m%d%H%M%S)"

    echo -e "\n${YELLOW}🔄 Creating pull request for $domain...${NC}"

    # Create PR using GitHub CLI or curl
    # This would require gh CLI to be installed and authenticated
    # For now, we'll just print the instructions

    echo -e "${BLUE}📋 Pull Request Instructions:${NC}"
    echo "1. Go to: https://github.com/$GITHUB_ORG/$REPO_NAME/compare"
    echo "2. Select branch: $branch_name"
    echo "3. Create pull request to main branch"
    echo "4. Title: Deploy $domain domain"
    echo "5. Description: Deployment of $domain with all services"
}

# Main execution
main() {
    # Step 1: Create GitHub repository
    create_github_repo

    # Step 2: Deploy each domain
    for domain in "${DOMAINS[@]}"; do
        echo -e "\n${BLUE}================================${NC}"
        deploy_domain "$domain"
        sleep 2  # Brief pause between deployments

        # Create PR after successful deployment
        if [ $? -eq 0 ]; then
            create_pull_request "$domain"
        fi
    done

    echo -e "\n${GREEN}🎉 All domains deployed successfully!${NC}"
    echo -e "${BLUE}Repository: https://github.com/$GITHUB_ORG/$REPO_NAME${NC}"
    echo -e "\n${YELLOW}Next Steps:${NC}"
    echo "1. Review and merge pull requests"
    echo "2. Add required secrets to GitHub repository"
    echo "3. Configure CI/CD environment variables"
    echo "4. Run the CI/CD pipeline"
}

# Run main function
main "$@"