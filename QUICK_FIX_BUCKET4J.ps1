# PowerShell Script to Fix bucket4j Dependency Issue
# This script will remove the problematic bucket4j dependency from all services

Write-Host "🔧 Fixing bucket4j dependency issue in all Foundation services..." -ForegroundColor Green

# Get all service directories
$services = Get-ChildItem -Path "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\central-configuration\backend-services\java-services" -Directory

foreach ($service in $services) {
    $pomPath = Join-Path $service.FullName "$($service.Name)\pom.xml"

    if (Test-Path $pomPath) {
        Write-Host "  Fixing $($service.Name)..." -ForegroundColor Yellow

        # Read the pom.xml file
        $content = Get-Content $pomPath -Raw

        # Remove bucket4j dependency (multiple patterns to catch it)
        $bucket4jPatterns = @(
            '(?s)\s*<dependency>\s*<groupId>com\.github\.vladimir-bukhtoyarov</groupId>\s*<artifactId>bucket4j-spring-boot-starter</artifactId>\s*<version>.*?</version>\s*</dependency>',
            '(?s)\s*<!-- Rate Limiting -->\s*<dependency>\s*<groupId>com\.github\.vladimir-bukhtoyarov</groupId>\s*<artifactId>bucket4j-spring-boot-starter</artifactId>\s*<version>.*?</version>\s*</dependency>'
        )

        $hasChanges = $false

        foreach ($pattern in $bucket4jPatterns) {
            if ($content -match $pattern) {
                $content = $content -replace $pattern, ''
                $hasChanges = $true
                Write-Host "    ✅ Removed bucket4j dependency" -ForegroundColor Green
            }
        }

        # If we made changes, save the file
        if ($hasChanges) {
            # Clean up any empty lines left behind
            $content = $content -replace '(?m)^\s*\n\s*\n', "`n"

            # Save the fixed pom.xml
            $content | Out-File -FilePath $pomPath -Encoding UTF8 -NoNewline

            # Test compilation
            Push-Location (Join-Path $service.FullName $service.Name)
            Write-Host "    Testing compilation..." -ForegroundColor Cyan

            try {
                $result = mvn clean compile -q 2>&1
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "    ✅ Compilation successful!" -ForegroundColor Green
                } else {
                    Write-Host "    ❌ Compilation failed: $result" -ForegroundColor Red
                }
            } catch {
                Write-Host "    ❌ Error during compilation: $($_.Exception.Message)" -ForegroundColor Red
            }
            Pop-Location
        } else {
            Write-Host "    ℹ️ No bucket4j dependency found" -ForegroundColor Cyan
        }

        Write-Host ""  # Add blank line for readability
    }
}

Write-Host "🎉 Bucket4j fix process complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Verify all services compile successfully" -ForegroundColor White
Write-Host "2. Implement business logic for each service" -ForegroundColor White
Write-Host "3. Add tests for all functionality" -ForegroundColor White
Write-Host "4. Deploy and test in development environment" -ForegroundColor White