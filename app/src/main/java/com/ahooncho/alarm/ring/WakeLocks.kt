package com.ahooncho.alarm.ring

import android.content.Context
import android.os.PowerManager

/** Keeps the CPU awake from the alarm broadcast until [AlarmService] holds its own wake lock. */
object WakeLocks {
    private const val HANDOFF_TIMEOUT_MS = 30_000L
    private var handoff: PowerManager.WakeLock? = null

    @Synchronized
    fun acquireHandoff(context: Context) {
        val lock = handoff ?: context.applicationContext.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alarm:handoff")
            .apply { setReferenceCounted(false) }
            .also { handoff = it }
        lock.acquire(HANDOFF_TIMEOUT_MS)
    }

    @Synchronized
    fun releaseHandoff() {
        handoff?.let { if (it.isHeld) it.release() }
    }
}
