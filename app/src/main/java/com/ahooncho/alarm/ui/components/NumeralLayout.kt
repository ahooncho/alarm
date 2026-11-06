package com.ahooncho.alarm.ui.components

import com.ahooncho.alarm.ui.theme.Tracking

/** Ink bounds of a glyph relative to its pen position on the baseline, y growing downward. */
data class InkBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Glyph measurements at [REFERENCE_SIZE] px, with tabular figures. Kept apart from Android for testing. */
interface GlyphMetrics {
    fun advance(ch: Char): Float
    fun ink(ch: Char): InkBox
    val capHeight: Float
}

const val REFERENCE_SIZE = 100f

enum class NumeralAlign { Start, Center }

data class PlacedGlyph(val ch: Char, val x: Float, val baseline: Float)

data class NumeralLayout(val fontSize: Float, val width: Float, val height: Float, val glyphs: List<PlacedGlyph>)

/**
 * Lays out display figures the way the design draws them:
 * - each glyph is placed on its own so the colon can be raised to the figures' centre;
 * - the first and last ink, not the advance box, sits on the column edge, and round sides
 *   ([ROUND_LEFT], [ROUND_RIGHT]) protrude past it by the font's overshoot, so they look aligned;
 * - stacked lines are spaced from one baseline to the next cap line by [LINE_GAP].
 * All values are in px. [density] converts px to dp for the size-dependent tracking.
 */
object Numerals {
    /** Geist rounds overshoot the cap and base lines by 16/1000 em; the same amount past a margin. */
    const val OVERSHOOT = 0.016f

    /** Geist's colon is centred at 0.358 of the cap height; this lifts it to 0.5. */
    const val COLON_RAISE = 0.101f

    /** Space between a line's baseline and the next line's cap line, in em. */
    const val LINE_GAP = 0.06f

    private const val ROUND_LEFT = "0689"
    private const val ROUND_RIGHT = "03689"

    private data class Line(val xs: List<Float>, val inkLeft: Float, val inkRight: Float, val protrudeLeft: Float, val protrudeRight: Float) {
        val span get() = inkRight - inkLeft - protrudeLeft - protrudeRight
    }

    private fun line(text: String, m: GlyphMetrics, size: Float, density: Float): Line {
        val scale = size / REFERENCE_SIZE
        val track = Tracking.latin(size / density) * size
        val xs = ArrayList<Float>(text.length)
        var pen = 0f
        text.forEachIndexed { i, ch ->
            xs += pen
            pen += m.advance(ch) * scale + if (i < text.lastIndex) track else 0f
        }
        val first = text.first()
        val last = text.last()
        return Line(
            xs = xs,
            inkLeft = xs.first() + m.ink(first).left * scale,
            inkRight = xs.last() + m.ink(last).right * scale,
            protrudeLeft = if (first in ROUND_LEFT) OVERSHOOT * size else 0f,
            protrudeRight = if (last in ROUND_RIGHT) OVERSHOOT * size else 0f,
        )
    }

    /** Font size whose widest line spans exactly [width] once round sides protrude. */
    fun fitSize(lines: List<String>, m: GlyphMetrics, width: Float, density: Float): Float {
        var size = width // any start; the span is nearly linear in size
        repeat(4) {
            val span = lines.maxOf { line(it, m, size, density).span }
            size *= width / span
        }
        return size
    }

    fun layout(
        lines: List<String>,
        m: GlyphMetrics,
        density: Float,
        availableWidth: Float,
        fontSize: Float? = null,
        align: NumeralAlign = NumeralAlign.Start,
    ): NumeralLayout {
        require(lines.isNotEmpty() && lines.all { it.isNotEmpty() })
        val size = fontSize ?: fitSize(lines, m, availableWidth, density)
        val scale = size / REFERENCE_SIZE
        val cap = m.capHeight * scale
        val over = OVERSHOOT * size
        val measured = lines.map { line(it, m, size, density) }
        val contentWidth = when (align) {
            NumeralAlign.Center -> availableWidth
            NumeralAlign.Start -> if (fontSize == null) availableWidth else measured.maxOf { it.span }
        }
        val glyphs = ArrayList<PlacedGlyph>()
        measured.forEachIndexed { index, l ->
            val shift = when (align) {
                NumeralAlign.Start -> -l.protrudeLeft - l.inkLeft
                NumeralAlign.Center -> contentWidth / 2f - (l.inkLeft + l.inkRight) / 2f
            }
            val baseline = over + cap + index * (cap + LINE_GAP * size)
            lines[index].forEachIndexed { i, ch ->
                val lift = if (ch == ':') COLON_RAISE * size else 0f
                glyphs += PlacedGlyph(ch, l.xs[i] + shift, baseline - lift)
            }
        }
        val height = over + cap + (lines.size - 1) * (cap + LINE_GAP * size) + over
        return NumeralLayout(size, contentWidth, height, glyphs)
    }
}
