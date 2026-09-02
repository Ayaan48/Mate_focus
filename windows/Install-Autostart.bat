@echo off
setlocal
cd /d "%~dp0"

net session >nul 2>&1
if %errorlevel% neq 0 (
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

set "TASK=Mate Focus Keeper"

if /i "%~1"=="remove" (
    schtasks /delete /tn "%TASK%" /f
    echo Mate will no longer start automatically.
    pause
    exit /b
)

for /f "delims=" %%P in ('where pythonw 2^>nul') do set "PYW=%%P"
if not defined PYW for /f "delims=" %%P in ('where pyw 2^>nul') do set "PYW=%%P"
if not defined PYW (
    echo Could not find pythonw.exe on PATH.
    pause
    exit /b
)

schtasks /create /tn "%TASK%" /f /sc onlogon /rl highest ^
  /tr "\"%PYW%\" -m mate" >nul

if %errorlevel% equ 0 (
    echo Mate will now start with Windows, already elevated.
    echo Working folder must stay at: %~dp0
    powershell -NoProfile -Command ^
      "$t=Get-ScheduledTask -TaskName '%TASK%'; $t.Actions[0].WorkingDirectory='%~dp0'; Set-ScheduledTask -InputObject $t | Out-Null"
    echo Done.
) else (
    echo Could not create the scheduled task.
)
pause
