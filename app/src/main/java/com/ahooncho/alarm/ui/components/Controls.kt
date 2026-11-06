package com.ahooncho.alarm.ui.components

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ahooncho.alarm.ui.theme.Fonts
import com.ahooncho.alarm.ui.theme.Theme
import com.ahooncho.alarm.ui.theme.Type
import kotlin.math.roundToInt

/** Horizontal rule. The design uses three weights only: 1, 2 and 4 dp. */
@Composable
fun Rule(thickness: Dp, color: Color = Theme.palette.fg, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

/**
 * Stacks children so that the first baseline sits [first] below the top, each following baseline
 * [steps] below the previous one, and the bottom edge [bottom] below the last baseline.
 */
@Composable
fun BaselineStack(
    first: Dp,
    modifier: Modifier = Modifier,
    steps: List<Dp> = emptyList(),
    bottom: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        var baseline = first.roundToPx()
        val ys = placeables.mapIndexed { i, p ->
            if (i > 0) baseline += (steps.getOrNull(i - 1) ?: steps.lastOrNull() ?: 0.dp).roundToPx()
            val fb = p[FirstBaseline]
            baseline - if (fb == androidx.compose.ui.layout.AlignmentLine.Unspecified) p.height else fb
        }
        val width = (placeables.maxOfOrNull { it.width } ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(width, baseline + bottom.roundToPx()) {
            placeables.forEachIndexed { i, p -> p.placeRelative(0, ys[i]) }
        }
    }
}

/** Two texts on one baseline, [baseline] below the top: one on the start edge, one on the end edge. */
@Composable
fun BaselinePair(
    baseline: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit = {},
) {
    Layout(contents = listOf(start, end), modifier = modifier) { (s, e), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val ps = s.map { it.measure(loose) }
        val pe = e.map { it.measure(loose) }
        val b = baseline.roundToPx()
        val width = constraints.maxWidth
        fun y(p: androidx.compose.ui.layout.Placeable): Int {
            val fb = p[FirstBaseline]
            return b - if (fb == androidx.compose.ui.layout.AlignmentLine.Unspecified) p.height / 2 else fb
        }
        layout(width, height.roundToPx()) {
            ps.forEach { it.placeRelative(0, y(it)) }
            pe.forEach { it.placeRelative(width - it.width, y(it)) }
        }
    }
}

/**
 * Text whose ink, not its advance box, sits on the edge it is aligned to. Hangul in Gothic A1 keeps
 * 1–2 dp of side bearing inside the box, which reads as an indent next to a rule.
 */
@Composable
fun EdgeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    end: Boolean = false,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val inset = remember(text, style, density) {
        val res = if (style.fontFamily == Fonts.geist) Fonts.geistRes(style.fontWeight ?: FontWeight.Normal)
        else Fonts.gothicRes(style.fontWeight ?: FontWeight.Normal)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = context.resources.getFont(res)
            textSize = with(density) { style.fontSize.toPx() }
            letterSpacing = style.letterSpacing.value.takeIf { !it.isNaN() } ?: 0f
            style.fontFeatureSettings?.let { fontFeatureSettings = it }
        }
        val r = Rect()
        paint.getTextBounds(text, 0, text.length, r)
        if (end) paint.measureText(text) - r.right else r.left.toFloat()
    }
    val dx = (if (end) inset else -inset).roundToInt()
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier.offset { IntOffset(dx, 0) },
    )
}

/** Square switch, 52×32. The off state is also spelled out in the row text. */
@Composable
fun SquareSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val p = Theme.palette
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .size(52.dp, 32.dp)
            .background(if (checked) p.fg else p.bg)
            .border(2.dp, p.fg)
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .background(if (checked) p.bg else p.fg),
        )
    }
}

enum class LineIcon { Plus, Minus, Back }

/** Icons drawn as 2 dp lines with square ends, so they centre exactly in their boxes. */
@Composable
fun LineIconView(icon: LineIcon, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = 2.dp.toPx()
        val s = this.size.width
        fun line(a: Offset, b: Offset) = drawLine(color, a, b, w, StrokeCap.Butt)
        when (icon) {
            LineIcon.Plus -> { line(Offset(s / 2, 0f), Offset(s / 2, s)); line(Offset(0f, s / 2), Offset(s, s / 2)) }
            LineIcon.Minus -> line(Offset(0f, s / 2), Offset(s, s / 2))
            LineIcon.Back -> backArrow(color, w, s)
        }
    }
}

private fun DrawScope.backArrow(color: Color, w: Float, s: Float) {
    val u = s / 24f
    drawLine(color, Offset(3 * u, 12 * u), Offset(22 * u, 12 * u), w, StrokeCap.Butt)
    drawLine(color, Offset(11 * u, 4 * u), Offset(3 * u, 12 * u), w, StrokeCap.Square)
    drawLine(color, Offset(3 * u, 12 * u), Offset(11 * u, 20 * u), w, StrokeCap.Square)
}

/** Settings stepper, 72 dp: label in grid columns 1–2, controls in 3–4. */
@Composable
fun Stepper(
    title: String,
    hint: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val p = Theme.palette
    Box(Modifier.fillMaxWidth().height(72.dp)) {
        BaselineStack(first = 32.dp, steps = listOf(20.dp), modifier = Modifier.fillMaxWidth(0.47f)) {
            EdgeText(title, Type.titleBold, p.fg)
            EdgeText(hint, Type.caption, p.muted)
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            StepButton(LineIcon.Minus, "줄이기", canDecrease, onDecrease)
            Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) {
                Text(value, style = Type.titleBold, color = p.fg, maxLines = 1)
            }
            StepButton(LineIcon.Plus, "늘리기", canIncrease, onIncrease)
        }
        Rule(1.dp, p.line, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun StepButton(icon: LineIcon, label: String, enabled: Boolean, onClick: () -> Unit) {
    val p = Theme.palette
    val color = if (enabled) p.fg else p.line
    Box(
        Modifier
            .size(48.dp)
            .border(2.dp, color)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        LineIconView(icon, color, 16.dp)
    }
}

/** Section head: 4 dp rule, title on a baseline 32 dp below it, optional note at the end, 1 dp rule. */
@Composable
fun SectionHead(title: String, note: String? = null, noteStrong: Boolean = false) {
    val p = Theme.palette
    Rule(4.dp)
    BaselinePair(baseline = 28.dp, height = 43.dp, modifier = Modifier.fillMaxWidth(), start = {
        EdgeText(title, Type.bodyBold, p.fg)
    }, end = {
        if (note != null) EdgeText(note, if (noteStrong) Type.captionBold else Type.caption, if (noteStrong) p.fg else p.muted, end = true)
    })
    Rule(1.dp, p.line)
}

/** Full-width bar at the bottom of a screen: inverted block, label on the start edge, detail or icon on the end. */
@Composable
fun BottomBar(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, end: @Composable () -> Unit) {
    val p = Theme.palette
    Box(
        modifier
            .fillMaxWidth()
            .background(p.block)
            .clickable(onClick = onClick)
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EdgeText(label, Type.headline, p.onBlock)
            Spacer(Modifier.weight(1f))
            end()
        }
    }
}
