package com.ahooncho.alarm.system

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/** Phone settings the alarm depends on, checked from the main screen. */
object Readiness {
    enum class Item { NOTIFICATIONS, FULL_SCREEN, EXACT_ALARM, BATTERY, DND_MEDIA }

    data class Status(val item: Item, val ok: Boolean)

    fun check(context: Context): List<Status> {
        val notifications = context.getSystemService(NotificationManager::class.java)
        val power = context.getSystemService(PowerManager::class.java)
        val alarms = context.getSystemService(AlarmManager::class.java)
        return listOf(
            Status(Item.NOTIFICATIONS, notifications.areNotificationsEnabled()),
            Status(Item.FULL_SCREEN, notifications.canUseFullScreenIntent()),
            Status(Item.EXACT_ALARM, alarms.canScheduleExactAlarms()),
            Status(Item.BATTERY, power.isIgnoringBatteryOptimizations(context.packageName)),
            Status(Item.DND_MEDIA, dndAllowsMedia(notifications)),
        )
    }

    /** The alarm plays as media, so Do Not Disturb has to let media sound through. */
    private fun dndAllowsMedia(notifications: NotificationManager): Boolean {
        if (notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) return false
        return try {
            notifications.notificationPolicy.priorityCategories and
                NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA != 0
        } catch (e: SecurityException) {
            true
        }
    }

    /** The settings screen that fixes [item]. */
    fun settingsIntent(context: Context, item: Item): Intent {
        val pkg = Uri.fromParts("package", context.packageName, null)
        return when (item) {
            Item.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            Item.FULL_SCREEN -> Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
            Item.EXACT_ALARM -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
            Item.BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
            // Not a public constant, but opens Do Not Disturb settings on Galaxy phones.
            Item.DND_MEDIA -> Intent("android.settings.ZEN_MODE_SETTINGS")
        }
    }

    fun appDetailsIntent(context: Context): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
}
