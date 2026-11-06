package com.ahooncho.alarm.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ahooncho.alarm.ring.RingingState
import com.ahooncho.alarm.ui.components.BaselinePair
import com.ahooncho.alarm.ui.components.BaselineStack
import com.ahooncho.alarm.ui.components.DisplayNumerals
import com.ahooncho.alarm.ui.components.EdgeText
import com.ahooncho.alarm.ui.components.Rule
import com.ahooncho.alarm.ui.theme.Theme
import com.ahooncho.alarm.ui.theme.Type
import com.ahooncho.alarm.util.TimeText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Shown over the lock screen while an alarm rings. Always dark, and the figures are outlined, so the
 * screen lights up as few pixels as possible in a shared room. Two choices only, always in the same
 * place: a large tap target to snooze and a held one to dismiss.
 */
@Composable
fun RingingScreen(
    state: RingingState.Ringing,
    nowMillis: Long,
    is24Hour: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = Theme.palette
    val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
    val (clock, marker) = TimeText.clock(now.hour, now.minute, is24Hour)
    val (hours, minutes) = clock.split(":")
    val snoozeAt = now.plusMinutes(state.snoozeMinutes.toLong())

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(40.dp)) {
            BaselinePair(baseline = 28.dp, height = 40.dp, modifier = Modifier.fillMaxWidth(), start = {
                EdgeText(marker ?: if (state.isTest) "테스트 알람" else "알람", Type.caption, p.muted)
            }, end = {
                EdgeText(state.repeatText, Type.caption, p.muted, end = true)
            })
            BaselineStack(first = 28.dp, modifier = Modifier.align(Alignment.TopCenter)) {
                EdgeText(TimeText.date(nowMillis), Type.caption, p.muted)
            }
        }
        Rule(2.dp)
        Spacer(Modifier.height(12.dp))
        DisplayNumerals(
            lines = listOf(hours, minutes),
            color = p.fg,
            contentDescription = TimeText.clockLine(now.hour, now.minute, is24Hour),
            modifier = Modifier.fillMaxWidth(),
            outline = 2.dp,
        )
        Spacer(Modifier.weight(1f))
        Rule(1.dp, p.line)
        Output(state)
        Box(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .border(2.dp, p.fg)
                .clickable(onClickLabel = "다시 알림", onClick = onSnooze),
        ) {
            BaselineStack(first = 44.dp, steps = listOf(48.dp), modifier = Modifier.padding(start = 20.dp)) {
                EdgeText("다시 알림", Type.display, p.fg)
                EdgeText(
                    "${state.snoozeMinutes}분 뒤 ${TimeText.clockLine(snoozeAt.hour, snoozeAt.minute, is24Hour)}에 다시 울려요",
                    Type.body, p.muted,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        HoldToDismiss(state.holdToDismissMillis, onDismiss)
        Spacer(Modifier.height(28.dp))
    }
}

/** Which earphone is ringing and how far the volume has ramped, or that only the phone vibrates. */
@Composable
private fun Output(state: RingingState.Ringing) {
    val p = Theme.palette
    val elapsed by produceState(SystemClock.elapsedRealtime(), state.soundingSince) {
        while (true) {
            value = SystemClock.elapsedRealtime()
            delay(100)
        }
    }
    val vibrateOnly = state.outputKnown && state.earphone == null
    val name = when {
        !state.outputKnown -> "이어폰 확인 중"
        state.earphone != null -> state.earphone
        else -> "이어폰 연결 안 됨"
    }
    BaselinePair(baseline = 27.dp, height = 39.dp, modifier = Modifier.fillMaxWidth(), start = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .offset(y = (-1).dp)
                    .size(8.dp)
                    .then(if (vibrateOnly) Modifier.border(1.dp, p.fg) else Modifier.background(p.fg)),
            )
            Spacer(Modifier.width(8.dp))
            EdgeText(name, Type.titleBold, p.fg)
        }
    }, end = {
        if (state.outputKnown) EdgeText(if (vibrateOnly) "진동만 울려요" else "볼륨 ${state.volumePercent}%", Type.caption, p.muted, end = true)
    })
    if (vibrateOnly) {
        BaselineStack(first = 8.dp, bottom = 13.dp, modifier = Modifier.fillMaxWidth()) {
            EdgeText("연결하면 이어폰으로 이어서 울려요", Type.caption, p.muted)
        }
    } else {
        val since = state.soundingSince
        val progress = when {
            since == null -> 0f
            state.rampMillis <= 0L -> 1f
            else -> ((elapsed - since).toFloat() / state.rampMillis).coerceIn(0f, 1f)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(p.line)) {
            Box(Modifier.fillMaxWidth(progress).height(2.dp).background(p.fg))
        }
        Spacer(Modifier.height(18.dp))
    }
}

/**
 * Dismiss needs a held press so a sleepy tap cannot end the alarm; the bar along the bottom fills
 * while it is held. With [holdMillis] 0 a tap dismisses. Accessibility services get a plain click.
 */
@Composable
private fun HoldToDismiss(holdMillis: Int, onDismiss: () -> Unit) {
    val p = Theme.palette
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val dismiss by rememberUpdatedState(onDismiss)
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(1.dp, p.fg)
            .semantics { onClick(label = "해제") { dismiss(); true } }
            .pointerInput(holdMillis) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (holdMillis <= 0) {
                        if (waitForUpOrCancellation() != null) dismiss()
                        return@awaitEachGesture
                    }
                    var done = false
                    val job: Job = scope.launch {
                        progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                        done = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dismiss()
                    }
                    waitForUpOrCancellation()
                    if (!done) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(150)) }
                    }
                }
            },
    ) {
        BaselinePair(baseline = 38.dp, height = 60.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), start = {
            EdgeText("해제", Type.headline, p.fg)
        }, end = {
            if (holdMillis > 0) EdgeText("길게 누르세요", Type.body, p.muted, end = true)
        })
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(p.line)) {
            Box(Modifier.fillMaxWidth(progress.value).height(4.dp).background(p.fg))
        }
    }
}
