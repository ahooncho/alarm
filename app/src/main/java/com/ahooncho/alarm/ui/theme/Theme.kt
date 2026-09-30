package com.ahooncho.alarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Black and white only: every role is the background, the foreground, or a gray between them. */
private fun monochrome(dark: Boolean): ColorScheme {
    val bg = if (dark) Color.Black else Color.White
    val fg = if (dark) Color.White else Color.Black
    fun gray(amount: Float) = lerp(bg, fg, amount)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = fg,
        onPrimary = bg,
        primaryContainer = gray(0.12f),
        onPrimaryContainer = fg,
        inversePrimary = bg,
        secondary = fg,
        onSecondary = bg,
        secondaryContainer = gray(0.12f),
        onSecondaryContainer = fg,
        tertiary = fg,
        onTertiary = bg,
        tertiaryContainer = gray(0.12f),
        onTertiaryContainer = fg,
        background = bg,
        onBackground = fg,
        surface = bg,
        onSurface = fg,
        surfaceVariant = gray(0.08f),
        onSurfaceVariant = gray(0.5f),
        surfaceTint = bg,
        inverseSurface = fg,
        inverseOnSurface = bg,
        error = fg,
        onError = bg,
        errorContainer = gray(0.12f),
        onErrorContainer = fg,
        outline = gray(0.35f),
        outlineVariant = gray(0.15f),
        scrim = Color.Black,
        surfaceBright = gray(0.1f),
        surfaceDim = bg,
        surfaceContainerLowest = bg,
        surfaceContainerLow = gray(0.03f),
        surfaceContainer = gray(0.05f),
        surfaceContainerHigh = gray(0.08f),
        surfaceContainerHighest = gray(0.12f),
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
    MaterialTheme(colorScheme = monochrome(dark), shapes = SquareShapes, content = content)
}

/** A small type scale: thin numerals for time, plain text for everything else. */
object Type {
    val huge = TextStyle(fontSize = 96.sp, lineHeight = 100.sp, fontWeight = FontWeight.Thin, letterSpacing = (-3).sp)
    val display = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Thin, letterSpacing = (-1.5).sp)
    val title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Light)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 22.sp)
    val caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val label = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
}
