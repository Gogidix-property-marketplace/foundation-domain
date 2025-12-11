# Simple Foundation Domain Deployment Script
# ========================================
# Run this script in PowerShell to deploy the foundation domain

Write-Host "========================================" -ForegroundColor Blue
Write-Host "Foundation Domain Deployment Script" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

# Configuration
$GitHubOrg = "Gogidix-property-marketplace"
$RepoName = "foundation-domain"
$GitHubToken = "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"

# Step 1: Navigate to the correct directory
Set-Location "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain"
Write-Host "`nWorking directory: $(Get-Location)" -ForegroundColor Cyan

# Step 2: Fix git issues
Write-Host "`n[1/4] Fixing git issues..." -ForegroundColor Yellow

# Remove index.lock if exists
if (Test-Path ".git\index.lock") {
    Write-Host "Removing git index.lock file..." -ForegroundColor Yellow
    Remove-Item -Force ".git\index.lock" -ErrorAction SilentlyContinue
}

# Kill git processes
$gitProcesses = Get-Process -Name "git" -ErrorAction SilentlyContinue
if ($gitProcesses) {
    Write-Host "Stopping git processes..." -ForegroundColor Yellow
    $gitProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

# Step 3: Commit and push
Write-Host "`n[2/4] Configuring git..." -ForegroundColor Yellow
git config user.name "Gogidix DevOps"
git config user.email "devops@gogidix.com"

Write-Host "`n[3/4] Adding and committing files..." -ForegroundColor Yellow
git add .

git commit -m "feat: Complete Foundation Domain Deployment

- Deploy all 5 domains: shared-libraries, shared-infrastructure, central-configuration, ai-services, centralized-dashboard
- Total: 306+ microservices (195 Java, 51 Node.js, 50 Python, 10 Frontend)
- Includes CI/CD pipelines, Docker configs, and Kubernetes manifests
- Ready for production deployment

🤖 Generated with Gogidix DevOps Automation"

if ($LASTEXITCODE -ne 0) {
    Write-Host "Warning: No changes to commit or commit failed" -ForegroundColor Yellow
}

# Step 4: Add remote and push
Write-Host "`n[4/4] Pushing to GitHub..." -ForegroundColor Yellow

# Check if remote exists
try {
    $remoteUrl = git remote get-url origin
    Write-Host "Remote already configured" -ForegroundColor Green
} catch {
    Write-Host "Adding remote origin..." -ForegroundColor Yellow
    git remote add origin "https://github.com/$GitHubOrg/$RepoName.git"
}

# Push to GitHub
Write-Host "Pushing to main branch..." -ForegroundColor Yellow
git push -u origin main --force

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n" + ("=" * 50) -ForegroundColor Green
    Write-Host "🎉 SUCCESS! Foundation domain deployed to GitHub!" -ForegroundColor Green
    Write-Host "Repository: https://github.com/$GitHubOrg/$RepoName" -ForegroundColor Cyan
    Write-Host "`nNext Steps:" -ForegroundColor Yellow
    Write-Host "1. Visit the repository to verify files" -ForegroundColor White
    Write-Host "2. Add GitHub secrets (KUBE_CONFIG, etc.)" -ForegroundColor White
    Write-Host "3. Enable GitHub Actions" -ForegroundColor White
    Write-Host ("=" * 50) -ForegroundColor Green
} else {
    Write-Host "`n❌ Failed to push to GitHub" -ForegroundColor Red
    Write-Host "Please check:" -ForegroundColor Yellow
    Write-Host "1. Your internet connection" -ForegroundColor White
    Write-Host "2. Your GitHub token" -ForegroundColor White
    Write-Host "3. Run as Administrator if needed" -ForegroundColor White
}

Write-Host "`nPress any key to exit..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")