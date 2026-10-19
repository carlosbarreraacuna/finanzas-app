package com.carlos.finanzas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlos.finanzas.R

/**
 * Colores que no tienen rol en Material: el campo oscuro del encabezado, la pista de las barras
 * y los tonos de atención (ámbar) y de deuda (ladrillo).
 */
@Immutable
data class FinColors(
    val ground: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val accent: Color,
    val mint: Color,
    val onMint: Color,
    val field: Color,
    val onField: Color,
    val onFieldSoft: Color,
    val track: Color,
    val chipBorder: Color,
    val amber: Color,
    val amberBar: Color,
    val amberTint: Color,
    val onAmberTint: Color,
    val brick: Color,
    val brickTint: Color,
    val onBrickTint: Color
)

private val LightFin = FinColors(
    ground = Color(0xFFEEF2F0),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF10201E),
    inkSoft = Color(0xFF4A5B58),
    accent = Color(0xFF0E5C57),
    mint = Color(0xFFCDEBE6),
    onMint = Color(0xFF0B3B38),
    field = Color(0xFF0B3B38),
    onField = Color(0xFFE7F2EF),
    onFieldSoft = Color(0xFFA9CBC5),
    track = Color(0xFFE3EBE8),
    chipBorder = Color(0xFFD5DFDC),
    amber = Color(0xFF7A4E00),
    amberBar = Color(0xFFC98A12),
    amberTint = Color(0xFFFBE9C6),
    onAmberTint = Color(0xFF5A3A00),
    brick = Color(0xFFA3361B),
    brickTint = Color(0xFFF6DDD5),
    onBrickTint = Color(0xFF7E2812)
)

private val DarkFin = FinColors(
    ground = Color(0xFF0D1615),
    card = Color(0xFF16221F),
    ink = Color(0xFFDCE6E4),
    inkSoft = Color(0xFFA9BAB7),
    accent = Color(0xFF7FD0C7),
    mint = Color(0xFF0B4A46),
    onMint = Color(0xFFCDEBE6),
    field = Color(0xFF12312E),
    onField = Color(0xFFE7F2EF),
    onFieldSoft = Color(0xFFA9CBC5),
    track = Color(0xFF24322F),
    chipBorder = Color(0xFF34443F),
    amber = Color(0xFFF2BD62),
    amberBar = Color(0xFFF2BD62),
    amberTint = Color(0xFF3A2A08),
    onAmberTint = Color(0xFFF2D9A6),
    brick = Color(0xFFFFA58B),
    brickTint = Color(0xFF4A1B10),
    onBrickTint = Color(0xFFFFD9CF)
)

val LocalFin = staticCompositionLocalOf { LightFin }

/** Atajo: `Fin.colors.brick`. */
object Fin {
    val colors: FinColors
        @Composable get() = LocalFin.current
}

private fun scheme(c: FinColors, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = c.accent, onPrimary = Color(0xFF00201E),
        primaryContainer = c.mint, onPrimaryContainer = c.onMint,
        secondary = c.amber, secondaryContainer = c.amberTint, onSecondaryContainer = c.onAmberTint,
        tertiary = c.field, onTertiary = c.onField,
        error = c.brick, errorContainer = c.brickTint, onErrorContainer = c.onBrickTint,
        background = c.ground, onBackground = c.ink,
        surface = c.ground, onSurface = c.ink, onSurfaceVariant = c.inkSoft,
        surfaceVariant = c.track, outline = c.chipBorder, outlineVariant = c.chipBorder,
        surfaceContainerLowest = c.card, surfaceContainerLow = c.card, surfaceContainer = c.card,
        surfaceContainerHigh = c.card, surfaceContainerHighest = c.track
    )
} else {
    lightColorScheme(
        primary = c.accent, onPrimary = Color.White,
        primaryContainer = c.mint, onPrimaryContainer = c.onMint,
        secondary = c.amber, secondaryContainer = c.amberTint, onSecondaryContainer = c.onAmberTint,
        tertiary = c.field, onTertiary = c.onField,
        error = c.brick, errorContainer = c.brickTint, onErrorContainer = c.onBrickTint,
        background = c.ground, onBackground = c.ink,
        surface = c.ground, onSurface = c.ink, onSurfaceVariant = c.inkSoft,
        surfaceVariant = c.track, outline = c.chipBorder, outlineVariant = c.chipBorder,
        surfaceContainerLowest = c.card, surfaceContainerLow = c.card, surfaceContainer = c.card,
        surfaceContainerHigh = c.card, surfaceContainerHighest = c.track
    )
}

@OptIn(ExperimentalTextApi::class)
private fun instrument(weight: Int) = Font(
    R.font.instrument_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Instrument Sans (OFL), fuente variable incluida en res/font. */
val InstrumentSans = FontFamily(instrument(400), instrument(500), instrument(600), instrument(700))

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = "tnum"
)

private val Type = Typography(
    displayLarge = style(58, FontWeight.Bold, 56, -1.8),
    displayMedium = style(46, FontWeight.Bold, 48, -1.4),
    displaySmall = style(40, FontWeight.Bold, 44, -1.2),
    headlineLarge = style(34, FontWeight.Bold, 40, -0.8),
    headlineMedium = style(30, FontWeight.Bold, 36, -0.6),
    headlineSmall = style(26, FontWeight.Bold, 31, -0.4),
    titleLarge = style(19, FontWeight.SemiBold, 25),
    titleMedium = style(17, FontWeight.SemiBold, 23),
    titleSmall = style(16, FontWeight.SemiBold, 21),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(15, FontWeight.Normal, 22),
    bodySmall = style(13, FontWeight.Normal, 18),
    labelLarge = style(15, FontWeight.SemiBold, 20),
    labelMedium = style(14, FontWeight.SemiBold, 18),
    labelSmall = style(12, FontWeight.Medium, 16)
)

private val Shape = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FinanzasTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val fin = if (dark) DarkFin else LightFin
    androidx.compose.runtime.CompositionLocalProvider(LocalFin provides fin) {
        MaterialTheme(colorScheme = scheme(fin, dark), shapes = Shape, typography = Type, content = content)
    }
}
