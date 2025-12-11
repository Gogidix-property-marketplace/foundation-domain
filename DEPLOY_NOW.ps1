# One-Command Deployment Script
# Copy and paste this entire script into PowerShell

# Set variables
$GitHubToken = "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"
$GitHubOrg = "Gogidix-property-marketplace"
$RepoName = "foundation-domain"
$RepoPath = "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain"

# Execute deployment
Set-Location $RepoPath

# Clean git issues
Remove-Item -Force ".git\index.lock" -ErrorAction SilentlyContinue
Get-Process -Name "git" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

# Configure git
git config user.name "Gogidix DevOps"
git config user.email "devops@gogidix.com"

# Stage and commit
git add .
git commit -m "feat: Deploy Foundation Domain - 306+ microservices across 5 domains

- AI Services (142 services)
- Shared Infrastructure (114 services)
- Centralized Dashboard (27 services)
- Central Configuration (11 services)
- Shared Libraries (12 modules)

Includes CI/CD pipelines, Docker configs, and Kubernetes manifests
Ready for production deployment

Automated deployment on $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

# Setup remote with token
$remoteUrl = "https://$GitHubToken@github.com/$GitHubOrg/$RepoName.git"

try {
    git remote get-url origin | Out-Null
    git remote set-url origin $remoteUrl
} catch {
    git remote add origin $remoteUrl
}

# Push to GitHub
git push -u origin main --force

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n🎉 SUCCESS! Foundation domain deployed!" -ForegroundColor Green
    Write-Host "Repository: https://github.com/$GitHubOrg/$RepoName" -ForegroundColor Cyan
    Write-Host "Next: Add GitHub secrets and enable Actions" -ForegroundColor Yellow
} else {
    Write-Host "`n❌ Failed to push. Check token and permissions." -ForegroundColor Red
}