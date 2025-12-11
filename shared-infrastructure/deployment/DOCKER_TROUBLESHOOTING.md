# Docker Diagnostic Script
Write-Host "=== Docker Status Check ===" -ForegroundColor Green

# Check if Docker Desktop is installed
try {
    $dockerVersion = docker --version 2>$null
    Write-Host "✓ Docker is installed: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Docker Desktop from https://www.docker.com/products/docker-desktop"
    exit 1
}

# Check if Docker Desktop is running
try {
    $dockerInfo = docker info 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Docker Desktop is running" -ForegroundColor Green

        # Show Docker info
        $serverVersion = docker info --format "{{.ServerVersion}}"
        Write-Host "  Server Version: $serverVersion"

        $containers = docker ps -q 2>$null
        $containerCount = ($containers | Measure-Object).Count
        Write-Host "  Running Containers: $containerCount"
    }
} catch {
    Write-Host "✗ Docker Desktop is NOT running" -ForegroundColor Red
    Write-Host ""
    Write-Host "To fix this:" -ForegroundColor Yellow
    Write-Host "1. Start Docker Desktop from Start Menu"
    Write-Host "2. Wait for the whale icon in system tray to stop animating"
    Write-Host "3. Run this script again"
    exit 1
}

# Check Docker Compose
try {
    $composeVersion = docker-compose --version 2>$null
    Write-Host "✓ Docker Compose is available: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker Compose is not available" -ForegroundColor Red
}

# Check system resources
Write-Host ""
Write-Host "=== System Resources ===" -ForegroundColor Green

$memory = Get-WmiObject -Class Win32_ComputerSystem
$totalMemory = [math]::Round($memory.TotalPhysicalMemory / 1GB, 2)
Write-Host "Total RAM: $totalMemory GB"

if ($totalMemory -lt 16) {
    Write-Host "⚠ Warning: Less than 16GB RAM available. Docker may run slowly." -ForegroundColor Yellow
}

$disk = Get-WmiObject -Class Win32_LogicalDisk -Filter "DeviceID='C:'"
$freeSpace = [math]::Round($disk.FreeSpace / 1GB, 2)
Write-Host "Free Disk Space: $freeSpace GB"

if ($freeSpace -lt 20) {
    Write-Host "⚠ Warning: Less than 20GB free disk space. Increase disk space." -ForegroundColor Yellow
}

# Check common ports
Write-Host ""
Write-Host "=== Port Check ===" -ForegroundColor Green

$ports = @(8080, 8888, 8761, 5432, 6379, 9090, 3000)
foreach ($port in $ports) {
    $connection = Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet
    if ($connection) {
        Write-Host "⚠ Port $port is in use" -ForegroundColor Yellow
    } else {
        Write-Host "✓ Port $port is available" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "=== Next Steps ===" -ForegroundColor Green
Write-Host "If all checks pass, run: docker-compose up -d"