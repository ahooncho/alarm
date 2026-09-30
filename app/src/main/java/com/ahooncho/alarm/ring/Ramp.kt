package com.ahooncho.alarm.ring

import kotlin.math.pow

object Ramp {
    /**
     * Player gain [elapsedMillis] into a ramp of [rampMillis]: linear in decibels from -40 dB to
     * 0 dB, which the ear hears as an even rise.
     */
    fun gain(elapsedMillis: Long, rampMillis: Long): Float {
        if (rampMillis <= 0L) return 1f
        val progress = (elapsedMillis.toFloat() / rampMillis).coerceIn(0f, 1f)
        return 10f.pow(2f * (progress - 1f))
    }
}
