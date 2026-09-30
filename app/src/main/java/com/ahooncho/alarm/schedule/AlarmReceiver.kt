package com.ahooncho.alarm.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ahooncho.alarm.ring.AlarmService
import com.ahooncho.alarm.ring.WakeLocks

/** Receives the AlarmManager alarm and hands it to [AlarmService]. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        if (id < 0) return
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis())
        // Keep the CPU awake between this receiver returning and the service taking over.
        WakeLocks.acquireHandoff(context)
        try {
            AlarmService.ring(context, id, triggerAt)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Could not start the alarm service", e)
            WakeLocks.releaseHandoff()
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val ACTION_FIRE = "com.ahooncho.alarm.action.FIRE"
        private const val EXTRA_ALARM_ID = "alarm_id"
        private const val EXTRA_TRIGGER_AT = "trigger_at"

        fun intent(context: Context, id: Int, triggerAt: Long): Intent =
            Intent(context, AlarmReceiver::class.java)
                .setAction(ACTION_FIRE)
                .putExtra(EXTRA_ALARM_ID, id)
                .putExtra(EXTRA_TRIGGER_AT, triggerAt)
    }
}
