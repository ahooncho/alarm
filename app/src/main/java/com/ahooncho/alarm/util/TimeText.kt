package com.ahooncho.alarm.util

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object TimeText {
    private val DAY_NAMES = listOf("월", "화", "수", "목", "금", "토", "일")

    fun dayName(isoDay: Int): String = DAY_NAMES[isoDay - 1]

    /** Full weekday name, e.g. "토요일". */
    fun weekday(isoDay: Int): String = dayName(isoDay) + "요일"

    /** Clock text and an optional 오전/오후 marker, e.g. ("6:00", "오전") or ("06:00", null). */
    fun clock(hour: Int, minute: Int, is24Hour: Boolean): Pair<String, String?> {
        val mm = minute.toString().padStart(2, '0')
        if (is24Hour) return "${hour.toString().padStart(2, '0')}:$mm" to null
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        return "$h12:$mm" to if (hour < 12) "오전" else "오후"
    }

    fun clockLine(hour: Int, minute: Int, is24Hour: Boolean): String {
        val (main, marker) = clock(hour, minute, is24Hour)
        return if (marker == null) main else "$marker $main"
    }

    /** Repeat summary in the words Korean alarm apps use. */
    fun days(days: Set<Int>): String = when {
        days.isEmpty() -> "한 번"
        days.size == 7 -> "매일"
        days == setOf(1, 2, 3, 4, 5) -> "평일"
        days == setOf(6, 7) -> "주말"
        else -> days.sorted().joinToString(" ") { dayName(it) }
    }

    /** "8시간 12분 후", rounded up to the minute. */
    fun until(fromMillis: Long, toMillis: Long): String {
        val totalMinutes = ((toMillis - fromMillis + 59_999) / 60_000).coerceAtLeast(0)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        val parts = buildList {
            if (days > 0) add("${days}일")
            if (hours > 0) add("${hours}시간")
            if (minutes > 0 || isEmpty()) add("${minutes}분")
        }
        return parts.joinToString(" ") + " 후"
    }

    /** "오늘", "내일", "모레" or "10월 3일 (토)" for the day of [targetMillis]. */
    fun relativeDay(targetMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val target = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return when (ChronoUnit.DAYS.between(today, target)) {
            0L -> "오늘"
            1L -> "내일"
            2L -> "모레"
            else -> "${target.monthValue}월 ${target.dayOfMonth}일 (${dayName(target.dayOfWeek.value)})"
        }
    }

    /** "내일 토요일", or "11월 10일 화요일" beyond the day after tomorrow. */
    fun dayLine(targetMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val target = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val weekday = weekday(target.dayOfWeek.value)
        return when (ChronoUnit.DAYS.between(today, target)) {
            0L -> "오늘 $weekday"
            1L -> "내일 $weekday"
            2L -> "모레 $weekday"
            else -> "${target.monthValue}월 ${target.dayOfMonth}일 $weekday"
        }
    }

    /** "11월 7일 토요일". */
    fun date(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val d = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return "${d.monthValue}월 ${d.dayOfMonth}일 ${weekday(d.dayOfWeek.value)}"
    }

    /** "1.2초", "0초", "2초". */
    fun seconds(millis: Int): String {
        val tenths = (millis + 50) / 100
        return if (tenths % 10 == 0) "${tenths / 10}초" else "${tenths / 10}.${tenths % 10}초"
    }
}
