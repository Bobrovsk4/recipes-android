package com.example.recipes.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

fun hslColor(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    return Color(
        ColorUtils.HSLToColor(floatArrayOf(hue, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f)))
    )
}

fun withAccent(base: ColorScheme, seed: Color, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val h = hsl[0]
    val s = hsl[1].coerceIn(0.4f, 0.9f)

    return if (dark) {
        base.copy(
            primary = hslColor(h, s * 0.9f, 0.72f),
            onPrimary = hslColor(h, s, 0.15f),
            primaryContainer = hslColor(h, s * 0.7f, 0.25f),
            onPrimaryContainer = hslColor(h, s, 0.90f)
        )
    } else {
        base.copy(
            primary = hslColor(h, s, 0.40f),
            onPrimary = Color.White,
            primaryContainer = hslColor(h, s, 0.90f),
            onPrimaryContainer = hslColor(h, s, 0.12f)
        )
    }
}