package com.example.docscanai.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Accumulates pages from the camera scan session and assembles them into a
 * single multi-page PDF.
 *
 * Lifecycle: cleared when the user finishes a scan session or navigates away.
 * Call [clearPages] after the PDF is created and handed off to navigation.
 */
object MultiPageScanManager {

    data class Page(
        val uri: Uri,
        val label: String,   // e.g. "Page 1"
    )

    /** Observable page list — compose screens can collect this as state. */
    val pages = mutableStateListOf<Page>()

    fun addPage(uri: Uri) {
        pages.add(Page(uri = uri, label = "Page ${pages.size + 1}"))
    }

    fun removePage(index: Int) {
        if (index in pages.indices) pages.removeAt(index)
        // Re-label remaining pages
        pages.forEachIndexed { i, page -> pages[i] = page.copy(label = "Page ${i + 1}") }
    }

    fun clearPages() = pages.clear()

    fun hasPages(): Boolean = pages.isNotEmpty()

    fun pageCount(): Int = pages.size

    // ── PDF generation ────────────────────────────────────────────────────────

    /**
     * Creates a multi-page PDF from all accumulated pages.
     * Returns a content URI usable by the rest of the app.
     * Returns null on failure.
     */
    suspend fun buildPdf(context: Context): Uri? = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) return@withContext null
        try {
            val pdfDoc = PdfDocument()
            pages.forEachIndexed { pageNum, page ->
                val bitmap = loadBitmap(context, page.uri) ?: return@forEachIndexed
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNum + 1).create()
                val pdfPage  = pdfDoc.startPage(pageInfo)
                pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, Paint())
                pdfDoc.finishPage(pdfPage)
                bitmap.recycle()
            }

            val filename = "DocScan_${System.currentTimeMillis()}.pdf"

            // On Q+ write via MediaStore; below Q write to cache and share via FileProvider
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/DocScan AI")
                }
                val mediaUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (mediaUri != null) {
                    context.contentResolver.openOutputStream(mediaUri)?.use { os ->
                        pdfDoc.writeTo(os)
                    }
                    mediaUri
                } else null
            } else {
                val dir  = File(context.cacheDir, "docscan_pdfs").also { it.mkdirs() }
                val file = File(dir, filename)
                pdfDoc.writeTo(FileOutputStream(file))
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            pdfDoc.close()
            uri
        } catch (_: Exception) { null }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) { null }
    }

    /** Returns thumbnail bitmaps for displaying page previews in the UI. */
    suspend fun thumbnails(context: Context, maxSize: Int = 300): List<Bitmap?> =
        withContext(Dispatchers.IO) {
            pages.map { page ->
                try {
                    val full = loadBitmap(context, page.uri) ?: return@map null
                    val scale = maxSize.toFloat() / maxOf(full.width, full.height)
                    Bitmap.createScaledBitmap(
                        full,
                        (full.width  * scale).toInt().coerceAtLeast(1),
                        (full.height * scale).toInt().coerceAtLeast(1),
                        true,
                    ).also { full.recycle() }
                } catch (_: Exception) { null }
            }
        }
}
