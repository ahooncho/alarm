package com.ahooncho.alarm

import android.app.Application
import android.content.Context
import com.ahooncho.alarm.data.AlarmRepository
import com.ahooncho.alarm.data.AlarmStore
import com.ahooncho.alarm.ring.Notifications
import com.ahooncho.alarm.schedule.AlarmScheduler

class AlarmApplication : Application() {
    lateinit var repository: AlarmRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AlarmRepository(AlarmStore(this), AlarmScheduler(this))
        Notifications.createChannels(this)
    }
}

val Context.alarmApp: AlarmApplication
    get() = applicationContext as AlarmApplication
