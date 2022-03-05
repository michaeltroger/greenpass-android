package com.michaeltroger.gruenerpass.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Green2,
    primaryVariant = Green4,
    onPrimary = Color.Black,

    secondary = Green5,
    secondaryVariant = Color.Black,
    onSecondary = Color.White,
)

private val LightColorPalette = lightColors(
    primary = Green2,
    primaryVariant = Green4,
    onPrimary = Color.White,

    secondary = Green5,
    secondaryVariant = Green6,
    onSecondary = Color.Black,
)

@Composable
fun GreenPassTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}