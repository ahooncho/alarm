package com.ahooncho.alarm.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeTextTest {
    @Test
    fun clockIn24And12HourFormats() {
        assertEquals("06:05" to null, TimeText.clock(6, 5, is24Hour = true))
        assertEquals("6:05" to "오전", TimeText.clock(6, 5, is24Hour = false))
        assertEquals("12:00" to "오전", TimeText.clock(0, 0, is24Hour = false))
        assertEquals("12:30" to "오후", TimeText.clock(12, 30, is24Hour = false))
        assertEquals("오후 11:59", TimeText.clockLine(23, 59, is24Hour = false))
    }

    @Test
    fun daySummaries() {
        assertEquals("한 번", TimeText.days(emptySet()))
        assertEquals("매일", TimeText.days((1..7).toSet()))
        assertEquals("주중", TimeText.days(setOf(1, 2, 3, 4, 5)))
        assertEquals("주말", TimeText.days(setOf(6, 7)))
        assertEquals("월 수 금", TimeText.days(setOf(5, 1, 3)))
    }

    @Test
    fun timeUntilRoundsUpToTheMinute() {
        val minute = 60_000L
        assertEquals("8시간 12분 후", TimeText.until(0, (8 * 60 + 12) * minute))
        assertEquals("1분 후", TimeText.until(0, 1_000))
        assertEquals("1일 2시간 후", TimeText.until(0, (26 * 60) * minute))
    }

    @Test
    fun relativeDays() {
        val seoul = ZoneId.of("Asia/Seoul")
        fun millis(day: Int, hour: Int) = ZonedDateTime.of(2026, 9, day, hour, 0, 0, 0, seoul).toInstant().toEpochMilli()
        val now = millis(29, 23)
        assertEquals("오늘", TimeText.relativeDay(millis(29, 23), now, seoul))
        assertEquals("내일", TimeText.relativeDay(millis(30, 6), now, seoul))
        assertEquals("10월 3일 (토)", TimeText.relativeDay(
            ZonedDateTime.of(2026, 10, 3, 6, 0, 0, 0, seoul).toInstant().toEpochMilli(), now, seoul,
        ))
    }
}
