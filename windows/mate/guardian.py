"""Standalone hosts-file guardian.

This file is deliberately self-contained: Install-Guardian.bat copies it to
%ProgramData%\\Mate and a SYSTEM scheduled task runs it every few minutes. It
must keep working after the Mate folder is deleted, so it imports nothing from
the mate package and reads everything it needs from guardian.json beside it.

Usage:
    python guardian.py           re-apply the block if it is missing or altered
    python guardian.py --status  print what it would do, change nothing
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

HOSTS_FILE = Path(os.environ.get("SystemRoot", r"C:\Windows")) / "System32" / "drivers" / "etc" / "hosts"
HOSTS_BEGIN = "# >>> MATE BLOCKLIST BEGIN >>>"
HOSTS_END = "# <<< MATE BLOCKLIST END <<<"

PAYLOAD = Path(__file__).resolve().parent / "guardian.json"
LOG = Path(__file__).resolve().parent / "guardian.log"


def log(message: str) -> None:
    try:
        from datetime import datetime
        with LOG.open("a", encoding="utf-8") as handle:
            handle.write(f"{datetime.now().isoformat(timespec='seconds')}  {message}\n")
        if LOG.stat().st_size > 200_000:
            LOG.write_text("", encoding="utf-8")
    except OSError:
        pass


def read_payload() -> dict:
    try:
        return json.loads(PAYLOAD.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def read_hosts() -> str:
    try:
        return HOSTS_FILE.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""


def strip_block(text: str) -> str:
    if HOSTS_BEGIN not in text:
        return text
    head, _, rest = text.partition(HOSTS_BEGIN)
    _, _, tail = rest.partition(HOSTS_END)
    return (head.rstrip("\r\n") + "\n" + tail.lstrip("\r\n")).rstrip("\n") + "\n"


def current_block(text: str) -> str:
    if HOSTS_BEGIN not in text or HOSTS_END not in text:
        return ""
    _, _, rest = text.partition(HOSTS_BEGIN)
    body, _, _ = rest.partition(HOSTS_END)
    return (HOSTS_BEGIN + body + HOSTS_END).strip()


def flush_dns() -> None:
    try:
        subprocess.run(["ipconfig", "/flushdns"], capture_output=True, timeout=15,
                       creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    except Exception:
        pass


def main(argv: list[str]) -> int:
    payload = read_payload()
    if not payload:
        log("no guardian.json - nothing to enforce")
        return 0
    if not payload.get("enabled", False):
        return 0

    wanted = (payload.get("block") or "").strip()
    if not wanted:
        log("guardian.json has no block text")
        return 0

    text = read_hosts()
    if current_block(text) == wanted:
        if "--status" in argv:
            print("Block intact.")
        return 0

    if "--status" in argv:
        print("Block MISSING or altered - would restore it.")
        return 1

    restored = strip_block(text).rstrip("\n") + "\n\n" + wanted + "\n"
    try:
        try:
            HOSTS_FILE.chmod(0o666)
        except OSError:
            pass
        HOSTS_FILE.write_text(restored, encoding="utf-8")
    except OSError as exc:
        log(f"restore failed: {exc}")
        return 1

    flush_dns()
    log(f"restored {wanted.count('0.0.0.0 ')} blocked hostnames")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
