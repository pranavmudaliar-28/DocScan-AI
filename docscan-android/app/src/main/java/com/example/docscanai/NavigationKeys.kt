package com.example.docscanai

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Splash : NavKey
@Serializable data object Onboarding : NavKey
@Serializable data object Main : NavKey
@Serializable data object CameraScan : NavKey
@Serializable data object GalleryImport : NavKey
@Serializable data class ScanResult(val scanId: String = "scan_001") : NavKey
@Serializable data class DocumentViewer(val docId: String = "doc_001") : NavKey
@Serializable data object AppSettings : NavKey
