package com.ahooncho.alarm.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

/**
 * Keeps all alarms and settings in one JSON file.
 *
 * The file lives in device-protected storage so that alarms can be read and rescheduled after a
 * restart, before the phone has been unlocked for the first time.
 */
class AlarmStore(context: Context) {
    private val file = AtomicFile(
        File(context.createDeviceProtectedStorageContext().filesDir, "alarms.json"),
    )
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()
    private val state = MutableStateFlow(load())

    val data: StateFlow<AppData> = state.asStateFlow()

    fun update(transform: (AppData) -> AppData): AppData = synchronized(lock) {
        val current = state.value
        val next = transform(current)
        if (next != current) {
            write(next)
            state.value = next
        }
        next
    }

    private fun load(): AppData {
        val text = try {
            file.readFully().decodeToString()
        } catch (e: FileNotFoundException) {
            return AppData()
        }
        return try {
            json.decodeFromString(AppData.serializer(), text)
        } catch (e: Exception) {
            Log.e(TAG, "Unreadable alarm file, keeping a copy and starting over", e)
            file.baseFile.copyTo(File(file.baseFile.path + ".bad"), overwrite = true)
            AppData()
        }
    }

    private fun write(data: AppData) {
        val out = file.startWrite()
        try {
            out.write(json.encodeToString(AppData.serializer(), data).encodeToByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    private companion object {
        const val TAG = "AlarmStore"
    }
}
