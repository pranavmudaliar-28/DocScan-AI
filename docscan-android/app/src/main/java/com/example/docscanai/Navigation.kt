package com.example.docscanai

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.docscanai.data.AuthRepository
import com.example.docscanai.ui.auth.LoginScreen
import com.example.docscanai.ui.auth.PasswordResetScreen
import com.example.docscanai.ui.auth.SignupScreen
import com.example.docscanai.ui.search.SearchScreen
import com.example.docscanai.ui.camera.CameraScanScreen
import com.example.docscanai.ui.gallery.GalleryImportScreen
import com.example.docscanai.ui.main.MainScreen
import com.example.docscanai.ui.onboarding.OnboardingScreen
import com.example.docscanai.ui.result.ScanResultScreen
import com.example.docscanai.ui.settings.SettingsScreen
import com.example.docscanai.ui.splash.SplashScreen
import com.example.docscanai.ui.convert.ConvertScreen
import com.example.docscanai.ui.editor.TextEditScreen
import com.example.docscanai.ui.viewer.DocumentEditScreen
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
                    // Route based on first-launch and auth state
                    val dest = when {
                        !AuthRepository.hasSeenOnboarding() -> Onboarding
                        !AuthRepository.hasToken()          -> Login
                        else                                -> Main
                    }
                    backStack[backStack.lastIndex] = dest
                })
            }

            entry<Onboarding> {
                OnboardingScreen(onFinish = {
                    AuthRepository.markOnboardingShown()
                    backStack[backStack.lastIndex] = Login
                })
            }

            entry<Login> {
                LoginScreen(
                    onLoginSuccess  = { backStack[backStack.lastIndex] = Main },
                    onSignUp        = { backStack.add(Signup) },
                    onForgotPassword = { backStack.add(PasswordReset) },
                )
            }

            entry<Signup> {
                SignupScreen(
                    onSignUpSuccess = {
                        backStack.clear()
                        backStack.add(Main)
                    },
                    onBack          = { backStack.removeLastOrNull() },
                )
            }

            entry<Main> {
                MainScreen(
                    onScan     = { backStack.add(CameraScan) },
                    onGallery  = { backStack.add(GalleryImport) },
                    onSettings = { backStack.add(AppSettings) },
                    onConvert  = { backStack.add(Convert) },
                    onSearch   = { backStack.add(Search) },
                    onScanItem = { record ->
                        backStack.add(DocumentViewer(docId = record.id, imageUri = record.imageUri))
                    },
                )
            }

            entry<CameraScan> {
                CameraScanScreen(
                    onBack    = { backStack.removeLastOrNull() },
                    onGallery = { backStack.add(GalleryImport) },
                    onCapture = { uri: Uri ->
                        backStack.add(
                            ScanResult(
                                scanId   = "scan_${System.currentTimeMillis()}",
                                imageUri = uri.toString(),
                            )
                        )
                    },
                )
            }

            entry<GalleryImport> {
                GalleryImportScreen(
                    onBack          = { backStack.removeLastOrNull() },
                    onImageSelected = { uri: Uri ->
                        backStack.add(
                            ScanResult(
                                scanId   = "gallery_${System.currentTimeMillis()}",
                                imageUri = uri.toString(),
                            )
                        )
                    },
                    onCamera = { backStack[backStack.lastIndex] = CameraScan },
                )
            }

            entry<ScanResult> { key ->
                ScanResultScreen(
                    scanId         = key.scanId,
                    imageUri       = key.imageUri,
                    onBack         = { backStack.removeLastOrNull() },
                    onViewDocument = {
                        backStack.add(DocumentViewer(docId = key.scanId, imageUri = key.imageUri))
                    },
                )
            }

            entry<DocumentViewer> { key ->
                DocumentViewerScreen(
                    docId    = key.docId,
                    imageUri = key.imageUri,
                    onBack   = { backStack.removeLastOrNull() },
                    onEdit   = { backStack.add(DocumentEdit(docId = key.docId, imageUri = key.imageUri)) },
                )
            }

            entry<DocumentEdit> { key ->
                DocumentEditScreen(
                    docId         = key.docId,
                    imageUri      = key.imageUri,
                    onBack        = { backStack.removeLastOrNull() },
                    onExtractText = { backStack.add(TextEdit(imageUri = key.imageUri)) },
                )
            }

            entry<TextEdit> { key ->
                TextEditScreen(
                    imageUri = key.imageUri,
                    onBack   = { backStack.removeLastOrNull() },
                )
            }

            entry<Convert> {
                ConvertScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<Search> {
                SearchScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onResultClick = { record ->
                        backStack.add(DocumentViewer(docId = record.id, imageUri = record.imageUri))
                    }
                )
            }

            entry<PasswordReset> {
                PasswordResetScreen(
                    onBack          = { backStack.removeLastOrNull() },
                    onResetComplete = { backStack[backStack.lastIndex] = Login },
                )
            }

            entry<AppSettings> {
                SettingsScreen(
                    onBack           = { backStack.removeLastOrNull() },
                    onViewOnboarding = { backStack.add(Onboarding) },
                    onSignOut        = {
                        AuthRepository.signOut()
                        // Clear entire back stack down to one entry, replace with Login
                        repeat(backStack.size - 1) { backStack.removeLastOrNull() }
                        backStack[0] = Login
                    },
                )
            }
        }
    )
}
