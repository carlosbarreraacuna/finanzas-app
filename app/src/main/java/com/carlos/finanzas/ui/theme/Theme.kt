package com.carlos.finanzas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Teal profundo = dinero bajo control; ámbar = atención; ladrillo = lo que debo.
private val Light = lightColorScheme(
    primary = Color(0xFF0E5C57),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEBE6),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF8A5A00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDEA6),
    onSecondaryContainer = Color(0xFF2B1800),
    error = Color(0xFFA83A1F),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFF1F5F4),
    onBackground = Color(0xFF14201F),
    surface = Color(0xFFF1F5F4),
    onSurface = Color(0xFF14201F),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF4B5C5A),
    outline = Color(0xFF8A9A98)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FD0C7),
    onPrimary = Color(0xFF00201E),
    primaryContainer = Color(0xFF0B4A46),
    onPrimaryContainer = Color(0xFFCDEBE6),
    secondary = Color(0xFFF2BD62),
    onSecondary = Color(0xFF2B1800),
    secondaryContainer = Color(0xFF5E3D00),
    onSecondaryContainer = Color(0xFFFFDEA6),
    error = Color(0xFFFFA58B),
    onError = Color(0xFF4A1100),
    background = Color(0xFF0D1615),
    onBackground = Color(0xFFDCE6E4),
    surface = Color(0xFF0D1615),
    onSurface = Color(0xFFDCE6E4),
    surfaceVariant = Color(0xFF16221F),
    onSurfaceVariant = Color(0xFFA9BAB7),
    outline = Color(0xFF5E706D)
)

private val Shape = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),   // campos de texto
    medium = RoundedCornerShape(22.dp),  // tarjetas
    large = RoundedCornerShape(28.dp),   // diálogos
    extraLarge = CircleShape
)

// Cifras tabulares: los montos se alinean en columna.
private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = "tnum"
)

private val Type = Typography(
    headlineSmall = style(26, FontWeight.Bold, 32, -0.4),
    titleMedium = style(18, FontWeight.SemiBold, 24),
    titleSmall = style(15, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 21),
    bodySmall = style(12, FontWeight.Normal, 18),
    labelMedium = style(12, FontWeight.Medium, 16),
    labelLarge = style(14, FontWeight.SemiBold, 20)
)

@Composable
fun FinanzasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = Shape,
        typography = Type,
        content = content
    )
}
