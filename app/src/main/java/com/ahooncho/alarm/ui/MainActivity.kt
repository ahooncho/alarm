package com.ahooncho.alarm.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahooncho.alarm.alarmApp
import com.ahooncho.alarm.data.Alarm
import com.ahooncho.alarm.data.AlarmRepository
import com.ahooncho.alarm.ring.EarphonePlayer
import com.ahooncho.alarm.ring.Earphones
import com.ahooncho.alarm.schedule.AlarmTime
import com.ahooncho.alarm.system.Readiness
import com.ahooncho.alarm.ui.theme.AlarmTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = alarmApp.repository
        setContent {
            AlarmTheme {
                MainRoute(repository)
            }
        }
    }
}

private sealed interface Editing {
    data object New : Editing
    data class Existing(val alarm: Alarm) : Editing
}

private const val PREVIEW_MILLIS = 6_000L
private const val PREVIEW_RAMP_MILLIS = 1_500L
private const val TEST_DELAY_MILLIS = 10_000L

@Composable
private fun MainRoute(repository: AlarmRepository) {
    val context = LocalContext.current
    val data by repository.data.collectAsStateWithLifecycle()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var is24Hour by remember { mutableStateOf(DateFormat.is24HourFormat(context)) }
    var readiness by remember { mutableStateOf(Readiness.check(context)) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var preview by remember { mutableStateOf<EarphonePlayer?>(null) }
    val earphone = rememberEarphone(context)

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        readiness = Readiness.check(context)
    }

    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(60_000L - nowMillis % 60_000L)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        readiness = Readiness.check(context)
        is24Hour = DateFormat.is24HourFormat(context)
        nowMillis = System.currentTimeMillis()
    }
    DisposableEffect(Unit) {
        onDispose { preview?.stop() }
    }
    LaunchedEffect(preview) {
        val playing = preview ?: return@LaunchedEffect
        delay(PREVIEW_MILLIS)
        playing.stop()
        if (preview === playing) preview = null
    }

    fun startPreview() {
        if (preview != null) return
        lateinit var player: EarphonePlayer
        player = EarphonePlayer(context, data.settings.volumePercent, PREVIEW_RAMP_MILLIS) { output ->
            if (output == null) {
                player.stop()
                if (preview === player) preview = null
                Toast.makeText(context, "이어폰이 연결돼 있지 않아요", Toast.LENGTH_SHORT).show()
            }
        }
        preview = player
        player.start()
    }

    val status = buildList {
        add(StatusLine("이어폰", earphone ?: "연결 안 됨", ok = earphone != null))
        for (item in readiness) {
            add(
                StatusLine(
                    label = statusLabel(item.item),
                    value = statusValue(item.item, item.ok),
                    ok = item.ok,
                    onFix = { openSettings(context, Readiness.settingsIntent(context, item.item)) },
                ),
            )
        }
    }

    MainScreen(
        alarms = data.alarms.sortedBy { it.hour * 60 + it.minute },
        settings = data.settings,
        nowMillis = nowMillis,
        nextAlarmAt = AlarmTime.nextOf(data.alarms, nowMillis),
        is24Hour = is24Hour,
        status = status,
        previewing = preview != null,
        onToggle = { alarm, enabled -> repository.setEnabled(alarm.id, enabled) },
        onEdit = { editing = Editing.Existing(it) },
        onAdd = { editing = Editing.New },
        onSettingsChange = { settings -> repository.updateSettings { settings } },
        onPreview = ::startPreview,
        onTestAlarm = {
            repository.scheduleTest(System.currentTimeMillis() + TEST_DELAY_MILLIS)
            Toast.makeText(context, "10초 뒤에 울려요. 화면을 끄고 기다려 보세요.", Toast.LENGTH_LONG).show()
        },
    )

    when (val target = editing) {
        null -> Unit
        Editing.New -> AlarmEditorDialog(
            initialHour = 6,
            initialMinute = 0,
            initialDays = emptySet(),
            is24Hour = is24Hour,
            isNew = true,
            onSave = { hour, minute, days ->
                repository.addAlarm(hour, minute, days)
                editing = null
            },
            onDelete = {},
            onDismiss = { editing = null },
        )
        is Editing.Existing -> AlarmEditorDialog(
            initialHour = target.alarm.hour,
            initialMinute = target.alarm.minute,
            initialDays = target.alarm.days,
            is24Hour = is24Hour,
            isNew = false,
            onSave = { hour, minute, days ->
                repository.saveAlarm(target.alarm.id, hour, minute, days)
                editing = null
            },
            onDelete = {
                repository.deleteAlarm(target.alarm.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

private fun statusLabel(item: Readiness.Item): String = when (item) {
    Readiness.Item.NOTIFICATIONS -> "알림"
    Readiness.Item.FULL_SCREEN -> "잠금 화면에 표시"
    Readiness.Item.EXACT_ALARM -> "정확한 시각에 울리기"
    Readiness.Item.BATTERY -> "배터리 사용"
    Readiness.Item.DND_MEDIA -> "방해 금지 중 미디어 소리"
}

private fun statusValue(item: Readiness.Item, ok: Boolean): String = when (item) {
    Readiness.Item.BATTERY -> if (ok) "제한 없음" else "제한됨"
    Readiness.Item.DND_MEDIA -> if (ok) "허용" else "차단됨"
    else -> if (ok) "허용" else "꺼짐"
}

private fun openSettings(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Readiness.appDetailsIntent(context))
    }
}

/** Name of the connected earphone, updated live as devices come and go. */
@Composable
private fun rememberEarphone(context: Context): String? {
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    var name by remember { mutableStateOf(currentEarphone(audioManager)) }
    DisposableEffect(audioManager) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                name = currentEarphone(audioManager)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                name = currentEarphone(audioManager)
            }
        }
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }
    return name
}

private fun currentEarphone(audioManager: AudioManager): String? =
    Earphones.find(audioManager)?.let(Earphones::displayName)
