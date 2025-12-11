param(
    [switch]$SkipDocker,
    [switch]$UseInMemory
)

# Color output
function Write-ColorOutput($Message, $Color = "White") {
    Write-Host $Message -ForegroundColor $Color
}

Write-ColorOutput "=== Gogidix Property Marketplace - Local Services Startup ===" "Green"
Write-ColorOutput ""

# Check prerequisites
Write-ColorOutput "Checking prerequisites..." "Yellow"

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-ColorOutput "✓ Java: $javaVersion" "Green"
} catch {
    Write-ColorOutput "✗ Java is not installed or not in PATH" "Red"
    Write-ColorOutput "Please install Java 21 from https://adoptium.net/" "Red"
    exit 1
}

# Check Maven
try {
    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven"
    Write-ColorOutput "✓ Maven: $mavenVersion" "Green"
} catch {
    Write-ColorOutput "✗ Maven is not installed or not in PATH" "Red"
    Write-ColorOutput "Please install Maven 3.9+ from https://maven.apache.org/" "Red"
    exit 1
}

# Service configuration
$services = @(
    @{
        Name = "Config Server"
        Path = "..\java-services\config-server"
        Port = 8888
        HealthUrl = "http://localhost:8888/actuator/health"
        Profiles = "local"
    },
    @{
        Name = "Eureka Server"
        Path = "..\java-services\eureka-server"
        Port = 8761
        HealthUrl = "http://localhost:8761/actuator/health"
        Profiles = "local"
        WaitAfter = 10  # Wait extra time for Eureka
    },
    @{
        Name = "API Gateway"
        Path = "..\java-services\api-gateway"
        Port = 8080
        HealthUrl = "http://localhost:8080/actuator/health"
        Profiles = "local"
        DependsOn = @("Config Server", "Eureka Server")
    },
    @{
        Name = "Rate Limiting Service"
        Path = "..\java-services\rate-limiting-service"
        Port = 8081
        HealthUrl = "http://localhost:8081/actuator/health"
        Profiles = "local"
        DependsOn = @("Config Server", "Eureka Server")
    }
)

# Optional services
if (!$UseInMemory) {
    $services += @(
        @{
            Name = "Dashboard Integration"
            Path = "..\java-services\dashboard-integration-service"
            Port = 8082
            HealthUrl = "http://localhost:8082/actuator/health"
            Profiles = "local"
            DependsOn = @("Config Server", "Eureka Server")
        },
        @{
            Name = "AI Integration"
            Path = "..\java-services\ai-integration-service"
            Port = 8083
            HealthUrl = "http://localhost:8083/actuator/health"
            Profiles = "local"
            DependsOn = @("Config Server", "Eureka Server")
        }
    )
}

# Start Docker services if needed
if (!$SkipDocker -and !$UseInMemory) {
    Write-ColorOutput "Starting Docker infrastructure services..." "Yellow"
    Set-Location $PSScriptRoot

    try {
        docker-compose up -d postgres redis elasticsearch
        Write-ColorOutput "✓ Database services started" "Green"
        Start-Sleep 5
    } catch {
        Write-ColorOutput "⚠ Could not start Docker services. Using in-memory configuration." "Yellow"
        $UseInMemory = $true
    }
}

# Function to check if port is available
function Test-Port($Port) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect("localhost", $Port)
        $tcp.Close()
        return $false  # Port is in use
    } catch {
        return $true  # Port is available
    }
}

# Function to check service health
function Test-ServiceHealth($Url, $Timeout = 30) {
    $attempt = 0
    while ($attempt -lt $Timeout / 5) {
        try {
            $response = Invoke-RestMethod -Uri $Url -TimeoutSec 5
            if ($response.status -eq "UP") {
                return $true
            }
        } catch {
            # Service not ready yet
        }
        $attempt++
        Start-Sleep 5
    }
    return $false
}

# Start services
$runningServices = @()
$baseDir = Get-Location

foreach ($service in $services) {
    Write-ColorOutput ""
    Write-ColorOutput "Starting $($service.Name)..." "Yellow"

    # Check if port is available
    if (!(Test-Port $service.Port)) {
        Write-ColorOutput "⚠ Port $($service.Port) is already in use. Skipping $($service.Name)." "Yellow"
        continue
    }

    # Check dependencies
    if ($service.DependsOn) {
        $allDepsRunning = $true
        foreach ($dep in $service.DependsOn) {
            if ($runningServices -notcontains $dep) {
                Write-ColorOutput "✗ Dependency '$dep' is not running. Skipping $($service.Name)." "Red"
                $allDepsRunning = $false
                break
            }
        }
        if (!$allDepsRunning) {
            continue
        }
    }

    # Navigate to service directory
    Set-Location (Join-Path $baseDir $service.Path)

    # Start the service in a new PowerShell window
    $args = "-NoExit", "-Command", "Set-Location '$(Get-Location)'; mvn spring-boot:run -Dspring-boot.run.profiles=$($service.Profiles)"
    Start-Process powershell -ArgumentList $args

    Write-ColorOutput "→ Starting $($service.Name) on port $($service.Port)" "Cyan"

    # Wait for service to start
    Write-ColorOutput "  Waiting for service to start..." "Gray"
    if (Test-ServiceHealth $service.HealthUrl) {
        Write-ColorOutput "✓ $($service.Name) is running!" "Green"
        $runningServices += $service.Name
    } else {
        Write-ColorOutput "⚠ $($service.Name) may not have started properly. Check the new PowerShell window." "Yellow"
    }

    # Wait for services that need extra time
    if ($service.WaitAfter) {
        Start-Sleep $service.WaitAfter
    }

    Set-Location $baseDir
}

# Summary
Write-ColorOutput ""
Write-ColorOutput "=== Startup Complete ===" "Green"
Write-ColorOutput ""
Write-ColorOutput "Running Services:" "Green"
foreach ($service in $runningServices) {
    Write-ColorOutput "  ✓ $service" "Cyan"
}

Write-ColorOutput ""
Write-ColorOutput "Service URLs:" "Green"
Write-ColorOutput "  Config Server:    http://localhost:8888" "White"
Write-ColorOutput "  Eureka:           http://localhost:8761" "White"
Write-ColorOutput "  API Gateway:      http://localhost:8080" "White"
Write-ColorOutput "  Rate Limiting:    http://localhost:8081" "White"

if (!$UseInMemory) {
    Write-ColorOutput "  Dashboard:         http://localhost:8082" "White"
    Write-ColorOutput "  AI Integration:    http://localhost:8083" "White"
}

Write-ColorOutput ""
Write-ColorOutput "To stop all services, close the opened PowerShell windows." "Yellow"
Write-ColorOutput "To check logs, see the individual PowerShell windows for each service." "Gray"