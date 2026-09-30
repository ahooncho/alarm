package com.ahooncho.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import com.ahooncho.alarm.ui.theme.Type
import com.ahooncho.alarm.util.TimeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorDialog(
    initialHour: Int,
    initialMinute: Int,
    initialDays: Set<Int>,
    is24Hour: Boolean,
    isNew: Boolean,
    onSave: (hour: Int, minute: Int, days: Set<Int>) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val time = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = is24Hour)
    var days by remember { mutableStateOf(initialDays) }
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = colors.background,
            border = BorderStroke(Dp.Hairline, colors.outline),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isNew) "새 알람" else "알람 편집",
                    style = Type.label,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(Modifier.height(24.dp))
                TimePicker(state = time)
                Spacer(Modifier.height(8.dp))
                DayPicker(days = days, onChange = { days = it })
                Spacer(Modifier.height(8.dp))
                Text(TimeText.days(days), style = Type.caption, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!isNew) TextAction("삭제", onDelete)
                    Spacer(Modifier.weight(1f))
                    TextAction("취소", onDismiss)
                    TextAction("저장", { onSave(time.hour, time.minute, days) }, emphasized = true)
                }
            }
        }
    }
}

@Composable
private fun DayPicker(days: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (day in 1..7) {
            val selected = day in days
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(Dp.Hairline, if (selected) colors.onBackground else colors.outline, CircleShape)
                    .background(if (selected) colors.onBackground else Color.Transparent, CircleShape)
                    .clickable { onChange(if (selected) days - day else days + day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    TimeText.dayName(day),
                    style = Type.caption,
                    color = if (selected) colors.background else colors.onBackground,
                )
            }
        }
    }
}

@Composable
private fun TextAction(text: String, onClick: () -> Unit, emphasized: Boolean = false) {
    Text(
        text,
        style = Type.body,
        fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
