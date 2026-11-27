package com.mefit.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Lime = Color(0xFFC6FF3D)
val Coral = Color(0xFFFF7A59)
val Sky = Color(0xFF4FC3F7)
val Ink = Color(0xFF0E1116)
val Panel = Color(0xFF171B22)
val PanelHigh = Color(0xFF20262F)
val Muted = Color(0xFF8A94A6)

private val scheme = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    primaryContainer = Color(0xFF2B3A0B),
    onPrimaryContainer = Lime,
    secondary = Coral,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A1F17),
    onSecondaryContainer = Coral,
    tertiary = Sky,
    onTertiary = Ink,
    background = Ink,
    onBackground = Color(0xFFECEFF4),
    surface = Ink,
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = PanelHigh,
    onSurfaceVariant = Muted,
    surfaceContainer = Panel,
    surfaceContainerLow = Panel,
    surfaceContainerHigh = PanelHigh,
    surfaceContainerHighest = Color(0xFF2A313C),
    outline = Color(0xFF39414E),
    outlineVariant = Color(0xFF2A313C),
)

private val base = Typography()
private val type = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun MeFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, shapes = shapes, content = content)
}
