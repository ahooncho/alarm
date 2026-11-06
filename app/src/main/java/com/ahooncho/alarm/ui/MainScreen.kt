package com.ahooncho.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahooncho.alarm.data.Alarm
import com.ahooncho.alarm.schedule.AlarmTime
import com.ahooncho.alarm.ui.components.BaselinePair
import com.ahooncho.alarm.ui.components.BaselineStack
import com.ahooncho.alarm.ui.components.BottomBar
import com.ahooncho.alarm.ui.components.DisplayNumerals
import com.ahooncho.alarm.ui.components.EdgeText
import com.ahooncho.alarm.ui.components.LineIcon
import com.ahooncho.alarm.ui.components.LineIconView
import com.ahooncho.alarm.ui.components.Numerals
import com.ahooncho.alarm.ui.components.Rule
import com.ahooncho.alarm.ui.components.SquareSwitch
import com.ahooncho.alarm.ui.theme.Theme
import com.ahooncho.alarm.ui.theme.Type
import com.ahooncho.alarm.util.TimeText
import java.time.Instant
import java.time.ZoneId

/** App bar: 48 dp, both labels on a baseline 30 dp below its top. */
@Composable
fun TopBar(start: @Composable () -> Unit, end: @Composable () -> Unit = {}) {
    BaselinePair(baseline = 30.dp, height = 48.dp, modifier = Modifier.fillMaxWidth(), start = start, end = end)
}

@Composable
fun MainScreen(
    alarms: List<Alarm>,
    nowMillis: Long,
    nextAlarmAt: Long?,
    is24Hour: Boolean,
    earphone: String?,
    problems: Int,
    onToggle: (Alarm, Boolean) -> Unit,
    onEdit: (Alarm) -> Unit,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val p = Theme.palette
    Column(Modifier.fillMaxSize().background(p.bg)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().statusBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            item {
                Column {
                    TopBar(
                        start = { EdgeText("알람", Type.titleBold, p.fg) },
                        end = {
                            EdgeText("설정", Type.title, p.fg, Modifier.clickable(onClick = onOpenSettings).padding(vertical = 12.dp), end = true)
                        },
                    )
                    Rule(4.dp)
                }
            }
            item { Hero(nextAlarmAt, is24Hour) }
            item { Meta(nextAlarmAt, nowMillis, is24Hour, earphone, problems, onOpenSettings) }
            item {
                Column {
                    BaselineStack(first = 27.dp, bottom = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        EdgeText("알람 ${alarms.size}개", Type.bodyBold, p.fg)
                    }
                    Rule(4.dp)
                }
            }
            if (alarms.isEmpty()) {
                item {
                    BaselineStack(first = 32.dp, steps = listOf(24.dp), modifier = Modifier.fillMaxWidth()) {
                        EdgeText("알람이 없어요", Type.titleBold, p.fg)
                        EdgeText("‘알람 추가’를 눌러 만들어 보세요.", Type.body, p.muted)
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmRow(alarm, nowMillis, is24Hour, onToggle = { onToggle(alarm, it) }, onClick = { onEdit(alarm) })
                }
            }
        }
        BottomBar("알람 추가", onClick = onAdd) {
            LineIconView(LineIcon.Plus, p.onBlock, 20.dp)
        }
    }
}

@Composable
private fun Hero(nextAlarmAt: Long?, is24Hour: Boolean) {
    val p = Theme.palette
    // Rule bottom to the figures' cap line is 16 dp; the drawing box already includes the overshoot.
    val gap = 16.dp - (115 * Numerals.OVERSHOOT).dp
    Column {
        Spacer(Modifier.height(gap))
        if (nextAlarmAt == null) {
            DisplayNumerals(listOf("00:00"), p.subtle, "다음 알람 없음", Modifier.fillMaxWidth(), outline = 2.dp)
        } else {
            val time = Instant.ofEpochMilli(nextAlarmAt).atZone(ZoneId.systemDefault())
            val (clock, _) = TimeText.clock(time.hour, time.minute, is24Hour)
            DisplayNumerals(
                listOf(clock), p.fg, "다음 알람 ${TimeText.clockLine(time.hour, time.minute, is24Hour)}",
                Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(gap))
        Rule(1.dp)
    }
}

@Composable
private fun Meta(nextAlarmAt: Long?, nowMillis: Long, is24Hour: Boolean, earphone: String?, problems: Int, onOpenSettings: () -> Unit) {
    val p = Theme.palette
    Column {
        Row(Modifier.fillMaxWidth()) {
            BaselineStack(first = 23.dp, steps = listOf(24.dp, 20.dp), bottom = 16.dp, modifier = Modifier.weight(1f)) {
                EdgeText("다음 알람", Type.caption, p.muted)
                if (nextAlarmAt == null) {
                    EdgeText("없음", Type.titleBold, p.fg)
                } else {
                    val time = Instant.ofEpochMilli(nextAlarmAt).atZone(ZoneId.systemDefault())
                    val marker = TimeText.clock(time.hour, time.minute, is24Hour).second
                    EdgeText(TimeText.dayLine(nextAlarmAt, nowMillis) + (marker?.let { " $it" } ?: ""), Type.titleBold, p.fg)
                    EdgeText(TimeText.until(nowMillis, nextAlarmAt) + "에 울려요", Type.caption, p.muted)
                }
            }
            Spacer(Modifier.width(16.dp))
            BaselineStack(first = 23.dp, steps = listOf(24.dp, 20.dp), bottom = 16.dp, modifier = Modifier.weight(1f)) {
                EdgeText("이어폰", Type.caption, p.muted)
                EdgeText(earphone ?: "연결 안 됨", Type.titleBold, p.fg)
                when {
                    problems > 0 -> EdgeText("설정 ${problems}개 확인 필요", Type.captionBold, p.fg, Modifier.clickable(onClick = onOpenSettings))
                    earphone == null -> EdgeText("진동만 울려요", Type.caption, p.muted)
                    else -> EdgeText("연결됨", Type.caption, p.muted)
                }
            }
        }
        Rule(1.dp)
    }
}

/** One alarm, 72 dp: figures with their cap line at 24 and baseline at 48, repeat column on grid column 3. */
@Composable
private fun AlarmRow(alarm: Alarm, nowMillis: Long, is24Hour: Boolean, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val p = Theme.palette
    val on = alarm.enabled
    val (clock, marker) = TimeText.clock(alarm.hour, alarm.minute, is24Hour)
    val next = when {
        !on -> "꺼짐"
        alarm.snoozeUntil != null && alarm.snoozeUntil > nowMillis -> {
            val t = Instant.ofEpochMilli(alarm.snoozeUntil).atZone(ZoneId.systemDefault())
            "${TimeText.clockLine(t.hour, t.minute, is24Hour)}에 다시 알림"
        }
        else -> AlarmTime.nextTriggerMillis(alarm, Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()))
            ?.let { TimeText.until(nowMillis, it) }.orEmpty()
    }
    val repeat = TimeText.days(alarm.days) + (marker?.let { " $it" } ?: "")
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClickLabel = "편집", onClick = onClick),
    ) {
        DisplayNumerals(
            lines = listOf(clock),
            color = if (on) p.fg else p.subtle,
            contentDescription = TimeText.clockLine(alarm.hour, alarm.minute, is24Hour),
            modifier = Modifier.padding(top = 24.dp - (34 * Numerals.OVERSHOOT).dp),
            fontSize = 34.sp,
            weight = FontWeight.SemiBold,
        )
        BaselineStack(first = 33.dp, steps = listOf(17.dp), modifier = Modifier.padding(start = 164.dp)) {
            EdgeText(repeat, Type.bodyBold, if (on) p.fg else p.muted)
            EdgeText(next, Type.caption, p.muted)
        }
        SquareSwitch(on, onToggle, Modifier.align(Alignment.CenterEnd))
        Rule(1.dp, p.line, Modifier.align(Alignment.BottomStart))
    }
}
