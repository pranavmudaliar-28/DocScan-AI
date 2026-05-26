package com.example.docscanai.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppThemeRepository {
    private const val PREFS_NAME = "docscan_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkTheme.value = prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun setDarkTheme(context: Context, isDark: Boolean) {
        _isDarkTheme.value = isDark
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, isDark)
            .apply()
    }
}
