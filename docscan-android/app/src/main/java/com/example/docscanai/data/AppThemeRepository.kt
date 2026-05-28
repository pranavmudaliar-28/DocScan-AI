package com.example.docscanai.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM_DEFAULT, LIGHT, DARK }

object AppThemeRepository {
    private const val PREFS_NAME = "docscan_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM_DEFAULT)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM_DEFAULT.name) ?: ThemeMode.SYSTEM_DEFAULT.name
        _themeMode.value = try { ThemeMode.valueOf(modeStr) } catch (e: Exception) { ThemeMode.SYSTEM_DEFAULT }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }
}
