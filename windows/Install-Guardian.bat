@echo off
setlocal
cd /d "%~dp0"

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Mate's tamper guard needs administrator rights.
    if /i "%~1"=="remove" (
        powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -ArgumentList 'remove' -Verb RunAs"
    ) else (
        powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    )
    exit /b
)

if /i "%~1"=="remove" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Install-Guardian.ps1" -Remove
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Install-Guardian.ps1"
)

echo.
pause
