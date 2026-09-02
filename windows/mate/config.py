"""Paths, constants and defaults for Mate."""
from __future__ import annotations

import os
from pathlib import Path

APP_NAME = "Mate"

DATA_DIR = Path(os.environ.get("APPDATA", Path.home())) / APP_NAME
STATE_FILE = DATA_DIR / "state.json"
LOG_FILE = DATA_DIR / "mate.log"

HOSTS_FILE = Path(os.environ.get("SystemRoot", r"C:\Windows")) / "System32" / "drivers" / "etc" / "hosts"
HOSTS_BEGIN = "# >>> MATE BLOCKLIST BEGIN >>>"
HOSTS_END = "# <<< MATE BLOCKLIST END <<<"

# How often the watcher samples the foreground window (seconds).
POLL_SECONDS = 2.0

# Grace period after a limit is hit before the app is force-closed.
GRACE_SECONDS = 12

# Apps set to 'warn only' get reminded this often instead of continuously.
SOFT_REMINDER_SECONDS = 60

# Strict mode: unlocking protection needs this phrase typed + this delay.
UNLOCK_DELAY_SECONDS = 90
UNLOCK_PHRASE_WORDS = 6

# Colours (dark, calm, high contrast).
BG = "#12141a"
BG_CARD = "#1a1d26"
BG_INPUT = "#232734"
FG = "#e8eaf0"
FG_DIM = "#8b91a3"
ACCENT = "#5b8cff"
GOOD = "#3ecf8e"
WARN = "#f5a623"
BAD = "#ff5d5d"

DEFAULT_STATE = {
    "version": 1,
    "apps": [],          # {name, process, limit_minutes, hard_block}
    "usage": {},         # {"YYYY-MM-DD": {process: seconds}}
    "sites_blocked": True,
    "extra_domains": [],
    "allow_domains": [],
    "strict_mode": True,
    "focus": None,       # {"ends_at": iso, "label": str}
    "streak": {"count": 0, "last_clean_day": None},
    "daily_goal_minutes": 120,
    "study_seconds": {},  # {"YYYY-MM-DD": seconds}
}
