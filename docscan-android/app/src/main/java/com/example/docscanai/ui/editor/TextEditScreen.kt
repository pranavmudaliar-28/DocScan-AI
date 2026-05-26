package com.example.docscanai.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

private sealed interface ExtractState {
    object Loading : ExtractState
    data class Ready(val text: String) : ExtractState
    data class Error(val msg: String) : ExtractState
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun TextEditScreen(
    imageUri: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var state       by remember { mutableStateOf<ExtractState>(ExtractState.Loading) }
    var editedText  by remember { mutableStateOf("") }
    var saving      by remember { mutableStateOf(false) }

    fun doExtract() {
        state = ExtractState.Loading
        scope.launch {
            val result = extractText(context, imageUri)
            result.fold(
                onSuccess = { txt -> state = ExtractState.Ready(txt); editedText = txt },
                onFailure = { e  -> state = ExtractState.Error(e.message ?: "Extraction failed") }
            )
        }
    }

    LaunchedEffect(imageUri) { doExtract() }

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
                "Extract & Edit Text",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                // Save as TXT
                IconButton(
                    onClick = {
                        if (editedText.isNotEmpty()) {
                            saving = true
                            scope.launch {
                                val ok = saveTxtFile(context, editedText)
                                saving = false
                                Toast.makeText(
                                    context,
                                    if (ok) "Saved to Downloads/DocScan AI" else "Save failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = state is ExtractState.Ready && !saving
                ) {
                    if (saving)
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else
                        Icon(Icons.Default.Save, "Save TXT", tint = MaterialTheme.colorScheme.primary)
                }
                // Copy all
                IconButton(
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Extracted text", editedText))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    enabled = state is ExtractState.Ready
                ) {
                    Icon(Icons.Default.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Content ──────────────────────────────────────────────────────────
        when (val s = state) {

            is ExtractState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Extracting text…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is ExtractState.Error -> {
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Could not extract text",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            s.msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { doExtract() }) { Text("Retry") }
                    }
                }
            }

            is ExtractState.Ready -> {
                Column(Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value            = editedText,
                        onValueChange    = { editedText = it },
                        modifier         = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(12.dp),
                        textStyle        = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        placeholder      = {
                            Text(
                                "No text found in document",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors           = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                    // Word / char count
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val words = editedText.trim()
                            .split("\\s+".toRegex())
                            .count { it.isNotEmpty() }
                        Text(
                            "$words words",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${editedText.length} characters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// OCR helpers
// ---------------------------------------------------------------------------

private suspend fun extractText(context: Context, imageUri: String): Result<String> =
    withContext(Dispatchers.Default) {
        try {
            val uri      = Uri.parse(imageUri)
            val mimeType = context.contentResolver.getType(uri) ?: ""
            if (mimeType == "application/pdf") extractFromPdf(context, uri)
            else extractFromImage(context, uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

private suspend fun extractFromImage(context: Context, uri: Uri): Result<String> =
    suspendCancellableCoroutine { cont ->
        try {
            val image      = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { if (cont.isActive) cont.resume(Result.success(it.text)) }
                .addOnFailureListener { if (cont.isActive) cont.resume(Result.failure(it)) }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(Result.failure(e))
        }
    }

private suspend fun extractFromPdf(context: Context, uri: Uri): Result<String> =
    withContext(Dispatchers.IO) {
        try {
            val fd       = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext Result.failure(Exception("Cannot open PDF"))
            val renderer = PdfRenderer(fd)
            val sb       = StringBuilder()

            for (i in 0 until renderer.pageCount) {
                if (i > 0) sb.append("\n\n— Page ${i + 1} —\n\n")
                val page = renderer.openPage(i)
                val bmp  = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                val pageText: String = suspendCancellableCoroutine { cont ->
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(InputImage.fromBitmap(bmp, 0))
                        .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
                        .addOnFailureListener { if (cont.isActive) cont.resume("") }
                }
                sb.append(pageText)
                bmp.recycle()
            }
            renderer.close()
            fd.close()
            Result.success(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

// ---------------------------------------------------------------------------
// Save TXT
// ---------------------------------------------------------------------------

private suspend fun saveTxtFile(context: Context, text: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val fileName = "DocScan_text_${System.currentTimeMillis()}.txt"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/DocScan AI")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            } else {
                val dir = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "DocScan AI"
                ).also { it.mkdirs() }
                java.io.File(dir, fileName).writeText(text)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
