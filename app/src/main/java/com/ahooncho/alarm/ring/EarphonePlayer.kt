package com.ahooncho.alarm.ring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRouting
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.ahooncho.alarm.R
import kotlin.math.roundToInt

/**
 * Plays the alarm sound through earphones and never through the phone's speaker.
 *
 * Android copies alarm-type audio to the speaker whenever a headset is connected, so the sound is
 * played as media instead, which only goes to the active earphone. On top of that:
 * - playback starts muted and becomes audible only once the player reports it is routed to an
 *   earphone;
 * - anything suggesting the route is moving away (Bluetooth dropping, unplugging, a route change)
 *   mutes and releases the player at once, and it is rebuilt when an earphone is back.
 *
 * [onOutputChanged] receives the earphone name while sound plays on it, or null once it is clear
 * that nothing can play right now.
 */
class EarphonePlayer(
    private val context: Context,
    private val volumePercent: Int,
    private val rampMillis: Long,
    private val onOutputChanged: (earphone: String?) -> Unit,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var running = false
    private var player: MediaPlayer? = null
    private var device: AudioDeviceInfo? = null
    private var attemptStartedAt = 0L
    private var routeConfirmedAt = 0L
    private var retryAt = 0L
    private var notPlayingSince = 0L
    private var reported = false
    private var lastReported: String? = null

    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false
    private var focusRetryAt = 0L

    private var savedVolume: Int? = null
    private var savedVolumeDevice: String? = null

    fun start() {
        if (running) return
        running = true
        context.registerReceiver(
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            Context.RECEIVER_NOT_EXPORTED,
        )
        audioManager.registerAudioDeviceCallback(deviceCallback, handler)
        requestFocus()
        handler.post(tick)
    }

    fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacksAndMessages(null)
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        context.unregisterReceiver(noisyReceiver)
        releasePlayer()
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        hasFocus = false
        restoreVolume()
    }

    private val tick = object : Runnable {
        override fun run() {
            reconcile()
            if (running) handler.postDelayed(this, TICK_MS)
        }
    }

    /** Brings the player in line with the current earphone and focus state. Safe to call anytime. */
    private fun reconcile() {
        if (!running) return
        val now = SystemClock.elapsedRealtime()
        if (!hasFocus && focusRetryAt != 0L && now >= focusRetryAt) requestFocus()
        val earphone = Earphones.find(audioManager)

        when {
            earphone == null || !hasFocus -> releasePlayer()
            player != null && device?.id == earphone.id -> checkRouteAndRamp(now)
            else -> {
                releasePlayer()
                if (now >= retryAt) startPlayback(earphone, now)
            }
        }

        val playing = player != null && routeConfirmedAt != 0L
        if (playing) {
            notPlayingSince = 0L
            report(device?.let(Earphones::displayName))
        } else {
            if (notPlayingSince == 0L) notPlayingSince = now
            if (earphone == null || now - notPlayingSince >= SILENT_GRACE_MS) report(null)
        }
    }

    private fun startPlayback(earphone: AudioDeviceInfo, now: Long) {
        val mp = try {
            createPlayer(earphone)
        } catch (e: Exception) {
            Log.e(TAG, "Could not prepare the alarm sound", e)
            retryAt = now + RETRY_MS
            return
        }
        applyVolume(earphone)
        player = mp
        device = earphone
        attemptStartedAt = now
        routeConfirmedAt = 0L
        mp.start()
    }

    private fun checkRouteAndRamp(now: Long) {
        val mp = player ?: return
        val routed = mp.routedDevice
        when {
            routed == null -> {
                if (now - attemptStartedAt > ROUTE_TIMEOUT_MS) giveUp(now, "no output route")
            }
            !Earphones.isEarphone(routed.type) -> giveUp(now, "routed to device type ${routed.type}")
            else -> {
                if (routeConfirmedAt == 0L) routeConfirmedAt = now
                val gain = Ramp.gain(now - routeConfirmedAt, rampMillis)
                mp.setVolume(gain, gain)
            }
        }
    }

    private fun giveUp(now: Long, reason: String) {
        Log.w(TAG, "Stopped playback: $reason")
        releasePlayer()
        retryAt = now + RETRY_MS
    }

    private fun muteNow() {
        player?.let { runCatching { it.setVolume(0f, 0f) } }
        routeConfirmedAt = 0L
    }

    private fun report(earphone: String?) {
        if (reported && earphone == lastReported) return
        reported = true
        lastReported = earphone
        onOutputChanged(earphone)
    }

    private fun createPlayer(earphone: AudioDeviceInfo): MediaPlayer {
        val alarmSound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        if (alarmSound != null) {
            try {
                return preparePlayer(earphone) { it.setDataSource(context, alarmSound) }
            } catch (e: Exception) {
                // Expected before the first unlock after a restart, when media files are locked.
                Log.w(TAG, "System alarm sound unavailable, using the built-in tone", e)
            }
        }
        return preparePlayer(earphone) { mp ->
            context.resources.openRawResourceFd(R.raw.fallback_tone).use { mp.setDataSource(it) }
        }
    }

    private fun preparePlayer(earphone: AudioDeviceInfo, setSource: (MediaPlayer) -> Unit): MediaPlayer {
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(attributes)
            mp.setPreferredDevice(earphone)
            mp.setVolume(0f, 0f)
            mp.isLooping = true
            setSource(mp)
            mp.prepare()
            mp.addOnRoutingChangedListener(routingListener, handler)
            mp.setOnErrorListener { failed, what, extra ->
                if (failed === player) giveUp(SystemClock.elapsedRealtime(), "player error $what/$extra")
                true
            }
            return mp
        } catch (e: Exception) {
            mp.release()
            throw e
        }
    }

    private fun releasePlayer() {
        val mp = player ?: return
        player = null
        device = null
        routeConfirmedAt = 0L
        runCatching { mp.setVolume(0f, 0f) }
        mp.removeOnRoutingChangedListener(routingListener)
        runCatching { mp.stop() }
        mp.release()
    }

    private fun applyVolume(earphone: AudioDeviceInfo) {
        val stream = AudioManager.STREAM_MUSIC
        if (savedVolume == null) {
            savedVolume = audioManager.getStreamVolume(stream)
            savedVolumeDevice = Earphones.key(earphone)
        }
        val max = audioManager.getStreamMaxVolume(stream)
        val min = audioManager.getStreamMinVolume(stream)
        val target = (max * volumePercent / 100f).roundToInt().coerceIn(min, max)
        runCatching { audioManager.setStreamVolume(stream, target, 0) }
            .onFailure { Log.w(TAG, "Could not set the media volume", it) }
    }

    private fun restoreVolume() {
        val volume = savedVolume ?: return
        savedVolume = null
        // Only while the same earphone is active; otherwise this would change another output's volume.
        val active = Earphones.find(audioManager)?.let(Earphones::key)
        if (active != null && active == savedVolumeDevice) {
            runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0) }
        }
    }

    private fun requestFocus() {
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(true)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(focusListener, handler)
            .build()
            .also { focusRequest = it }
        when (audioManager.requestAudioFocus(request)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                hasFocus = true
                focusRetryAt = 0L
            }
            // A call is in progress; the listener gets AUDIOFOCUS_GAIN when it ends.
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                hasFocus = false
                focusRetryAt = 0L
            }
            else -> {
                hasFocus = false
                focusRetryAt = SystemClock.elapsedRealtime() + FOCUS_RETRY_MS
            }
        }
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                focusRetryAt = 0L
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                loseFocus()
                focusRetryAt = SystemClock.elapsedRealtime() + FOCUS_RETRY_MS
            }
            // Transient losses (an incoming call) end with AUDIOFOCUS_GAIN.
            else -> loseFocus()
        }
        reconcile()
    }

    private fun loseFocus() {
        hasFocus = false
        muteNow()
        releasePlayer()
    }

    private val routingListener = AudioRouting.OnRoutingChangedListener { router ->
        val routed = router.routedDevice
        if (routed != null && !Earphones.isEarphone(routed.type) && router === player) {
            muteNow()
            giveUp(SystemClock.elapsedRealtime(), "route changed to device type ${routed.type}")
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
            // Sent just before Android moves media from a departing earphone to the speaker.
            muteNow()
            releasePlayer()
            retryAt = SystemClock.elapsedRealtime() + NOISY_SETTLE_MS
            reconcile()
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = reconcile()

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            if (removedDevices.orEmpty().any { it.id == device?.id }) {
                muteNow()
                releasePlayer()
            }
            reconcile()
        }
    }

    private companion object {
        const val TAG = "EarphonePlayer"
        const val TICK_MS = 200L
        const val ROUTE_TIMEOUT_MS = 3_000L
        const val RETRY_MS = 2_000L
        const val NOISY_SETTLE_MS = 1_500L
        const val SILENT_GRACE_MS = 2_500L
        const val FOCUS_RETRY_MS = 3_000L
    }
}
