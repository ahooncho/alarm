package com.ahooncho.alarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ahooncho.alarm.R
import kotlin.math.exp

/**
 * Black, white and grays only. The grays are picked in CIE L* so each role has a known contrast:
 * - [muted]: secondary text, L* 45 / 55, 5.37:1 on white and 5.59:1 on black (WCAG 4.5:1).
 * - [subtle]: off-state text of 24 sp and larger only, L* 60 / 40, 3.17:1 and 3.25:1 (large text 3:1).
 * - [block]: large inverted areas. At night it drops to a dark gray so the screen emits less light.
 */
@Immutable
data class Palette(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val subtle: Color,
    val line: Color,
    val fill: Color,
    val block: Color,
    val onBlock: Color,
)

val LightPalette = Palette(
    bg = Color.White,
    fg = Color.Black,
    muted = Color(0xFF6A6A6A),
    subtle = Color(0xFF919191),
    line = Color(0xFFD7D7D7),
    fill = Color(0xFFF3F3F3),
    block = Color.Black,
    onBlock = Color.White,
)

val DarkPalette = Palette(
    bg = Color.Black,
    fg = Color.White,
    muted = Color(0xFF848484),
    subtle = Color(0xFF5E5E5E),
    line = Color(0xFF242424),
    fill = Color(0xFF0E0E0E),
    block = Color(0xFF282828),
    onBlock = Color(0xFFEBEBEB),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

object Theme {
    val palette: Palette
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current
}

private fun materialScheme(p: Palette, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.fg, onPrimary = p.bg, secondary = p.fg, onSecondary = p.bg, tertiary = p.fg, onTertiary = p.bg,
        background = p.bg, onBackground = p.fg, surface = p.bg, onSurface = p.fg, surfaceVariant = p.fill,
        onSurfaceVariant = p.muted, outline = p.fg, outlineVariant = p.line, surfaceTint = p.bg,
        error = p.fg, onError = p.bg,
    )
}

private val SquareShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun AlarmTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    MaterialTheme(colorScheme = materialScheme(palette, dark), shapes = SquareShapes) {
        CompositionLocalProvider(LocalPalette provides palette, content = content)
    }
}

object Fonts {
    /** Latin and figures. Static instances: a variable font's overlapping contours show when outlined. */
    val geist = FontFamily(
        Font(R.font.geist_regular, FontWeight.Normal),
        Font(R.font.geist_medium, FontWeight.Medium),
        Font(R.font.geist_semibold, FontWeight.SemiBold),
        Font(R.font.geist_bold, FontWeight.Bold),
    )

    /** Hangul. Two weights only. */
    val gothic = FontFamily(
        Font(R.font.gothic_a1_regular, FontWeight.Normal),
        Font(R.font.gothic_a1_bold, FontWeight.Bold),
    )

    fun geistRes(weight: FontWeight): Int = when {
        weight >= FontWeight.Bold -> R.font.geist_bold
        weight >= FontWeight.SemiBold -> R.font.geist_semibold
        weight >= FontWeight.Medium -> R.font.geist_medium
        else -> R.font.geist_regular
    }

    fun gothicRes(weight: FontWeight): Int =
        if (weight >= FontWeight.Bold) R.font.gothic_a1_bold else R.font.gothic_a1_regular
}

object Tracking {
    /** Letter spacing in em for Latin and figures: looser when small, −0.022 em when large (Inter dynamic metrics). */
    fun latin(size: Float): Float = -0.0223f + 0.185f * exp(-0.1745f * size)

    /** Hangul keeps zero spacing at text sizes (W3C klreq 7.3.1) and tightens slightly at display sizes. */
    fun hangul(size: Float): Float = if (size >= 24f) -0.02f else 0f
}

/** The whole type scale. Line heights are multiples of 4 so baselines land on a 4 dp grid. */
object Type {
    private val centred = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

    private fun gothic(size: Int, lineHeight: Int, bold: Boolean) = TextStyle(
        fontFamily = Fonts.gothic,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = Tracking.hangul(size.toFloat()).em,
        lineHeightStyle = centred,
    )

    private fun geist(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
        fontFamily = Fonts.geist,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = Tracking.latin(size.toFloat()).em,
        fontFeatureSettings = "tnum",
        lineHeightStyle = centred,
    )

    val caption = gothic(12, 16, bold = false)
    val captionBold = gothic(12, 16, bold = true)
    val body = gothic(14, 20, bold = false)
    val bodyBold = gothic(14, 20, bold = true)
    val title = gothic(16, 24, bold = false)
    val titleBold = gothic(16, 24, bold = true)
    val headline = gothic(20, 28, bold = true)
    val display = gothic(24, 32, bold = true)
    val hero = gothic(64, 72, bold = true)
    val clock = geist(12, 16, FontWeight.Medium)
}
