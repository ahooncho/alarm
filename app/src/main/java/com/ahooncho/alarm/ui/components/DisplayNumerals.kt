package com.ahooncho.alarm.ui.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.ahooncho.alarm.ui.theme.Fonts

/** [GlyphMetrics] read from an Android [Paint] with tabular figures switched on. */
private class PaintMetrics(typeface: Typeface) : GlyphMetrics {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = REFERENCE_SIZE
        fontFeatureSettings = "tnum"
    }
    private val advances = HashMap<Char, Float>()
    private val inks = HashMap<Char, InkBox>()

    override fun advance(ch: Char): Float = advances.getOrPut(ch) { paint.measureText(ch.toString()) }

    override fun ink(ch: Char): InkBox = inks.getOrPut(ch) {
        val r = Rect()
        paint.getTextBounds(ch.toString(), 0, 1, r)
        InkBox(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())
    }

    override val capHeight: Float = -ink('H').top
}

/**
 * Display figures drawn glyph by glyph (see [Numerals]). Without [fontSize] the figures fill the
 * available width edge to edge; with it they keep that size. [outline] draws strokes instead of fills,
 * which keeps the lit area small on the ringing screen.
 */
@Composable
fun DisplayNumerals(
    lines: List<String>,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    weight: FontWeight = FontWeight.Bold,
    outline: Dp = 0.dp,
    align: NumeralAlign = NumeralAlign.Start,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val typeface = remember(weight) { context.resources.getFont(Fonts.geistRes(weight)) }
    val metrics = remember(typeface) { PaintMetrics(typeface) }
    BoxWithConstraints(modifier) {
        val available = constraints.maxWidth.toFloat()
        val sizePx = if (fontSize.isSpecified) with(density) { fontSize.toPx() } else null
        val layout = remember(lines, available, sizePx, align, metrics) {
            Numerals.layout(lines, metrics, density.density, available, sizePx, align)
        }
        val strokePx = with(density) { outline.toPx() }
        val paint = remember(typeface, color, strokePx, layout.fontSize) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = layout.fontSize
                fontFeatureSettings = "tnum"
                this.color = color.toArgb()
                if (strokePx > 0f) {
                    style = Paint.Style.STROKE
                    strokeWidth = strokePx
                    strokeJoin = Paint.Join.MITER
                }
            }
        }
        Canvas(
            Modifier
                .width(with(density) { layout.width.toDp() })
                .height(with(density) { layout.height.toDp() })
                .clearAndSetSemantics { this.contentDescription = contentDescription },
        ) {
            drawIntoCanvas { canvas ->
                for (g in layout.glyphs) canvas.nativeCanvas.drawText(g.ch.toString(), g.x, g.baseline, paint)
            }
        }
    }
}
