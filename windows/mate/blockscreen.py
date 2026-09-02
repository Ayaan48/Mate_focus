"""Fullscreen overlay shown when a blocked app grabs focus."""
from __future__ import annotations

import tkinter as tk

from . import motivation
from .config import ACCENT, BAD, BG, FG, FG_DIM, WARN


class BlockScreen:
    """A single reusable always-on-top curtain."""

    def __init__(self, root: tk.Tk):
        self.root = root
        self.win: tk.Toplevel | None = None
        self._line = motivation.block_line()
        self._current: str | None = None

    def show(self, app: dict, seconds_left: float, reason: str) -> None:
        if self._current != app["process"]:
            self._current = app["process"]
            self._line = motivation.block_line()
            self._quote = motivation.random_quote()

        if self.win is None or not self.win.winfo_exists():
            self._build()

        title = "Focus session running" if reason == "focus" else "Daily limit reached"
        self.title.config(text=title)
        self.app_label.config(text=app["name"])
        self.message.config(text=self._line)
        text, author = getattr(self, "_quote", motivation.quote_of_the_day())
        self.quote.config(text=f"\u201c{text}\u201d" + (f"\n\u2014 {author}" if author else ""))

        if app.get("hard_block", True):
            self.countdown.config(
                text=f"Closing {app['name']} in {int(seconds_left)}s",
                fg=BAD if seconds_left <= 5 else WARN,
            )
        else:
            self.countdown.config(text="You are past your limit.", fg=WARN)

        try:
            self.win.deiconify()
            self.win.lift()
            self.win.attributes("-topmost", True)
            self.win.focus_force()
        except tk.TclError:
            pass

    def hide(self) -> None:
        self._current = None
        if self.win is not None and self.win.winfo_exists():
            try:
                self.win.withdraw()
            except tk.TclError:
                pass

    def _build(self) -> None:
        win = tk.Toplevel(self.root)
        win.title("Mate")
        win.configure(bg=BG)
        win.attributes("-fullscreen", True)
        win.attributes("-topmost", True)
        win.overrideredirect(False)
        win.protocol("WM_DELETE_WINDOW", lambda: None)

        wrap = tk.Frame(win, bg=BG)
        wrap.place(relx=0.5, rely=0.5, anchor="center")

        self.title = tk.Label(wrap, text="", bg=BG, fg=ACCENT,
                              font=("Segoe UI Semibold", 15))
        self.title.pack(pady=(0, 6))

        self.app_label = tk.Label(wrap, text="", bg=BG, fg=FG,
                                  font=("Segoe UI", 44, "bold"))
        self.app_label.pack()

        self.message = tk.Label(wrap, text="", bg=BG, fg=FG, wraplength=880,
                                justify="center", font=("Segoe UI", 19))
        self.message.pack(pady=(22, 26))

        self.quote = tk.Label(wrap, text="", bg=BG, fg=FG_DIM, wraplength=760,
                              justify="center", font=("Segoe UI", 13, "italic"))
        self.quote.pack(pady=(0, 34))

        self.countdown = tk.Label(wrap, text="", bg=BG, fg=WARN,
                                  font=("Segoe UI Semibold", 16))
        self.countdown.pack()

        tk.Label(wrap, text="Press Esc to dismiss this screen — the app still closes.",
                 bg=BG, fg="#555b6b", font=("Segoe UI", 10)).pack(pady=(30, 0))

        win.bind("<Escape>", lambda _e: self.hide())
        win.withdraw()
        self.win = win
