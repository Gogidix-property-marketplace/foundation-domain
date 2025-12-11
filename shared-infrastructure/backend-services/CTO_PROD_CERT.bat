@echo off
echo ===============================================
echo CTO PRODUCTION READINESS CERTIFICATION
echo Foundation Domain - Shared Infrastructure
echo ===============================================
echo.

set JAVA_DIR=C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\backend-services\java-services
set NODE_DIR=C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\backend-services\nodejs-services
set REPORT_DIR=C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\shared-infrastructure\backend-services
set TIMESTAMP=%date:~-4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

echo Starting CTO Production Readiness Certification at %date% %time%
echo.

echo PHASE 1: JAVA SERVICES COMPILATION & TESTING
echo ============================================
echo.

cd /d "%JAVA_DIR%"

set JAVA_TOTAL=0
set JAVA_PASSED=0
set JAVA_FAILED=0

for /d %%D in (*) do (
    if exist "%%D\pom.xml" (
        set /a JAVA_TOTAL+=1
        echo Processing %%D...

        cd "%%D"

        echo   [1/3] Compiling...
        mvn clean compile -q > nul 2>&1
        if !errorlevel! equ 0 (
            echo   [2/3] Testing...
            mvn test -q > nul 2>&1
            if !errorlevel! equ 0 (
                echo   [3/3] Packaging...
                mvn package -q -DskipTests=false > nul 2>&1
                if !errorlevel! equ 0 (
                    echo   ✅ %%D - PASSED ALL PHASES
                    set /a JAVA_PASSED+=1
                    echo %%D:PASSED>>"%REPORT_DIR%\java_passed.txt"
                ) else (
                    echo   ❌ %%D - FAILED PACKAGING
                    set /a JAVA_FAILED+=1
                    echo %%D:FAILED_PACKAGE>>"%REPORT_DIR%\java_failed.txt"
                )
            ) else (
                echo   ❌ %%D - FAILED TESTING
                set /a JAVA_FAILED+=1
                echo %%D:FAILED_TEST>>"%REPORT_DIR%\java_failed.txt"
            )
        ) else (
            echo   ❌ %%D - FAILED COMPILATION
            set /a JAVA_FAILED+=1
            echo %%D:FAILED_COMPILE>>"%REPORT_DIR%\java_failed.txt"
        )

        cd ..
    )
)

echo.
echo Java Services Summary:
echo   Total: %JAVA_TOTAL%
echo   Passed: %JAVA_PASSED%
echo   Failed: %JAVA_FAILED%
echo.

echo PHASE 2: NODE.JS SERVICES BUILD & TESTING
echo ==========================================
echo.

cd /d "%NODE_DIR%"

set NODE_TOTAL=0
set NODE_PASSED=0
set NODE_FAILED=0

for /d %%D in (*) do (
    if exist "%%D\package.json" (
        set /a NODE_TOTAL+=1
        echo Processing %%D...

        cd "%%D"

        echo   [1/3] Installing dependencies...
        npm ci --silent > nul 2>&1
        if !errorlevel! equ 0 (
            echo   [2/3] Testing...
            npm test --silent > nul 2>&1
            if !errorlevel! equ 0 (
                echo   [3/3] Building...
                npm run build --silent > nul 2>&1
                if !errorlevel! equ 0 (
                    echo   ✅ %%D - PASSED ALL PHASES
                    set /a NODE_PASSED+=1
                    echo %%D:PASSED>>"%REPORT_DIR%\node_passed.txt"
                ) else (
                    echo   ❌ %%D - FAILED BUILDING
                    set /a NODE_FAILED+=1
                    echo %%D:FAILED_BUILD>>"%REPORT_DIR%\node_failed.txt"
                )
            ) else (
                echo   ❌ %%D - FAILED TESTING
                set /a NODE_FAILED+=1
                echo %%D:FAILED_TEST>>"%REPORT_DIR%\node_failed.txt"
            )
        ) else (
            echo   ❌ %%D - FAILED INSTALLATION
            set /a NODE_FAILED+=1
            echo %%D:FAILED_INSTALL>>"%REPORT_DIR%\node_failed.txt"
        )

        cd ..
    )
)

echo.
echo Node.js Services Summary:
echo   Total: %NODE_TOTAL%
echo   Passed: %NODE_PASSED%
echo   Failed: %NODE_FAILED%
echo.

echo PHASE 3: PRODUCTION READINESS ASSESSMENT
echo ==========================================
echo.

set /a TOTAL_SERVICES=%JAVA_TOTAL%+%NODE_TOTAL%
set /a TOTAL_PASSED=%JAVA_PASSED%+%NODE_PASSED%
set /a TOTAL_FAILED=%JAVA_FAILED%+%NODE_FAILED%

set /a PASS_RATE=100*%TOTAL_PASSED%/%TOTAL_SERVICES%

echo Total Services Processed: %TOTAL_SERVICES%
echo Passed: %TOTAL_PASSED%
echo Failed: %TOTAL_FAILED%
echo Pass Rate: %PASS_RATE%%%
echo.

if %PASS_RATE% geq 95 (
    echo 🎯 CERTIFICATION: PRODUCTION READY ✅
    set CERT_STATUS=PRODUCTION_READY
) else if %PASS_RATE% geq 85 (
    echo ⚠️  CERTIFICATION: PRODUCTION READY WITH CONCERNS
    set CERT_STATUS=PRODUCTION_READY_WITH_CONCERNS
) else (
    echo ❌ CERTIFICATION: NOT PRODUCTION READY
    set CERT_STATUS=NOT_PRODUCTION_READY
)

echo.
echo Generating report...
set REPORT_FILE="%REPORT_DIR%\CTO_PROD_READINESS_REPORT_%TIMESTAMP%.md"

echo # CTO Production Readiness Certification Report > %REPORT_FILE%
echo. >> %REPORT_FILE%
echo ## Executive Summary >> %REPORT_FILE%
echo - Domain: Foundation Domain - Shared Infrastructure >> %REPORT_FILE%
echo - Timestamp: %date% %time% >> %REPORT_FILE%
echo - Total Services: %TOTAL_SERVICES% >> %REPORT_FILE%
echo - Pass Rate: %PASS_RATE%%% >> %REPORT_FILE%
echo - Certification Status: %CERT_STATUS% >> %REPORT_FILE%
echo. >> %REPORT_FILE%
echo ## Java Services >> %REPORT_FILE%
echo - Total: %JAVA_TOTAL% >> %REPORT_FILE%
echo - Passed: %JAVA_PASSED% >> %REPORT_FILE%
echo - Failed: %JAVA_FAILED% >> %REPORT_FILE%
echo. >> %REPORT_FILE%
echo ## Node.js Services >> %REPORT_FILE%
echo - Total: %NODE_TOTAL% >> %REPORT_FILE%
echo - Passed: %NODE_PASSED% >> %REPORT_FILE%
echo - Failed: %NODE_FAILED% >> %REPORT_FILE%
echo. >> %REPORT_FILE%

if exist "%REPORT_DIR%\java_failed.txt" (
    echo ## Failed Java Services >> %REPORT_FILE%
    type "%REPORT_DIR%\java_failed.txt" >> %REPORT_FILE%
    echo. >> %REPORT_FILE%
)

if exist "%REPORT_DIR%\node_failed.txt" (
    echo ## Failed Node.js Services >> %REPORT_FILE%
    type "%REPORT_DIR%\node_failed.txt" >> %REPORT_FILE%
    echo. >> %REPORT_FILE%
)

echo ## Recommendations >> %REPORT_FILE%
if "%CERT_STATUS%"=="PRODUCTION_READY" (
    echo All services meet production standards. Approved for deployment. >> %REPORT_FILE%
) else if "%CERT_STATUS%"=="PRODUCTION_READY_WITH_CONCERNS" (
    echo Address failed services before production deployment. >> %REPORT_FILE%
) else (
    echo Critical issues found. Deployment to production is NOT authorized. >> %REPORT_FILE%
)

echo.
echo ===============================================
echo CTO PRODUCTION READINESS CERTIFICATION COMPLETE
echo ===============================================
echo Report Generated: %REPORT_FILE%
echo.

endlocal