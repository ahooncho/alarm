package com.ahooncho.alarm.ring

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.text.format.DateFormat
import android.util.Log
import com.ahooncho.alarm.alarmApp
import com.ahooncho.alarm.schedule.AlarmScheduler.Companion.TEST_ALARM_ID
import com.ahooncho.alarm.util.TimeText
import java.time.Instant
import java.time.ZoneId

/**
 * Runs while an alarm rings: sound through earphones via [EarphonePlayer], vibration when no
 * earphone can play, and the full-screen notification that opens the ringing screen.
 */
class AlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val ringingIds = mutableSetOf<Int>()
    private var player: EarphonePlayer? = null
    private var vibrator: AlarmVibrator? = null
    private var session: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val autoStop = Runnable { onAutoStop() }

    private val repository get() = alarmApp.repository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> onRing(
                id = intent.getIntExtra(EXTRA_ALARM_ID, -1),
                triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis()),
            )
            ACTION_SNOOZE -> onSnooze()
            ACTION_DISMISS -> onDismiss()
            else -> if (ringingIds.isEmpty()) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun onRing(id: Int, triggerAt: Long) {
        val first = ringingIds.isEmpty()
        if (first) {
            RingingState.set(RingingState.Ringing(triggerAt = triggerAt, isTest = id == TEST_ALARM_ID))
        }
        // Required promptly after startForegroundService, even if this alarm turns out to be stale.
        startForeground(Notifications.ID_RINGING, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        WakeLocks.releaseHandoff()

        if (id != TEST_ALARM_ID && repository.alarm(id)?.enabled != true) {
            Log.w(TAG, "Ignoring alarm $id, which is no longer enabled")
            if (first) finish()
            return
        }
        ringingIds += id
        // Move repeating alarms on to their next day now, in case this process dies while ringing.
        repository.resync(notBefore = triggerAt + 1)
        if (first) {
            startRinging()
        } else {
            RingingState.update { it.copy(isTest = ringingIds.all { ringing -> ringing == TEST_ALARM_ID }) }
        }
    }

    private fun startRinging() {
        val settings = repository.data.value.settings
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alarm:ringing")
            .apply { acquire((settings.autoStopMinutes + 1) * 60_000L) }
        session = MediaSession(this, "AlarmRinging").apply {
            // AirPods stem presses arrive as media buttons. Swallowing them keeps a sleepy press
            // from silencing the alarm or starting music in another app.
            setCallback(object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean = true
            })
            setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                    .build(),
            )
            isActive = true
        }
        vibrator = AlarmVibrator(this)
        player = EarphonePlayer(
            context = this,
            volumePercent = settings.volumePercent,
            rampMillis = settings.rampSeconds * 1_000L,
            onOutputChanged = ::onOutputChanged,
        ).also { it.start() }
        handler.postDelayed(autoStop, settings.autoStopMinutes * 60_000L)
    }

    private fun onOutputChanged(earphone: String?) {
        if (earphone == null) vibrator?.start() else vibrator?.stop()
        RingingState.update { it.copy(earphone = earphone, outputKnown = true) }
        getSystemService(NotificationManager::class.java).notify(Notifications.ID_RINGING, buildNotification())
    }

    private fun onSnooze() {
        val ids = ringingIds.toSet()
        if (ids.isNotEmpty()) {
            val until = System.currentTimeMillis() + repository.data.value.settings.snoozeMinutes * 60_000L
            repository.snooze(ids - TEST_ALARM_ID, until)
            if (TEST_ALARM_ID in ids) repository.scheduleTest(until)
        }
        finish()
    }

    private fun onDismiss() {
        repository.dismiss(ringingIds - TEST_ALARM_ID)
        finish()
    }

    private fun onAutoStop() {
        val alarmTime = alarmTimeText()
        val minutes = repository.data.value.settings.autoStopMinutes
        onDismiss()
        Notifications.notifyAutoStopped(this, "$alarmTime 알람이 ${minutes}분 동안 울린 뒤 꺼졌어요")
    }

    private fun finish() {
        teardown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun teardown() {
        handler.removeCallbacks(autoStop)
        player?.stop()
        player = null
        vibrator?.stop()
        vibrator = null
        session?.release()
        session = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        ringingIds.clear()
        RingingState.set(null)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun alarmTimeText(): String {
        val at = RingingState.current.value?.triggerAt ?: System.currentTimeMillis()
        val time = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
        return TimeText.clockLine(time.hour, time.minute, DateFormat.is24HourFormat(this))
    }

    private fun buildNotification(): Notification {
        val state = RingingState.current.value
        val title = alarmTimeText() + if (state?.isTest == true) " 테스트 알람" else " 알람"
        val text = when {
            state == null || !state.outputKnown -> "이어폰 확인 중"
            state.earphone != null -> "${state.earphone}에서 울리는 중"
            else -> "이어폰 없음 · 진동만"
        }
        return Notifications.ringing(this, title, text)
    }

    companion object {
        private const val TAG = "AlarmService"
        private const val ACTION_RING = "com.ahooncho.alarm.action.RING"
        private const val ACTION_SNOOZE = "com.ahooncho.alarm.action.SNOOZE"
        private const val ACTION_DISMISS = "com.ahooncho.alarm.action.DISMISS"
        private const val EXTRA_ALARM_ID = "alarm_id"
        private const val EXTRA_TRIGGER_AT = "trigger_at"

        fun ring(context: Context, id: Int, triggerAt: Long) {
            context.startForegroundService(
                Intent(context, AlarmService::class.java)
                    .setAction(ACTION_RING)
                    .putExtra(EXTRA_ALARM_ID, id)
                    .putExtra(EXTRA_TRIGGER_AT, triggerAt),
            )
        }

        fun snooze(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE))
        }

        fun dismiss(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS))
        }

        fun snoozeIntent(context: Context): PendingIntent = PendingIntent.getService(
            context, 1, Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_IMMUTABLE,
        )

        fun dismissIntent(context: Context): PendingIntent = PendingIntent.getService(
            context, 2, Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
