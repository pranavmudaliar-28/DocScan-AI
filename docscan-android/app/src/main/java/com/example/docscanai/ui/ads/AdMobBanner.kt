package com.example.docscanai.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // Use Test ID for debug builds to ensure ads show, use Real ID for production
                val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebuggable) {
                    adUnitId = "ca-app-pub-3940256099942544/6300978111" // Google's official Test Banner ID
                } else {
                    adUnitId = "ca-app-pub-3486794301895160/1031054799" // Your Real ID
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
