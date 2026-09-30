package com.ahooncho.alarm.schedule

import com.ahooncho.alarm.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTimeTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    /** 2026-09-30 is a Wednesday. */
    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 9, day, hour, minute, second, 0, seoul)

    @Test
    fun oneTimeLaterToday() {
        assertEquals(at(30, 6, 0), AlarmTime.nextOccurrence(6, 0, emptySet(), at(30, 5, 59)))
    }

    @Test
    fun oneTimeAlreadyPassedMovesToTomorrow() {
        assertEquals(
            ZonedDateTime.of(2026, 10, 1, 6, 0, 0, 0, seoul),
            AlarmTime.nextOccurrence(6, 0, emptySet(), at(30, 6, 0, 1)),
        )
    }

    @Test
    fun exactlyNowIsNotNext() {
        assertEquals(
            ZonedDateTime.of(2026, 10, 1, 6, 0, 0, 0, seoul),
            AlarmTime.nextOccurrence(6, 0, emptySet(), at(30, 6, 0)),
        )
    }

    @Test
    fun weekdaysFromFridayEveningGoToMonday() {
        val friday = ZonedDateTime.of(2026, 10, 2, 22, 0, 0, 0, seoul)
        assertEquals(
            ZonedDateTime.of(2026, 10, 5, 6, 0, 0, 0, seoul),
            AlarmTime.nextOccurrence(6, 0, setOf(1, 2, 3, 4, 5), friday),
        )
    }

    @Test
    fun sameWeekdayAfterItsTimeWaitsAWeek() {
        // Wednesday 07:00, alarm on Wednesdays at 06:00.
        assertEquals(at(30, 6, 0).plusWeeks(1), AlarmTime.nextOccurrence(6, 0, setOf(3), at(30, 7, 0)))
    }

    @Test
    fun disabledAlarmDoesNotRing() {
        val alarm = Alarm(id = 1, hour = 6, minute = 0, enabled = false)
        assertNull(AlarmTime.nextTriggerMillis(alarm, at(30, 5, 0)))
    }

    @Test
    fun pendingSnoozeWinsOverNextOccurrence() {
        val snoozeUntil = at(30, 6, 5).toInstant().toEpochMilli()
        val alarm = Alarm(id = 1, hour = 6, minute = 0, days = setOf(3), snoozeUntil = snoozeUntil)
        assertEquals(snoozeUntil, AlarmTime.nextTriggerMillis(alarm, at(30, 6, 1)))
    }

    @Test
    fun passedSnoozeIsIgnored() {
        val alarm = Alarm(id = 1, hour = 6, minute = 0, snoozeUntil = at(30, 6, 5).toInstant().toEpochMilli())
        assertEquals(
            ZonedDateTime.of(2026, 10, 1, 6, 0, 0, 0, seoul).toInstant().toEpochMilli(),
            AlarmTime.nextTriggerMillis(alarm, at(30, 6, 10)),
        )
    }

    @Test
    fun nextOfPicksTheEarliestEnabledAlarm() {
        val now = at(30, 5, 0)
        val alarms = listOf(
            Alarm(id = 1, hour = 7, minute = 0),
            Alarm(id = 2, hour = 6, minute = 0),
            Alarm(id = 3, hour = 5, minute = 30, enabled = false),
        )
        assertEquals(at(30, 6, 0).toInstant().toEpochMilli(), AlarmTime.nextOf(alarms, now.toInstant().toEpochMilli(), seoul))
    }
}
