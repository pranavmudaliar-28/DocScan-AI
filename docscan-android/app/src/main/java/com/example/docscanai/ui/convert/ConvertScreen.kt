package com.example.docscanai.ui.convert

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

private enum class ConvertStatus { Pending, Converting, Done, Error }

private data class ConvertItem(
    val id: Long = System.nanoTime(),
    val sourceUri: Uri,
    val sourceName: String,
    val sourceMime: String,
    val status: ConvertStatus = ConvertStatus.Pending,
    val resultFile: File? = null,
    val resultName: String = "",
    val errorMsg: String = "",
)

private val OUTPUT_FORMATS = listOf("PDF", "JPEG", "PNG", "WEBP", "TXT")

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun ConvertScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf("PDF") }
    val items          = remember { mutableStateListOf<ConvertItem>() }
    var converting     by remember { mutableStateOf(false) }
    var zipping        by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            if (items.none { it.sourceUri == uri }) {
                items.add(
                    ConvertItem(
                        sourceUri  = uri,
                        sourceName = getDisplayName(context, uri),
                        sourceMime = context.contentResolver.getType(uri) ?: "",
                    )
                )
            }
        }
    }

    val pendingCount = items.count { it.status == ConvertStatus.Pending }
    val doneCount    = items.count { it.status == ConvertStatus.Done }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Convert Files",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Download All as ZIP (shown only when ≥2 done)
            if (doneCount >= 2) {
                IconButton(
                    onClick = {
                        zipping = true
                        scope.launch {
                            val ok = downloadAllZip(context, items.filter { it.status == ConvertStatus.Done })
                            zipping = false
                            Toast.makeText(
                                context,
                                if (ok) "ZIP saved to Downloads/DocScan AI" else "ZIP failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !zipping
                ) {
                    if (zipping)
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else
                        Icon(Icons.Default.Archive, "Download all ZIP", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        LazyColumn(
            contentPadding        = PaddingValues(bottom = 100.dp),
            verticalArrangement   = Arrangement.spacedBy(0.dp),
            modifier              = Modifier.weight(1f)
        ) {
            // ── Format selector ──────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        "Output Format",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(OUTPUT_FORMATS) { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick  = { if (!converting) selectedFormat = fmt },
                                label    = { Text(fmt) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                                )
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // ── Add files button ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !converting) {
                            filePicker.launch(arrayOf("image/*", "image/jpeg", "image/png", "application/pdf"))
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            "Add Files",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Images and PDFs · tap to select multiple",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (items.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // ── File list ─────────────────────────────────────────────────────
            items(items, key = { it.id }) { item ->
                FileItemRow(
                    item           = item,
                    outputFormat   = selectedFormat,
                    onRemove       = { if (!converting) items.removeIf { it.id == item.id } },
                    onDownload     = {
                        scope.launch {
                            val ok = downloadSingle(context, item)
                            Toast.makeText(
                                context,
                                if (ok) "Saved to ${if (selectedFormat == "TXT") "Downloads" else "Pictures"}/DocScan AI" else "Save failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Empty state
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows, null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                "Add files to convert",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom action bar ─────────────────────────────────────────────────
        if (pendingCount > 0 || converting) {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick  = {
                            converting = true
                            scope.launch {
                                val jobs = items.mapIndexed { idx, item ->
                                    async {
                                        if (item.status != ConvertStatus.Pending) return@async
                                        items[idx] = item.copy(status = ConvertStatus.Converting)
                                        val outFile = doConvert(context, item, selectedFormat)
                                        items[idx] = if (outFile != null) {
                                            item.copy(
                                                status     = ConvertStatus.Done,
                                                resultFile = outFile,
                                                resultName = buildResultName(item.sourceName, selectedFormat),
                                            )
                                        } else {
                                            item.copy(status = ConvertStatus.Error, errorMsg = "Conversion failed")
                                        }
                                    }
                                }
                                jobs.awaitAll()
                                converting = false
                            }
                        },
                        enabled = pendingCount > 0 && !converting,
                    ) {
                        if (converting) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (converting) "Converting…" else "Convert $pendingCount File${if (pendingCount != 1) "s" else ""}")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// File item row
// ---------------------------------------------------------------------------

@Composable
private fun FileItemRow(
    item: ConvertItem,
    outputFormat: String,
    onRemove: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    when (item.status) {
                        ConvertStatus.Done       -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ConvertStatus.Error      -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ConvertStatus.Converting -> MaterialTheme.colorScheme.primaryContainer
                        else                     -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            when (item.status) {
                ConvertStatus.Converting ->
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                ConvertStatus.Done ->
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                ConvertStatus.Error ->
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                else -> {
                    val icon = if (item.sourceMime == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.Image
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Name + status label
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.sourceName,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                when (item.status) {
                    ConvertStatus.Pending    -> "→ $outputFormat"
                    ConvertStatus.Converting -> "Converting…"
                    ConvertStatus.Done       -> "Ready · ${item.resultName}"
                    ConvertStatus.Error      -> item.errorMsg
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (item.status) {
                    ConvertStatus.Done  -> MaterialTheme.colorScheme.primary
                    ConvertStatus.Error -> MaterialTheme.colorScheme.error
                    else                -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Action buttons
        if (item.status == ConvertStatus.Done) {
            IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FileDownload, "Download", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        } else if (item.status == ConvertStatus.Pending) {
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Conversion logic
// ---------------------------------------------------------------------------

private suspend fun doConvert(context: Context, item: ConvertItem, format: String): File? {
    val mime = item.sourceMime
    return when {
        format == "TXT"                                    -> convertToTxt(context, item.sourceUri, mime)
        format == "PDF" && mime.startsWith("image/")       -> imageToPdf(context, item.sourceUri)
        mime.startsWith("image/")                          -> convertImageFormat(context, item.sourceUri, format)
        mime == "application/pdf" && format != "PDF"       -> pdfToImage(context, item.sourceUri, format)
        else                                               -> null
    }
}

private fun cacheDir(context: Context) =
    File(context.cacheDir, "convert_output").also { it.mkdirs() }

private fun cacheFile(context: Context, ext: String) =
    File(cacheDir(context), "convert_${System.currentTimeMillis()}_${(0..999).random()}.$ext")

// Image → PDF
private suspend fun imageToPdf(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
    try {
        val bmp = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri)) ?: return@withContext null
        val doc  = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create()
        val page = doc.startPage(info)
        page.canvas.drawBitmap(bmp, 0f, 0f, null)
        doc.finishPage(page)
        val out = cacheFile(context, "pdf")
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        out
    } catch (e: Exception) { null }
}

// Image → JPEG / PNG / WEBP
private suspend fun convertImageFormat(context: Context, uri: Uri, format: String): File? =
    withContext(Dispatchers.IO) {
        try {
            val bmp = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri)) ?: return@withContext null
            val compressFormat = when (format) {
                "PNG"  -> Bitmap.CompressFormat.PNG
                "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
                else   -> Bitmap.CompressFormat.JPEG
            }
            val out = cacheFile(context, format.lowercase())
            FileOutputStream(out).use { bmp.compress(compressFormat, 95, it) }
            out
        } catch (e: Exception) { null }
    }

// PDF first page → image
private suspend fun pdfToImage(context: Context, uri: Uri, format: String): File? =
    withContext(Dispatchers.IO) {
        try {
            val fd       = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            val renderer = PdfRenderer(fd)
            val page     = renderer.openPage(0)
            val bmp      = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            val compressFormat = when (format) {
                "PNG"  -> Bitmap.CompressFormat.PNG
                "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
                else   -> Bitmap.CompressFormat.JPEG
            }
            val out = cacheFile(context, format.lowercase())
            FileOutputStream(out).use { bmp.compress(compressFormat, 95, it) }
            out
        } catch (e: Exception) { null }
    }

// Any → TXT via ML Kit OCR
private suspend fun convertToTxt(context: Context, uri: Uri, mime: String): File? =
    withContext(Dispatchers.IO) {
        try {
            val text = if (mime == "application/pdf") extractPdfText(context, uri)
                       else extractImageText(context, uri)
            val out = cacheFile(context, "txt")
            out.writeText(text)
            out
        } catch (e: Exception) { null }
    }

private suspend fun extractImageText(context: Context, uri: Uri): String =
    suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
                .addOnFailureListener { if (cont.isActive) cont.resume("") }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume("")
        }
    }

private suspend fun extractPdfText(context: Context, uri: Uri): String =
    withContext(Dispatchers.IO) {
        val fd       = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext ""
        val renderer = PdfRenderer(fd)
        val sb       = StringBuilder()
        for (i in 0 until renderer.pageCount) {
            if (i > 0) sb.append("\n\n— Page ${i + 1} —\n\n")
            val page = renderer.openPage(i)
            val bmp  = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            val pageText: String = suspendCancellableCoroutine { cont ->
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
                    .addOnFailureListener { if (cont.isActive) cont.resume("") }
            }
            sb.append(pageText)
            bmp.recycle()
        }
        renderer.close()
        fd.close()
        sb.toString()
    }

// ---------------------------------------------------------------------------
// Download helpers
// ---------------------------------------------------------------------------

// Copy single result to MediaStore
private suspend fun downloadSingle(context: Context, item: ConvertItem): Boolean =
    withContext(Dispatchers.IO) {
        val file = item.resultFile ?: return@withContext false
        try {
            val isTxt    = file.name.endsWith(".txt")
            val isPdf    = file.name.endsWith(".pdf")
            val mimeType = when {
                isTxt -> "text/plain"
                isPdf -> "application/pdf"
                file.name.endsWith(".png") -> "image/png"
                file.name.endsWith(".webp") -> "image/webp"
                else -> "image/jpeg"
            }
            if (isTxt || isPdf) {
                // Save to Downloads
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, item.resultName)
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/DocScan AI")
                    }
                    val dest = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext false
                    context.contentResolver.openOutputStream(dest)?.use { file.inputStream().copyTo(it) }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DocScan AI").also { it.mkdirs() }
                    file.copyTo(File(dir, item.resultName), overwrite = true)
                }
            } else {
                // Save to Pictures
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, item.resultName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DocScan AI")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val dest = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                context.contentResolver.openOutputStream(dest)?.use { file.inputStream().copyTo(it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(dest, values, null, null)
                }
            }
            true
        } catch (e: Exception) { false }
    }

// ZIP all done results and save to Downloads
private suspend fun downloadAllZip(context: Context, doneItems: List<ConvertItem>): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val zipName = "DocScan_converted_${System.currentTimeMillis()}.zip"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, zipName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/DocScan AI")
                }
                val dest = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                context.contentResolver.openOutputStream(dest)?.use { os ->
                    ZipOutputStream(os).use { zos ->
                        doneItems.forEach { item ->
                            val f = item.resultFile ?: return@forEach
                            zos.putNextEntry(ZipEntry(item.resultName.ifEmpty { f.name }))
                            f.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            } else {
                val dir  = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DocScan AI").also { it.mkdirs() }
                val zip  = File(dir, zipName)
                FileOutputStream(zip).use { os ->
                    ZipOutputStream(os).use { zos ->
                        doneItems.forEach { item ->
                            val f = item.resultFile ?: return@forEach
                            zos.putNextEntry(ZipEntry(item.resultName.ifEmpty { f.name }))
                            f.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) { false }
    }

// ---------------------------------------------------------------------------
// Utils
// ---------------------------------------------------------------------------

private fun getDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return cursor.getString(idx) ?: "file"
        }
    }
    return uri.lastPathSegment ?: "file"
}

private fun buildResultName(sourceName: String, format: String): String {
    val base = sourceName.substringBeforeLast('.').ifEmpty { sourceName }
    return "$base.${format.lowercase()}"
}
