# Foundation Domain Deployment PowerShell Script
# ==========================================
# This script deploys the foundation domain to GitHub with robust error handling

param(
    [string]$GitHubToken = "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",
    [string]$GitHubOrg = "Gogidix-property-marketplace",
    [string]$RepoName = "foundation-domain",
    [switch]$SkipCleanup = $false
)

# Set error action preference
$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Blue
Write-Host "Foundation Domain Deployment Script" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# Function to create GitHub repository
function Create-GitHubRepo {
    Write-Host "Creating GitHub repository..." -ForegroundColor Yellow

    $headers = @{
        "Authorization" = "token $GitHubToken"
        "Accept" = "application/vnd.github.v3+json"
    }

    $body = @{
        name = $RepoName
        description = "Gogidix Foundation Domain - Core Infrastructure Services"
        private = $false
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "https://api.github.com/orgs/$GitHubOrg/repos" -Method Post -Headers $headers -Body $body -SkipCertificateCheck
        Write-Host "✅ Repository created successfully" -ForegroundColor Green
    } catch {
        if ($_.Exception.Response.StatusCode -eq "UnprocessableEntity") {
            Write-Host "⚠️  Repository already exists" -ForegroundColor Yellow
        } else {
            Write-Host "❌ Failed to create repository: $_" -ForegroundColor Red
        }
    }
}

# Function to deploy domain
function Deploy-Domain {
    param($Domain)

    Write-Host "`nDeploying domain: $Domain" -ForegroundColor Yellow
    Write-Host "----------------------------" -ForegroundColor Yellow

    if (-not (Test-Path $Domain)) {
        Write-Host "❌ Domain directory not found: $Domain" -ForegroundColor Red
        return
    }

    # Create temp directory
    $tempDir = "temp-deploy-$Domain-$(Get-Date -Format 'yyyyMMddHHmmss')"
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

    try {
        # Copy domain files
        Copy-Item -Path $Domain -Destination $tempDir -Recurse

        # Copy common files
        if (Test-Path "README.md") { Copy-Item "README.md" $tempDir }
        if (Test-Path ".gitignore") { Copy-Item ".gitignore" $tempDir }
        if (Test-Path ".github") { Copy-Item ".github" $tempDir -Recurse }

        # Initialize git
        Push-Location $tempDir
        git init
        git config user.name "Gogidix DevOps"
        git config user.email "devops@gogidix.com"

        # Create branch
        $branchName = "deploy-$Domain-$(Get-Date -Format 'yyyyMMddHHmmss')"
        git checkout -b $branchName

        # Add files
        git add .
        git commit -m "feat: Deploy $Domain domain

- Add $Domain services and configurations
- Includes all microservices and infrastructure
- Ready for CI/CD pipeline

🤖 Generated with Gogidix DevOps Automation"

        # Add remote and push
        git remote add origin "https://github.com/$GitHubOrg/$RepoName.git"

        Write-Host "Pushing to GitHub..." -ForegroundColor Blue
        git push -u origin $branchName --force

        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Successfully deployed $Domain" -ForegroundColor Green

            Write-Host "`nPull Request Instructions:" -ForegroundColor Blue
            Write-Host "1. Go to: https://github.com/$GitHubOrg/$RepoName/compare"
            Write-Host "2. Select branch: $branchName"
            Write-Host "3. Create pull request to main branch"
            Write-Host "4. Title: Deploy $Domain domain"
            Write-Host "5. Description: Deployment of $Domain with all services"
        } else {
            Write-Host "❌ Failed to push $Domain" -ForegroundColor Red
        }

        Pop-Location
    } finally {
        # Cleanup
        Pop-Location
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# Main execution
Create-GitHubRepo

foreach ($domain in $Domains) {
    Write-Host "`n================================" -ForegroundColor Blue
    Deploy-Domain -Domain $domain

    # Pause between deployments
    Start-Sleep -Seconds 3
}

Write-Host "`n🎉 All domains have been processed!" -ForegroundColor Green
Write-Host "Repository: https://github.com/$GitHubOrg/$RepoName" -ForegroundColor Blue
Write-Host "`nNext Steps:" -ForegroundColor Yellow
Write-Host "1. Review and merge pull requests"
Write-Host "2. Add required secrets to GitHub repository:"
Write-Host "   - KUBE_CONFIG"
Write-Host "   - KUBE_CONFIG_PROD"
Write-Host "   - SLACK_WEBHOOK_URL"
Write-Host "3. Configure CI/CD environment variables"
Write-Host "4. Run the CI/CD pipeline"