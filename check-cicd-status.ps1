# Simple CI/CD Status Checker
# ===========================
# Checks GitHub Actions status without direct API calls

param(
    [string]$GitHubToken = "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",
    [string]$GitHubOrg = "Gogidix-property-marketplace",
    [string]$RepoName = "foundation-domain"
)

Write-Host "========================================" -ForegroundColor Blue
Write-Host "CI/CD Status Checker" -ForegroundColor Blue
Write-Host "Repository: $GitHubOrg/$RepoName" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

# Function to check if GitHub CLI is available
function Test-GitHubCLI {
    try {
        $version = gh --version 2>$null
        if ($version) {
            Write-Host "✅ GitHub CLI is available" -ForegroundColor Green
            return $true
        }
    } catch {
        Write-Host "❌ GitHub CLI not found" -ForegroundColor Red
        return $false
    }
    return $false
}

# Function to check status with GitHub CLI
function Get-GitHubStatus {
    if (Test-GitHubCLI) {
        try {
            Write-Host "`n📊 Checking recent workflow runs..." -ForegroundColor Yellow
            gh run list --repo "$GitHubOrg/$RepoName" --limit 5 --json status,conclusion,headBranch,createdAt,displayTitle

            Write-Host "`n🔍 Checking current run status..." -ForegroundColor Yellow
            gh run view --repo "$GitHubOrg/$RepoName" --json status,conclusion,jobs

            return $true
        } catch {
            Write-Host "❌ Failed to fetch status with GitHub CLI: $_" -ForegroundColor Red
            return $false
        }
    }
    return $false
}

# Function to provide manual instructions
function Show-ManualCheck {
    Write-Host "`n⚠️  Automated check failed. Manual verification required:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Open GitHub Repository:" -ForegroundColor White
    Write-Host "   https://github.com/$GitHubOrg/$RepoName" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "2. Click on 'Actions' tab" -ForegroundColor White
    Write-Host ""
    Write-Host "3. Check for running workflows" -ForegroundColor White
    Write-Host ""
    Write-Host "4. Look for any failures in recent runs" -ForegroundColor White
    Write-Host ""
    Write-Host "Common Issues to Check:" -ForegroundColor Cyan
    Write-Host "- Build timeouts" -ForegroundColor Gray
    Write-Host "- Missing dependencies" -ForegroundColor Gray
    Write-Host "- Permission errors" -ForegroundColor Gray
    Write-Host "- Resource exhaustion" -ForegroundColor Gray
    Write-Host ""
}

# Function to check local git status
function Get-LocalStatus {
    Write-Host "`n📍 Local Repository Status:" -ForegroundColor Yellow
    Write-Host "Current branch: $(git branch --show-current)" -ForegroundColor Gray
    Write-Host "Last commit: $(git log -1 --oneline)" -ForegroundColor Gray

    Write-Host "`n🌳 Branches:" -ForegroundColor Yellow
    git branch -a | ForEach-Object {
        if ($_ -match "->") {
            Write-Host "  $_" -ForegroundColor Cyan
        } else {
            Write-Host "  $_" -ForegroundColor Gray
        }
    }
}

# Function to check workflow files
function Get-WorkflowStatus {
    Write-Host "`n📋 Workflow Files:" -ForegroundColor Yellow
    $workflowPath = ".github/workflows"
    if (Test-Path $workflowPath) {
        Get-ChildItem $workflowPath -Filter "*.yml" | ForEach-Object {
            Write-Host "  ✅ $($_.Name)" -ForegroundColor Green
        }
    } else {
        Write-Host "  ❌ No workflow directory found" -ForegroundColor Red
    }
}

# Main execution
try {
    # Check local status
    Get-LocalStatus
    Get-WorkflowStatus

    # Try to get GitHub status
    $statusRetrieved = Get-GitHubStatus

    if (-not $statusRetrieved) {
        Show-ManualCheck
    }

    # Provide recommendations
    Write-Host "`n💡 Recommendations:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. If workflows are failing:" -ForegroundColor White
    Write-Host "   - Check the workflow logs for errors" -ForegroundColor Gray
    Write-Host "   - Verify all secrets are configured" -ForegroundColor Gray
    Write-Host "   - Check if there are syntax errors in YAML" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. If builds are timing out:" -ForegroundColor White
    Write-Host "   - The repository has 10,000+ files" -ForegroundColor Gray
    Write-Host "   - Consider optimizing the workflow with path filters" -ForegroundColor Gray
    Write-Host "   - Use caching for dependencies" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. To trigger workflows manually:" -ForegroundColor White
    Write-Host "   - Visit: https://github.com/$GitHubOrg/$RepoName/actions" -ForegroundColor Gray
    Write-Host "   - Click on the workflow and 'Run workflow'" -ForegroundColor Gray
    Write-Host ""

    Write-Host "✅ Status check complete!" -ForegroundColor Green

} catch {
    Write-Host "❌ Error during status check: $_" -ForegroundColor Red
    Show-ManualCheck
}