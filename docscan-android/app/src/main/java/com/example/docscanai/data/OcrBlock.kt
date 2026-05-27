package com.example.docscanai.data

import kotlinx.serialization.Serializable

@Serializable
data class OcrBlock(
    val id: String,
    val text: String,
    val editedText: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val confidence: Int,
    val fontSize: Float = 13f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val color: String = "#1a1a1a",
    val pageNum: Int = 1,
) {
    val confidenceLevel: ConfidenceLevel get() = when {
        confidence >= 85 -> ConfidenceLevel.HIGH
        confidence >= 60 -> ConfidenceLevel.MEDIUM
        else             -> ConfidenceLevel.LOW
    }

    fun withEdit(newText: String) = copy(editedText = newText)
}

@Serializable
enum class ConfidenceLevel { HIGH, MEDIUM, LOW }
