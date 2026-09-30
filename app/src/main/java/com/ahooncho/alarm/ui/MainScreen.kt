package com.ahooncho.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ahooncho.alarm.data.Alarm
import com.ahooncho.alarm.data.Settings
import com.ahooncho.alarm.ui.theme.Type
import com.ahooncho.alarm.util.TimeText
import java.time.Instant
import java.time.ZoneId

/** One line of the status section. [onFix] opens whatever resolves a problem. */
data class StatusLine(
    val label: String,
    val value: String,
    val ok: Boolean,
    val onFix: (() -> Unit)? = null,
)

@Composable
fun MainScreen(
    alarms: List<Alarm>,
    settings: Settings,
    nowMillis: Long,
    nextAlarmAt: Long?,
    is24Hour: Boolean,
    status: List<StatusLine>,
    previewing: Boolean,
    onToggle: (Alarm, Boolean) -> Unit,
    onEdit: (Alarm) -> Unit,
    onAdd: () -> Unit,
    onSettingsChange: (Settings) -> Unit,
    onPreview: () -> Unit,
    onTestAlarm: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
    ) {
        item { Header(nextAlarmAt, nowMillis, is24Hour) }
        item { Rule() }
        items(alarms, key = { it.id }) { alarm ->
            AlarmRow(
                alarm = alarm,
                nowMillis = nowMillis,
                is24Hour = is24Hour,
                onToggle = { onToggle(alarm, it) },
                onClick = { onEdit(alarm) },
            )
            Rule()
        }
        item { ActionLine("알람 추가", trailing = "+", onClick = onAdd) }

        item { Section("상태") }
        items(status) { StatusRow(it) }

        item { Section("설정") }
        item {
            Stepper(
                label = "이어폰 볼륨",
                value = "${settings.volumePercent}%",
                onMinus = { onSettingsChange(settings.copy(volumePercent = step(settings.volumePercent, -5, 5..100))) },
                onPlus = { onSettingsChange(settings.copy(volumePercent = step(settings.volumePercent, 5, 5..100))) },
            )
        }
        item {
            Stepper(
                label = "점점 크게",
                value = if (settings.rampSeconds == 0) "바로" else "${settings.rampSeconds}초",
                onMinus = { onSettingsChange(settings.copy(rampSeconds = step(settings.rampSeconds, -5, 0..60))) },
                onPlus = { onSettingsChange(settings.copy(rampSeconds = step(settings.rampSeconds, 5, 0..60))) },
            )
        }
        item {
            Stepper(
                label = "스누즈",
                value = "${settings.snoozeMinutes}분",
                onMinus = { onSettingsChange(settings.copy(snoozeMinutes = step(settings.snoozeMinutes, -1, 1..30))) },
                onPlus = { onSettingsChange(settings.copy(snoozeMinutes = step(settings.snoozeMinutes, 1, 1..30))) },
            )
        }
        item {
            Stepper(
                label = "자동 종료",
                value = "${settings.autoStopMinutes}분",
                onMinus = { onSettingsChange(settings.copy(autoStopMinutes = step(settings.autoStopMinutes, -5, 5..60))) },
                onPlus = { onSettingsChange(settings.copy(autoStopMinutes = step(settings.autoStopMinutes, 5, 5..60))) },
            )
        }

        item { Section("테스트") }
        item {
            ActionLine(
                text = if (previewing) "재생 중" else "이어폰으로 미리 듣기",
                onClick = onPreview,
                enabled = !previewing,
            )
        }
        item { ActionLine("10초 뒤 테스트 알람", onClick = onTestAlarm) }
    }
}

private fun step(value: Int, delta: Int, range: IntRange): Int = (value + delta).coerceIn(range)

@Composable
private fun Header(nextAlarmAt: Long?, nowMillis: Long, is24Hour: Boolean) {
    val next = if (nextAlarmAt == null) {
        "켜진 알람 없음"
    } else {
        val time = Instant.ofEpochMilli(nextAlarmAt).atZone(ZoneId.systemDefault())
        val day = TimeText.relativeDay(nextAlarmAt, nowMillis)
        val clock = TimeText.clockLine(time.hour, time.minute, is24Hour)
        "$day $clock · ${TimeText.until(nowMillis, nextAlarmAt)}"
    }
    Column(Modifier.padding(bottom = 24.dp)) {
        Text("알람", style = Type.title, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(next, style = Type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    nowMillis: Long,
    is24Hour: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (alarm.enabled) colors.onBackground else colors.onSurfaceVariant
    val (clock, marker) = TimeText.clock(alarm.hour, alarm.minute, is24Hour)
    val snooze = alarm.snoozeUntil?.takeIf { it > nowMillis }?.let {
        val time = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
        " · 스누즈 ${TimeText.clockLine(time.hour, time.minute, is24Hour)}"
    }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row {
                if (marker != null) {
                    Text("$marker ", style = Type.body, color = tint, modifier = Modifier.alignByBaseline())
                }
                Text(clock, style = Type.display, color = tint, modifier = Modifier.alignByBaseline())
            }
            Text(TimeText.days(alarm.days) + snooze, style = Type.caption, color = colors.onSurfaceVariant)
        }
        Switch(checked = alarm.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = Type.label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 48.dp, bottom = 8.dp),
    )
}

@Composable
private fun Rule() {
    HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StatusRow(line: StatusLine) {
    val colors = MaterialTheme.colorScheme
    val fix = line.onFix.takeUnless { line.ok }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (fix != null) Modifier.clickable(onClick = fix) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(line.label, style = Type.body, color = colors.onBackground, modifier = Modifier.weight(1f))
        Text(
            text = if (fix != null) "${line.value}  →" else line.value,
            style = Type.body,
            fontWeight = if (line.ok) FontWeight.Normal else FontWeight.SemiBold,
            color = if (line.ok) colors.onSurfaceVariant else colors.onBackground,
        )
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Type.body, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        StepButton("−", onMinus)
        Text(
            value,
            style = Type.body,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.widthIn(min = 64.dp),
        )
        StepButton("+", onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = Type.title, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ActionLine(text: String, onClick: () -> Unit, trailing: String = "→", enabled: Boolean = true) {
    val color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = Type.body, color = color)
        Text(trailing, style = Type.body, color = color)
    }
}
