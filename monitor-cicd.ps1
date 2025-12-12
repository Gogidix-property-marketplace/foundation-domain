# Foundation Domain CI/CD Pipeline Monitor
# =======================================
# Real-time monitoring and troubleshooting of GitHub Actions

param(
    [string]$GitHubToken = "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",
    [string]$GitHubOrg = "Gogidix-property-marketplace",
    [string]$RepoName = "foundation-domain",
    [int]$RefreshInterval = 30,  # seconds
    [switch]$Continuous = $false
)

# Set error action preference
$ErrorActionPreference = "Stop"

# Headers for GitHub API
$headers = @{
    "Authorization" = "token $GitHubToken"
    "Accept" = "application/vnd.github.v3+json"
}

Write-Host "========================================" -ForegroundColor Blue
Write-Host "CI/CD Pipeline Monitor" -ForegroundColor Blue
Write-Host "Repository: $GitHubOrg/$RepoName" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

# Function to get workflow runs
function Get-WorkflowRuns {
    $url = "https://api.github.com/repos/$GitHubOrg/$RepoName/actions/runs?per_page=20"

    try {
        $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers -SkipCertificateCheck
        return $response.workflow_runs
    } catch {
        Write-Host "❌ Error fetching workflow runs: $_" -ForegroundColor Red
        return $null
    }
}

# Function to analyze failure
function Analyze-Failure {
    param($run)

    Write-Host "`n🔍 Analyzing failure for run #$($run.id)" -ForegroundColor Yellow
    Write-Host "Workflow: $($run.name)" -ForegroundColor Cyan
    Write-Host "Event: $($run.event)" -ForegroundColor Cyan
    Write-Host "Branch: $($run.head_branch)" -ForegroundColor Cyan

    # Get jobs for this run
    $jobsUrl = "https://api.github.com/repos/$GitHubOrg/$RepoName/actions/runs/$($run.id)/jobs"
    $jobs = Invoke-RestMethod -Uri $jobsUrl -Method Get -Headers $headers -SkipCertificateCheck

    foreach ($job in $jobs.jobs) {
        if ($job.conclusion -eq "failure") {
            Write-Host "`n❌ Failed Job: $($job.name)" -ForegroundColor Red

            # Get job log
            $logUrl = "https://api.github.com/repos/$GitHubOrg/$RepoName/actions/jobs/$($job.id)/logs"
            try {
                $logs = Invoke-RestMethod -Uri $logUrl -Method Get -Headers $headers -SkipCertificateCheck
                $logLines = $logs -split "`n"

                # Look for error patterns
                $errorPatterns = @(
                    "Error:",
                    "ERROR:",
                    "FAIL:",
                    "FAILED:",
                    "Exception:",
                    "fatal:",
                    "permission denied",
                    "access denied",
                    "timeout",
                    "not found",
                    "command not found",
                    "npm ERR!",
                    "maven",
                    "java.lang",
                    "Build failed"
                )

                $foundErrors = $false
                for ($i = [Math]::Max(0, $logLines.Length - 100); $i -lt $logLines.Length; $i++) {
                    foreach ($pattern in $errorPatterns) {
                        if ($logLines[$i] -match $pattern) {
                            Write-Host "  → Error: $($logLines[$i].Trim())" -ForegroundColor Red
                            $foundErrors = $true
                            if ($foundErrors) { break }
                        }
                    }
                    if ($foundErrors) { break }
                }

                # Check for common issues
                if (!$foundErrors) {
                    Write-Host "  → No specific error pattern found. Check full logs." -ForegroundColor Yellow
                }
            } catch {
                Write-Host "  → Could not fetch job logs: $_" -ForegroundColor Red
            }
        }
    }
}

# Function to display status
function Display-Status {
    $runs = Get-WorkflowRuns

    if ($null -eq $runs) {
        return
    }

    Clear-Host
    Write-Host "========================================" -ForegroundColor Blue
    Write-Host "CI/CD Pipeline Status - $(Get-Date)" -ForegroundColor Blue
    Write-Host "========================================" -ForegroundColor Blue

    # Show recent runs
    $recentRuns = $runs | Select-Object -First 10

    foreach ($run in $recentRuns) {
        $status = $run.status
        $conclusion = $run.conclusion

        # Determine color
        if ($status -eq "in_progress") {
            $color = "Yellow"
            $icon = "⏳"
        } elseif ($conclusion -eq "success") {
            $color = "Green"
            $icon = "✅"
        } elseif ($conclusion -eq "failure") {
            $color = "Red"
            $icon = "❌"
        } elseif ($conclusion -eq "cancelled") {
            $color = "Gray"
            $icon = "⏹️"
        } else {
            $color = "White"
            $icon = "⭕"
        }

        Write-Host "$icon Run #$($run.id) - $($run.name)" -ForegroundColor $color
        Write-Host "   Branch: $($run.head_branch) | Event: $($run.event) | Created: $($run.created_at)" -ForegroundColor Gray

        if ($status -eq "in_progress") {
            Write-Host "   Progress: $($run.run_number) | Status: $status" -ForegroundColor $color
        } else {
            Write-Host "   Progress: $($run.run_number) | Conclusion: $conclusion | Duration: $([math]::Round(([DateTime]$run.updated_at - [DateTime]$run.created_at).TotalMinutes, 2)) minutes" -ForegroundColor Gray
        }

        Write-Host ""
    }

    # Show workflow statistics
    $totalRuns = $runs.Count
    $successfulRuns = ($runs | Where-Object { $_.conclusion -eq "success" }).Count
    $failedRuns = ($runs | Where-Object { $_.conclusion -eq "failure" }).Count
    $inProgressRuns = ($runs | Where-Object { $_.status -eq "in_progress" }).Count

    Write-Host "`n📊 Statistics (Last $totalRuns runs):" -ForegroundColor Cyan
    Write-Host "   Success Rate: $([math]::Round($successfulRuns/$totalRuns*100, 1))% ($successfulRuns/$totalRuns)" -ForegroundColor Green
    Write-Host "   Failed: $failedRuns | In Progress: $inProgressRuns" -ForegroundColor Gray

    # Check for any recent failures
    $recentFailures = $runs | Where-Object { $_.conclusion -eq "failure" } | Select-Object -First 3
    if ($recentFailures.Count -gt 0) {
        Write-Host "`n⚠️  Recent failures detected:" -ForegroundColor Red
        foreach ($failure in $recentFailures) {
            Write-Host "   - $($failure.name) on $($failure.head_branch) ($($failure.created_at))" -ForegroundColor Red
        }
    }
}

# Main monitoring loop
do {
    Display-Status

    # Check for failed runs and analyze them
    $runs = Get-WorkflowRuns
    $failedRuns = $runs | Where-Object { $_.conclusion -eq "failure" -and [DateTime]$_.created_at -gt (Get-Date).AddMinutes(-30) }

    if ($failedRuns.Count -gt 0) {
        Write-Host "`n🚨 Analyzing recent failures..." -ForegroundColor Red
        foreach ($failure in $failedRuns) {
            Analyze-Failure -run $failure
            Write-Host "`n" + ("-" * 50)
        }
    }

    if ($Continuous) {
        Write-Host "`nRefreshing in $RefreshInterval seconds... (Press Ctrl+C to stop)" -ForegroundColor Gray
        Start-Sleep -Seconds $RefreshInterval
    }
} while ($Continuous)

Write-Host "`n✅ Monitoring complete!" -ForegroundColor Green