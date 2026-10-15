package com.example.recipes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.recipes.data.SettingsStore
import com.example.recipes.ui.AppNavigation
import com.example.recipes.ui.withAccent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsStore.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val dark = SettingsStore.darkTheme ?: isSystemInDarkTheme()
            val accent = SettingsStore.accentColor
            val context = LocalContext.current
            val view = LocalView.current

            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }

            val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val colorScheme = if (accent != null) withAccent(base, Color(accent), dark) else base

            MaterialTheme(colorScheme = colorScheme) {
                AppNavigation()
            }
        }
    }
}