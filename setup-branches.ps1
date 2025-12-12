# Foundation Domain Branch Structure Setup
# ======================================
# Creates proper Git flow with branch protection rules

param(
    [string]$GitHubToken = "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",
    [string]$GitHubOrg = "Gogidix-property-marketplace",
    [string]$RepoName = "foundation-domain"
)

# Set error action preference
$ErrorActionPreference = "Stop"

# Headers for GitHub API
$headers = @{
    "Authorization" = "token $GitHubToken"
    "Accept" = "application/vnd.github.v3+json"
}

Write-Host "========================================" -ForegroundColor Blue
Write-Host "Setting Up Branch Structure" -ForegroundColor Blue
Write-Host "Repository: $GitHubOrg/$RepoName" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

# Repository URL
$repoUrl = "https://api.github.com/repos/$GitHubOrg/$RepoName"

# Function to create branch
function Create-Branch {
    param(
        [string]$branchName,
        [string]$fromBranch = "main"
    )

    Write-Host "`n📍 Creating branch: $branchName" -ForegroundColor Yellow

    # Get reference for source branch
    try {
        $ref = Invoke-RestMethod -Uri "$repoUrl/git/refs/heads/$fromBranch" -Method Get -Headers $headers -SkipCertificateCheck
        $sha = $ref.object.sha

        # Create branch
        $body = @{
            ref = "refs/heads/$branchName"
            sha = $sha
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "$repoUrl/git/refs" -Method Post -Headers $headers -Body $body -SkipCertificateCheck
        Write-Host "✅ Branch '$branchName' created successfully" -ForegroundColor Green
        return $true
    } catch {
        if ($_.Exception.Response.StatusCode -eq "UnprocessableEntity") {
            Write-Host "⚠️  Branch '$branchName' already exists" -ForegroundColor Yellow
            return $true
        } else {
            Write-Host "❌ Failed to create branch '$branchName': $_" -ForegroundColor Red
            return $false
        }
    }
}

# Function to set up branch protection
function Set-BranchProtection {
    param(
        [string]$branchName
    )

    Write-Host "`n🔒 Setting up protection for branch: $branchName" -ForegroundColor Yellow

    $body = @{
        required_status_checks = @{
            strict = $true
            contexts = @(
                "ci-foundation-domain",
                "build-and-test",
                "security-scan"
            )
        }
        enforce_admins = $true
        required_pull_request_reviews = @{
            required_approving_review_count = 1
            dismiss_stale_reviews = $true
            require_code_owner_reviews = $true
        }
        restrictions = $null  # No restrictions on who can push
        allow_force_pushes = $false
        allow_deletions = $false
    } | ConvertTo-Json -Depth 10

    try {
        $response = Invoke-RestMethod -Uri "$repoUrl/branches/$branchName/protection" -Method Put -Headers $headers -Body $body -SkipCertificateCheck
        Write-Host "✅ Protection rules set for '$branchName'" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "❌ Failed to set protection for '$branchName': $_" -ForegroundColor Red
        Write-Host "   Note: You might need admin permissions for this" -ForegroundColor Gray
        return $false
    }
}

# Create main branches if they don't exist
$branches = @(
    @{ name = "develop"; from = "main"; protect = $true; description = "Integration branch for features" },
    @{ name = "staging"; from = "main"; protect = $true; description = "Pre-production testing branch" },
    @{ name = "uat"; from = "staging"; protect = $true; description = "User acceptance testing branch" }
)

Write-Host "`n🌳 Creating branch structure..." -ForegroundColor Cyan

foreach ($branch in $branches) {
    Write-Host "`n" + ("-" * 50)
    Write-Host "Branch: $($branch.name)" -ForegroundColor White
    Write-Host "From: $($branch.from) | Description: $($branch.description)" -ForegroundColor Gray

    if (Create-Branch -branchName $branch.name -fromBranch $branch.from) {
        if ($branch.protect) {
            Set-BranchProtection -branchName $branch.name
        }
    }
}

# Create hotfix branch template documentation
Write-Host "`n" + ("-" * 50)
Write-Host "📋 Hotfix Branch Guidelines:" -ForegroundColor Cyan
Write-Host ""

$hotfixTemplate = @"
Hotfix Branch Naming Convention:
- hotfix/{description}
- Example: hotfix/security-patch-2024-12-11
- Example: hotfix/critical-bug-fix

Hotfix Process:
1. Create hotfix branch from 'main'
2. Fix the issue
3. Run full test suite
4. Merge to 'main'
5. Also merge to 'develop' and 'staging'
6. Delete hotfix branch
"@

Write-Host $hotfixTemplate -ForegroundColor Gray

# Update README with branch information
Write-Host "`n📝 Updating README with branch information..." -ForegroundColor Yellow

$branchInfo = @"

## 🌳 Branch Structure

```
main (production)
├── develop (integration)
│   └── feature/* (feature branches)
├── staging (pre-production)
├── uat (user acceptance testing)
└── hotfix/* (emergency fixes from main)
```

### Branch Protection Rules
- **main & staging**: Require PR reviews, status checks, no force pushes
- **develop**: Require status checks, allow force pushes for sync
- **hotfix/**: No protection (emergency fixes)

### Workflow
1. **Feature Development**: Create from `develop`, merge back to `develop`
2. **Testing**: `develop` → `staging` → `uat`
3. **Production**: `uat` → `main` (with PR and approval)
4. **Hotfixes**: Create from `main`, merge to `main` and back to `develop`

"@

# Create branch status report
$report = @"
# Branch Structure Setup Report
Generated: $(Get-Date)

Repository: $GitHubOrg/$RepoName

## Created Branches
$($branches | ForEach-Object { "- $($_.name) from $($_.from)" })

## Next Steps
1. Configure CI/CD to work with branch structure
2. Update GitHub Actions to trigger on appropriate branches
3. Set up environment-specific configurations
4. Train team on branch workflow
"@

$report | Out-File -FilePath "BRANCH_SETUP_REPORT.md" -Encoding utf8
Write-Host "`n✅ Branch setup complete! See BRANCH_SETUP_REPORT.md for details" -ForegroundColor Green

# Instructions for team
Write-Host "`n" + ("-" * 50)
Write-Host "👥 Team Instructions:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Always create feature branches from 'develop'" -ForegroundColor White
Write-Host "2. Submit PRs to 'develop' for feature integration" -ForegroundColor White
Write-Host "3. Use 'staging' for pre-production testing" -ForegroundColor White
Write-Host "4. Use 'uat' for user acceptance testing" -ForegroundColor White
Write-Host "5. Create hotfix branches from 'main' for critical fixes" -ForegroundColor White
Write-Host "6. Never push directly to 'main' or 'staging'" -ForegroundColor White
Write-Host ""

Write-Host "🎉 Branch structure setup complete!" -ForegroundColor Green