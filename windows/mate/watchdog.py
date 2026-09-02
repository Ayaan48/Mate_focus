"""Restart Mate if it is not running.

Also standalone: Install-Guardian.bat copies this next to guardian.py and a
scheduled task runs it in your session every couple of minutes. It finds Mate
through the same named mutex the app itself uses, so it does not care where the
process came from.
"""
from __future__ import annotations

import ctypes
import json
import os
import subprocess
import sys
from pathlib import Path

MUTEX_NAME = r"Global\MateFocusKeeper"
SYNCHRONIZE = 0x00100000

PAYLOAD = Path(__file__).resolve().parent / "guardian.json"


def mate_is_running() -> bool:
    kernel32 = ctypes.windll.kernel32
    handle = kernel32.OpenMutexW(SYNCHRONIZE, False, MUTEX_NAME)
    if handle:
        kernel32.CloseHandle(handle)
        return True
    return False


def launcher() -> Path | None:
    try:
        payload = json.loads(PAYLOAD.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None
    path = payload.get("launcher")
    if not path:
        return None
    candidate = Path(path)
    return candidate if candidate.exists() else None


def main() -> int:
    if mate_is_running():
        return 0

    target = launcher()
    if target is None:
        # The Mate folder is gone. The hosts guardian still holds the line.
        return 0

    try:
        subprocess.Popen(
            [sys.executable.replace("python.exe", "pythonw.exe"), "-m", "mate"],
            cwd=str(target.parent),
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0)
            | getattr(subprocess, "DETACHED_PROCESS", 0),
        )
    except OSError:
        try:
            os.startfile(str(target))
        except OSError:
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
