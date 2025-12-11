@echo off
echo ?? Stopping Gogidix AI Services...

REM Kill Python processes for our services
taskkill /F /IM python.exe /FI "WINDOWTITLE eq AI Gateway*" 2>nul
taskkill /F /IM python.exe /FI "WINDOWTITLE eq Property Intelligence*" 2>nul
taskkill /F /IM python.exe /FI "WINDOWTITLE eq Conversational AI*" 2>nul
taskkill /F /IM python.exe /FI "WINDOWTITLE eq Analytics*" 2>nul

echo ? Services stopped!
pause
