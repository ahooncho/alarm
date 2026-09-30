package com.ahooncho.alarm.schedule

import com.ahooncho.alarm.data.Alarm
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmTime {
    /** The first moment strictly after [after] at [hour]:[minute] on one of [days] (any day if empty). */
    fun nextOccurrence(hour: Int, minute: Int, days: Set<Int>, after: ZonedDateTime): ZonedDateTime {
        val time = LocalTime.of(hour, minute)
        val startDate = after.toLocalDate()
        for (offset in 0L..7L) {
            val date = startDate.plusDays(offset)
            if (days.isNotEmpty() && date.dayOfWeek.value !in days) continue
            val candidate = ZonedDateTime.of(date, time, after.zone)
            if (candidate.isAfter(after)) return candidate
        }
        error("No occurrence of $hour:$minute on days $days")
    }

    /** Epoch millis at which [alarm] should ring next, or null if it should not ring. */
    fun nextTriggerMillis(alarm: Alarm, now: ZonedDateTime): Long? {
        if (!alarm.enabled) return null
        val nowMillis = now.toInstant().toEpochMilli()
        alarm.snoozeUntil?.let { if (it > nowMillis) return it }
        return nextOccurrence(alarm.hour, alarm.minute, alarm.days, now).toInstant().toEpochMilli()
    }

    /** The earliest upcoming ring among [alarms]. */
    fun nextOf(alarms: List<Alarm>, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        return alarms.mapNotNull { nextTriggerMillis(it, now) }.minOrNull()
    }
}
