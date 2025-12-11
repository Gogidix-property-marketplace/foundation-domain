@echo off
echo ?? Service Monitor
echo ================

:loop
cls
echo.
echo Checking services at %time%...
echo.

REM Check if services are responding
powershell -Command "try { Invoke-RestMethod -Uri 'http://localhost:8000/health' -TimeoutSec 2 | Out-Null; Write-Host '? AI Gateway (8000): Running' -ForegroundColor Green } catch { Write-Host '? AI Gateway (8000): Down' -ForegroundColor Red }"

powershell -Command "try { Invoke-RestMethod -Uri 'http://localhost:8001/health' -TimeoutSec 2 | Out-Null; Write-Host '? Property Intel (8001): Running' -ForegroundColor Green } catch { Write-Host '? Property Intel (8001): Down' -ForegroundColor Red }"

powershell -Command "try { Invoke-RestMethod -Uri 'http://localhost:8002/health' -TimeoutSec 2 | Out-Null; Write-Host '? Conversational AI (8002): Running' -ForegroundColor Green } catch { Write-Host '? Conversational AI (8002): Down' -ForegroundColor Red }"

powershell -Command "try { Invoke-RestMethod -Uri 'http://localhost:8003/health' -TimeoutSec 2 | Out-Null; Write-Host '? Analytics (8003): Running' -ForegroundColor Green } catch { Write-Host '? Analytics (8003): Down' -ForegroundColor Red }"

echo.
echo Memory Usage:
powershell -Command "Get-Process -Name python | Select-Object ProcessName, @{Name='Memory(MB)';Expression={[math]::Round(.WorkingSet/1MB,2)}} | Format-Table -AutoSize"

echo.
echo Press Ctrl+C to stop monitoring
timeout /t 5 /nobreak
goto loop
