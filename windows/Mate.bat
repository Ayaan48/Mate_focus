@echo off
setlocal
cd /d "%~dp0"

rem --- elevate so Mate can manage the hosts file ---
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Mate needs administrator rights to block websites.
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

rem --- find a python launcher ---
where pythonw >nul 2>&1
if %errorlevel% equ 0 (
    start "Mate" pythonw -m mate
    exit /b
)

where py >nul 2>&1
if %errorlevel% equ 0 (
    start "Mate" pyw -m mate
    exit /b
)

echo Python was not found on PATH. Install Python 3.10+ and run: pip install psutil
pause
