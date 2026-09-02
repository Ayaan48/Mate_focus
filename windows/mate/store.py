"""Persistent state for Mate: apps, daily usage, streaks, settings."""
from __future__ import annotations

import copy
import json
import threading
from datetime import date, datetime, timedelta

from .config import DATA_DIR, DEFAULT_STATE, STATE_FILE


def today_key() -> str:
    return date.today().isoformat()


class Store:
    """Thread-safe JSON-backed state. All writes are atomic."""

    def __init__(self, path=STATE_FILE):
        self.path = path
        self._lock = threading.RLock()
        self.data = self._load()

    # ---------- persistence ----------

    def _load(self) -> dict:
        state = copy.deepcopy(DEFAULT_STATE)
        try:
            raw = json.loads(self.path.read_text(encoding="utf-8"))
            if isinstance(raw, dict):
                state.update(raw)
        except (OSError, ValueError):
            pass
        return state

    def save(self) -> None:
        with self._lock:
            DATA_DIR.mkdir(parents=True, exist_ok=True)
            tmp = self.path.with_suffix(".tmp")
            tmp.write_text(json.dumps(self.data, indent=2), encoding="utf-8")
            tmp.replace(self.path)

    # ---------- apps ----------

    @property
    def apps(self) -> list[dict]:
        return self.data.setdefault("apps", [])

    def add_app(self, name: str, process: str, limit_minutes: int, hard_block: bool = True) -> None:
        process = process.strip().lower()
        if not process.endswith(".exe"):
            process += ".exe"
        with self._lock:
            for app in self.apps:
                if app["process"] == process:
                    app.update(name=name, limit_minutes=limit_minutes, hard_block=hard_block)
                    break
            else:
                self.apps.append({
                    "name": name.strip() or process,
                    "process": process,
                    "limit_minutes": int(limit_minutes),
                    "hard_block": bool(hard_block),
                })
            self.save()

    def remove_app(self, process: str) -> None:
        with self._lock:
            self.data["apps"] = [a for a in self.apps if a["process"] != process]
            self.save()

    def find_app(self, process: str) -> dict | None:
        process = process.lower()
        for app in self.apps:
            if app["process"] == process:
                return app
        return None

    # ---------- usage ----------

    def usage_today(self) -> dict[str, float]:
        return self.data.setdefault("usage", {}).setdefault(today_key(), {})

    def add_usage(self, process: str, seconds: float) -> float:
        with self._lock:
            today = self.usage_today()
            today[process] = today.get(process, 0.0) + seconds
            return today[process]

    def seconds_used(self, process: str) -> float:
        return self.usage_today().get(process, 0.0)

    def add_study(self, seconds: float) -> None:
        with self._lock:
            study = self.data.setdefault("study_seconds", {})
            study[today_key()] = study.get(today_key(), 0.0) + seconds

    def study_today(self) -> float:
        return self.data.setdefault("study_seconds", {}).get(today_key(), 0.0)

    def prune(self, keep_days: int = 60) -> None:
        """Drop usage history older than keep_days."""
        cutoff = (date.today() - timedelta(days=keep_days)).isoformat()
        with self._lock:
            for bucket in ("usage", "study_seconds"):
                data = self.data.get(bucket, {})
                for day in [d for d in data if d < cutoff]:
                    del data[day]
            self.save()

    def history(self, days: int = 7) -> list[tuple[str, float, float]]:
        """[(day, distracted_seconds, study_seconds)] oldest first."""
        out = []
        for i in range(days - 1, -1, -1):
            day = (date.today() - timedelta(days=i)).isoformat()
            distracted = sum(self.data.get("usage", {}).get(day, {}).values())
            study = self.data.get("study_seconds", {}).get(day, 0.0)
            out.append((day, distracted, study))
        return out

    # ---------- streak ----------

    def update_streak(self) -> int:
        """A day counts as clean when no tracked app went over its limit."""
        with self._lock:
            streak = self.data.setdefault("streak", {"count": 0, "last_clean_day": None})
            today = today_key()
            if streak.get("last_clean_day") == today:
                return streak["count"]
            # Reaching the limit means you were blocked, so the day is not clean.
            over = any(
                a["limit_minutes"] > 0
                and self.seconds_used(a["process"]) >= a["limit_minutes"] * 60
                for a in self.apps
            )
            if over:
                return streak["count"]
            yesterday = (date.today() - timedelta(days=1)).isoformat()
            streak["count"] = streak["count"] + 1 if streak.get("last_clean_day") == yesterday else 1
            streak["last_clean_day"] = today
            self.save()
            return streak["count"]

    # ---------- focus session ----------

    def start_focus(self, minutes: int, label: str = "Deep work") -> None:
        with self._lock:
            ends = datetime.now() + timedelta(minutes=minutes)
            self.data["focus"] = {"ends_at": ends.isoformat(), "label": label}
            self.save()

    def focus_remaining(self) -> float:
        focus = self.data.get("focus")
        if not focus:
            return 0.0
        try:
            remaining = (datetime.fromisoformat(focus["ends_at"]) - datetime.now()).total_seconds()
        except (ValueError, KeyError):
            return 0.0
        if remaining <= 0:
            self.data["focus"] = None
            return 0.0
        return remaining

    def cancel_focus(self) -> None:
        with self._lock:
            self.data["focus"] = None
            self.save()

    # ---------- settings ----------

    def get(self, key, default=None):
        return self.data.get(key, default)

    def set(self, key, value) -> None:
        with self._lock:
            self.data[key] = value
            self.save()
