package com.ahooncho.alarm.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.ahooncho.alarm.R
import com.ahooncho.alarm.ui.AlarmActivity

object Notifications {
    private const val CHANNEL_RINGING = "ringing"
    private const val CHANNEL_INFO = "info"
    const val ID_RINGING = 1
    private const val ID_AUTO_STOPPED = 2

    fun createChannels(context: Context) {
        // Both channels are silent: sound only ever comes from EarphonePlayer.
        val ringing = NotificationChannel(CHANNEL_RINGING, "울리는 알람", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "알람이 울리는 동안 표시돼요. 소리는 이어폰으로만 나가요."
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val info = NotificationChannel(CHANNEL_INFO, "안내", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(ringing, info))
    }

    fun ringing(context: Context, title: String, text: String): Notification {
        val screen = PendingIntent.getActivity(
            context,
            0,
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = Icon.createWithResource(context, R.drawable.ic_notification)
        return Notification.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setFullScreenIntent(screen, true)
            .setContentIntent(screen)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(Notification.Action.Builder(icon, "스누즈", AlarmService.snoozeIntent(context)).build())
            .addAction(Notification.Action.Builder(icon, "끄기", AlarmService.dismissIntent(context)).build())
            .build()
    }

    fun notifyAutoStopped(context: Context, text: String) {
        val notification = Notification.Builder(context, CHANNEL_INFO)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("알람이 자동으로 꺼졌어요")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ID_AUTO_STOPPED, notification)
    }
}
