<#
    Installs Mate's tamper guard.

      * copies guardian.py + watchdog.py to %ProgramData%\Mate, outside the
        Mate folder, so deleting Mate does not take the site block with it
      * registers a SYSTEM task that restores the hosts block every 5 minutes
      * registers a user task that relaunches Mate every 2 minutes if it died
      * locks the payload folder so a non-elevated delete is refused

    Run Install-Guardian.bat (which elevates), not this file directly.
    Pass -Remove to undo everything.
#>
param([switch]$Remove)

$ErrorActionPreference = "Stop"

$GuardTask = "Mate Site Guardian"
$WatchTask = "Mate Watchdog"
$Payload   = Join-Path $env:ProgramData "Mate"
$Source    = Join-Path $PSScriptRoot "mate"

function Remove-Guard {
    foreach ($name in @($GuardTask, $WatchTask)) {
        try {
            Unregister-ScheduledTask -TaskName $name -Confirm:$false -ErrorAction Stop
            Write-Host "removed task: $name"
        } catch {
            Write-Host "task not present: $name"
        }
    }
    if (Test-Path $Payload) {
        & icacls $Payload /reset /T /C | Out-Null
        Remove-Item $Payload -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "removed $Payload"
    }
    Write-Host ""
    Write-Host "Tamper guard removed. The hosts block is no longer self-healing."
}

function Find-Pythonw {
    $cmd = Get-Command pythonw.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $cmd = Get-Command python.exe -ErrorAction SilentlyContinue
    if ($cmd) { return ($cmd.Source -replace "python\.exe$", "pythonw.exe") }
    return $null
}

if ($Remove) { Remove-Guard; return }

$pyw = Find-Pythonw
if (-not $pyw -or -not (Test-Path $pyw)) {
    Write-Host "Could not find pythonw.exe on PATH. Install Python 3.10+ first."
    return
}

# ---- payload -------------------------------------------------------------

New-Item -ItemType Directory -Path $Payload -Force | Out-Null
Copy-Item (Join-Path $Source "guardian.py") $Payload -Force
Copy-Item (Join-Path $Source "watchdog.py") $Payload -Force
Write-Host "payload installed at $Payload"

# Ask Mate to write the current blocklist into the payload folder.
$sync = @"
import sys
sys.path.insert(0, r'$PSScriptRoot')
from mate import hosts
from mate.store import Store
s = Store()
hosts.sync_guardian(s.get('sites_blocked', True), s.get('extra_domains', []),
                    s.get('allow_domains', []), s.get('safesearch', False))
print('guardian.json written')
"@
$sync | & ($pyw -replace "pythonw\.exe$", "python.exe") -

# ---- tasks ---------------------------------------------------------------

$every5 = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) `
    -RepetitionInterval (New-TimeSpan -Minutes 5)
$guardAction = New-ScheduledTaskAction -Execute $pyw `
    -Argument ('"{0}"' -f (Join-Path $Payload "guardian.py"))
$guardPrincipal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries -StartWhenAvailable -MultipleInstances IgnoreNew

Register-ScheduledTask -TaskName $GuardTask -Action $guardAction `
    -Trigger $every5 -Principal $guardPrincipal -Settings $settings -Force | Out-Null
Write-Host "registered task: $GuardTask  (SYSTEM, every 5 min)"

$every2 = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) `
    -RepetitionInterval (New-TimeSpan -Minutes 2)
$watchAction = New-ScheduledTaskAction -Execute $pyw `
    -Argument ('"{0}"' -f (Join-Path $Payload "watchdog.py"))
$watchPrincipal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" `
    -LogonType Interactive -RunLevel Limited

Register-ScheduledTask -TaskName $WatchTask -Action $watchAction `
    -Trigger $every2 -Principal $watchPrincipal -Settings $settings -Force | Out-Null
Write-Host "registered task: $WatchTask  ($env:USERNAME, every 2 min)"

# ---- lock the payload ----------------------------------------------------

& icacls $Payload /inheritance:r /C | Out-Null
& icacls $Payload /grant "SYSTEM:(OI)(CI)F" "Administrators:(OI)(CI)F" /C | Out-Null
& icacls $Payload /grant "Users:(OI)(CI)RX" /C | Out-Null
Write-Host "payload folder locked to administrators"

Write-Host ""
Write-Host "Done. The porn block now survives deleting the Mate folder."
Write-Host "To undo: run Install-Guardian.bat remove (needs admin)."
