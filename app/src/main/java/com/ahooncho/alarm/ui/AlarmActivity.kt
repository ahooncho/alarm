package com.ahooncho.alarm.ui

import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahooncho.alarm.ring.AlarmService
import com.ahooncho.alarm.ring.RingingState
import com.ahooncho.alarm.ui.theme.AlarmTheme
import kotlinx.coroutines.delay

/** Full-screen ringing screen, shown over the lock screen by the alarm notification. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        val is24Hour = DateFormat.is24HourFormat(this)
        setContent {
            AlarmTheme(dark = true) {
                val ringing by RingingState.current.collectAsStateWithLifecycle()
                val state = ringing
                if (state == null) {
                    LaunchedEffect(Unit) { finish() }
                } else {
                    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(1_000L - System.currentTimeMillis() % 1_000L)
                            nowMillis = System.currentTimeMillis()
                        }
                    }
                    RingingScreen(
                        nowMillis = nowMillis,
                        is24Hour = is24Hour,
                        isTest = state.isTest,
                        earphone = state.earphone,
                        outputKnown = state.outputKnown,
                        onSnooze = { AlarmService.snooze(this@AlarmActivity) },
                        onDismiss = { AlarmService.dismiss(this@AlarmActivity) },
                    )
                }
            }
        }
    }
}
