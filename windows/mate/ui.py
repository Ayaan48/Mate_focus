"""Mate main window."""
from __future__ import annotations

import tkinter as tk
from tkinter import messagebox, ttk

from . import hosts, motivation
from .blocklist import DOOMSCROLL_PRESET
from .blockscreen import BlockScreen
from .config import (ACCENT, BAD, BG, BG_CARD, BG_INPUT, FG, FG_DIM, GOOD,
                     UNLOCK_DELAY_SECONDS, UNLOCK_PHRASE_WORDS, WARN)
from .monitor import Monitor, running_processes
from .store import Store

DASH = "—"
MINUS = "−"


def fmt(seconds: float) -> str:
    seconds = int(max(0, seconds))
    h, m = divmod(seconds // 60, 60)
    return f"{h}h {m:02d}m" if h else f"{m}m {seconds % 60:02d}s"


def fmt_short(seconds: float) -> str:
    seconds = int(max(0, seconds))
    h, m = divmod(seconds // 60, 60)
    return f"{h}h {m:02d}m" if h else f"{m}m"


class UnlockDialog(tk.Toplevel):
    """Strict-mode friction: type a random phrase, then wait out a cooldown."""

    def __init__(self, parent, action: str):
        super().__init__(parent)
        self.result = False
        self.phrase = motivation.unlock_phrase(UNLOCK_PHRASE_WORDS)
        self.remaining = UNLOCK_DELAY_SECONDS

        self.title("Are you sure?")
        self.configure(bg=BG_CARD, padx=26, pady=22)
        self.resizable(False, False)
        self.transient(parent)
        self.grab_set()

        tk.Label(self, text=action, bg=BG_CARD, fg=FG,
                 font=("Segoe UI Semibold", 13), wraplength=430,
                 justify="left").pack(anchor="w")
        tk.Label(self, text="Strict mode is on. Type this phrase exactly, then wait.",
                 bg=BG_CARD, fg=FG_DIM, wraplength=430,
                 justify="left").pack(anchor="w", pady=(6, 14))

        tk.Label(self, text=self.phrase, bg=BG, fg=ACCENT, padx=12, pady=10,
                 font=("Consolas", 13)).pack(fill="x")

        self.entry = tk.Entry(self, bg=BG_INPUT, fg=FG, insertbackground=FG,
                              relief="flat", font=("Consolas", 12))
        self.entry.pack(fill="x", pady=(10, 6), ipady=6)
        self.entry.bind("<KeyRelease>", lambda _e: self._refresh())

        self.status = tk.Label(self, text="", bg=BG_CARD, fg=FG_DIM,
                               font=("Segoe UI", 10))
        self.status.pack(anchor="w", pady=(0, 14))

        row = tk.Frame(self, bg=BG_CARD)
        row.pack(fill="x")
        self.ok = tk.Button(row, text="Unlock", state="disabled", relief="flat",
                            bg=BG_INPUT, fg=FG_DIM, activebackground=BAD,
                            activeforeground="#ffffff", padx=18, pady=7,
                            command=self._accept, cursor="hand2")
        self.ok.pack(side="right")
        tk.Button(row, text="Keep it locked", relief="flat", bg=GOOD, fg="#08130d",
                  activebackground=GOOD, padx=18, pady=7, cursor="hand2",
                  font=("Segoe UI Semibold", 9),
                  command=self.destroy).pack(side="right", padx=(0, 8))

        self.entry.focus_set()
        self._countdown()
        self.protocol("WM_DELETE_WINDOW", self.destroy)
        parent.wait_window(self)

    def _countdown(self):
        if not self.winfo_exists():
            return
        self.remaining = max(0, self.remaining - 1)
        self._refresh()
        if self.remaining:
            self.after(1000, self._countdown)

    def _refresh(self):
        typed = self.entry.get().strip() == self.phrase
        if not typed:
            self.status.config(text="Phrase does not match yet.", fg=FG_DIM)
        elif self.remaining:
            self.status.config(text=f"Phrase matches. Cooling down: {self.remaining}s left.",
                               fg=WARN)
        else:
            self.status.config(text="You can unlock now, or you can walk away.", fg=BAD)
        ready = typed and not self.remaining
        self.ok.config(state="normal" if ready else "disabled",
                       bg=BAD if ready else BG_INPUT,
                       fg="#ffffff" if ready else FG_DIM)

    def _accept(self):
        self.result = True
        self.destroy()


class MateApp:
    def __init__(self, root: tk.Tk, store: Store):
        self.root = root
        self.store = store
        self.blockscreen = BlockScreen(root)

        root.title("Mate - focus keeper")
        root.geometry("1000x720")
        root.minsize(900, 640)
        root.configure(bg=BG)

        self._style()
        self._header()

        self.tabs = ttk.Notebook(root, style="Mate.TNotebook")
        self.tabs.pack(fill="both", expand=True, padx=18, pady=(6, 16))
        self._tab_today()
        self._tab_apps()
        self._tab_sites()
        self._tab_progress()

        self.monitor = Monitor(store,
                               on_block=self._on_block,
                               on_tick=self._on_tick,
                               on_release=self._on_release)
        self.monitor.start()

        self.refresh()
        root.protocol("WM_DELETE_WINDOW", self.on_close)

    # ---------- chrome ----------

    def _style(self):
        style = ttk.Style()
        try:
            style.theme_use("clam")
        except tk.TclError:
            pass
        style.configure("Mate.TNotebook", background=BG, borderwidth=0, tabmargins=0)
        style.configure("Mate.TNotebook.Tab", background=BG, foreground=FG_DIM,
                        padding=(20, 10), font=("Segoe UI Semibold", 10), borderwidth=0)
        style.map("Mate.TNotebook.Tab",
                  background=[("selected", BG_CARD)], foreground=[("selected", FG)])
        for name, colour in (("Good", GOOD), ("Warn", WARN), ("Bad", BAD)):
            style.configure(f"{name}.Horizontal.TProgressbar", troughcolor=BG_INPUT,
                            background=colour, borderwidth=0, thickness=8,
                            lightcolor=colour, darkcolor=colour)

    def _header(self):
        bar = tk.Frame(self.root, bg=BG)
        bar.pack(fill="x", padx=18, pady=(16, 0))
        tk.Label(bar, text="Mate", bg=BG, fg=FG,
                 font=("Segoe UI", 22, "bold")).pack(side="left")
        self.header_status = tk.Label(bar, text="", bg=BG, fg=FG_DIM,
                                      font=("Segoe UI", 10))
        self.header_status.pack(side="right")

    def _button(self, parent, text, command, kind="accent"):
        colours = {"accent": (ACCENT, "#0a1020"), "good": (GOOD, "#08130d"),
                   "bad": (BAD, "#1a0707"), "flat": (BG_INPUT, FG)}
        bg, fg = colours[kind]
        return tk.Button(parent, text=text, command=command, relief="flat",
                         bg=bg, fg=fg, activebackground=bg, activeforeground=fg,
                         font=("Segoe UI Semibold", 10), padx=16, pady=8,
                         cursor="hand2", borderwidth=0)

    # ---------- tab: today ----------

    def _tab_today(self):
        page = tk.Frame(self.tabs, bg=BG_CARD)
        self.tabs.add(page, text="  Today  ")

        quote_box = tk.Frame(page, bg=BG, padx=18, pady=16)
        quote_box.pack(fill="x", padx=22, pady=(20, 0))
        text, author = motivation.quote_of_the_day()
        tk.Label(quote_box, text=f"“{text}”", bg=BG, fg=FG, wraplength=880,
                 justify="left", font=("Segoe UI", 13, "italic")).pack(anchor="w")
        if author:
            tk.Label(quote_box, text=f"{DASH} {author}", bg=BG, fg=FG_DIM,
                     font=("Segoe UI", 10)).pack(anchor="w", pady=(6, 0))

        stats = tk.Frame(page, bg=BG_CARD)
        stats.pack(fill="x", padx=22, pady=(16, 0))
        self.stat_streak = self._stat(stats, "Clean-day streak", "0 days")
        self.stat_focus = self._stat(stats, "Focused today", "0m")
        self.stat_distract = self._stat(stats, "On tracked apps", "0m")

        session = tk.Frame(page, bg=BG_CARD)
        session.pack(fill="x", padx=22, pady=(18, 0))
        tk.Label(session, text="Focus session", bg=BG_CARD, fg=FG,
                 font=("Segoe UI Semibold", 12)).pack(anchor="w")
        tk.Label(session, text="Every tracked app is locked while this runs.",
                 bg=BG_CARD, fg=FG_DIM, font=("Segoe UI", 9)).pack(anchor="w", pady=(2, 10))

        row = tk.Frame(session, bg=BG_CARD)
        row.pack(fill="x")
        for minutes in (25, 45, 60, 90):
            self._button(row, f"{minutes} min", lambda m=minutes: self.start_focus(m),
                         "flat").pack(side="left", padx=(0, 8))
        self.focus_label = tk.Label(row, text="", bg=BG_CARD, fg=GOOD,
                                    font=("Segoe UI Semibold", 12))
        self.focus_label.pack(side="left", padx=(16, 0))
        self.focus_stop = self._button(row, "End session", self.stop_focus, "bad")

        tk.Label(page, text="Today's budget", bg=BG_CARD, fg=FG,
                 font=("Segoe UI Semibold", 12)).pack(anchor="w", padx=22, pady=(22, 8))

        self.today_list = tk.Frame(page, bg=BG_CARD)
        self.today_list.pack(fill="both", expand=True, padx=22, pady=(0, 20))

    def _stat(self, parent, title, value):
        box = tk.Frame(parent, bg=BG, padx=18, pady=14)
        box.pack(side="left", expand=True, fill="x", padx=(0, 10))
        tk.Label(box, text=title, bg=BG, fg=FG_DIM, font=("Segoe UI", 9)).pack(anchor="w")
        label = tk.Label(box, text=value, bg=BG, fg=FG, font=("Segoe UI", 20, "bold"))
        label.pack(anchor="w")
        return label

    # ---------- tab: apps ----------

    def _tab_apps(self):
        page = tk.Frame(self.tabs, bg=BG_CARD)
        self.tabs.add(page, text="  Apps  ")

        form = tk.Frame(page, bg=BG, padx=20, pady=18)
        form.pack(fill="x", padx=22, pady=(20, 0))
        tk.Label(form, text="Add an app to limit", bg=BG, fg=FG,
                 font=("Segoe UI Semibold", 12)).pack(anchor="w", pady=(0, 12))

        grid = tk.Frame(form, bg=BG)
        grid.pack(fill="x")

        tk.Label(grid, text="Running app", bg=BG, fg=FG_DIM,
                 font=("Segoe UI", 9)).grid(row=0, column=0, sticky="w")
        self.proc_var = tk.StringVar()
        self.proc_combo = ttk.Combobox(grid, textvariable=self.proc_var, width=28)
        self.proc_combo.grid(row=1, column=0, sticky="we", padx=(0, 10), ipady=3)
        self.proc_combo.bind("<<ComboboxSelected>>", self._on_pick_process)

        tk.Label(grid, text="Label", bg=BG, fg=FG_DIM,
                 font=("Segoe UI", 9)).grid(row=0, column=1, sticky="w")
        self.name_var = tk.StringVar()
        tk.Entry(grid, textvariable=self.name_var, bg=BG_INPUT, fg=FG, relief="flat",
                 insertbackground=FG).grid(row=1, column=1, sticky="we", padx=(0, 10), ipady=4)

        tk.Label(grid, text="Minutes / day", bg=BG, fg=FG_DIM,
                 font=("Segoe UI", 9)).grid(row=0, column=2, sticky="w")
        self.limit_var = tk.StringVar(value="30")
        tk.Entry(grid, textvariable=self.limit_var, bg=BG_INPUT, fg=FG, relief="flat",
                 insertbackground=FG, width=8).grid(row=1, column=2, sticky="we",
                                                    padx=(0, 10), ipady=4)

        self.hard_var = tk.BooleanVar(value=True)
        tk.Checkbutton(grid, text="Force close", variable=self.hard_var, bg=BG, fg=FG_DIM,
                       selectcolor=BG_INPUT, activebackground=BG, activeforeground=FG,
                       relief="flat", borderwidth=0, font=("Segoe UI", 9),
                       highlightthickness=0).grid(row=1, column=3, padx=(0, 10))

        self._button(grid, "Add", self.add_app).grid(row=1, column=4)
        grid.columnconfigure(0, weight=2)
        grid.columnconfigure(1, weight=2)

        actions = tk.Frame(form, bg=BG)
        actions.pack(fill="x", pady=(12, 0))
        self._button(actions, "Refresh running apps", self.refresh_processes,
                     "flat").pack(side="left")
        tk.Label(actions, text="Not running right now? Type its .exe name yourself.",
                 bg=BG, fg=FG_DIM, font=("Segoe UI", 9)).pack(side="left", padx=12)

        tk.Label(page, text="Tracked apps", bg=BG_CARD, fg=FG,
                 font=("Segoe UI Semibold", 12)).pack(anchor="w", padx=22, pady=(20, 8))
        self.apps_list = tk.Frame(page, bg=BG_CARD)
        self.apps_list.pack(fill="both", expand=True, padx=22, pady=(0, 20))

        self.refresh_processes()

    # ---------- tab: sites ----------

    def _tab_sites(self):
        page = tk.Frame(self.tabs, bg=BG_CARD)
        self.tabs.add(page, text="  Sites  ")

        card = tk.Frame(page, bg=BG, padx=20, pady=18)
        card.pack(fill="x", padx=22, pady=(20, 0))

        tk.Label(card, text="Adult site blocking", bg=BG, fg=FG,
                 font=("Segoe UI Semibold", 12)).pack(anchor="w")
        tk.Label(card, text="Sends a large list of pornography domains to nowhere, "
                            "system-wide, in every browser on this PC.",
                 bg=BG, fg=FG_DIM, wraplength=820, justify="left",
                 font=("Segoe UI", 9)).pack(anchor="w", pady=(4, 14))

        row = tk.Frame(card, bg=BG)
        row.pack(fill="x")
        self.sites_state = tk.Label(row, text="", bg=BG, fg=FG_DIM,
                                    font=("Segoe UI Semibold", 11))
        self.sites_state.pack(side="left")
        self.sites_btn = self._button(row, "Turn on", self.toggle_sites, "good")
        self.sites_btn.pack(side="right")

        self.safesearch_var = tk.BooleanVar(value=self.store.get("safesearch", False))
        tk.Checkbutton(card,
                       text="Also force SafeSearch on Google, Bing, DuckDuckGo and YouTube",
                       variable=self.safesearch_var, command=self.apply_sites,
                       bg=BG, fg=FG_DIM, selectcolor=BG_INPUT, activebackground=BG,
                       activeforeground=FG, relief="flat", borderwidth=0,
                       highlightthickness=0, font=("Segoe UI", 9)).pack(anchor="w",
                                                                        pady=(14, 0))

        extra = tk.Frame(page, bg=BG, padx=20, pady=18)
        extra.pack(fill="both", expand=True, padx=22, pady=(16, 20))
        tk.Label(extra, text="Your own blocked domains", bg=BG, fg=FG,
                 font=("Segoe UI Semibold", 12)).pack(anchor="w")
        tk.Label(extra, text="One per line. The www. and m. subdomains are added for you.",
                 bg=BG, fg=FG_DIM, font=("Segoe UI", 9)).pack(anchor="w", pady=(4, 10))

        self.extra_text = tk.Text(extra, bg=BG_INPUT, fg=FG, relief="flat", height=8,
                                  insertbackground=FG, font=("Consolas", 10),
                                  padx=10, pady=8)
        self.extra_text.pack(fill="both", expand=True)
        self.extra_text.insert("1.0", "\n".join(self.store.get("extra_domains", [])))

        btns = tk.Frame(extra, bg=BG)
        btns.pack(fill="x", pady=(12, 0))
        self._button(btns, "Save and apply", self.save_extra, "accent").pack(side="left")
        self._button(btns, "Add doomscroll preset", self.add_preset,
                     "flat").pack(side="left", padx=8)

        self.admin_note = tk.Label(extra, text="", bg=BG, fg=WARN,
                                   wraplength=820, justify="left", font=("Segoe UI", 9))
        self.admin_note.pack(anchor="w", pady=(12, 0))

    # ---------- tab: progress ----------

    def _tab_progress(self):
        page = tk.Frame(self.tabs, bg=BG_CARD)
        self.tabs.add(page, text="  Progress  ")

        tk.Label(page, text="Last 7 days", bg=BG_CARD, fg=FG,
                 font=("Segoe UI Semibold", 12)).pack(anchor="w", padx=22, pady=(20, 4))
        tk.Label(page, text="Green is focused time. Red is time spent on tracked apps.",
                 bg=BG_CARD, fg=FG_DIM, font=("Segoe UI", 9)).pack(anchor="w", padx=22)

        self.chart = tk.Canvas(page, bg=BG, highlightthickness=0, height=300)
        self.chart.pack(fill="both", expand=True, padx=22, pady=(14, 10))
        self.chart.bind("<Configure>", lambda _e: self.draw_chart())

        strict = tk.Frame(page, bg=BG, padx=20, pady=16)
        strict.pack(fill="x", padx=22, pady=(0, 20))
        self.strict_var = tk.BooleanVar(value=self.store.get("strict_mode", True))
        tk.Checkbutton(strict,
                       text="Strict mode - loosening any limit needs a typed phrase "
                            "and a 90 second wait",
                       variable=self.strict_var, command=self.toggle_strict,
                       bg=BG, fg=FG, selectcolor=BG_INPUT, activebackground=BG,
                       activeforeground=FG, relief="flat", borderwidth=0,
                       highlightthickness=0, font=("Segoe UI", 10)).pack(anchor="w")

    # ---------- actions ----------

    def guard(self, action: str) -> bool:
        """Confirm a loosening change; strict mode makes it deliberately slow."""
        if not self.store.get("strict_mode", True):
            return messagebox.askyesno("Mate", action, parent=self.root)
        return UnlockDialog(self.root, action).result

    def refresh_processes(self):
        procs = running_processes()
        self.proc_combo["values"] = [f"{name}  {DASH}  {label}" for name, label in procs]

    def _on_pick_process(self, _event=None):
        raw = self.proc_var.get()
        if DASH in raw:
            proc, label = [p.strip() for p in raw.split(DASH, 1)]
            self.proc_var.set(proc)
            if not self.name_var.get():
                self.name_var.set(label)

    def add_app(self):
        proc = self.proc_var.get().split(DASH)[0].strip().lower()
        if not proc:
            messagebox.showwarning("Mate", "Pick or type a process name first.",
                                   parent=self.root)
            return
        try:
            limit = max(0, int(self.limit_var.get()))
        except ValueError:
            messagebox.showwarning("Mate", "Minutes must be a whole number.",
                                   parent=self.root)
            return
        name = self.name_var.get().strip() or proc.replace(".exe", "").title()
        self.store.add_app(name, proc, limit, self.hard_var.get())
        self.proc_var.set("")
        self.name_var.set("")
        self.refresh()

    def edit_limit(self, app: dict, delta: int):
        if delta > 0 and not self.guard(
                f"Give yourself {delta} more minutes on {app['name']} today?"):
            return
        app["limit_minutes"] = max(0, app["limit_minutes"] + delta)
        self.store.save()
        self.refresh()

    def remove_app(self, app: dict):
        if not self.guard(f"Stop limiting {app['name']} altogether?"):
            return
        self.store.remove_app(app["process"])
        self.refresh()

    def start_focus(self, minutes: int):
        self.store.start_focus(minutes)
        self.refresh()

    def stop_focus(self):
        if self.store.focus_remaining() <= 0:
            return
        if not self.guard("End the focus session early?"):
            return
        self.store.cancel_focus()
        self.blockscreen.hide()
        self.refresh()

    def toggle_strict(self):
        if not self.strict_var.get() and self.store.get("strict_mode", True):
            if not UnlockDialog(self.root, "Turn strict mode off?").result:
                self.strict_var.set(True)
                return
        self.store.set("strict_mode", self.strict_var.get())

    def toggle_sites(self):
        on = hosts.is_active()
        if on and not self.guard("Unblock adult sites on this computer?"):
            return
        self.store.set("sites_blocked", not on)
        self.apply_sites()

    def save_extra(self):
        domains = [d.strip().lower() for d in self.extra_text.get("1.0", "end").splitlines()]
        domains = [d for d in domains if d and not d.startswith("#")]
        previous = set(self.store.get("extra_domains", []))
        if previous - set(domains) and not self.guard(
                "Remove domains from your own block list?"):
            self.extra_text.delete("1.0", "end")
            self.extra_text.insert("1.0", "\n".join(sorted(previous)))
            return
        self.store.set("extra_domains", domains)
        self.apply_sites()

    def add_preset(self):
        current = [d.strip() for d in self.extra_text.get("1.0", "end").splitlines()
                   if d.strip()]
        merged = sorted(set(current) | set(DOOMSCROLL_PRESET))
        self.extra_text.delete("1.0", "end")
        self.extra_text.insert("1.0", "\n".join(merged))
        self.save_extra()

    def apply_sites(self):
        self.store.set("safesearch", self.safesearch_var.get())
        ok, message = hosts.apply(
            enabled=self.store.get("sites_blocked", True),
            extra=self.store.get("extra_domains", []),
            allow=self.store.get("allow_domains", []),
            safesearch=self.safesearch_var.get(),
        )
        self.refresh_sites()
        self.admin_note.config(text=message, fg=GOOD if ok else WARN)

    # ---------- rendering ----------

    def refresh(self):
        self.refresh_today()
        self.refresh_apps()
        self.refresh_sites()
        self.draw_chart()

        streak = self.store.data.get("streak", {}).get("count", 0)
        self.stat_streak.config(text=f"{streak} day" + ("s" if streak != 1 else ""))
        self.stat_focus.config(text=fmt_short(self.store.study_today()))
        self.stat_distract.config(text=fmt_short(sum(self.store.usage_today().values())))

        admin = "administrator" if hosts.is_admin() else "no admin rights"
        blocked = "site block on" if hosts.is_active() else "site block off"
        self.header_status.config(text=f"{blocked}  |  {admin}")

    def refresh_today(self):
        remaining = self.store.focus_remaining()
        if remaining > 0:
            self.focus_label.config(text=f"{fmt(remaining)} left")
            self.focus_stop.pack(side="left", padx=(12, 0))
        else:
            self.focus_label.config(text="")
            self.focus_stop.pack_forget()

        for child in self.today_list.winfo_children():
            child.destroy()

        if not self.store.apps:
            tk.Label(self.today_list, text="No apps tracked yet. Add one in the Apps tab.",
                     bg=BG_CARD, fg=FG_DIM, font=("Segoe UI", 10)).pack(anchor="w")
            return

        for app in self.store.apps:
            used = self.store.seconds_used(app["process"])
            limit = app["limit_minutes"] * 60
            pct = min(100.0, used / limit * 100) if limit else 100.0
            style = "Good" if pct < 60 else "Warn" if pct < 100 else "Bad"

            row = tk.Frame(self.today_list, bg=BG, padx=16, pady=12)
            row.pack(fill="x", pady=(0, 8))

            head = tk.Frame(row, bg=BG)
            head.pack(fill="x")
            tk.Label(head, text=app["name"], bg=BG, fg=FG,
                     font=("Segoe UI Semibold", 11)).pack(side="left")
            left = max(0, limit - used)
            tail = f"{fmt(left)} left" if left else "limit reached"
            tk.Label(head, text=f"{fmt(used)} / {app['limit_minutes']}m  |  {tail}",
                     bg=BG, fg=FG_DIM if left else BAD,
                     font=("Segoe UI", 10)).pack(side="right")

            ttk.Progressbar(row, style=f"{style}.Horizontal.TProgressbar",
                            maximum=100, value=pct).pack(fill="x", pady=(8, 0))

    def refresh_apps(self):
        for child in self.apps_list.winfo_children():
            child.destroy()

        if not self.store.apps:
            tk.Label(self.apps_list, text="Nothing tracked yet.", bg=BG_CARD, fg=FG_DIM,
                     font=("Segoe UI", 10)).pack(anchor="w")
            return

        for app in self.store.apps:
            row = tk.Frame(self.apps_list, bg=BG, padx=16, pady=10)
            row.pack(fill="x", pady=(0, 6))
            info = tk.Frame(row, bg=BG)
            info.pack(side="left", fill="x", expand=True)
            tk.Label(info, text=app["name"], bg=BG, fg=FG,
                     font=("Segoe UI Semibold", 11)).pack(anchor="w")
            mode = "force close" if app.get("hard_block", True) else "warn only"
            tk.Label(info,
                     text=f"{app['process']}  |  {app['limit_minutes']} min/day  |  {mode}",
                     bg=BG, fg=FG_DIM, font=("Segoe UI", 9)).pack(anchor="w")

            self._button(row, "Remove", lambda a=app: self.remove_app(a),
                         "bad").pack(side="right")
            self._button(row, "+10", lambda a=app: self.edit_limit(a, 10),
                         "flat").pack(side="right", padx=6)
            self._button(row, f"{MINUS}10", lambda a=app: self.edit_limit(a, -10),
                         "flat").pack(side="right")

    def refresh_sites(self):
        active = hosts.is_active()
        self.sites_state.config(text="Blocking is ACTIVE" if active else "Blocking is OFF",
                                fg=GOOD if active else BAD)
        self.sites_btn.config(text="Turn off" if active else "Turn on")
        if not hosts.is_admin():
            self.admin_note.config(
                text="Mate is not running as administrator, so it cannot change site "
                     "blocking. Close it and start it with Mate.bat, which asks for "
                     "elevation.", fg=WARN)

    def draw_chart(self):
        canvas = self.chart
        canvas.delete("all")
        width = canvas.winfo_width() or 800
        height = canvas.winfo_height() or 280
        history = self.store.history(7)
        peak = max([max(d, s) for _, d, s in history] + [3600.0])

        pad_x, pad_b, pad_t = 40, 34, 24
        span = (width - pad_x * 2) / max(1, len(history))
        base = height - pad_b

        canvas.create_line(pad_x - 10, base, width - pad_x + 10, base, fill="#2a2f3d")

        for i, (day, distracted, study) in enumerate(history):
            cx = pad_x + span * i + span / 2
            bw = min(26, span / 3)
            for offset, value, colour in ((-bw * 0.6, study, GOOD),
                                          (bw * 0.6, distracted, BAD)):
                h = (value / peak) * (base - pad_t)
                if value > 0:
                    h = max(h, 2)
                canvas.create_rectangle(cx + offset - bw / 2, base - h,
                                        cx + offset + bw / 2, base,
                                        fill=colour, outline="")
                if value:
                    canvas.create_text(cx + offset, base - h - 8,
                                       text=fmt_short(value), fill=FG_DIM,
                                       font=("Segoe UI", 7))
            label = "today" if i == len(history) - 1 else day[5:]
            canvas.create_text(cx, base + 14, text=label, fill=FG_DIM,
                               font=("Segoe UI", 8))

    # ---------- monitor callbacks (worker thread) ----------

    def _on_block(self, app, seconds_left, reason):
        self.root.after(0, lambda: self.blockscreen.show(app, seconds_left, reason))

    def _on_release(self):
        self.root.after(0, self.blockscreen.hide)

    def _on_tick(self):
        self.root.after(0, self._tick_ui)

    def _tick_ui(self):
        try:
            self.stat_focus.config(text=fmt_short(self.store.study_today()))
            self.stat_distract.config(text=fmt_short(sum(self.store.usage_today().values())))
            self.refresh_today()
        except tk.TclError:
            pass

    def on_close(self):
        self.monitor.stop()
        self.store.update_streak()
        self.store.save()
        self.root.destroy()
