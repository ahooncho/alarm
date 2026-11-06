package com.ahooncho.alarm.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ahooncho.alarm.data.Settings
import com.ahooncho.alarm.system.Readiness
import com.ahooncho.alarm.ui.components.BaselinePair
import com.ahooncho.alarm.ui.components.BaselineStack
import com.ahooncho.alarm.ui.components.EdgeText
import com.ahooncho.alarm.ui.components.LineIcon
import com.ahooncho.alarm.ui.components.LineIconView
import com.ahooncho.alarm.ui.components.Rule
import com.ahooncho.alarm.ui.components.SectionHead
import com.ahooncho.alarm.ui.components.Stepper
import com.ahooncho.alarm.ui.theme.Theme
import com.ahooncho.alarm.ui.theme.Type
import com.ahooncho.alarm.util.TimeText

/** One line of the permissions list. [onFix] opens whatever resolves it. */
data class CheckLine(val label: String, val value: String, val ok: Boolean, val onFix: (() -> Unit)?)

@Composable
fun SettingsScreen(
    settings: Settings,
    checks: List<CheckLine>,
    previewing: Boolean,
    onSettingsChange: (Settings) -> Unit,
    onPreview: () -> Unit,
    onTestAlarm: () -> Unit,
    onBack: () -> Unit,
) {
    val p = Theme.palette
    BackHandler(onBack = onBack)
    val problems = checks.count { !it.ok }
    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            // The arrow's ink starts on the margin; the 48 dp touch target reaches into it.
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-14).dp)
                    .size(48.dp)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = "뒤로" },
                contentAlignment = Alignment.Center,
            ) { LineIconView(LineIcon.Back, p.fg, 24.dp) }
        }
        BaselineStack(first = 64.dp, bottom = 32.dp, modifier = Modifier.fillMaxWidth()) {
            EdgeText("설정", Type.hero, p.fg)
        }

        SectionHead("소리", "이어폰으로만 나와요")
        Stepper(
            title = "이어폰 볼륨", hint = "미디어 최대 볼륨 기준", value = "${settings.volumePercent}%",
            canDecrease = settings.volumePercent > 5, canIncrease = settings.volumePercent < 100,
            onDecrease = { onSettingsChange(settings.copy(volumePercent = settings.volumePercent - 5)) },
            onIncrease = { onSettingsChange(settings.copy(volumePercent = settings.volumePercent + 5)) },
        )
        Stepper(
            title = "점점 커지는 시간", hint = "작게 시작해 커져요",
            value = if (settings.rampSeconds == 0) "바로" else "${settings.rampSeconds}초",
            canDecrease = settings.rampSeconds > 0, canIncrease = settings.rampSeconds < 60,
            onDecrease = { onSettingsChange(settings.copy(rampSeconds = settings.rampSeconds - 5)) },
            onIncrease = { onSettingsChange(settings.copy(rampSeconds = settings.rampSeconds + 5)) },
        )

        Spacer(Modifier.height(32.dp))
        SectionHead("울림")
        Stepper(
            title = "다시 알림 간격", hint = "다시 울리기까지", value = "${settings.snoozeMinutes}분",
            canDecrease = settings.snoozeMinutes > 1, canIncrease = settings.snoozeMinutes < 30,
            onDecrease = { onSettingsChange(settings.copy(snoozeMinutes = settings.snoozeMinutes - 1)) },
            onIncrease = { onSettingsChange(settings.copy(snoozeMinutes = settings.snoozeMinutes + 1)) },
        )
        Stepper(
            title = "길게 눌러 해제", hint = "0초면 바로 해제", value = TimeText.seconds(settings.holdToDismissMillis),
            canDecrease = settings.holdToDismissMillis > 0, canIncrease = settings.holdToDismissMillis < 3_000,
            onDecrease = { onSettingsChange(settings.copy(holdToDismissMillis = (settings.holdToDismissMillis - 200).coerceAtLeast(0))) },
            onIncrease = { onSettingsChange(settings.copy(holdToDismissMillis = (settings.holdToDismissMillis + 200).coerceAtMost(3_000))) },
        )
        Stepper(
            title = "자동으로 멈추기", hint = "해제하지 않을 때", value = "${settings.autoStopMinutes}분",
            canDecrease = settings.autoStopMinutes > 5, canIncrease = settings.autoStopMinutes < 60,
            onDecrease = { onSettingsChange(settings.copy(autoStopMinutes = settings.autoStopMinutes - 5)) },
            onIncrease = { onSettingsChange(settings.copy(autoStopMinutes = settings.autoStopMinutes + 5)) },
        )

        Spacer(Modifier.height(32.dp))
        SectionHead(
            "권한과 설정",
            if (problems > 0) "${problems}개를 확인해 주세요" else "모두 준비됐어요",
            noteStrong = problems > 0,
        )
        for (line in checks) CheckRow(line)

        Spacer(Modifier.height(32.dp))
        SectionHead("테스트")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TestBlock("미리 듣기", if (previewing) "재생 중" else "이어폰으로 6초 동안", enabled = !previewing, onClick = onPreview, modifier = Modifier.weight(1f))
            TestBlock("테스트 알람", "10초 뒤에 울려요", enabled = true, onClick = onTestAlarm, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** 48 dp row. A row that needs attention is inverted; the block bleeds 12 dp so its text stays on the margin. */
@Composable
private fun CheckRow(line: CheckLine) {
    val p = Theme.palette
    val bad = !line.ok
    val fix = line.onFix
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (bad) Modifier.drawBehind {
                val bleed = 12.dp.toPx()
                drawRect(p.block, Offset(-bleed, 0f), Size(size.width + 2 * bleed, size.height))
            } else Modifier)
            .then(if (fix != null) Modifier.clickable(onClick = fix) else Modifier),
    ) {
        BaselinePair(baseline = 29.dp, height = 48.dp, modifier = Modifier.fillMaxWidth(), start = {
            EdgeText(line.label, if (bad) Type.bodyBold else Type.body, if (bad) p.onBlock else p.fg)
        }, end = {
            EdgeText(line.value, if (bad) Type.bodyBold else Type.body, if (bad) p.onBlock else p.muted, end = true)
        })
        if (!bad) Rule(1.dp, p.line, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun TestBlock(title: String, detail: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val p = Theme.palette
    Box(
        modifier
            .height(96.dp)
            .border(2.dp, if (enabled) p.fg else p.line)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        BaselineStack(first = 36.dp, steps = listOf(44.dp), modifier = Modifier.padding(start = 16.dp)) {
            EdgeText(title, Type.titleBold, if (enabled) p.fg else p.muted)
            EdgeText(detail, Type.caption, p.muted)
        }
    }
}

/** Labels and values for the permissions list, in the names the system settings use. */
object CheckText {
    fun label(item: Readiness.Item): String = when (item) {
        Readiness.Item.NOTIFICATIONS -> "알림"
        Readiness.Item.FULL_SCREEN -> "전체 화면 알림"
        Readiness.Item.EXACT_ALARM -> "알람 및 리마인더"
        Readiness.Item.BATTERY -> "배터리"
        Readiness.Item.DND_MEDIA -> "방해 금지 예외"
    }

    fun value(item: Readiness.Item, ok: Boolean): String = when (item) {
        Readiness.Item.BATTERY -> if (ok) "제한 없음" else "제한됨"
        Readiness.Item.DND_MEDIA -> if (ok) "미디어 소리 허용" else "미디어 소리 꺼짐"
        else -> if (ok) "허용" else "꺼짐"
    }
}
