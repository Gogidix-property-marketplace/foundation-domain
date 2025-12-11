# CTO Production Readiness Certification - PowerShell Version
# Foundation Domain - Shared Infrastructure

Write-Host "🚀 CTO PRODUCTION READINESS CERTIFICATION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Domain: Foundation - Shared Infrastructure"
Write-Host "Services: Java (42) | Node.js (21)"
Write-Host "Timestamp: $(Get-Date)"
Write-Host ""

# Configuration
$JavaDir = "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\backend-services\java-services"
$NodeDir = "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\backend-services\nodejs-services"
$LogFile = "production_readiness_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"

# Initialize counters
$JavaServices = Get-ChildItem -Path $JavaDir -Directory | Where-Object { Test-Path "$($_.FullName)\pom.xml" }
$NodeServices = Get-ChildItem -Path $NodeDir -Directory | Where-Object { Test-Path "$($_.FullName)\package.json" }

$JavaCount = $JavaServices.Count
$NodeCount = $NodeServices.Count
$JavaPassed = 0
$JavaFailed = 0
$NodePassed = 0
$NodeFailed = 0

Write-Host "📊 PHASE 1: JAVA SERVICES ($JavaCount) - COMPILATION & TESTING" -ForegroundColor Yellow
Write-Host "-------------------------------------------------------------" -ForegroundColor Yellow

# Process Java services in parallel using jobs
$JavaJobs = @()
foreach ($service in $JavaServices) {
    $job = Start-Job -ScriptBlock {
        param($ServicePath, $ServiceName)

        Set-Location $ServicePath

        # Compile
        $compileResult = & mvn clean compile -q 2>&1
        if ($LASTEXITCODE -eq 0) {
            # Test
            $testResult = & mvn test -q 2>&1
            if ($LASTEXITCODE -eq 0) {
                # Package
                $packageResult = & mvn package -q -DskipTests=false 2>&1
                if ($LASTEXITCODE -eq 0) {
                    return @{ Service=$ServiceName; Status="PASSED"; Phase="COMPLETE" }
                } else {
                    return @{ Service=$ServiceName; Status="FAILED"; Phase="PACKAGE" }
                }
            } else {
                return @{ Service=$ServiceName; Status="FAILED"; Phase="TEST" }
            }
        } else {
            return @{ Service=$ServiceName; Status="FAILED"; Phase="COMPILE" }
        }
    } -ArgumentList $service.FullName, $service.Name

    $JavaJobs += $job
}

# Wait for all Java jobs and collect results
$JavaResults = $JavaJobs | Wait-Job | Receive-Job
$JavaJobs | Remove-Job

# Process Java results
foreach ($result in $JavaResults) {
    if ($result.Status -eq "PASSED") {
        Write-Host "✅ $($result.Service) - ALL PHASES PASSED" -ForegroundColor Green
        $JavaPassed++
    } else {
        Write-Host "❌ $($result.Service) - FAILED AT $($result.Phase)" -ForegroundColor Red
        $JavaFailed++
    }
}

Write-Host ""
Write-Host "📊 JAVA SERVICES SUMMARY:" -ForegroundColor Cyan
Write-Host "  ✅ Passed: $JavaPassed/$JavaCount" -ForegroundColor Green
Write-Host "  ❌ Failed: $JavaFailed/$JavaCount" -ForegroundColor Red

Write-Host ""
Write-Host "📊 PHASE 2: NODE.JS SERVICES ($NodeCount) - BUILD & TESTING" -ForegroundColor Yellow
Write-Host "--------------------------------------------------------" -ForegroundColor Yellow

# Process Node.js services in parallel
$NodeJobs = @()
foreach ($service in $NodeServices) {
    $job = Start-Job -ScriptBlock {
        param($ServicePath, $ServiceName)

        Set-Location $ServicePath

        # Install
        $installResult = & npm ci --silent 2>&1
        if ($LASTEXITCODE -eq 0) {
            # Test
            $testResult = & npm test --silent 2>&1
            if ($LASTEXITCODE -eq 0) {
                # Build
                $buildResult = & npm run build --silent 2>&1
                if ($LASTEXITCODE -eq 0) {
                    return @{ Service=$ServiceName; Status="PASSED"; Phase="COMPLETE" }
                } else {
                    return @{ Service=$ServiceName; Status="FAILED"; Phase="BUILD" }
                }
            } else {
                return @{ Service=$ServiceName; Status="FAILED"; Phase="TEST" }
            }
        } else {
            return @{ Service=$ServiceName; Status="FAILED"; Phase="INSTALL" }
        }
    } -ArgumentList $service.FullName, $service.Name

    $NodeJobs += $job
}

# Wait for all Node jobs and collect results
$NodeResults = $NodeJobs | Wait-Job | Receive-Job
$NodeJobs | Remove-Job

# Process Node.js results
foreach ($result in $NodeResults) {
    if ($result.Status -eq "PASSED") {
        Write-Host "✅ $($result.Service) - ALL PHASES PASSED" -ForegroundColor Green
        $NodePassed++
    } else {
        Write-Host "❌ $($result.Service) - FAILED AT $($result.Phase)" -ForegroundColor Red
        $NodeFailed++
    }
}

Write-Host ""
Write-Host "📊 NODE.JS SERVICES SUMMARY:" -ForegroundColor Cyan
Write-Host "  ✅ Passed: $NodePassed/$NodeCount" -ForegroundColor Green
Write-Host "  ❌ Failed: $NodeFailed/$NodeCount" -ForegroundColor Red

# Phase 3: Final Assessment
Write-Host ""
Write-Host "📊 PHASE 3: PRODUCTION READINESS ASSESSMENT" -ForegroundColor Magenta
Write-Host "--------------------------------------------" -ForegroundColor Magenta

$TotalServices = $JavaCount + $NodeCount
$TotalPassed = $JavaPassed + $NodePassed
$TotalFailed = $JavaFailed + $NodeFailed
$PassRate = [math]::Round(($TotalPassed / $TotalServices) * 100, 2)

Write-Host "TOTAL SERVICES PROCESSED: $TotalServices"
Write-Host "PASSED: $TotalPassed"
Write-Host "FAILED: $TotalFailed"
Write-Host "PASS RATE: $PassRate%"

if ($PassRate -ge 95) {
    Write-Host "🎯 CERTIFICATION: PRODUCTION READY ✅" -ForegroundColor Green
    $CertificationStatus = "PRODUCTION_READY"
} elseif ($PassRate -ge 85) {
    Write-Host "⚠️  CERTIFICATION: PRODUCTION READY WITH CONCERNS" -ForegroundColor Yellow
    $CertificationStatus = "PRODUCTION_READY_WITH_CONCERNS"
} else {
    Write-Host "❌ CERTIFICATION: NOT PRODUCTION READY" -ForegroundColor Red
    $CertificationStatus = "NOT_PRODUCTION_READY"
}

# Generate report
$ReportPath = "CTO_PRODUCTION_READINESS_REPORT_$(Get-Date -Format 'yyyyMMdd_HHmmss').md"
$ReportContent = @"
# CTO Production Readiness Certification Report

## Executive Summary
- **Domain**: Foundation Domain - Shared Infrastructure
- **Timestamp**: $(Get-Date)
- **Total Services**: $TotalServices
- **Pass Rate**: $PassRate%
- **Certification Status**: $CertificationStatus

## Java Services (Spring Boot)
- Total: $JavaCount
- Passed: $JavaPassed
- Failed: $JavaFailed

## Node.js Services
- Total: $NodeCount
- Passed: $NodePassed
- Failed: $NodeFailed

## Detailed Results

### Java Services Results
$($JavaResults | ForEach-Object { "- $($_.Service): $($_.Status) ($($_.Phase))`n" })

### Node.js Services Results
$($NodeResults | ForEach-Object { "- $($_.Service): $($_.Status) ($($_.Phase))`n" })

## Recommendations
"@

if ($CertificationStatus -eq "PRODUCTION_READY") {
    $ReportContent += "`n✅ All services meet production standards. Approved for deployment."
} elseif ($CertificationStatus -eq "PRODUCTION_READY_WITH_CONCERNS") {
    $ReportContent += "`n⚠️ Address failed services before production deployment."
} else {
    $ReportContent += "`n❌ Critical issues found. Deployment to production is NOT authorized."
}

$ReportContent | Out-File -FilePath $ReportPath -Encoding UTF8

Write-Host ""
Write-Host "🎯 CTO PRODUCTION READINESS CERTIFICATION COMPLETE" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Report Generated: $ReportPath" -ForegroundColor Cyan