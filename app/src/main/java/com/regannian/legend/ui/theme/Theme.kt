package com.regannian.legend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ReganmianColorScheme = lightColorScheme(
    primary = BrickRed,
    onPrimary = Color.White,
    secondary = BambooGreen,
    onSecondary = Color.White,
    tertiary = SauceBrown,
    background = CreamBg,
    onBackground = InkText,
    surface = CreamBg,
    onSurface = InkText,
)

@Composable
fun ReganmianLegendTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ReganmianColorScheme,
        content = content,
    )
}
