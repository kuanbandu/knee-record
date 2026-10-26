package com.kuanbandu.kneerecord.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brand = Color(0xFF0A84FF)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    background = Color(0xFFF4F6F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFF4F6F9),
)

@Composable
fun KneeRecordTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
