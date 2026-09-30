package com.ahooncho.alarm.ring

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager

/** The only fallback when no earphone can play: the phone vibrates, it never makes a sound. */
class AlarmVibrator(context: Context) {
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private var vibrating = false

    fun start() {
        if (vibrating) return
        vibrating = true
        vibrator.vibrate(
            VibrationEffect.createWaveform(PATTERN, 0),
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
        )
    }

    fun stop() {
        if (!vibrating) return
        vibrating = false
        vibrator.cancel()
    }

    private companion object {
        /** Off/on durations in ms, repeated from the start. */
        val PATTERN = longArrayOf(0, 800, 600)
    }
}
