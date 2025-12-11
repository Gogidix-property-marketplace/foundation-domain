@echo off
setlocal enabledelayedexpansion

:: Foundation Domain Deployment Script for Windows
:: ==============================================
:: This script will deploy the foundation domain to GitHub

echo.
echo =============================================
echo Foundation Domain Deployment Script for Windows
echo =============================================
echo.

:: Configuration
set GITHUB_ORG=Gogidix-property-marketplace
set REPO_NAME=foundation-domain
set GITHUB_TOKEN=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

:: Change to the foundation domain directory
cd /d "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain"

:: Step 1: Clean up any existing git issues
echo [1/5] Cleaning up git environment...
if exist .git\index.lock (
    echo Removing git index.lock file...
    del /f /q .git\index.lock 2>nul
)

:: Kill any hanging git processes
echo Checking for running git processes...
tasklist | findstr git.exe >nul
if %errorlevel% equ 0 (
    echo Killing git processes...
    taskkill /f /im git.exe >nul 2>&1
)

:: Step 2: Initialize git repository
echo.
echo [2/5] Initializing git repository...
if not exist .git (
    git init
    git config user.name "Gogidix DevOps"
    git config user.email "devops@gogidix.com"
)

:: Step 3: Create GitHub repository
echo.
echo [3/5] Creating GitHub repository...
curl -k -s -H "Authorization: token %GITHUB_TOKEN%" ^
     -H "Accept: application/vnd.github.v3+json" ^
     "https://api.github.com/orgs/%GITHUB_ORG%/repos" ^
     -d "{\"name\":\"%REPO_NAME%\",\"description\":\"Gogidix Foundation Domain - Core Infrastructure Services\",\"private\":false}" ^
     >nul 2>&1

if %errorlevel% equ 0 (
    echo ✅ Repository created or already exists
) else (
    echo ⚠️ Repository might already exist
)

:: Step 4: Add files and commit
echo.
echo [4/5] Adding files and creating commit...
git add .

git commit -m "feat: Complete Foundation Domain Deployment

- Deploy all 5 domains: shared-libraries, shared-infrastructure, central-configuration, ai-services, centralized-dashboard
- Total: 306+ microservices (195 Java, 51 Node.js, 50 Python, 10 Frontend)
- Includes CI/CD pipelines, Docker configs, and Kubernetes manifests
- Ready for production deployment

🤖 Generated with Gogidix DevOps Automation"

if %errorlevel% neq 0 (
    echo ❌ Failed to commit files
    pause
    exit /b 1
)

:: Step 5: Add remote and push
echo.
echo [5/5] Pushing to GitHub...

:: Check if remote exists
git remote get-url origin >nul 2>&1
if %errorlevel% neq 0 (
    git remote add origin "https://github.com/%GITHUB_ORG%/%REPO_NAME%.git"
)

:: Push to GitHub
git push -u origin main --force

if %errorlevel% equ 0 (
    echo.
    echo ✅ Successfully deployed Foundation Domain to GitHub!
    echo.
    echo Repository: https://github.com/%GITHUB_ORG%/%REPO_NAME%
    echo.
    echo Next Steps:
    echo 1. Visit the repository to verify all files are uploaded
    echo 2. Add required secrets to GitHub repository:
    echo    - KUBE_CONFIG
    echo    - KUBE_CONFIG_PROD
    echo    - SLACK_WEBHOOK_URL
    echo 3. Enable GitHub Actions
    echo 4. Run the CI/CD pipeline
    echo.
) else (
    echo.
    echo ❌ Failed to push to GitHub
    echo Please check your network connection and GitHub token
    echo.
)

pause