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
        AppThemeRepository.init(this)
        AuthRepository.init(this)
        ScanHistoryRepository.init(this)
        
        // Initialize AdMob and preload Interstitial
        MobileAds.initialize(this) {}
        AdMobInterstitial.loadAd(this)
        enableEdgeToEdge()
        setContent {
            val isDark by AppThemeRepository.isDarkTheme.collectAsStateWithLifecycle()
            DocScanAITheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainNavigation()
                }
            }
        }
    }
}
