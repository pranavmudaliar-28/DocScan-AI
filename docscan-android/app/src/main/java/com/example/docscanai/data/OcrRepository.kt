package com.example.docscanai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume

// Serialisable shape of the columns we read from the documents table
@Serializable
private data class OcrFetchResult(
    val ocrBlocks: JsonElement = JsonArray(emptyList()),
    val originalDimensions: JsonElement = buildJsonObject { },
    val filename: String = "",
    val mimeType: String = "",
    val status: String = "",
    val documentCategory: String? = null,
)

// Only the ocrBlocks column for the PATCH call
@Serializable
private data class OcrBlocksUpdate(
    val ocrBlocks: JsonElement,
)

object OcrRepository {

    // ─── Supabase fetch / save ────────────────────────────────────────────────

    suspend fun fetchBlocks(
        docId: String,
    ): Result<Pair<List<OcrBlock>, Pair<Int, Int>>> = withContext(Dispatchers.IO) {
        // Local scan / gallery IDs never exist in the database
        if (docId.isEmpty() || docId.startsWith("scan_") || docId.startsWith("gallery_")) {
            return@withContext Result.failure(Exception("Local-only document"))
        }
        try {
            val row = SupabaseModule.client
                .from("documents")
                .select(
                    columns = Columns.list(
                        "ocrBlocks", "originalDimensions",
                        "filename", "mimeType", "status", "documentCategory",
                    )
                ) {
                    filter { eq("id", docId) }
                }
                .decodeSingle<OcrFetchResult>()

            val blocks = parseBlocksFromJson(row.ocrBlocks)

            val dims = row.originalDimensions
            val w = runCatching {
                dims.jsonObject["width"]?.jsonPrimitive?.int ?: 0
            }.getOrDefault(0)
            val h = runCatching {
                dims.jsonObject["height"]?.jsonPrimitive?.int ?: 0
            }.getOrDefault(0)

            Result.success(Pair(blocks, Pair(w, h)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBlocks(docId: String, blocks: List<OcrBlock>): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (docId.isEmpty() || docId.startsWith("scan_") || docId.startsWith("gallery_")) {
                return@withContext Result.success(Unit)
            }
            try {
                val blocksJson = Json.encodeToJsonElement(blocks)
                SupabaseModule.client
                    .from("documents")
                    .update(OcrBlocksUpdate(ocrBlocks = blocksJson)) {
                        filter { eq("id", docId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── Local ML Kit OCR ─────────────────────────────────────────────────────

    suspend fun runLocalOcr(
        context: Context,
        imageUri: String,
        mimeType: String?,
    ): Triple<List<OcrBlock>, Int, Int> = withContext(Dispatchers.IO) {
        try {
            val bitmap: Bitmap? = when {
                mimeType == "application/pdf" || imageUri.endsWith(".pdf", true) ->
                    renderPdfFirstPage(context, imageUri)
                else ->
                    BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(Uri.parse(imageUri))
                    )
            }
            if (bitmap == null) return@withContext Triple(emptyList(), 0, 0)
            val blocks = runMlKit(bitmap)
            Triple(blocks, bitmap.width, bitmap.height)
        } catch (_: Exception) {
            Triple(emptyList(), 0, 0)
        }
    }

    private suspend fun runMlKit(bitmap: Bitmap): List<OcrBlock> =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result ->
                    val blocks = mutableListOf<OcrBlock>()
                    var idx = 0
                    result.textBlocks.forEach { textBlock ->
                        textBlock.lines.forEach { line ->
                            val box = line.boundingBox ?: return@forEach
                            val text = line.text.trim()
                            if (text.isEmpty()) return@forEach
                            val confidence = (line.confidence?.times(100))?.toInt() ?: 80
                            val fontSize   = (box.height() * 0.65f).coerceAtLeast(10f)
                            blocks.add(
                                OcrBlock(
                                    id         = "mlkit-$idx",
                                    text       = text,
                                    editedText = text,
                                    x          = box.left.toFloat(),
                                    y          = box.top.toFloat(),
                                    width      = box.width().toFloat().coerceAtLeast(40f),
                                    height     = box.height().toFloat().coerceAtLeast(16f),
                                    confidence = confidence,
                                    fontSize   = fontSize,
                                    bold       = false, italic = false,
                                    pageNum    = 1,
                                )
                            )
                            idx++
                        }
                    }
                    cont.resume(blocks)
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

    private fun renderPdfFirstPage(context: Context, uri: String): Bitmap? {
        return try {
            val fd = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r") ?: return null
            val renderer = PdfRenderer(fd)
            if (renderer.pageCount == 0) { renderer.close(); fd.close(); return null }
            val page  = renderer.openPage(0)
            val scale = (context.resources.displayMetrics.widthPixels.toFloat() / page.width).coerceAtMost(3f)
            val bmp   = Bitmap.createBitmap(
                (page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888,
            )
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close(); renderer.close(); fd.close()
            bmp
        } catch (_: Exception) { null }
    }

    // ─── Local text cleanup ───────────────────────────────────────────────────

    fun cleanupText(text: String): String =
        text
            .replace(Regex("^[|\\-*.]+"), "")
            .replace(Regex("[|]+"), " ")
            .replace(Regex("(?<=[A-Za-z])0(?=[A-Za-z])"), "O")
            .replace(Regex("(?<=\\d)O(?=\\d)"), "0")
            .replace(Regex("\\s{2,}"), " ")
            .replace(Regex("\\bI(?=\\d)"), "1")
            .trim()

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun parseBlocksFromJson(json: JsonElement): List<OcrBlock> {
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromJsonElement<List<OcrBlock>>(json)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
