package com.example.recipes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.recipes.data.SettingsStore

private val presets = listOf(
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF00ACC1),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFFD81B60)
)

@Composable
fun AccentCard() {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val accent = SettingsStore.accentColor

    var hue by remember {
        mutableFloatStateOf(
            accent?.let {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(it, hsl)
                hsl[0]
            } ?: 200f
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainer,
        contentColor = scheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Palette, contentDescription = null, tint = scheme.primary)
                Text(
                    "Акцентный цвет",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.primary
                )
            }

            Text(
                if (accent == null) "Сейчас: цвета из обоев" else "Сейчас: свой цвет",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceVariant)
                        .border(
                            width = if (accent == null) 3.dp else 1.dp,
                            color = if (accent == null) scheme.primary else scheme.outlineVariant,
                            shape = CircleShape
                        )
                        .clickable { SettingsStore.saveAccent(context, null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Wallpaper,
                        contentDescription = "Цвета из обоев",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                presets.forEach { color ->
                    val selected = accent == color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = scheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable {
                                SettingsStore.saveAccent(context, color.toArgb())
                                val hsl = FloatArray(3)
                                ColorUtils.colorToHSL(color.toArgb(), hsl)
                                hue = hsl[0]
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            Text(
                "Свой оттенок",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    SettingsStore.saveAccent(context, hslColor(it, 0.7f, 0.5f).toArgb())
                },
                valueRange = 0f..360f
            )
        }
    }
}