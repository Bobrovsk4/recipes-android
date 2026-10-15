package com.example.recipes.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SettingsStore {
    private const val PREFS = "settings"
    private const val KEY_URL = "base_url"
    private const val KEY_DARK = "dark_theme"
    private const val KEY_ACCENT = "accent_color"
    const val DEFAULT_URL = "http://192.168.0.18:8080/"

    var darkTheme by mutableStateOf<Boolean?>(null)
        private set

    var accentColor by mutableStateOf<Int?>(null)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ApiClient.baseUrl = prefs.getString(KEY_URL, null) ?: DEFAULT_URL
        darkTheme = if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, false) else null
        accentColor = if (prefs.contains(KEY_ACCENT)) prefs.getInt(KEY_ACCENT, 0) else null
    }

    fun saveUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, url)
            .apply()
        ApiClient.baseUrl = url
    }

    fun saveDarkTheme(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK, dark)
            .apply()
        darkTheme = dark
    }

    fun saveAccent(context: Context, color: Int?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (color == null) remove(KEY_ACCENT) else putInt(KEY_ACCENT, color)
        }.apply()
        accentColor = color
    }
}