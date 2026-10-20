package com.ghostpocket.app.util

import android.content.Context
import android.content.SharedPreferences

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

object ThemePreferences {
    private const val PREFS_NAME = "ghostpocket_theme_prefs"
    private const val KEY_THEME_MODE = "app_theme_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): AppThemeMode {
        val saved = getPrefs(context).getString(KEY_THEME_MODE, AppThemeMode.DARK.name)
        return try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.DARK.name)
        } catch (e: Exception) {
            AppThemeMode.DARK
        }
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}
