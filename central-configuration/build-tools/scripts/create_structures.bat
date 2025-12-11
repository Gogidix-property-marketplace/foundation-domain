@echo off
setlocal enabledelayedexpansion

cd /d "C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\central-configuration\backend-services\java-services"

echo Creating directory structures for all services...

rem Create directories for each service
for /d %%d in (*) do (
    echo Processing %%d...
    cd "%%d"

    for /d %%e in (*) do (
        echo Creating structure in %%e

        mkdir "%%e\src\main\java\com\gogidix\controller" 2>nul
        mkdir "%%e\src\main\java\com\gogidix\entity" 2>nul
        mkdir "%%e\src\main\java\com\gogidix\repository" 2>nul
        mkdir "%%e\src\main\java\com\gogidix\dto" 2>nul
        mkdir "%%e\src\main\java\com\gogidix\service" 2>nul
        mkdir "%%e\src\main\resources\db\migration" 2>nul
        mkdir "%%e\src\test\java\com\gogidix\service" 2>nul
    )

    cd ..
)

echo Directory structures created!
pause