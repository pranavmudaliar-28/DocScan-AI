package com.example.docscanai.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppSettingsRepository {
    private const val PREFS_NAME = "docscan_app_settings"

    private val _highQuality = MutableStateFlow(true)
    val highQuality: StateFlow<Boolean> = _highQuality

    private val _autoDetect = MutableStateFlow(true)
    val autoDetect: StateFlow<Boolean> = _autoDetect

    private val _saveMetadata = MutableStateFlow(false)
    val saveMetadata: StateFlow<Boolean> = _saveMetadata

    private val _notifications = MutableStateFlow(true)
    val notifications: StateFlow<Boolean> = _notifications

    private val _aiSuggestions = MutableStateFlow(true)
    val aiSuggestions: StateFlow<Boolean> = _aiSuggestions

    private val _autoCleanup = MutableStateFlow(true)
    val autoCleanup: StateFlow<Boolean> = _autoCleanup

    private val _smartTagging = MutableStateFlow(true)
    val smartTagging: StateFlow<Boolean> = _smartTagging

    private val _autoSync = MutableStateFlow(true)
    val autoSync: StateFlow<Boolean> = _autoSync

    private val _wifiOnlySync = MutableStateFlow(false)
    val wifiOnlySync: StateFlow<Boolean> = _wifiOnlySync

    private val _viewMode = MutableStateFlow("LIST") // "GRID" or "LIST"
    val viewMode: StateFlow<String> = _viewMode

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _highQuality.value = prefs.getBoolean("highQuality", true)
        _autoDetect.value = prefs.getBoolean("autoDetect", true)
        _saveMetadata.value = prefs.getBoolean("saveMetadata", false)
        _notifications.value = prefs.getBoolean("notifications", true)
        _aiSuggestions.value = prefs.getBoolean("aiSuggestions", true)
        _autoCleanup.value = prefs.getBoolean("autoCleanup", true)
        _smartTagging.value = prefs.getBoolean("smartTagging", true)
        _autoSync.value = prefs.getBoolean("autoSync", true)
        _wifiOnlySync.value = prefs.getBoolean("wifiOnlySync", false)
        _viewMode.value = prefs.getString("viewMode", "LIST") ?: "LIST"
    }

    fun setSetting(context: Context, key: String, value: Boolean) {
        when (key) {
            "highQuality" -> _highQuality.value = value
            "autoDetect" -> _autoDetect.value = value
            "saveMetadata" -> _saveMetadata.value = value
            "notifications" -> _notifications.value = value
            "aiSuggestions" -> _aiSuggestions.value = value
            "autoCleanup" -> _autoCleanup.value = value
            "smartTagging" -> _smartTagging.value = value
            "autoSync" -> _autoSync.value = value
            "wifiOnlySync" -> _wifiOnlySync.value = value
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    fun setViewMode(context: Context, mode: String) {
        _viewMode.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("viewMode", mode)
            .apply()
    }
}
