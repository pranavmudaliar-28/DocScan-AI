package com.example.docscanai

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.docscanai.ui.camera.CameraScanScreen
import com.example.docscanai.ui.gallery.GalleryImportScreen
import com.example.docscanai.ui.main.MainScreen
import com.example.docscanai.ui.onboarding.OnboardingScreen
import com.example.docscanai.ui.result.ScanResultScreen
import com.example.docscanai.ui.settings.SettingsScreen
import com.example.docscanai.ui.splash.SplashScreen
import com.example.docscanai.ui.viewer.DocumentViewerScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Splash)

    NavDisplay(
        backStack = backStack,
        onBack    = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry<Splash> {
                SplashScreen(onSplashComplete = {
                    backStack[backStack.lastIndex] = Main
                })
            }

            entry<Onboarding> {
                OnboardingScreen(onFinish = {
                    backStack.removeLastOrNull()
                })
            }

            entry<Main> {
                MainScreen(
                    onScan     = { backStack.add(CameraScan) },
                    onGallery  = { backStack.add(GalleryImport) },
                    onSettings = { backStack.add(AppSettings) },
                    onScanItem = { id -> backStack.add(DocumentViewer(docId = id)) },
                )
            }

            entry<CameraScan> {
                CameraScanScreen(
                    onBack    = { backStack.removeLastOrNull() },
                    onCapture = {
                        backStack.add(ScanResult(scanId = "scan_${System.currentTimeMillis()}"))
                    },
                )
            }

            entry<GalleryImport> {
                GalleryImportScreen(
                    onBack            = { backStack.removeLastOrNull() },
                    onImageSelected   = {
                        backStack.add(ScanResult(scanId = "gallery_${System.currentTimeMillis()}"))
                    },
                )
            }

            entry<ScanResult> { key ->
                ScanResultScreen(
                    scanId          = key.scanId,
                    onBack          = { backStack.removeLastOrNull() },
                    onViewDocument  = { backStack.add(DocumentViewer(docId = key.scanId)) },
                )
            }

            entry<DocumentViewer> { key ->
                DocumentViewerScreen(
                    docId  = key.docId,
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<AppSettings> {
                SettingsScreen(
                    onBack            = { backStack.removeLastOrNull() },
                    onViewOnboarding  = { backStack.add(Onboarding) },
                )
            }
        }
    )
}
