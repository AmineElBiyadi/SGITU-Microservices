@echo off
setlocal EnableDelayedExpansion

set OUTPUT=full_project_dump.txt

if exist %OUTPUT% del %OUTPUT%

echo =============================================== >> %OUTPUT%
echo        FULL PROJECT DUMP - SERVICE PAIEMENT
echo =============================================== >> %OUTPUT%
echo. >> %OUTPUT%

for /R %%F in (*) do (

    set "filepath=%%F"
    set "skip=false"

    echo !filepath! | findstr /I "\.git\\" >nul && set skip=true
    echo !filepath! | findstr /I "\target\\" >nul && set skip=true
    echo !filepath! | findstr /I "\node_modules\\" >nul && set skip=true

    if "!skip!"=="false" (
        if not "%%~nxF"=="%OUTPUT%" (
            echo =============================================== >> %OUTPUT%
            echo FILE: %%F >> %OUTPUT%
            echo =============================================== >> %OUTPUT%
            type "%%F" >> %OUTPUT% 2>nul
            echo. >> %OUTPUT%
            echo. >> %OUTPUT%
        )
    )
)

echo Dump completed in %OUTPUT%
pause