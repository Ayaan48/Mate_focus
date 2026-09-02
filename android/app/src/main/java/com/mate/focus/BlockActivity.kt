package com.mate.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The wall you hit. Full screen, no way through except leaving. */
class BlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_LABEL = "label"
        const val EXTRA_REASON = "reason"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Prefs.init(this)

        val label = intent.getStringExtra(EXTRA_LABEL) ?: "That app"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: MateAccessibilityService.REASON_LIMIT

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        setContent {
            MateTheme(dark = true) {
                BlockScreen(
                    label = label,
                    reason = reason,
                    onLeave = ::goHome,
                    onOpenMate = {
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    },
                )
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}

@Composable
private fun BlockScreen(
    label: String,
    reason: String,
    onLeave: () -> Unit,
    onOpenMate: () -> Unit,
) {
    val line = remember {
        mutableStateOf(
            if (reason == MateAccessibilityService.REASON_TAMPER) {
                "You set this up when you were thinking clearly. If you really want " +
                    "Mate gone, open it and use “Let me uninstall” - it costs a " +
                    "phrase and ninety seconds, and then it steps aside."
            } else {
                Motivation.blockLine()
            }
        )
    }
    val quote = remember { mutableStateOf(Motivation.randomQuote()) }
    val message by line
    val saying by quote

    val heading = when (reason) {
        MateAccessibilityService.REASON_FOCUS -> "Focus session running"
        MateAccessibilityService.REASON_SITE -> "Blocked for good"
        MateAccessibilityService.REASON_TAMPER -> "Not this way"
        else -> "Daily limit reached"
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(heading, color = Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                label,
                color = Chalk,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                message,
                color = Chalk,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                "“${saying.text}”" + if (saying.author.isNotEmpty()) "\n— ${saying.author}" else "",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp),
            )
            Button(
                onClick = onLeave,
                colors = ButtonDefaults.buttonColors(containerColor = Good, contentColor = Ink),
                modifier = Modifier.padding(top = 40.dp),
            ) {
                Text("Back to what matters", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onOpenMate,
                colors = ButtonDefaults.buttonColors(containerColor = Field, contentColor = Muted),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Text("Open Mate")
            }
        }
    }
}
