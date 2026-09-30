package com.ahooncho.alarm.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ahooncho.alarm.alarmApp

/**
 * AlarmManager forgets everything on reboot, so alarms are rescheduled here. LOCKED_BOOT_COMPLETED
 * matters on Galaxy phones: the nightly auto restart leaves the phone locked until morning.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> context.alarmApp.repository.resync()
        }
    }
}
