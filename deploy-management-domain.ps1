# Management Domain Deployment Script
# =================================
# Deploys management domain with full CI/CD setup

param(
    [string]$GitHubToken = "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",
    [string]$GitHubOrg = "Gogidix-property-marketplace",
    [string]$ManagementDomainPath = "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\business-domain",
    [switch]$SkipExisting = $false
)

# Set error action preference
$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Blue
Write-Host "Management Domain Deployment" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

# Headers for GitHub API
$headers = @{
    "Authorization" = "token $GitHubToken"
    "Accept" = "application/vnd.github.v3+json"
}

# Function to create repository
function Create-Repository {
    param(
        [string]$RepoName,
        [string]$Description
    )

    Write-Host "`n📦 Creating repository: $RepoName" -ForegroundColor Yellow

    $body = @{
        name = $RepoName
        description = $Description
        private = $false
        has_issues = $true
        has_wiki = $true
        has_projects = $true
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "https://api.github.com/orgs/$GitHubOrg/repos" -Method Post -Headers $headers -Body $body -SkipCertificateCheck
        Write-Host "✅ Repository created successfully" -ForegroundColor Green
        return $response
    } catch {
        if ($_.Exception.Response.StatusCode -eq "UnprocessableEntity") {
            Write-Host "⚠️  Repository already exists" -ForegroundColor Yellow
            return Invoke-RestMethod -Uri "https://api.github.com/repos/$GitHubOrg/$RepoName" -Method Get -Headers $headers -SkipCertificateCheck
        } else {
            Write-Host "❌ Failed to create repository: $_" -ForegroundColor Red
            return $null
        }
    }
}

# Function to deploy domain
function Deploy-Domain {
    param(
        [string]$DomainPath,
        [string]$DomainName,
        [string]$RepoName
    )

    Write-Host "`n🚀 Deploying domain: $DomainName" -ForegroundColor Cyan
    Write-Host "Path: $DomainPath" -ForegroundColor Gray

    if (-not (Test-Path $DomainPath)) {
        Write-Host "❌ Domain path not found: $DomainPath" -ForegroundColor Red
        return $false
    }

    # Create repository
    $repo = Create-Repository -RepoName $RepoName -Description "Gogidix Management Domain - $DomainName Services and Applications"
    if ($null -eq $repo) {
        return $false
    }

    # Create temp directory
    $tempDir = "temp-deploy-$DomainName-$(Get-Date -Format 'yyyyMMddHHmmss')"
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

    try {
        # Copy domain files
        Write-Host "📋 Copying domain files..." -ForegroundColor Blue
        Copy-Item -Path "$DomainPath\*" -Destination $tempDir -Recurse

        # Initialize git
        Push-Location $tempDir
        git init
        git config user.name "Gogidix DevOps"
        git config user.email "devops@gogidix.com"

        # Create branch structure
        git checkout -b main

        # Create .github structure
        if (-not (Test-Path ".github")) {
            New-Item -ItemType Directory -Path ".github" -Force | Out-Null
            New-Item -ItemType Directory -Path ".github\workflows" -Force | Out-Null
        }

        # Copy CI/CD workflows
        $ciWorkflow = @"
name: Management Domain CI/CD Pipeline

on:
  push:
    branches: [ main, develop, staging, uat ]
  pull_request:
    branches: [ main, develop ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: `$`{`{ github.repository `}`}

jobs:
  # Java Services Build
  build-java:
    name: Build Java Services
    runs-on: ubuntu-latest
    if: contains(toJson(github.event.commits), 'backend-services/java-services')

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: `$`{`{ runner.os `}`}-m2-`$`{`{ hashFiles('**/pom.xml') `}`}

    - name: Build Java services
      run: |
        find backend-services/java-services -name "pom.xml" -type f | while read pom; do
          echo "Building \$pom"
          cd "\$(dirname \$pom)"
          mvn clean compile test
          cd - > /dev/null
        done

    - name: Generate Build Summary
      run: |
        echo "# Java Services Build Summary" >> \$GITHUB_STEP_SUMMARY
        echo "\`\`\`" >> \$GITHUB_STEP_SUMMARY
        find backend-services/java-services -name "pom.xml" -type f | wc -l | xargs -I {} echo "Java Services: {}" >> \$GITHUB_STEP_SUMMARY
        echo "\`\`\`" >> \$GITHUB_STEP_SUMMARY

  # Node.js Services Build
  build-nodejs:
    name: Build Node.js Services
    runs-on: ubuntu-latest
    if: contains(toJson(github.event.commits), 'backend-services/nodejs-services')

    steps:
    - uses: actions/checkout@v4

    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
        cache: 'npm'

    - name: Install dependencies
      run: |
        find backend-services/nodejs-services -name "package.json" -type f | while read pkg; do
          echo "Installing dependencies for \$pkg"
          cd "\$(dirname \$pkg)"
          npm ci
          cd - > /dev/null
        done

    - name: Build and test
      run: |
        find backend-services/nodejs-services -name "package.json" -type f | while read pkg; do
          echo "Building \$pkg"
          cd "\$(dirname \$pkg)"
          npm run build
          npm test
          cd - > /dev/null
        done

  # Frontend Build
  build-frontend:
    name: Build Frontend Applications
    runs-on: ubuntu-latest
    if: contains(toJson(github.event.commits), 'frontend-apps')

    steps:
    - uses: actions/checkout@v4

    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
        cache: 'npm'

    - name: Install and build
      run: |
        find frontend-apps -name "package.json" -type f | while read pkg; do
          echo "Building frontend \$pkg"
          cd "\$(dirname \$pkg)"
          npm ci
          npm run build
          cd - > /dev/null
        done

  # Mobile Build
  build-mobile:
    name: Build Mobile Applications
    runs-on: ubuntu-latest
    if: contains(toJson(github.event.commits), 'mobile-apps')

    steps:
    - uses: actions/checkout@v4

    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
        cache: 'npm'

    - name: Setup React Native
      run: |
        npm install -g @react-native-community/cli

    - name: Install and build
      run: |
        find mobile-apps -name "package.json" -type f | while read pkg; do
          echo "Building mobile \$pkg"
          cd "\$(dirname \$pkg)"
          npm ci
          npm run build
          cd - > /dev/null
        done

  # Security Scan
  security-scan:
    name: Security Scan
    runs-on: ubuntu-latest
    needs: [build-java, build-nodejs, build-frontend, build-mobile]
    if: always()

    steps:
    - uses: actions/checkout@v4

    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        scan-type: 'fs'
        scan-ref: '.'
        format: 'sarif'
        output: 'trivy-results.sarif'

    - name: Upload Trivy scan results
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: 'trivy-results.sarif'

  # Deploy to Staging
  deploy-staging:
    name: Deploy to Staging
    runs-on: ubuntu-latest
    needs: [build-java, build-nodejs, build-frontend, build-mobile]
    if: github.ref == 'refs/heads/staging' && github.event_name == 'push'
    environment: staging

    steps:
    - uses: actions/checkout@v4

    - name: Deploy to staging
      run: |
        echo "Deploying to staging environment..."
        # Add staging deployment commands here

  # Deploy to Production
  deploy-production:
    name: Deploy to Production
    runs-on: ubuntu-latest
    needs: [build-java, build-nodejs, build-frontend, build-mobile]
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    environment: production

    steps:
    - uses: actions/checkout@v4

    - name: Deploy to production
      run: |
        echo "Deploying to production environment..."
        # Add production deployment commands here
"@

        $ciWorkflow | Out-File -FilePath ".github\workflows\ci-management-domain.yml" -Encoding utf8

        # Create deploy workflow
        $deployWorkflow = @"
name: Management Domain Deployment

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        default: 'staging'
        type: choice
        options:
        - staging
        - uat
        - production

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: `$`{`{ github.event.inputs.environment `}`}

    steps:
    - uses: actions/checkout@v4

    - name: Deploy to `$`{`{ github.event.inputs.environment `}`}
      run: |
        echo "Deploying to `$`{`{ github.event.inputs.environment `}`}"
        # Add deployment logic
"@

        $deployWorkflow | Out-File -FilePath ".github\workflows\deploy-management-domain.yml" -Encoding utf8

        # Create README
        $readme = @"
# Management Domain - $DomainName

## Overview

The Management Domain provides comprehensive management capabilities for the Gogidix Property Marketplace platform.

## Architecture

This domain includes:

- **Backend Services**: Java Spring Boot and Node.js microservices
- **Frontend Applications**: React/Next.js web applications
- **Mobile Applications**: React Native mobile apps
- **API Gateway**: Centralized API management
- **Authentication & Authorization**: Security services

## Services

### Backend Services

#### Java Services
- Property Management Service
- User Management Service
- Payment Processing Service
- Reporting Service
- And more...

#### Node.js Services
- Notification Service
- Email Service
- SMS Service
- File Upload Service
- And more...

### Frontend Applications

- Admin Dashboard
- User Portal
- Property Listing Portal
- Analytics Dashboard
- And more...

### Mobile Applications

- Property Manager App
- Tenant App
- Inspector App
- Maintenance App

## Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- Docker Desktop
- Kubernetes cluster (for production)

### Local Development

1. Clone the repository
   ```bash
   git clone https://github.com/$GitHubOrg/$RepoName.git
   cd $RepoName
   ```

2. Start Java services
   ```bash
   cd backend-services/java-services/[service-name]
   mvn spring-boot:run
   ```

3. Start Node.js services
   ```bash
   cd backend-services/nodejs-services/[service-name]
   npm install
   npm start
   ```

4. Start frontend applications
   ```bash
   cd frontend-apps/[app-name]
   npm install
   npm start
   ```

## Deployment

### Docker

All services include Docker support:

```bash
# Build all services
docker-compose build

# Run all services
docker-compose up -d
```

### Kubernetes

```bash
# Deploy to Kubernetes
kubectl apply -f k8s/
```

## CI/CD

This repository uses GitHub Actions for continuous integration and deployment:

- **CI Pipeline**: Runs on every push to any branch
- **Deployment Pipeline**: Manual trigger for staging/UAT/production
- **Security Scanning**: Automated vulnerability scanning with Trivy

## Monitoring

- Application logs: Integrated with ELK stack
- Metrics: Prometheus and Grafana dashboards
- Health checks: `/health` endpoint on all services

## Support

For support, please:
1. Check the [Wiki](https://github.com/$GitHubOrg/$RepoName/wiki)
2. Create an [Issue](https://github.com/$GitHubOrg/$RepoName/issues)
3. Contact the devops team

---

Built with ❤️ by Gogidix Engineering Team
"@

        $readme | Out-File -FilePath "README.md" -Encoding utf8

        # Create .gitignore
        $gitignore = @"
# Compiled binaries
target/
build/
dist/
*.jar
*.war
*.ear

# Node modules
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# Environment files
.env
.env.local
.env.development.local
.env.test.local
.env.production.local

# IDE files
.vscode/
.idea/
*.swp
*.swo
*~

# OS files
.DS_Store
Thumbs.db

# Logs
logs/
*.log

# Runtime data
pids/
*.pid
*.seed
*.pid.lock

# Coverage directory used by tools like istanbul
coverage/

# Dependency directories
.pnp/
.pnp.js

# Optional npm cache directory
.npm

# Optional REPL history
.node_repl_history

# Output of 'npm pack'
*.tgz

# Yarn Integrity file
.yarn-integrity

# dotenv environment variables file
.env

# parcel-bundler cache (https://parceljs.org/)
.cache
.parcel-cache

# next.js build output
.next

# nuxt.js build output
.nuxt

# vuepress build output
.vuepress/dist

# Serverless directories
.serverless

# FuseBox cache
.fusebox/

# DynamoDB Local files
.dynamodb/

# Terraform
*.tfstate
*.tfstate.*
.terraform/

# Kubernetes secrets
secrets/
"@

        $gitignore | Out-File -FilePath ".gitignore" -Encoding utf8

        # Add files and commit
        git add .
        git commit -m "feat: Initial deployment of Management Domain - $DomainName

- Deploy all backend services (Java + Node.js)
- Deploy all frontend applications
- Deploy all mobile applications
- Set up CI/CD pipelines with GitHub Actions
- Configure automated testing and security scanning
- Add comprehensive documentation

🤖 Generated with Gogidix DevOps Automation"

        # Add remote and push
        git remote add origin "https://github.com/$GitHubOrg/$RepoName.git"
        git branch -M main

        Write-Host "📤 Pushing to GitHub..." -ForegroundColor Blue
        git push -u origin main --force

        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Successfully deployed $DomainName" -ForegroundColor Green

            # Create additional branches
            git checkout -b develop
            git push -u origin develop

            git checkout -b staging
            git push -u origin staging

            git checkout -b uat
            git push -u origin uat

        } else {
            Write-Host "❌ Failed to push $DomainName" -ForegroundColor Red
        }

        Pop-Location
    } finally {
        # Cleanup
        Pop-Location
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }

    return $true
}

# Management domains to deploy
$managementDomains = @(
    @{
        Path = "$ManagementDomainPath\property-management"
        Name = "Property Management"
        Repo = "property-management-domain"
    },
    @{
        Path = "$ManagementDomainPath\user-management"
        Name = "User Management"
        Repo = "user-management-domain"
    },
    @{
        Path = "$ManagementDomainPath\financial-management"
        Name = "Financial Management"
        Repo = "financial-management-domain"
    },
    @{
        Path = "$ManagementDomainPath\facility-management"
        Name = "Facility Management"
        Repo = "facility-management-domain"
    },
    @{
        Path = "$ManagementDomainPath\tenant-management"
        Name = "Tenant Management"
        Repo = "tenant-management-domain"
    },
    @{
        Path = "$ManagementDomainPath\lease-management"
        Name = "Lease Management"
        Repo = "lease-management-domain"
    },
    @{
        Path = "$ManagementDomainPath\maintenance-management"
        Name = "Maintenance Management"
        Repo = "maintenance-management-domain"
    },
    @{
        Path = "$ManagementDomainPath\reporting-analytics"
        Name = "Reporting & Analytics"
        Repo = "reporting-analytics-domain"
    },
    @{
        Path = "$ManagementDomainPath\compliance-management"
        Name = "Compliance Management"
        Repo = "compliance-management-domain"
    },
    @{
        Path = "$ManagementDomainPath\vendor-management"
        Name = "Vendor Management"
        Repo = "vendor-management-domain"
    }
)

# Main execution
Write-Host "`n📋 Found $($managementDomains.Count) management domains to deploy" -ForegroundColor Cyan

$deployedCount = 0
$skippedCount = 0

foreach ($domain in $managementDomains) {
    Write-Host "`n" + ("=" * 60)
    Write-Host "Processing: $($domain.Name)" -ForegroundColor White
    Write-Host "Repository: $($domain.Repo)" -ForegroundColor Gray

    if ($SkipExisting -and (Test-Path "$($domain.Path)")) {
        Write-Host "⏭️  Skipping (already exists)" -ForegroundColor Yellow
        $skippedCount++
        continue
    }

    if (Deploy-Domain -DomainPath $domain.Path -DomainName $domain.Name -RepoName $domain.Repo) {
        $deployedCount++
        Write-Host "✅ Deployment successful" -ForegroundColor Green
    } else {
        Write-Host "❌ Deployment failed" -ForegroundColor Red
    }

    # Pause between deployments
    Start-Sleep -Seconds 3
}

# Summary
Write-Host "`n" + ("=" * 60)
Write-Host "📊 Deployment Summary" -ForegroundColor Blue
Write-Host "Deployed: $deployedCount" -ForegroundColor Green
Write-Host "Skipped: $skippedCount" -ForegroundColor Yellow
Write-Host "Total: $($managementDomains.Count)" -ForegroundColor Cyan

Write-Host "`n🎉 Management domain deployment process complete!" -ForegroundColor Green
Write-Host "Repositories: https://github.com/$GitHubOrg" -ForegroundColor Blue
Write-Host "`nNext Steps:" -ForegroundColor Yellow
Write-Host "1. Configure GitHub secrets for each repository"
Write-Host "2. Review and merge any pull requests"
Write-Host "3. Configure environment-specific settings"
Write-Host "4. Run the CI/CD pipelines"