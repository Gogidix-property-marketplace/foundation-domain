# Health Check Script for Gogidix AI Services
# This script checks the health of all running services

Write-Host "🔍 Checking Gogidix AI Services Health" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

# Services to check
$services = @(
    @{name="AI Gateway"; port=8000; endpoint="/health"},
    @{name="Property Intelligence"; port=8001; endpoint="/health"},
    @{name="Conversational AI"; port=8002; endpoint="/health"},
    @{name="Analytics"; port=8003; endpoint="/health"},
    @{name="ML Platform"; port=8004; endpoint="/health"},
    @{name="Ethical AI"; port=8005; endpoint="/health"},
    @{name="MLflow"; port=5000; endpoint="/"},
    @{name="Prometheus"; port=9090; endpoint="/"},
    @{name="Grafana"; port=3000; endpoint="/api/health"}
)

# Infrastructure services
$infraServices = @(
    @{name="PostgreSQL"; port=5432; container="gogidix-postgres"},
    @{name="Redis"; port=6379; container="gogidix-redis"},
    @{name="Kafka"; port=9092; container="gogidix-kafka"},
    @{name="Elasticsearch"; port=9200; container="gogidix-elasticsearch"}
)

$healthyServices = 0
$totalServices = $services.Count

Write-Host ""
Write-Host "Checking Application Services:" -ForegroundColor Yellow

foreach ($service in $services) {
    $url = "http://localhost:$($service.port)$($service.endpoint)"

    try {
        $response = Invoke-RestMethod -Uri $url -Method GET -TimeoutSec 5 -ErrorAction Stop
        Write-Host "✓ $($service.name) (Port $($service.port)): Healthy" -ForegroundColor Green
        $healthyServices++
    } catch {
        if ($_.Exception.Response.StatusCode) {
            Write-Host "✗ $($service.name) (Port $($service.port)): HTTP $($_.Exception.Response.StatusCode)" -ForegroundColor Red
        } else {
            Write-Host "✗ $($service.name) (Port $($service.port)): Connection Failed" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "Checking Infrastructure Services:" -ForegroundColor Yellow

foreach ($service in $infraServices) {
    try {
        $result = & docker exec $service.container pg_isready -U gogidix 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ $($service.name) (Port $($service.port)): Running" -ForegroundColor Green
        } else {
            Write-Host "✗ $($service.name) (Port $($service.port)): Not Ready" -ForegroundColor Red
        }
    } catch {
        # Try port check as fallback
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $tcpClient.Connect("localhost", $service.port)
            $tcpClient.Close()
            Write-Host "✓ $($service.name) (Port $($service.port)): Running" -ForegroundColor Green
        } catch {
            Write-Host "✗ $($service.name) (Port $($service.port)): Not Running" -ForegroundColor Red
        }
    }
}

# Check Docker containers
Write-Host ""
Write-Host "Checking Docker Containers:" -ForegroundColor Yellow

try {
    $containers = docker-compose -f docker-compose.local.yml ps --format "table {{.Name}}\t{{.Status}}"
    Write-Host $containers
} catch {
    Write-Host "⚠ Could not retrieve container status" -ForegroundColor Yellow
}

# Display summary
Write-Host ""
Write-Host "Health Summary:" -ForegroundColor Cyan
Write-Host "================" -ForegroundColor Cyan
Write-Host "Services Running: $healthyServices/$totalServices" -ForegroundColor $(($healthyServices -eq $totalServices) ? 'Green' : 'Yellow')

if ($healthyServices -eq $totalServices) {
    Write-Host "✅ All services are healthy!" -ForegroundColor Green
} else {
    Write-Host "⚠ Some services are not responding. Check the logs for details:" -ForegroundColor Yellow
    Write-Host "   docker-compose -f docker-compose.local.yml logs -f [service-name]" -ForegroundColor Gray
}

# Check system resources
Write-Host ""
Write-Host "System Resources:" -ForegroundColor Yellow
Write-Host "==================" -ForegroundColor Yellow

try {
    # Docker system info
    $dockerInfo = docker system df
    Write-Host "Docker Disk Usage:" -ForegroundColor Gray
    $dockerInfo
} catch {
    Write-Host "Could not retrieve Docker info" -ForegroundColor Red
}

# Check if API is accessible with test
Write-Host ""
Write-Host "Quick API Test:" -ForegroundColor Yellow
Write-Host "================" -ForegroundColor Yellow

try {
    $testResponse = Invoke-RestMethod -Uri "http://localhost:8000/health" -Method GET -TimeoutSec 5
    Write-Host "✅ AI Gateway is responding" -ForegroundColor Green
    Write-Host "Response: $($testResponse | ConvertTo-Json -Compress)" -ForegroundColor Gray
} catch {
    Write-Host "❌ AI Gateway is not responding" -ForegroundColor Red
}

# Provide next steps
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "============" -ForegroundColor Cyan

if ($healthyServices -lt $totalServices) {
    Write-Host "To view logs:" -ForegroundColor White
    Write-Host "  docker-compose -f docker-compose.local.yml logs -f [service-name]" -ForegroundColor Gray
    Write-Host ""
    Write-Host "To restart services:" -ForegroundColor White
    Write-Host "  docker-compose -f docker-compose.local.yml restart" -ForegroundColor Gray
} else {
    Write-Host "✨ Everything looks good! You can:" -ForegroundColor White
    Write-Host "  • Test APIs at: http://localhost:8000/docs" -ForegroundColor Gray
    Write-Host "  • View Grafana: http://localhost:3000" -ForegroundColor Gray
    Write-Host "  • Run API tests: .\test_api.bat" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")