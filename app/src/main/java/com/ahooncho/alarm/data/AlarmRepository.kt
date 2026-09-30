package com.ahooncho.alarm.data

import com.ahooncho.alarm.schedule.AlarmScheduler
import kotlinx.coroutines.flow.StateFlow

/** Every change to alarms goes through here so AlarmManager always matches what is stored. */
class AlarmRepository(
    private val store: AlarmStore,
    private val scheduler: AlarmScheduler,
) {
    val data: StateFlow<AppData> get() = store.data

    fun alarm(id: Int): Alarm? = store.data.value.alarms.firstOrNull { it.id == id }

    fun addAlarm(hour: Int, minute: Int, days: Set<Int>) = mutate { data ->
        val alarm = Alarm(id = data.nextId, hour = hour, minute = minute, days = days.cleaned())
        data.copy(alarms = data.alarms + alarm, nextId = data.nextId + 1)
    }

    fun saveAlarm(id: Int, hour: Int, minute: Int, days: Set<Int>) = mutate { data ->
        data.copy(alarms = data.alarms.map {
            if (it.id == id) {
                it.copy(hour = hour, minute = minute, days = days.cleaned(), enabled = true, snoozeUntil = null)
            } else {
                it
            }
        })
    }

    fun setEnabled(id: Int, enabled: Boolean) = mutate { data ->
        data.copy(alarms = data.alarms.map {
            if (it.id == id) it.copy(enabled = enabled, snoozeUntil = null) else it
        })
    }

    fun deleteAlarm(id: Int) {
        scheduler.cancel(id)
        mutate { data -> data.copy(alarms = data.alarms.filterNot { it.id == id }) }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        store.update { it.copy(settings = transform(it.settings)) }
    }

    fun snooze(ids: Set<Int>, until: Long) = mutate { data ->
        data.copy(alarms = data.alarms.map { if (it.id in ids) it.copy(snoozeUntil = until) else it })
    }

    /** Clears pending snoozes; one-time alarms are switched off once dismissed. */
    fun dismiss(ids: Set<Int>) = mutate { data ->
        data.copy(alarms = data.alarms.map {
            if (it.id in ids) it.copy(snoozeUntil = null, enabled = it.enabled && it.days.isNotEmpty()) else it
        })
    }

    fun resync(notBefore: Long = 0L) = scheduler.sync(store.data.value.alarms, notBefore)

    fun scheduleTest(triggerAt: Long) = scheduler.scheduleTest(triggerAt)

    private fun mutate(transform: (AppData) -> AppData) {
        scheduler.sync(store.update(transform).alarms)
    }

    private fun Set<Int>.cleaned(): Set<Int> = filterTo(sortedSetOf()) { it in 1..7 }
}
