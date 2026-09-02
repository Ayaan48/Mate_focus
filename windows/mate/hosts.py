"""Site blocking through the Windows hosts file.

Mate only ever touches the region between its own markers, so anything
else in the file (corporate entries, dev overrides) is preserved.
"""
from __future__ import annotations

import ctypes
import json
import os
import subprocess
import sys
from pathlib import Path

from .blocklist import ADULT_DOMAINS, SAFESEARCH_MAP
from .config import HOSTS_BEGIN, HOSTS_END, HOSTS_FILE

SINK = "0.0.0.0"

# Where Install-Guardian.bat parks the SYSTEM-side copy. Present only when
# the guardian is installed; Mate keeps its payload in step when it is.
GUARDIAN_DIR = Path(os.environ.get("ProgramData", r"C:\ProgramData")) / "Mate"


def is_admin() -> bool:
    try:
        return bool(ctypes.windll.shell32.IsUserAnAdmin())
    except Exception:
        return False


def relaunch_as_admin() -> bool:
    """Re-run this process elevated. Returns True if the prompt was accepted."""
    if is_admin():
        return True
    params = " ".join(f'"{a}"' for a in sys.argv)
    try:
        rc = ctypes.windll.shell32.ShellExecuteW(
            None, "runas", sys.executable, params, os.getcwd(), 1
        )
        return rc > 32
    except Exception:
        return False


def _expand(domain: str) -> list[str]:
    domain = domain.strip().lower().lstrip(".")
    if not domain or domain.startswith("#"):
        return []
    if domain.startswith("www."):
        domain = domain[4:]
    return [domain, f"www.{domain}", f"m.{domain}"]


def _read_hosts() -> str:
    try:
        return HOSTS_FILE.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""


def _strip_block(text: str) -> str:
    if HOSTS_BEGIN not in text:
        return text
    head, _, rest = text.partition(HOSTS_BEGIN)
    _, _, tail = rest.partition(HOSTS_END)
    return (head.rstrip("\r\n") + "\n" + tail.lstrip("\r\n")).rstrip("\n") + "\n"


def build_block(extra: list[str] | None = None,
                allow: list[str] | None = None,
                safesearch: bool = False) -> str:
    allowed = {d.strip().lower() for d in (allow or [])}
    entries: list[str] = []
    seen: set[str] = set()

    for domain in ADULT_DOMAINS + list(extra or []):
        if domain.strip().lower() in allowed:
            continue
        for host in _expand(domain):
            if host not in seen:
                seen.add(host)
                entries.append(f"{SINK} {host}")

    lines = [HOSTS_BEGIN,
             "# Managed by Mate. Edit through the app, not by hand.",
             *entries]
    if safesearch:
        lines.append("# SafeSearch enforcement")
        lines += [f"{ip} {host}" for host, ip in SAFESEARCH_MAP.items()]
    lines.append(HOSTS_END)
    return "\n".join(lines) + "\n"


def apply(enabled: bool, extra: list[str] | None = None,
          allow: list[str] | None = None, safesearch: bool = False) -> tuple[bool, str]:
    """Write (or remove) Mate's hosts block. Returns (ok, message)."""
    if not is_admin():
        return False, "Administrator rights are required to edit the hosts file."

    text = _strip_block(_read_hosts())
    if enabled:
        text = text.rstrip("\n") + "\n\n" + build_block(extra, allow, safesearch)

    try:
        # Clear the read-only bit some security tools set on hosts.
        try:
            HOSTS_FILE.chmod(0o666)
        except OSError:
            pass
        HOSTS_FILE.write_text(text, encoding="utf-8")
    except PermissionError:
        return False, ("Windows refused the write. Turn off Controlled Folder Access "
                       "for hosts, or add python.exe as an allowed app.")
    except OSError as exc:
        return False, f"Could not write hosts file: {exc}"

    flush_dns()
    sync_guardian(enabled, extra, allow, safesearch)
    count = text.count(f"{SINK} ") if enabled else 0
    return True, (f"Blocking {count} hostnames." if enabled else "Site blocking turned off.")


def guardian_installed() -> bool:
    return (GUARDIAN_DIR / "guardian.py").exists()


def sync_guardian(enabled: bool, extra=None, allow=None, safesearch: bool = False) -> None:
    """Hand the SYSTEM guardian the exact block it should keep restoring."""
    if not guardian_installed():
        return
    payload = {
        "enabled": bool(enabled),
        "block": build_block(extra, allow, safesearch).strip() if enabled else "",
        "launcher": str(Path(__file__).resolve().parent.parent / "Mate.bat"),
    }
    try:
        (GUARDIAN_DIR / "guardian.json").write_text(
            json.dumps(payload, indent=2), encoding="utf-8")
    except OSError:
        pass


def is_active() -> bool:
    return HOSTS_BEGIN in _read_hosts()


def flush_dns() -> None:
    try:
        subprocess.run(["ipconfig", "/flushdns"], capture_output=True,
                       creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0), timeout=15)
    except Exception:
        pass
