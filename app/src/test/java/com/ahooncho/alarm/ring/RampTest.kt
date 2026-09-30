package com.ahooncho.alarm.ring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RampTest {
    @Test
    fun startsBarelyAudibleAndEndsAtFullVolume() {
        assertEquals(0.01f, Ramp.gain(0, 30_000), 1e-6f)
        assertEquals(1f, Ramp.gain(30_000, 30_000), 1e-6f)
        assertEquals(1f, Ramp.gain(90_000, 30_000), 1e-6f)
    }

    @Test
    fun risesSteadily() {
        val gains = (0..30).map { Ramp.gain(it * 1_000L, 30_000) }
        assertTrue(gains.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun noRampMeansFullVolume() {
        assertEquals(1f, Ramp.gain(0, 0), 0f)
    }
}
