# Mate

A focus keeper for **Windows** and **Android**. You tell it which apps eat your day and
how many minutes each one gets. When the budget is gone, the app closes. Pornography
sites are blocked outright. Everything stays on your machine — no account, no server,
no telemetry.

```
Mate/
├── windows/                  Python + Tkinter desktop app
├── android/                  Kotlin + Compose phone app (Android Studio project)
└── Mate-android-debug.apk    ready to sideload
```

## Download

### Android

**[⬇ Download Mate-android-debug.apk](https://github.com/Ayaan48/Mate_focus/raw/main/Mate-android-debug.apk)** — 8.8 MB · Android 8.0 or newer

Open that link on your phone and allow "install unknown apps" when it asks — the APK is
signed with a debug key, which is why the warning appears. From a PC instead:

```powershell
adb install -r Mate-android-debug.apk
```

After installing, open Mate → **Setup** and grant both permissions. Nothing is enforced
until you do.

### Windows

No installer — clone the repo and run it:

```powershell
git clone https://github.com/Ayaan48/Mate_focus.git
cd Mate_focus
pip install psutil
windows\Mate.bat
```

Accept the UAC prompt; elevation is what lets Mate edit the hosts file.

---

## Windows

### Run it

```powershell
pip install psutil
```

Then double-click **`windows\Mate.bat`** and accept the UAC prompt. Elevation is what
lets Mate edit the hosts file; without it everything else still works, but the Sites
tab will say it cannot block anything.

To have it start with Windows, already elevated, run **`windows\Install-Autostart.bat`**
once. (`Install-Autostart.bat remove` undoes it.)

### What each tab does

| Tab | What it is for |
|---|---|
| **Today** | Quote of the day, your streak, focus-session timer, and a progress bar per app |
| **Apps** | Pick a running app (or type any `.exe`), give it a daily budget, choose force-close or warn-only |
| **Sites** | Turn adult-site blocking on, optionally force SafeSearch, add your own domains |
| **Progress** | Seven-day bar chart — green is focused time, red is time on tracked apps — plus the strict-mode switch |

### How enforcement works

A watcher samples the foreground window every 2 seconds and banks that time against
whichever tracked app is in front. Background time does not count, so Chrome sitting
minimised costs you nothing.

When a budget runs out, or while a focus session is running:

1. A full-screen motivational curtain appears (`Esc` dismisses it).
2. The window is minimised and a 12-second countdown starts.
3. The process is closed — `terminate()` first, `kill()` if it ignores that.

Apps marked **warn only** skip steps 2 and 3 and just get a reminder once a minute.

Windows' own critical processes (`explorer.exe`, `csrss.exe`, `svchost.exe`, and
friends) are on a protected list and are never touched, whatever you add.

### Site blocking

Mate writes a marked region into `C:\Windows\System32\drivers\etc\hosts` and points
223 adult domains — plus their `www.` and `m.` subdomains, 669 hostnames in all — at
`0.0.0.0`. Everything else already in that file is left exactly as it was, and turning
blocking off removes only Mate's own region. The block is re-applied on every launch,
so editing the file by hand does not survive a restart. DNS is flushed after each change.

**SafeSearch** (optional) additionally pins Google, Bing, DuckDuckGo and YouTube to
their filtered front-ends.

**Add doomscroll preset** drops Instagram, TikTok, Reddit, X and friends into your own
list in one click.

### Strict mode

On by default. Anything that *loosens* a limit — deleting an app, adding minutes,
ending a focus session early, unblocking sites — makes you type a random six-word
phrase and then wait 90 seconds staring at it. Tightening a limit is always instant.
The switch to turn strict mode off is itself behind the same gate.

State lives in `%APPDATA%\Mate\state.json`.

### Surviving deletion

Run **`windows\Install-Guardian.bat`** once, as admin. It:

- copies `guardian.py` and `watchdog.py` to `%ProgramData%\Mate`, **outside** the
  Mate folder, and locks that folder to administrators
- registers **Mate Site Guardian** — a SYSTEM task that every 5 minutes compares the
  hosts file against the exact block it was given and rewrites it if so much as one
  line was commented out
- registers **Mate Watchdog** — a task that relaunches Mate every 2 minutes if the
  process is gone, found via the same named mutex Mate itself holds

So deleting `Desktop\Mate` no longer unblocks anything. The site block keeps healing
until you also run `Install-Guardian.bat remove` from an admin prompt.

Turning site blocking off *inside the app* stands the guardian down properly — it is
friction against impulse, not a trap.

Check it any time with:

```powershell
python %ProgramData%\Mate\guardian.py --status
```

---

## Android

### Install the built APK

`Mate-android-debug.apk` at the root of this folder is signed with a debug key and
installs straight away:

```powershell
adb install -r Mate-android-debug.apk
```

Or copy it to the phone and open it (you will have to allow "install unknown apps").

### Build it yourself

Open the `android/` folder in Android Studio and hit Run, or from a terminal:

```powershell
cd android
.\gradlew assembleDebug
```

Gradle 8.9 / AGP 8.6.1 / Kotlin 2.0.21 / compileSdk 35, minSdk 26 (Android 8.0).

### Two permissions, both needed

Open Mate → **Setup** tab. Both cards turn green once granted.

- **Accessibility access** — how Mate sees which app is in front and what address a
  browser is on. Nothing is enforced without it.
- **Display over other apps** — lets the block screen appear instantly on top of
  whatever you just opened.

### What it does

- **Today** — quote, streak, focus timer, a progress bar for every app you limit.
- **Apps** — every launchable app on the phone with a search box; flip a switch to
  start limiting one, then `-15 / -5 / +5 / +15` to tune the daily budget.
- **Sites** — adult blocking on/off, plus your own domain list and the doomscroll preset.
- **Setup** — permissions, strict mode, and the seven-day chart.

Open a blocked app, or one you have used up, and Mate sends you home and shows the
block screen. In a browser it reads the address bar and does the same thing — matching
223 domains by suffix (so subdomains are covered) and a keyword list that catches
searches like `?q=free+porn`. Ordinary browsing is untouched: the matcher is precise
enough that `beeg.com.example.org` and `notbeeg.com` both pass through.

Strict mode works exactly as it does on Windows — same phrase, same 90 seconds.

State lives in the app's own `SharedPreferences`; usage history is kept for 30 days.

### Surviving deletion

Two layers, both in the **Setup** tab:

**Uninstall guard (device admin).** Android refuses to uninstall an app whose device
admin is active — `adb uninstall` included, which fails with
`DELETE_FAILED_DEVICE_POLICY_MANAGER`. Mate declares an empty `<uses-policies />`: it
asks for *no* powers, so it cannot wipe the phone, lock the screen, change your
password, or read anything. Being active is the entire point.

> **Known limitation — ColorOS / Realme UI / MIUI.** These skins refuse to show the
> device-admin consent screen for sideloaded apps. Tapping *Turn on* flashes the screen
> open and closes it within ~50 ms; the OEM activity
> (`com.oplus.settings.feature.security.OplusDeviceAdminAdd`) finishes itself before it
> draws. This is not something app code can fix — declaring a policy such as
> `force-lock` was tried and changes nothing, and the device-admin *list* screen refuses
> too. The framework itself is happy to accept the admin, so enable it from a PC:
>
> ```powershell
> adb shell dpm set-active-admin com.mate.focus/.MateDeviceAdminReceiver
> ```
>
> Screen interception works without device admin.

**Getting the admin back off.** Two routes, both tested:

- **Setup → Turn off uninstall guard**, behind the usual phrase and 90 second wait.
- From a PC, if the phone UI will not cooperate:
  ```powershell
  adb shell am start -n com.mate.focus/.MainActivity --ez release_admin true
  ```

`adb shell dpm remove-active-admin` does **not** work — the framework rejects it with
"Attempt to remove non-test admin". Only the app can drop its own admin, which is why
those two routes exist.

**Screen interception.** The accessibility service already sees every screen. When
Settings or the package installer shows a screen that both names Mate and offers to
end it, Mate presses Back and puts up the block screen. The rule is two-tier so it
does not misfire:

| Screen text | Intercepted? |
|---|---|
| `Mate` + `Uninstall` / `Deactivate` / `Force stop` | yes |
| `Mate focus guard` + `Turn off` | yes |
| `Automate` / `Teammate` / `Checkmate` + `Uninstall` | no |
| Wi-Fi, Bluetooth, Battery saver + `Turn off` | no |
| A Huawei **Mate** phone's About screen + `Turn off` | no |

Generic words like "turn off" only count next to an exact marker
(`com.mate.focus`, `mate focus guard`, `mate uninstall guard`); unmistakable ones like
"uninstall" count whenever the screen names Mate as a whole word.

**The way out.** Setup → **Let me uninstall**. It costs the strict-mode phrase and the
90-second wait, then stands the guard down for five minutes so you can genuinely
remove the app. That is the honest design: slow, not impossible.

---

## Keeping the two in step

`android/app/src/main/java/com/mate/focus/Blocklist.kt` and `Motivation.kt` are
generated from `windows/mate/blocklist.py` and `windows/mate/motivation.py`. Add a
domain or a quote on the Windows side and regenerate, so the phone and the PC always
agree.

## Honest limits

- Windows blocking is per-process-name. Killing `chrome.exe` closes every Chrome
  window, which is the point, but it will not ask you to save first.
- Android URL blocking covers the browsers listed in `Blocklist.BROWSERS` (Chrome,
  Firefox, Samsung Internet, Brave, Edge, Opera, DuckDuckGo, UC, Vivaldi). A browser
  that is not listed, or one in incognito with a hidden address bar, can slip past.
  A network-level filter (a DNS provider like `family.cloudflare-dns.com` set as your
  Private DNS) is a good belt-and-braces layer.
- Anyone with your PC password or phone unlock can turn Mate off. Strict mode and the
  tamper guards make that slow and deliberate, not impossible — that is the honest goal
  of a self-control tool.
- Escape hatches that remain on Android: Safe Mode (accessibility services do not run
  there on most ROMs) and a factory reset. `adb uninstall` is **not** one of them once
  device admin is active — release the admin first, by either route above. On Windows:
  an admin prompt can delete the scheduled tasks.
- Tapping a permission button in Setup opens a 30 second window during which screen
  interception stands down. That is deliberate: without it, an OEM Settings screen whose
  wording happens to trip the guard could lock you out of granting the permissions Mate
  needs. It is also, honestly, 30 seconds in which you could reach the uninstall screen.
- The guardian only heals the *hosts* block. If you delete the Mate folder, app time
  limits stop being enforced until you restore it — the watchdog cannot start a program
  that is no longer on disk.
