"""Entry point: python -m mate"""
from __future__ import annotations

import ctypes
import sys
import tkinter as tk
from tkinter import messagebox

from . import hosts
from .config import DATA_DIR
from .store import Store
from .ui import MateApp

MUTEX_NAME = r"Global\MateFocusKeeper"


def already_running() -> bool:
    kernel32 = ctypes.windll.kernel32
    kernel32.CreateMutexW(None, False, MUTEX_NAME)
    return kernel32.GetLastError() == 183  # ERROR_ALREADY_EXISTS


def main() -> int:
    if already_running():
        root = tk.Tk()
        root.withdraw()
        messagebox.showinfo("Mate", "Mate is already running.")
        return 0

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    store = Store()
    store.prune()

    # Re-apply the hosts block on every launch so it survives manual edits.
    if store.get("sites_blocked", True) and hosts.is_admin():
        hosts.apply(True,
                    store.get("extra_domains", []),
                    store.get("allow_domains", []),
                    store.get("safesearch", False))

    root = tk.Tk()
    try:
        root.iconbitmap(default="")
    except tk.TclError:
        pass
    MateApp(root, store)
    root.mainloop()
    return 0


if __name__ == "__main__":
    sys.exit(main())
