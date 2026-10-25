package com.vrajapro.japa.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** "Warm Saffron & Cream · Temple sunrise" palette from the web app. */
object JapaColors {
    val Bg = Color(0xFFFDF4E7)
    val Bg2 = Color(0xFFFEF8F0)
    val Surface = Color(0xFFF8E8CF)
    val Surface2 = Color(0xFFEED5B0)
    val Saffron = Color(0xFFD96814)
    val SaffronDeep = Color(0xFFA84D06)
    val SaffronLight = Color(0xFFF09040)
    val GradStart = Color(0xFFF5A254)
    val GradEnd = Color(0xFFC45E0A)
    val Terra = Color(0xFF9A4E35)
    val Amber = Color(0xFFB87C08)
    val Text = Color(0xFF2A1305)
    val TextDim = Color(0xFF6B4220)
    val Border = Color(0xFFD9BFA0)
}

private val scheme = lightColorScheme(
    primary = JapaColors.Saffron,
    onPrimary = Color.White,
    primaryContainer = JapaColors.Surface,
    onPrimaryContainer = JapaColors.Text,
    secondary = JapaColors.Terra,
    onSecondary = Color.White,
    background = JapaColors.Bg,
    onBackground = JapaColors.Text,
    surface = JapaColors.Bg2,
    onSurface = JapaColors.Text,
    surfaceVariant = JapaColors.Surface,
    onSurfaceVariant = JapaColors.TextDim,
    surfaceContainerHigh = JapaColors.Bg2,
    outline = JapaColors.Border,
    outlineVariant = JapaColors.Surface2,
)

@Composable
fun JapaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
