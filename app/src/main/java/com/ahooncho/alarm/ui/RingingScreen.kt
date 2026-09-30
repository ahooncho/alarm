package com.ahooncho.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahooncho.alarm.ui.theme.Type
import com.ahooncho.alarm.util.TimeText
import java.time.Instant
import java.time.ZoneId

@Composable
fun RingingScreen(
    nowMillis: Long,
    is24Hour: Boolean,
    isTest: Boolean,
    earphone: String?,
    outputKnown: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val time = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
    val (clock, marker) = TimeText.clock(time.hour, time.minute, is24Hour)
    val output = when {
        !outputKnown -> "이어폰 확인 중"
        earphone != null -> earphone
        else -> "이어폰 없음 · 진동만"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        Text(if (isTest) "테스트 알람" else "알람", style = Type.label, color = colors.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        if (marker != null) Text(marker, style = Type.title, color = colors.onBackground)
        Text(clock, style = Type.huge, color = colors.onBackground)
        Spacer(Modifier.height(16.dp))
        Text(output, style = Type.body, color = colors.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton("스누즈", filled = false, onClick = onSnooze, modifier = Modifier.weight(1f))
            BigButton("끄기", filled = true, onClick = onDismiss, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BigButton(text: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(72.dp)
            .border(1.dp, colors.onBackground)
            .background(if (filled) colors.onBackground else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Type.body,
            fontWeight = FontWeight.Medium,
            color = if (filled) colors.background else colors.onBackground,
        )
    }
}
