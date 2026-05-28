package com.example.docscanai

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Splash : NavKey
@Serializable data object Onboarding : NavKey
@Serializable data object Login : NavKey
@Serializable data object Signup : NavKey
@Serializable data object Main : NavKey
@Serializable data object CameraScan : NavKey
@Serializable data object GalleryImport : NavKey
@Serializable data class ScanResult(val scanId: String = "scan_001", val imageUri: String = "") : NavKey
@Serializable data class DocumentViewer(val docId: String = "doc_001", val imageUri: String = "") : NavKey
@Serializable data class DocumentEdit(val docId: String = "", val imageUri: String = "") : NavKey
@Serializable data class TextEdit(val imageUri: String = "") : NavKey
@Serializable data class PdfViewer(val fileUri: String = "") : NavKey
@Serializable data class ImageEditor(val imageUri: String = "") : NavKey
@Serializable data class GenericDocument(val fileUri: String = "") : NavKey
@Serializable data object Convert : NavKey
@Serializable data object AppSettings : NavKey
@Serializable data object Search : NavKey
@Serializable data object PasswordReset : NavKey
@Serializable data object PrivacyPolicy : NavKey
@Serializable data object TermsAndConditions : NavKey
