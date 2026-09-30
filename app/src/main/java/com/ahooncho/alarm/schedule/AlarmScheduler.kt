package com.ahooncho.alarm.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ahooncho.alarm.data.Alarm
import com.ahooncho.alarm.ui.MainActivity
import java.time.Instant
import java.time.ZoneId

/** Mirrors the stored alarms into AlarmManager, one alarm-clock entry per alarm. */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedules every enabled alarm at its next occurrence and cancels the rest.
     * Occurrences at or before [notBefore] are skipped, so an alarm that is ringing right now is
     * moved on to its following occurrence.
     */
    fun sync(alarms: List<Alarm>, notBefore: Long = 0L) {
        val nowMillis = maxOf(System.currentTimeMillis(), notBefore)
        val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
        for (alarm in alarms) {
            val triggerAt = AlarmTime.nextTriggerMillis(alarm, now)
            if (triggerAt == null) cancel(alarm.id) else schedule(alarm.id, triggerAt)
        }
    }

    fun scheduleTest(triggerAt: Long) = schedule(TEST_ALARM_ID, triggerAt)

    fun cancel(id: Int) {
        val existing = PendingIntent.getBroadcast(
            context, id, AlarmReceiver.intent(context, id, 0L),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    private fun schedule(id: Int, triggerAt: Long) {
        val operation = PendingIntent.getBroadcast(
            context, id, AlarmReceiver.intent(context, id, triggerAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), operation)
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarms are not allowed; alarm $id was not scheduled", e)
        }
    }

    companion object {
        /** Id used by the "test alarm" button; stored alarms start at 1. */
        const val TEST_ALARM_ID = 0
        private const val TAG = "AlarmScheduler"
    }
}
