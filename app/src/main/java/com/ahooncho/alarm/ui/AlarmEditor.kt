package com.ahooncho.alarm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahooncho.alarm.schedule.AlarmTime
import com.ahooncho.alarm.ui.components.BaselinePair
import com.ahooncho.alarm.ui.components.BaselineStack
import com.ahooncho.alarm.ui.components.BottomBar
import com.ahooncho.alarm.ui.components.DisplayNumerals
import com.ahooncho.alarm.ui.components.EdgeText
import com.ahooncho.alarm.ui.components.NumeralAlign
import com.ahooncho.alarm.ui.components.Rule
import com.ahooncho.alarm.ui.theme.Theme
import com.ahooncho.alarm.ui.theme.Type
import com.ahooncho.alarm.util.TimeText
import java.time.Instant
import java.time.ZoneId

private val PRESETS = listOf(
    "매일" to (1..7).toSet(),
    "평일" to (1..5).toSet(),
    "주말" to setOf(6, 7),
    "반복 안 함" to emptySet(),
)

/** Full-screen editor. Times are picked on two drums; vertical drags step them, taps on the dim values too. */
@Composable
fun AlarmEditorScreen(
    initialHour: Int,
    initialMinute: Int,
    initialDays: Set<Int>,
    is24Hour: Boolean,
    isNew: Boolean,
    nowMillis: Long,
    onSave: (hour: Int, minute: Int, days: Set<Int>) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    val p = Theme.palette
    var hour by rememberSaveable { mutableIntStateOf(initialHour) }
    var minute by rememberSaveable { mutableIntStateOf(initialMinute) }
    var days by remember { mutableStateOf(initialDays) }
    BackHandler(onBack = onCancel)

    val next = remember(hour, minute, days, nowMillis) {
        val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
        AlarmTime.nextOccurrence(hour, minute, days, now).toInstant().toEpochMilli()
    }

    Column(Modifier.fillMaxSize().background(p.bg)) {
        Column(
            Modifier
                .weight(1f)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            TopBar(
                start = { EdgeText("취소", Type.title, p.fg, Modifier.clickable(onClick = onCancel).padding(vertical = 12.dp)) },
                end = {
                    if (!isNew) EdgeText("삭제", Type.title, p.fg, Modifier.clickable(onClick = onDelete).padding(vertical = 12.dp), end = true)
                },
            )
            Rule(4.dp)
            // Drum band: dim value 52, rule, current 118, rule, dim value 48.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(222.dp)
                    .drawBehind {
                        val w = 2.dp.toPx()
                        for (y in listOf(52.dp.toPx(), 172.dp.toPx())) drawRect(p.fg, Offset(0f, y), Size(size.width, w))
                    },
            ) {
                Row(Modifier.fillMaxSize()) {
                    Drum(hour, 24, "시", { hour = it }, Modifier.weight(1f))
                    Box(Modifier.width(16.dp).fillMaxHeight()) {
                        Box(Modifier.padding(top = 54.dp).height(118.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            DisplayNumerals(listOf(":"), p.fg, "", fontSize = 108.sp, align = NumeralAlign.Center)
                        }
                    }
                    Drum(minute, 60, "분", { minute = it }, Modifier.weight(1f))
                }
            }
            BaselineStack(first = 26.dp, bottom = 16.dp, modifier = Modifier.fillMaxWidth()) {
                EdgeText(TimeText.until(nowMillis, next) + "에 울려요", Type.body, p.fg)
            }
            Rule(4.dp)
            BaselinePair(baseline = 28.dp, height = 40.dp, modifier = Modifier.fillMaxWidth(), start = {
                EdgeText("반복", Type.bodyBold, p.fg)
            }, end = {
                EdgeText(TimeText.days(days), Type.body, p.muted, end = true)
            })
            Rule(1.dp, p.line)
            Spacer(Modifier.height(15.dp))
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (day in 1..7) {
                    val selected = day in days
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (selected) p.fg else p.bg)
                            .border(2.dp, p.fg)
                            .toggleable(selected, role = Role.Checkbox) { days = if (it) days + day else days - day },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(TimeText.dayName(day), style = if (selected) Type.bodyBold else Type.body, color = if (selected) p.bg else p.fg)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(48.dp).border(2.dp, p.fg)) {
                for ((label, set) in PRESETS) {
                    val selected = days == set
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (selected) p.fg else p.bg)
                            .selectable(selected, role = Role.RadioButton) { days = set },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = if (selected) Type.bodyBold else Type.body, color = if (selected) p.bg else p.fg, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        BottomBar("저장", onClick = { onSave(hour, minute, days) }) {
            EdgeText("${TimeText.days(days)} ${TimeText.clockLine(hour, minute, is24Hour)}", Type.body, p.onBlock, end = true)
        }
    }
}

/** One drum column. Dragging up by 40 dp advances the value by one; tapping a dim value moves to it. */
@Composable
private fun Drum(value: Int, count: Int, unit: String, onChange: (Int) -> Unit, modifier: Modifier) {
    val p = Theme.palette
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val step = with(LocalDensity.current) { 40.dp.toPx() }
    fun wrap(v: Int) = ((v % count) + count) % count
    fun label(v: Int) = wrap(v).toString().padStart(2, '0')
    Column(
        modifier
            .fillMaxHeight()
            .pointerInput(count) {
                var acc = 0f
                var v = 0
                detectVerticalDragGestures(
                    onDragStart = { acc = 0f; v = current },
                    onVerticalDrag = { pointer, dy ->
                        pointer.consume()
                        acc += dy
                        while (acc <= -step) { acc += step; v = wrap(v + 1); change(v) }
                        while (acc >= step) { acc -= step; v = wrap(v - 1); change(v) }
                    },
                )
            }
            .semantics {
                contentDescription = "$value$unit"
                customActions = listOf(
                    CustomAccessibilityAction("늘리기") { change(wrap(current + 1)); true },
                    CustomAccessibilityAction("줄이기") { change(wrap(current - 1)); true },
                )
            },
    ) {
        Box(Modifier.fillMaxWidth().height(52.dp).clickable { change(wrap(current - 1)) }, contentAlignment = Alignment.Center) {
            DisplayNumerals(listOf(label(value - 1)), p.subtle, "", fontSize = 32.sp, weight = FontWeight.Medium, align = NumeralAlign.Center)
        }
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().height(118.dp), contentAlignment = Alignment.Center) {
            DisplayNumerals(listOf(label(value)), p.fg, "$value$unit", fontSize = 108.sp, align = NumeralAlign.Center)
        }
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().height(48.dp).clickable { change(wrap(current + 1)) }, contentAlignment = Alignment.Center) {
            DisplayNumerals(listOf(label(value + 1)), p.subtle, "", fontSize = 32.sp, weight = FontWeight.Medium, align = NumeralAlign.Center)
        }
    }
}
