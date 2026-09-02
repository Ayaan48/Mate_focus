package com.mate.focus

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * The engine. It watches which app is in front, keeps its own clock for every
 * tracked app, and slams the door when a budget runs out or a blocked address
 * appears in a browser.
 */
class MateAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var running: Boolean = false
            private set

        const val REASON_LIMIT = "limit"
        const val REASON_FOCUS = "focus"
        const val REASON_SITE = "site"
        const val REASON_TAMPER = "tamper"

        /** Settings apps across the common OEM skins. */
        private val SETTINGS_PACKAGES = setOf(
            "com.android.settings",
            "com.android.settings.intelligence",
            "com.samsung.android.settings",
            "com.miui.securitycenter",
            "com.coloros.safecenter",
            "com.oplus.safecenter",
            "com.oppo.safe",
            "com.vivo.settings",
            "com.transsion.phonemanager",
        )

        /** Whatever draws the "do you want to uninstall?" confirmation. */
        private val INSTALLER_PACKAGES = setOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.miui.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.oplus.packageinstaller",
        )

        private const val MAX_NODES = 400
        private const val TICK_MS = 3_000L
        private const val BLOCK_COOLDOWN_MS = 1_200L

        fun isEnabled(context: Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            return enabled.contains(context.packageName)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var foregroundPkg: String? = null
    private var foregroundSince = 0L
    private var lastBlockAt = 0L
    private var lastFocusFlush = 0L

    private val ticker = object : Runnable {
        override fun run() {
            flushClock()
            foregroundPkg?.let { evaluate(it) }
            Prefs.refreshStreak()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Prefs.init(this)
        running = true
        lastFocusFlush = SystemClock.elapsedRealtime()
        handler.postDelayed(ticker, TICK_MS)
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        flushClock()
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                switchForeground(pkg)
                evaluate(pkg)
                inspectBrowser(pkg)
                guardSelf(pkg)
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            -> {
                inspectBrowser(pkg)
                guardSelf(pkg)
            }
        }
    }

    // ---------- clock ----------

    private fun switchForeground(pkg: String) {
        if (pkg == foregroundPkg) return
        flushClock()
        foregroundPkg = pkg
        foregroundSince = SystemClock.elapsedRealtime()
    }

    /** Bank the time spent in the current app, and any focus-session time. */
    private fun flushClock() {
        val now = SystemClock.elapsedRealtime()

        val pkg = foregroundPkg
        if (pkg != null && foregroundSince > 0) {
            val seconds = (now - foregroundSince) / 1000
            if (seconds > 0 && Prefs.appFor(pkg) != null && !isBlockedNow(pkg)) {
                Prefs.addUsage(pkg, seconds)
            }
            if (seconds > 0) foregroundSince = now
        }

        if (Prefs.focusActive) {
            val seconds = (now - lastFocusFlush) / 1000
            if (seconds > 0) {
                Prefs.addFocusSeconds(seconds)
                lastFocusFlush = now
            }
        } else {
            lastFocusFlush = now
        }
    }

    // ---------- decisions ----------

    private fun isBlockedNow(pkg: String): Boolean {
        if (pkg in Blocklist.ADULT_PACKAGES && Prefs.blockAdult) return true
        val app = Prefs.appFor(pkg) ?: return false
        if (Prefs.focusActive) return true
        return Prefs.usedSeconds(pkg) >= app.limitMinutes * 60L
    }

    private fun evaluate(pkg: String) {
        if (pkg in Blocklist.ADULT_PACKAGES && Prefs.blockAdult) {
            block(labelOf(pkg), REASON_SITE)
            return
        }
        val app = Prefs.appFor(pkg) ?: return
        if (Prefs.focusActive) {
            block(app.label, REASON_FOCUS)
        } else if (Prefs.usedSeconds(pkg) >= app.limitMinutes * 60L && app.hardBlock) {
            block(app.label, REASON_LIMIT)
        }
    }

    private fun inspectBrowser(pkg: String) {
        if (!Prefs.blockAdult) return
        val fields = Blocklist.BROWSERS[pkg] ?: return
        val root = rootInActiveWindow ?: return
        try {
            val url = fields.firstNotNullOfOrNull { readField(root, it) } ?: return
            if (Blocklist.isBlockedUrl(url, Prefs.extraDomains)) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                block(hostLabel(url), REASON_SITE)
            }
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun readField(root: AccessibilityNodeInfo, viewId: String): String? {
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId) ?: return null
        try {
            return nodes.firstNotNullOfOrNull { node ->
                (node.text ?: node.contentDescription)?.toString()?.takeIf { it.isNotBlank() }
            }
        } finally {
            @Suppress("DEPRECATION")
            nodes.forEach { it.recycle() }
        }
    }

    private fun hostLabel(url: String): String =
        url.substringAfter("://").substringBefore('/').removePrefix("www.").ifBlank { "That site" }

    private fun labelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        pkg
    }

    // ---------- self defence ----------

    /**
     * Bounces you off the screens that would end Mate: its App Info page, the
     * uninstall confirmation, the device-admin deactivation screen, and its own
     * accessibility toggle. The way through is the Setup tab, which costs the
     * strict-mode phrase and a 90 second wait and then stands this down for
     * five minutes.
     */
    private fun guardSelf(pkg: String) {
        if (!Prefs.tamperGuard || Prefs.tamperGraceActive) return
        if (Prefs.grantingWindowOpen) return
        if (pkg !in SETTINGS_PACKAGES && pkg !in INSTALLER_PACKAGES) return

        val root = rootInActiveWindow ?: return
        val texts = try {
            collectTexts(root)
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }

        if (!SelfDefence.shouldIntercept(texts)) return

        performGlobalAction(GLOBAL_ACTION_BACK)
        block("Mate itself", REASON_TAMPER)
    }

    /** Lower-cased visible text of a window, capped so deep trees stay cheap. */
    private fun collectTexts(root: AccessibilityNodeInfo): List<String> {
        val out = ArrayList<String>(64)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var seen = 0
        while (queue.isNotEmpty() && seen < MAX_NODES) {
            val node = queue.removeFirst()
            seen++
            (node.text ?: node.contentDescription)?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let { out.add(it.lowercase()) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return out
    }

    // ---------- enforcement ----------

    private fun block(label: String, reason: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockAt < BLOCK_COOLDOWN_MS) return
        lastBlockAt = now

        performGlobalAction(GLOBAL_ACTION_HOME)

        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(BlockActivity.EXTRA_LABEL, label)
            putExtra(BlockActivity.EXTRA_REASON, reason)
        }
        startActivity(intent)
    }
}
