package com.ahooncho.alarm.ui.components

import com.ahooncho.alarm.ui.theme.Tracking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumeralLayoutTest {
    /** Geist Bold-like metrics at 100 px: tabular figures 63.6 wide, '0' inked from 1.55 to 62.05. */
    private val metrics = object : GlyphMetrics {
        override fun advance(ch: Char) = if (ch == ':') 31.1f else 63.6f
        override fun ink(ch: Char) = if (ch == ':') InkBox(7.4f, -50.9f, 23.7f, 0f) else InkBox(1.55f, -72.6f, 62.05f, 1.6f)
        override val capHeight = 71f
    }

    private fun inkLeft(l: NumeralLayout, g: PlacedGlyph) = g.x + metrics.ink(g.ch).left * l.fontSize / REFERENCE_SIZE
    private fun inkRight(l: NumeralLayout, g: PlacedGlyph) = g.x + metrics.ink(g.ch).right * l.fontSize / REFERENCE_SIZE

    @Test
    fun fittedFiguresOvershootBothMarginsByTheFontsOvershoot() {
        val l = Numerals.layout(listOf("07:30"), metrics, density = 1f, availableWidth = 312f)
        val over = Numerals.OVERSHOOT * l.fontSize
        assertEquals(-over, inkLeft(l, l.glyphs.first()), 0.01f)
        assertEquals(312f + over, inkRight(l, l.glyphs.last()), 0.01f)
        assertEquals(312f, l.width, 0f)
    }

    @Test
    fun colonIsRaisedToTheFiguresCentre() {
        val l = Numerals.layout(listOf("07:30"), metrics, density = 1f, availableWidth = 312f)
        val digit = l.glyphs.first()
        val colon = l.glyphs.first { it.ch == ':' }
        assertEquals(digit.baseline - Numerals.COLON_RAISE * l.fontSize, colon.baseline, 0.001f)
    }

    @Test
    fun figuresKeepTabularSteps() {
        val l = Numerals.layout(listOf("1180"), metrics, density = 1f, availableWidth = 0f, fontSize = 34f)
        val steps = l.glyphs.zipWithNext { a, b -> b.x - a.x }
        assertTrue(steps.all { kotlin.math.abs(it - steps.first()) < 1e-4f })
        assertEquals(63.6f * 0.34f + Tracking.latin(34f) * 34f, steps.first(), 1e-4f)
    }

    @Test
    fun stackedLinesAreSpacedFromBaselineToCapLine() {
        val l = Numerals.layout(listOf("07", "30"), metrics, density = 1f, availableWidth = 312f)
        val cap = metrics.capHeight * l.fontSize / REFERENCE_SIZE
        val first = l.glyphs.first().baseline
        val second = l.glyphs.last().baseline
        assertEquals(cap + Numerals.LINE_GAP * l.fontSize, second - first, 0.01f)
        assertEquals(second + Numerals.OVERSHOOT * l.fontSize, l.height, 0.01f)
    }

    @Test
    fun centredFiguresCentreTheirInk() {
        val l = Numerals.layout(listOf("06"), metrics, density = 1f, availableWidth = 148f, fontSize = 108f, align = NumeralAlign.Center)
        val centre = (inkLeft(l, l.glyphs.first()) + inkRight(l, l.glyphs.last())) / 2f
        assertEquals(74f, centre, 0.01f)
    }

    @Test
    fun trackingIsLooserWhenSmallAndConverges() {
        assertTrue(Tracking.latin(12f) > Tracking.latin(16f))
        assertEquals(-0.0223f, Tracking.latin(200f), 1e-4f)
        assertEquals(0f, Tracking.hangul(14f), 0f)
        assertEquals(-0.02f, Tracking.hangul(24f), 0f)
    }
}
