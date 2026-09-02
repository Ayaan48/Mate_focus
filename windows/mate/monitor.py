"""Background watcher: measures foreground app time and enforces limits."""
from __future__ import annotations

import ctypes
import threading
import time
from ctypes import wintypes

import psutil

from .config import GRACE_SECONDS, POLL_SECONDS, SOFT_REMINDER_SECONDS
from .store import Store

user32 = ctypes.windll.user32
SW_MINIMIZE = 6

# Never terminate these, whatever the user adds to the list.
PROTECTED = {
    "system", "system idle process", "registry", "smss.exe", "csrss.exe",
    "wininit.exe", "winlogon.exe", "services.exe", "lsass.exe", "svchost.exe",
    "explorer.exe", "dwm.exe", "fontdrvhost.exe", "sihost.exe", "ctfmon.exe",
    "taskhostw.exe", "runtimebroker.exe", "python.exe", "pythonw.exe",
    "conhost.exe", "audiodg.exe", "spoolsv.exe", "lsaiso.exe",
}


def foreground_process() -> tuple[str, int, int]:
    """(process_name_lower, pid, hwnd) of the active window."""
    hwnd = user32.GetForegroundWindow()
    if not hwnd:
        return "", 0, 0
    pid = wintypes.DWORD()
    user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
    if not pid.value:
        return "", 0, hwnd
    try:
        return psutil.Process(pid.value).name().lower(), pid.value, hwnd
    except (psutil.NoSuchProcess, psutil.AccessDenied):
        return "", pid.value, hwnd


def running_processes() -> list[tuple[str, str]]:
    """Distinct (process_name, best-guess friendly name), windowed apps first."""
    seen: dict[str, str] = {}
    for proc in psutil.process_iter(["name", "exe"]):
        name = (proc.info.get("name") or "").lower()
        if not name or name in PROTECTED:
            continue
        seen.setdefault(name, name[:-4].title() if name.endswith(".exe") else name.title())
    return sorted(seen.items())


def kill_process_tree(process_name: str) -> int:
    """Terminate every process with this name. Returns how many were closed."""
    process_name = process_name.lower()
    if process_name in PROTECTED:
        return 0
    victims = []
    for proc in psutil.process_iter(["name"]):
        if (proc.info.get("name") or "").lower() == process_name:
            victims.append(proc)
    for proc in victims:
        try:
            proc.terminate()
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass
    gone, alive = psutil.wait_procs(victims, timeout=3)
    for proc in alive:
        try:
            proc.kill()
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass
    return len(victims)


class Monitor(threading.Thread):
    """Samples the foreground window and enforces per-app daily limits.

    on_block(app, seconds_left) is called while a blocked app holds focus;
    on_tick() fires after every sample so the UI can refresh.
    """

    daemon = True

    def __init__(self, store: Store, on_block=None, on_tick=None, on_release=None):
        super().__init__(name="mate-monitor")
        self.store = store
        self.on_block = on_block
        self.on_tick = on_tick
        self.on_release = on_release
        self._stop = threading.Event()
        self._grace: dict[str, float] = {}     # process -> monotonic kill deadline
        self._nagged: dict[str, float] = {}    # process -> last soft reminder
        self._blocked_now: str | None = None

    def stop(self) -> None:
        self._stop.set()

    def is_blocked(self, app: dict) -> tuple[bool, str]:
        """Should this app be denied right now, and why."""
        if self.store.focus_remaining() > 0:
            return True, "focus"
        limit = app.get("limit_minutes", 0) * 60
        if limit and self.store.seconds_used(app["process"]) >= limit:
            return True, "limit"
        return False, ""

    def run(self) -> None:
        last = time.monotonic()
        while not self._stop.wait(POLL_SECONDS):
            now = time.monotonic()
            elapsed = min(now - last, POLL_SECONDS * 3)
            last = now
            try:
                self._sample(elapsed)
            except Exception:
                pass
            if self.on_tick:
                try:
                    self.on_tick()
                except Exception:
                    pass

    def _sample(self, elapsed: float) -> None:
        name, _pid, hwnd = foreground_process()
        app = self.store.find_app(name) if name else None

        if self.store.focus_remaining() > 0:
            self.store.add_study(elapsed)

        if not app:
            if self._blocked_now:
                self._blocked_now = None
                if self.on_release:
                    self.on_release()
            return

        blocked, reason = self.is_blocked(app)
        if not blocked:
            self.store.add_usage(app["process"], elapsed)
            self._grace.pop(app["process"], None)
            if self._blocked_now == app["process"]:
                self._blocked_now = None
                if self.on_release:
                    self.on_release()
            return

        # Blocked: warn, then close.
        self._blocked_now = app["process"]
        deadline = self._grace.get(app["process"])
        if deadline is None:
            deadline = time.monotonic() + GRACE_SECONDS
            self._grace[app["process"]] = deadline

        hard = app.get("hard_block", True)
        left = max(0.0, deadline - time.monotonic())

        if hard:
            if self.on_block:
                self.on_block(app, left, reason)
            if hwnd:
                try:
                    user32.ShowWindow(hwnd, SW_MINIMIZE)
                except Exception:
                    pass
            if left <= 0:
                kill_process_tree(app["process"])
                self._grace[app["process"]] = time.monotonic() + GRACE_SECONDS
            return

        # Warn-only apps get a reminder now and then, not a permanent curtain.
        last = self._nagged.get(app["process"], 0.0)
        if time.monotonic() - last >= SOFT_REMINDER_SECONDS:
            self._nagged[app["process"]] = time.monotonic()
            if self.on_block:
                self.on_block(app, 0, reason)
