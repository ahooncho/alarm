package com.ahooncho.alarm.ring

import android.media.AudioDeviceInfo
import android.media.AudioManager

object Earphones {
    /** Outputs that sit in or on the ears, most preferred first. */
    private val TYPES = listOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    )

    fun isEarphone(type: Int): Boolean = type in TYPES

    /** The connected earphone the alarm should play on, if any. */
    fun find(audioManager: AudioManager): AudioDeviceInfo? =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { isEarphone(it.type) }
            .minByOrNull { TYPES.indexOf(it.type) }

    fun displayName(device: AudioDeviceInfo): String =
        device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "이어폰"

    /** Identifies the same physical earphone across reconnects, which hand out new device ids. */
    fun key(device: AudioDeviceInfo): String = "${device.type}:${device.address}"
}
