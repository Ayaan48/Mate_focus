package com.mate.focus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class InstalledApp(val pkg: String, val label: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)

        // Recovery hatch for a phone whose OEM Settings will not show the
        // device-admin screen:
        //     adb shell am start -n com.mate.focus/.MainActivity --ez release_admin true
        // Needs USB debugging, i.e. the same physical access adb uninstall needs.
        if (intent?.getBooleanExtra("release_admin", false) == true) {
            MateDeviceAdminReceiver.release(this)
            Prefs.startGrantingWindow(120)
        }

        enableEdgeToEdge()
        setContent { MateTheme(dark = true) { MateRoot() } }
    }
}

// ---------------------------------------------------------------- helpers

fun formatShort(seconds: Long): String {
    val minutes = seconds / 60
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}

fun formatLong(millis: Long): String {
    val total = millis / 1000
    val m = total / 60
    val s = total % 60
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m ${s}s"
}

private fun overlayGranted(context: Context) = Settings.canDrawOverlays(context)

// ---------------------------------------------------------------- root

@Composable
fun MateRoot() {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }

    // Re-read prefs whenever the app comes back to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Tick so timers and progress bars stay live.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            refreshKey++
        }
    }

    val tabs = listOf("Today", "Apps", "Sites", "Setup")

    Scaffold(containerColor = Ink) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Ink)) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Mate", color = Chalk, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                val guarding = MateAccessibilityService.isEnabled(context)
                Text(
                    if (guarding) "guarding" else "not guarding yet",
                    color = if (guarding) Good else Warn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            TabRow(selectedTabIndex = tab, containerColor = Ink, contentColor = Chalk) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                color = if (tab == index) Chalk else Muted,
                            )
                        },
                    )
                }
            }

            when (tab) {
                0 -> TodayTab(refreshKey)
                1 -> AppsTab(refreshKey)
                2 -> SitesTab()
                else -> SetupTab(refreshKey)
            }
        }
    }
}

// ---------------------------------------------------------------- shared UI

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Card),
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun StatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Text(title, color = Muted, fontSize = 11.sp)
        Text(value, color = Chalk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/** Strict-mode friction: type a random phrase, then wait out a cooldown. */
@Composable
fun UnlockDialog(action: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val phrase = remember { Motivation.unlockPhrase() }
    var typed by remember { mutableStateOf("") }
    var left by remember { mutableIntStateOf(90) }

    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }

    val ready = typed.trim() == phrase && left == 0

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        titleContentColor = Chalk,
        textContentColor = Muted,
        title = { Text(action, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("Strict mode is on. Type this phrase, then wait it out.", fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    phrase,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        typed.trim() != phrase -> "Phrase does not match yet."
                        left > 0 -> "Phrase matches. Cooling down: ${left}s."
                        else -> "You can unlock now, or you can walk away."
                    },
                    fontSize = 12.sp,
                    color = if (ready) Bad else Muted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = ready) {
                Text("Unlock", color = if (ready) Bad else Muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep it locked", color = Good) }
        },
    )
}

/** Runs [action] straight away when strict mode is off, behind friction when on. */
@Composable
fun rememberGuard(): GuardState {
    val state = remember { GuardState() }
    state.pending?.let { pending ->
        UnlockDialog(
            action = pending.first,
            onDismiss = { state.pending = null },
            onConfirm = {
                pending.second()
                state.pending = null
            },
        )
    }
    return state
}

class GuardState {
    var pending by mutableStateOf<Pair<String, () -> Unit>?>(null)

    fun run(action: String, block: () -> Unit) {
        if (Prefs.strictMode) pending = action to block else block()
    }
}

// ---------------------------------------------------------------- today

@Composable
fun TodayTab(refreshKey: Int) {
    val guard = rememberGuard()
    val quote = remember { Motivation.quoteOfTheDay() }
    val apps = remember(refreshKey) { Prefs.apps }
    val focusLeft = remember(refreshKey) { Prefs.focusRemaining() }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Panel {
                Text(
                    "“${quote.text}”",
                    color = Chalk,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
                if (quote.author.isNotEmpty()) {
                    Text("— ${quote.author}", color = Muted, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("Streak", "${Prefs.streak}d", Modifier.weight(1f))
                StatBox("Focused", formatShort(Prefs.focusToday()), Modifier.weight(1f))
                StatBox("Distracted", formatShort(Prefs.distractedToday()), Modifier.weight(1f))
            }
        }

        item {
            Panel {
                Text("Focus session", color = Chalk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Every tracked app is locked while this runs.",
                    color = Muted, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                )
                if (focusLeft > 0) {
                    Text(
                        "${formatLong(focusLeft)} left",
                        color = Good, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    )
                    Button(
                        onClick = {
                            guard.run("End the focus session early?") { Prefs.cancelFocus() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Bad, contentColor = Ink),
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text("End session") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(25, 45, 60, 90).forEach { minutes ->
                            Button(
                                onClick = { Prefs.startFocus(minutes) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Field, contentColor = Chalk),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 12.dp, vertical = 6.dp),
                            ) { Text("${minutes}m", fontSize = 13.sp) }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Today's budget",
                color = Chalk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }

        if (apps.isEmpty()) {
            item {
                Panel { Text("No apps tracked yet. Add one in the Apps tab.",
                    color = Muted, fontSize = 13.sp) }
            }
        }

        items(apps, key = { it.pkg }) { app ->
            val used = Prefs.usedSeconds(app.pkg)
            val limit = app.limitMinutes * 60L
            val fraction = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 1f
            val colour = when {
                fraction < 0.6f -> Good
                fraction < 1f -> Warn
                else -> Bad
            }
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.label, color = Chalk, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    val leftSeconds = (limit - used).coerceAtLeast(0)
                    Text(
                        if (leftSeconds > 0) "${formatShort(leftSeconds)} left" else "limit reached",
                        color = if (leftSeconds > 0) Muted else Bad,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    color = colour,
                    trackColor = Field,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------- apps

@Composable
fun AppsTab(refreshKey: Int) {
    val context = LocalContext.current
    val guard = rememberGuard()
    var installed by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var version by remember { mutableIntStateOf(0) }
    val tracked = remember(version, refreshKey) { Prefs.apps.associateBy { it.pkg } }

    LaunchedEffect(Unit) {
        installed = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }

    val visible = remember(installed, query) {
        installed.filter { it.label.contains(query, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search your apps", color = Muted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it.pkg }) { app ->
                val entry = tracked[app.pkg]
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = Chalk, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold)
                            Text(
                                entry?.let { "${it.limitMinutes} min a day" } ?: app.pkg,
                                color = Muted, fontSize = 11.sp,
                            )
                        }
                        Switch(
                            checked = entry != null,
                            onCheckedChange = { on ->
                                if (on) {
                                    Prefs.upsertApp(TrackedApp(app.pkg, app.label, 30))
                                    version++
                                } else {
                                    guard.run("Stop limiting ${app.label}?") {
                                        Prefs.removeApp(app.pkg)
                                        version++
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Ink,
                                checkedTrackColor = Good,
                                uncheckedTrackColor = Field,
                            ),
                        )
                    }
                    if (entry != null) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(-15, -5, 5, 15).forEach { delta ->
                                Button(
                                    onClick = {
                                        val apply: () -> Unit = {
                                            Prefs.upsertApp(
                                                entry.copy(
                                                    limitMinutes =
                                                        (entry.limitMinutes + delta).coerceAtLeast(0)
                                                )
                                            )
                                            version++
                                        }
                                        if (delta > 0) {
                                            guard.run(
                                                "Give yourself $delta more minutes on ${app.label}?",
                                                apply,
                                            )
                                        } else {
                                            apply()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Field, contentColor = Chalk),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(if (delta > 0) "+$delta" else "$delta", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun loadLaunchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcher, 0)
        .asSequence()
        .map { it.activityInfo.applicationInfo }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
        .sortedBy { it.label.lowercase() }
        .toList()
}

// ---------------------------------------------------------------- sites

@Composable
fun SitesTab() {
    val guard = rememberGuard()
    var blocking by remember { mutableStateOf(Prefs.blockAdult) }
    var domains by remember { mutableStateOf(Prefs.extraDomains.joinToString("\n")) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Adult site blocking", color = Chalk, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Blocklist.ADULT_DOMAINS.size} domains plus keyword matching, " +
                            "in every browser Mate can read.",
                        color = Muted, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = blocking,
                    onCheckedChange = { on ->
                        if (on) {
                            Prefs.blockAdult = true
                            blocking = true
                        } else {
                            guard.run("Unblock adult sites on this phone?") {
                                Prefs.blockAdult = false
                                blocking = false
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Ink,
                        checkedTrackColor = Good,
                        uncheckedTrackColor = Field,
                    ),
                )
            }
        }

        Panel(Modifier.weight(1f)) {
            Text("Your own blocked domains", color = Chalk, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold)
            Text("One per line. Subdomains are covered.", color = Muted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            OutlinedTextField(
                value = domains,
                onValueChange = { domains = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Chalk, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            )
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val next = domains.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        val removing = Prefs.extraDomains.any { it !in next }
                        if (removing) {
                            guard.run("Remove domains from your block list?") {
                                Prefs.extraDomains = next
                            }
                        } else {
                            Prefs.extraDomains = next
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                ) { Text("Save") }
                Button(
                    onClick = {
                        val preset = listOf(
                            "instagram.com", "facebook.com", "tiktok.com", "x.com",
                            "reddit.com", "9gag.com", "snapchat.com", "twitch.tv",
                        )
                        val merged = (domains.lines().map { it.trim() } + preset)
                            .filter { it.isNotEmpty() }.distinct().sorted()
                        domains = merged.joinToString("\n")
                        Prefs.extraDomains = merged
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Field, contentColor = Chalk),
                ) { Text("Add doomscroll preset") }
            }
        }
    }
}

// ---------------------------------------------------------------- setup

@Composable
fun SetupTab(refreshKey: Int) {
    val context = LocalContext.current
    val guard = rememberGuard()
    var strict by remember { mutableStateOf(Prefs.strictMode) }
    val accessibilityOn = remember(refreshKey) { MateAccessibilityService.isEnabled(context) }
    val overlayOn = remember(refreshKey) { overlayGranted(context) }
    val adminOn = remember(refreshKey) { MateDeviceAdminReceiver.isActive(context) }
    val graceLeft = remember(refreshKey) { Prefs.tamperGraceRemaining() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PermissionCard(
            title = "Accessibility access",
            body = "Lets Mate see which app is in front and close it when your time is up. " +
                "Without this, nothing is enforced.",
            granted = accessibilityOn,
            action = "Open settings",
        ) {
            Prefs.startGrantingWindow()
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        PermissionCard(
            title = "Display over other apps",
            body = "Lets the block screen appear instantly on top of whatever you opened.",
            granted = overlayOn,
            action = "Grant",
        ) {
            Prefs.startGrantingWindow()
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        if (adminOn) {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Good, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text("Uninstall guard is on", color = Chalk, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Android will refuse to uninstall Mate while this is on - including " +
                        "from a computer. Turn it off here before removing the app.",
                    color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Button(
                    onClick = {
                        guard.run("Turn the uninstall guard off?") {
                            MateDeviceAdminReceiver.release(context)
                            Prefs.startGrantingWindow(120)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Field, contentColor = Bad),
                    modifier = Modifier.padding(top = 10.dp),
                ) { Text("Turn off uninstall guard") }
            }
        } else PermissionCard(
            title = "Uninstall guard (device admin)",
            body = "Android refuses to uninstall an app while its device admin is " +
                "active. Mate asks for no other powers - it cannot wipe your phone, " +
                "lock it, or read your data.\n\n" +
                "Some skins (ColorOS, Realme UI, MIUI) refuse to show this screen for " +
                "sideloaded apps - it flashes open and closes. If that happens, run this " +
                "once from a computer with USB debugging on:\n" +
                "adb shell dpm set-active-admin com.mate.focus/.MateDeviceAdminReceiver\n\n" +
                "The screen interception below works without it.",
            granted = adminOn,
            action = "Turn on",
        ) {
            Prefs.startGrantingWindow()
            context.startActivity(
                MateDeviceAdminReceiver.enableIntent(context)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        Panel {
            Text("Getting out", color = Chalk, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold)
            Text(
                if (graceLeft > 0) {
                    "Mate has stepped aside. You can uninstall it for the next " +
                        formatLong(graceLeft) + "."
                } else {
                    "Mate blocks its own App Info, uninstall and accessibility " +
                        "screens. This is the honest way out, and it is deliberately slow."
                },
                color = if (graceLeft > 0) Warn else Muted,
                fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (graceLeft <= 0) {
                Button(
                    onClick = {
                        guard.run("Let yourself uninstall Mate for five minutes?") {
                            Prefs.startTamperGrace(5)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Field, contentColor = Bad),
                    modifier = Modifier.padding(top = 10.dp),
                ) { Text("Let me uninstall") }
            }
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Strict mode", color = Chalk, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        "Loosening any limit needs a typed phrase and a 90 second wait.",
                        color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = strict,
                    onCheckedChange = { on ->
                        if (on) {
                            Prefs.strictMode = true
                            strict = true
                        } else {
                            guard.run("Turn strict mode off?") {
                                Prefs.strictMode = false
                                strict = false
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Ink,
                        checkedTrackColor = Good,
                        uncheckedTrackColor = Field,
                    ),
                )
            }
        }

        Panel {
            Text("Last 7 days", color = Chalk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            val history = remember(refreshKey / 30) { Prefs.history(7) }
            val peak = (history.maxOfOrNull { maxOf(it.distractedSeconds, it.focusSeconds) } ?: 0L)
                .coerceAtLeast(3600L)
            Row(
                Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                history.forEach { stat ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Bar(stat.focusSeconds, peak, Good)
                            Bar(stat.distractedSeconds, peak, Bad)
                        }
                        Text(
                            stat.day.takeLast(5),
                            color = Muted, fontSize = 9.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            Text("Green is focused time, red is time on tracked apps.",
                color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun Bar(value: Long, peak: Long, colour: Color) {
    val fraction = (value.toFloat() / peak).coerceIn(0f, 1f)
    Box(
        Modifier
            .width(10.dp)
            .height((90 * fraction).dp.coerceAtLeast(2.dp))
            .background(colour, RoundedCornerShape(2.dp))
    )
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    granted: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (granted) Good else Warn, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(title, color = Chalk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                if (granted) "on" else "off",
                color = if (granted) Good else Warn,
                fontSize = 12.sp,
            )
        }
        Text(body, color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
            modifier = Modifier.padding(top = 6.dp))
        if (!granted) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                modifier = Modifier.padding(top = 10.dp),
            ) { Text(action) }
        }
    }
}
