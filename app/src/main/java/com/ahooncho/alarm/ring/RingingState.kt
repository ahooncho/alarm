package com.ahooncho.alarm.ring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What [AlarmService] is doing right now, for the ringing screen. Null when nothing rings. */
object RingingState {
    data class Ringing(
        val triggerAt: Long,
        val isTest: Boolean,
        /** Earphone the sound is playing on; null while only the phone vibrates. */
        val earphone: String? = null,
        /** False until it is known whether an earphone can play. */
        val outputKnown: Boolean = false,
        /** Repeat summary of the ringing alarm, e.g. "평일". */
        val repeatText: String = "",
        val snoozeMinutes: Int = 5,
        val holdToDismissMillis: Int = 1_200,
        val volumePercent: Int = 40,
        val rampMillis: Long = 30_000,
        /** elapsedRealtime when sound started on the current earphone; drives the volume bar. */
        val soundingSince: Long? = null,
    )

    private val state = MutableStateFlow<Ringing?>(null)

    val current: StateFlow<Ringing?> = state.asStateFlow()

    internal fun set(value: Ringing?) {
        state.value = value
    }

    internal fun update(transform: (Ringing) -> Ringing) {
        state.update { it?.let(transform) }
    }
}
