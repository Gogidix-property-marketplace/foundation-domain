@echo off
setlocal enabledelayedexpansion

echo ==================================================
echo Building All 11 Foundation Services for Production
echo ==================================================
echo.

:: List of all 11 services
set services[0]=ConfigManagementService
set services[1]=DynamicConfigService
set services[2]=SecretsManagementService
set services[3]=SecretsRotationService
set services[4]=FeatureFlagsService
set services[5]=RateLimitingService
set services[6]=AuditLoggingConfigService
set services[7]=BackupConfigService
set services[8]=DisasterRecoveryConfigService
set services[9]=EnvironmentVarsService
set services[10]=PolicyManagementService

:: Results tracking
set success=0
set failed=0

:: Function to build a service
:build_service
set service_name=%~1
echo --------------------------------------------------
echo Building %service_name% (Port: 8888)...
echo --------------------------------------------------

cd "%base_path%\%service_name%\%service_name%"

echo [1/4] Cleaning...
call mvn clean -q
if errorlevel 1 goto :fail

echo [2/4] Compiling...
call mvn compile -q
if errorlevel 1 goto :fail

echo [3/4] Running tests...
call mvn test -q -Dmaven.test.failure.ignore=false
if errorlevel 1 goto :fail

echo [4/4] Packaging...
call mvn package -DskipITs=false -Ddockerfile.skip=true -q
if errorlevel 1 goto :fail

:: Check if JAR was created
if exist "target\%service_name%-1.0.0.jar" (
    for %%F in ("target\%service_name%-1.0.0.jar") do set size=%%~zF
    set /a sizeMB=!size!/1048576
    echo ✅ SUCCESS: %service_name% (JAR: !sizeMB!MB)
    set /a success+=1
) else (
    echo ❌ FAILED: %service_name% (JAR not found)
    set /a failed+=1
)
goto :end

:fail
echo ❌ FAILED: %service_name%
set /a failed+=1

:end
cd /d "%base_path%"
echo.
goto :eof

:: Main execution
set base_path=C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\central-configuration\backend-services\java-services
cd /d "%base_path%"

:: Build each service
for /L %%i in (0,1,10) do (
    call :build_service !services[%%i]!
)

:: Summary
echo ==================================================
echo BUILD SUMMARY
echo ==================================================
echo.
echo ✅ Successfully Built: %success% services
echo ❌ Failed: %failed% services
echo.
echo Total: 11 services

if %failed%==0 (
    echo.
    echo 🎉 ALL SERVICES BUILT SUCCESSFULLY!
    echo ✅ PRODUCTION CERTIFICATION: PASSED
) else (
    echo.
    echo ⚠️  %failed% services failed build
    echo ❌ PRODUCTION CERTIFICATION: INCOMPLETE
)

echo.
pause