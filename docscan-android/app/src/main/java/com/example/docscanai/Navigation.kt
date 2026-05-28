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
import com.example.docscanai.ui.settings.PrivacyPolicyScreen
import com.example.docscanai.ui.settings.TermsAndConditionsScreen
import com.example.docscanai.ui.splash.SplashScreen
import com.example.docscanai.ui.convert.ConvertScreen
import com.example.docscanai.ui.editor.TextEditScreen
import com.example.docscanai.ui.viewer.DocumentEditScreen
import com.example.docscanai.ui.viewer.DocumentViewerScreen
import com.example.docscanai.ui.editor.ImageEditorScreen
import com.example.docscanai.ui.viewer.PdfViewerScreen
import com.example.docscanai.ui.viewer.GenericDocumentScreen

import androidx.compose.runtime.LaunchedEffect

@Composable
fun MainNavigation(startWithScan: Boolean = false) {
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
                LaunchedEffect(Unit) {
                    if (startWithScan) {
                        backStack.add(CameraScan)
                    }
                }
                val context = androidx.compose.ui.platform.LocalContext.current
                MainScreen(
                    onScan     = { backStack.add(CameraScan) },
                    onGallery  = { backStack.add(GalleryImport) },
                    onSettings = { backStack.add(AppSettings) },
                    onConvert  = { backStack.add(Convert) },
                    onSearch   = { backStack.add(Search) },
                    onScanItem = { record ->
                        val uri = android.net.Uri.parse(record.imageUri)
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        if (mimeType.startsWith("image/")) {
                            backStack.add(ImageEditor(imageUri = record.imageUri))
                        } else if (mimeType == "application/pdf") {
                            backStack.add(PdfViewer(fileUri = record.imageUri))
                        } else {
                            backStack.add(GenericDocument(fileUri = record.imageUri))
                        }
                    },
                )
            }

            entry<CameraScan> {
                val context = androidx.compose.ui.platform.LocalContext.current
                CameraScanScreen(
                    onBack    = { backStack.removeLastOrNull() },
                    onGallery = { backStack.add(GalleryImport) },
                    onCapture = { uri: Uri ->
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        if (mimeType.startsWith("image/")) {
                            backStack.add(ImageEditor(imageUri = uri.toString()))
                        } else if (mimeType == "application/pdf") {
                            backStack.add(PdfViewer(fileUri = uri.toString()))
                        } else {
                            backStack.add(GenericDocument(fileUri = uri.toString()))
                        }
                    },
                )
            }

            entry<GalleryImport> {
                val context = androidx.compose.ui.platform.LocalContext.current
                GalleryImportScreen(
                    onBack          = { backStack.removeLastOrNull() },
                    onImageSelected = { uri: Uri ->
                        // Attempt to resolve MIME type to route correctly
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        
                        if (mimeType.startsWith("image/")) {
                            backStack.add(ImageEditor(imageUri = uri.toString()))
                        } else if (mimeType == "application/pdf") {
                            backStack.add(PdfViewer(fileUri = uri.toString()))
                        } else {
                            backStack.add(GenericDocument(fileUri = uri.toString()))
                        }
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

            entry<ImageEditor> { key ->
                ImageEditorScreen(
                    imageUri = key.imageUri,
                    onBack = { backStack.removeLastOrNull() },
                    onExtractText = { uri -> backStack.add(TextEdit(imageUri = uri)) }
                )
            }

            entry<PdfViewer> { key ->
                PdfViewerScreen(
                    fileUri = key.fileUri,
                    onBack = { backStack.removeLastOrNull() },
                    onExtractText = { uri -> backStack.add(TextEdit(imageUri = uri)) }
                )
            }

            entry<GenericDocument> { key ->
                GenericDocumentScreen(
                    fileUri = key.fileUri,
                    onBack = { backStack.removeLastOrNull() }
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
                val context = androidx.compose.ui.platform.LocalContext.current
                SearchScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onResultClick = { record ->
                        val uri = android.net.Uri.parse(record.imageUri)
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        if (mimeType.startsWith("image/")) {
                            backStack.add(ImageEditor(imageUri = record.imageUri))
                        } else if (mimeType == "application/pdf") {
                            backStack.add(PdfViewer(fileUri = record.imageUri))
                        } else {
                            backStack.add(GenericDocument(fileUri = record.imageUri))
                        }
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
                    onPrivacyPolicy  = { backStack.add(PrivacyPolicy) },
                    onTermsAndConditions = { backStack.add(TermsAndConditions) },
                    onSignOut        = {
                        AuthRepository.signOut()
                        // Clear entire back stack down to one entry, replace with Login
                        repeat(backStack.size - 1) { backStack.removeLastOrNull() }
                        backStack[0] = Login
                    },
                )
            }

            entry<PrivacyPolicy> {
                PrivacyPolicyScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<TermsAndConditions> {
                TermsAndConditionsScreen(onBack = { backStack.removeLastOrNull() })
            }
        }
    )
}
