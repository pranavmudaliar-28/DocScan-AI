package com.example.docscanai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.docscanai.data.AppThemeRepository
import com.example.docscanai.data.AuthRepository
import com.example.docscanai.data.ScanHistoryRepository
import com.example.docscanai.ui.theme.DocScanAITheme
import com.google.android.gms.ads.MobileAds
import com.example.docscanai.ui.ads.AdMobInterstitial

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force the app name in the Recent Apps view (fixes an issue on custom OEM ROMs with debug builds)
        try {
            val label = getString(ai.docscan.app.R.string.app_name)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                setTaskDescription(
                    android.app.ActivityManager.TaskDescription.Builder()
                        .setLabel(label)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                setTaskDescription(
                    android.app.ActivityManager.TaskDescription(label)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        AppThemeRepository.init(this)
        com.example.docscanai.data.AppSettingsRepository.init(this)
        AuthRepository.init(this)
        ScanHistoryRepository.init(this)
        
        // Initialize AdMob and preload Interstitial
        MobileAds.initialize(this) {}
        AdMobInterstitial.loadAd(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by AppThemeRepository.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                com.example.docscanai.data.ThemeMode.DARK -> true
                com.example.docscanai.data.ThemeMode.LIGHT -> false
                com.example.docscanai.data.ThemeMode.SYSTEM_DEFAULT -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            DocScanAITheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val startWithScan = intent?.action == "com.example.docscanai.action.SCAN_DOCUMENT"
                    MainNavigation(startWithScan = startWithScan)
                }
            }
        }
    }
}
