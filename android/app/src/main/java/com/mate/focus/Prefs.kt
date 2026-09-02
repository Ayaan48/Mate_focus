package com.mate.focus

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TrackedApp(
    val pkg: String,
    val label: String,
    val limitMinutes: Int,
    val hardBlock: Boolean = true,
)

data class DayStat(val day: String, val distractedSeconds: Long, val focusSeconds: Long)

/** Every persisted setting Mate has, backed by one SharedPreferences file. */
object Prefs {

    private const val FILE = "mate_prefs"
    private const val KEY_APPS = "apps"
    private const val KEY_USAGE = "usage"
    private const val KEY_FOCUS_SECONDS = "focus_seconds"
    private const val KEY_FOCUS_ENDS = "focus_ends_at"
    private const val KEY_BLOCK_ADULT = "block_adult"
    private const val KEY_EXTRA_DOMAINS = "extra_domains"
    private const val KEY_STRICT = "strict_mode"
    private const val KEY_STREAK = "streak_count"
    private const val KEY_STREAK_DAY = "streak_day"
    private const val KEY_ONBOARDED = "onboarded"
    private const val KEY_TAMPER_GUARD = "tamper_guard"
    private const val KEY_TAMPER_GRACE = "tamper_grace_until"
    private const val KEY_PROTECTION_DROPPED = "protection_dropped_at"
    private const val KEY_GRANTING_UNTIL = "granting_until"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun dayOffset(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    // ---------- tracked apps ----------

    var apps: List<TrackedApp>
        get() {
            val out = mutableListOf<TrackedApp>()
            val array = JSONArray(prefs.getString(KEY_APPS, "[]").orEmpty())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                out += TrackedApp(
                    pkg = o.getString("pkg"),
                    label = o.optString("label", o.getString("pkg")),
                    limitMinutes = o.optInt("limit", 30),
                    hardBlock = o.optBoolean("hard", true),
                )
            }
            return out
        }
        set(value) {
            val array = JSONArray()
            value.forEach {
                array.put(
                    JSONObject()
                        .put("pkg", it.pkg)
                        .put("label", it.label)
                        .put("limit", it.limitMinutes)
                        .put("hard", it.hardBlock)
                )
            }
            prefs.edit().putString(KEY_APPS, array.toString()).apply()
        }

    fun appFor(pkg: String): TrackedApp? = apps.firstOrNull { it.pkg == pkg }

    fun upsertApp(app: TrackedApp) {
        apps = apps.filterNot { it.pkg == app.pkg } + app
    }

    fun removeApp(pkg: String) {
        apps = apps.filterNot { it.pkg == pkg }
    }

    // ---------- usage ----------

    private fun usageRoot(): JSONObject =
        JSONObject(prefs.getString(KEY_USAGE, "{}").orEmpty())

    private fun saveUsage(root: JSONObject) =
        prefs.edit().putString(KEY_USAGE, root.toString()).apply()

    fun addUsage(pkg: String, seconds: Long) {
        if (seconds <= 0) return
        val root = usageRoot()
        val day = root.optJSONObject(today()) ?: JSONObject()
        day.put(pkg, day.optLong(pkg, 0L) + seconds)
        root.put(today(), day)
        prune(root)
        saveUsage(root)
    }

    fun usedSeconds(pkg: String): Long =
        usageRoot().optJSONObject(today())?.optLong(pkg, 0L) ?: 0L

    fun distractedSecondsOn(day: String): Long {
        val obj = usageRoot().optJSONObject(day) ?: return 0L
        var total = 0L
        obj.keys().forEach { total += obj.optLong(it, 0L) }
        return total
    }

    fun distractedToday(): Long = distractedSecondsOn(today())

    private fun prune(root: JSONObject) {
        val cutoff = dayOffset(30)
        root.keys().asSequence().filter { it < cutoff }.toList().forEach { root.remove(it) }
    }

    // ---------- focus ----------

    fun addFocusSeconds(seconds: Long) {
        val root = JSONObject(prefs.getString(KEY_FOCUS_SECONDS, "{}").orEmpty())
        root.put(today(), root.optLong(today(), 0L) + seconds)
        prefs.edit().putString(KEY_FOCUS_SECONDS, root.toString()).apply()
    }

    fun focusSecondsOn(day: String): Long =
        JSONObject(prefs.getString(KEY_FOCUS_SECONDS, "{}").orEmpty()).optLong(day, 0L)

    fun focusToday(): Long = focusSecondsOn(today())

    fun startFocus(minutes: Int) {
        prefs.edit()
            .putLong(KEY_FOCUS_ENDS, System.currentTimeMillis() + minutes * 60_000L)
            .apply()
    }

    fun cancelFocus() = prefs.edit().putLong(KEY_FOCUS_ENDS, 0L).apply()

    /** Milliseconds left in the current focus session, 0 when none is running. */
    fun focusRemaining(): Long =
        (prefs.getLong(KEY_FOCUS_ENDS, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    val focusActive: Boolean get() = focusRemaining() > 0

    // ---------- settings ----------

    var blockAdult: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_ADULT, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_ADULT, value).apply()

    var strictMode: Boolean
        get() = prefs.getBoolean(KEY_STRICT, true)
        set(value) = prefs.edit().putBoolean(KEY_STRICT, value).apply()

    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    var extraDomains: List<String>
        get() = prefs.getString(KEY_EXTRA_DOMAINS, "").orEmpty()
            .split("\n").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        set(value) = prefs.edit()
            .putString(KEY_EXTRA_DOMAINS, value.joinToString("\n"))
            .apply()

    // ---------- tamper guard ----------

    /** Whether Mate defends its own settings and uninstall screens. */
    var tamperGuard: Boolean
        get() = prefs.getBoolean(KEY_TAMPER_GUARD, true)
        set(value) = prefs.edit().putBoolean(KEY_TAMPER_GUARD, value).apply()

    /**
     * Opens a window in which Mate stops defending itself, so you can
     * genuinely uninstall it. Reaching this costs the strict-mode phrase and
     * the 90 second wait, which is the whole point.
     */
    fun startTamperGrace(minutes: Int) {
        prefs.edit()
            .putLong(KEY_TAMPER_GRACE, System.currentTimeMillis() + minutes * 60_000L)
            .apply()
    }

    fun tamperGraceRemaining(): Long =
        (prefs.getLong(KEY_TAMPER_GRACE, 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    val tamperGraceActive: Boolean get() = tamperGraceRemaining() > 0

    /**
     * Opened for half a minute when you tap a permission button in Setup.
     * Without it, an OEM Settings screen whose wording happens to trip the
     * guard would lock you out of granting the very permissions Mate needs.
     */
    fun startGrantingWindow(seconds: Int = 30) {
        prefs.edit()
            .putLong(KEY_GRANTING_UNTIL, System.currentTimeMillis() + seconds * 1000L)
            .apply()
    }

    val grantingWindowOpen: Boolean
        get() = prefs.getLong(KEY_GRANTING_UNTIL, 0L) > System.currentTimeMillis()

    /** Recorded when device admin is switched off, so the app can say so later. */
    fun noteProtectionDropped() {
        prefs.edit().putLong(KEY_PROTECTION_DROPPED, System.currentTimeMillis()).apply()
    }

    fun protectionDroppedAt(): Long = prefs.getLong(KEY_PROTECTION_DROPPED, 0L)

    // ---------- streak ----------

    val streak: Int get() = prefs.getInt(KEY_STREAK, 0)

    fun refreshStreak() {
        val today = today()
        if (prefs.getString(KEY_STREAK_DAY, null) == today) return
        // Reaching the limit means you were blocked, so the day is not clean.
        val overspent = apps.any {
            it.limitMinutes > 0 && usedSeconds(it.pkg) >= it.limitMinutes * 60L
        }
        if (overspent) return
        val next = if (prefs.getString(KEY_STREAK_DAY, null) == dayOffset(1)) streak + 1 else 1
        prefs.edit().putInt(KEY_STREAK, next).putString(KEY_STREAK_DAY, today).apply()
    }

    // ---------- history ----------

    fun history(days: Int = 7): List<DayStat> =
        (days - 1 downTo 0).map { offset ->
            val day = dayOffset(offset)
            DayStat(day, distractedSecondsOn(day), focusSecondsOn(day))
        }
}
