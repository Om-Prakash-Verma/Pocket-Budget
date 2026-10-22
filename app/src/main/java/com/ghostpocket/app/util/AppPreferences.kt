package com.ghostpocket.app.util

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREFS_NAME = "ghostpocket_app_prefs"
    private const val KEY_TOUR_COMPLETED = "has_completed_tour_v2"
    private const val KEY_ACCOUNT_CREATION_MONTH = "account_creation_month_key"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasCompletedTour(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TOUR_COMPLETED, false)
    }

    fun setTourCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TOUR_COMPLETED, completed).apply()
    }

    fun getAccountCreationMonthKey(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCOUNT_CREATION_MONTH, null)
    }

    fun setAccountCreationMonthKey(context: Context, monthKey: String) {
        getPrefs(context).edit().putString(KEY_ACCOUNT_CREATION_MONTH, monthKey).apply()
    }
}
