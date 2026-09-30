package com.ahooncho.alarm.data

import kotlinx.serialization.Serializable

@Serializable
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    /** ISO day-of-week values (1 = Monday … 7 = Sunday). Empty means the alarm rings once. */
    val days: Set<Int> = emptySet(),
    /** Whether the alarm will ring at its next occurrence. One-time alarms turn off when dismissed. */
    val enabled: Boolean = true,
    /** Epoch millis the alarm was snoozed until, if a snooze is pending. */
    val snoozeUntil: Long? = null,
)

@Serializable
data class Settings(
    /** Share of the maximum media volume used while ringing through earphones. */
    val volumePercent: Int = 40,
    /** How long the sound takes to rise from barely audible to full volume. */
    val rampSeconds: Int = 30,
    val snoozeMinutes: Int = 5,
    /** Ringing stops by itself after this long without snooze or dismiss. */
    val autoStopMinutes: Int = 15,
)

@Serializable
data class AppData(
    val alarms: List<Alarm> = emptyList(),
    val settings: Settings = Settings(),
    val nextId: Int = 1,
)
