@echo off
setlocal enabledelayedexpansion

:: Foundation Domain Deployment Script for Windows
:: Deploys one domain at a time to GitHub

echo.
echo ===================================
echo Foundation Domain Deployment Script
echo ===================================
echo.

:: Configuration
set GITHUB_ORG=Gogidix-property-marketplace
set REPO_NAME=foundation-domain
set GITHUB_TOKEN=ghp_kKxDALOjjsuRCDIfcUCluqvdQvVvD30RSLJk

:: Domains to deploy
set DOMAINS[0]=shared-libraries
set DOMAINS[1]=shared-infrastructure
set DOMAINS[2]=central-configuration
set DOMAINS[3]=ai-services
set DOMAINS[4]=centralized-dashboard

:: Step 1: Create GitHub repository
echo Creating GitHub repository...
curl -s -H "Authorization: token %GITHUB_TOKEN%" ^
     -H "Accept: application/vnd.github.v3+json" ^
     "https://api.github.com/orgs/%GITHUB_ORG%/repos" ^
     -d "{\"name\":\"%REPO_NAME%\",\"description\":\"Gogidix Foundation Domain - Core Infrastructure Services\",\"private\":false}" ^
     >nul 2>&1

if %errorlevel% equ 0 (
    echo ✅ Repository created successfully
) else (
    echo ⚠️  Repository might already exist
)
echo.

:: Initialize git if not already done
if not exist ".git" (
    git init
    git config user.name "Gogidix DevOps"
    git config user.email "devops@gogidix.com"
)

:: Step 2: Deploy each domain
for %%i in (0,1,2,3,4) do (
    call set CURRENT_DOMAIN=%%DOMAINS[%%i]%%
    call echo Deploying domain: !CURRENT_DOMAIN!
    echo --------------------------------

    if exist "!CURRENT_DOMAIN!" (
        :: Create a branch for this domain
        set BRANCH_NAME=deploy-!CURRENT_DOMAIN!-%date:~0,4%%date:~5,2%%date:~8,2%%date:~11,2%%date:~14,2%

        git checkout -b !BRANCH_NAME! 2>nul || git checkout -b !BRANCH_NAME! main

        :: Add all files
        git add .

        :: Commit
        git commit -m "feat: Deploy !CURRENT_DOMAIN! domain

- Add !CURRENT_DOMAIN! services and configurations
- Includes all microservices and infrastructure
- Ready for CI/CD pipeline

🤖 Generated with Gogidix DevOps Automation" 2>nul

        :: Add remote if not exists
        git remote get-url origin >nul 2>&1
        if errorlevel 1 (
            git remote add origin https://github.com/%GITHUB_ORG%/%REPO_NAME%.git
        )

        :: Push to GitHub
        echo Pushing to GitHub...
        git push -u origin !BRANCH_NAME! --force

        if !errorlevel! equ 0 (
            echo ✅ Successfully deployed !CURRENT_DOMAIN!
        ) else (
            echo ❌ Failed to push !CURRENT_DOMAIN!
        )

        :: Return to main branch
        git checkout main 2>nul || git checkout master
    ) else (
        echo ❌ Domain directory not found: !CURRENT_DOMAIN!
    )

    echo.
    timeout /t 2 >nul
)

echo.
echo 🎉 All domains have been processed!
echo Repository: https://github.com/%GITHUB_ORG%/%REPO_NAME%
echo.
echo Next Steps:
echo 1. Review and merge pull requests
echo 2. Add required secrets to GitHub repository
echo 3. Configure CI/CD environment variables
echo 4. Run the CI/CD pipeline
echo.

pause