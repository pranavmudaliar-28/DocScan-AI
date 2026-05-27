package com.example.docscanai.ui.viewer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

// ─── DocType enum ─────────────────────────────────────────────────────────────

/**
 * Granular file-type classification used to route to the correct editor.
 * Images and scanned PDFs go to the OCR overlay editor.
 * Digital PDFs get the native text viewer.
 * DOCX / TXT / CSV get their dedicated editors.
 */
private enum class DocType {
    IMAGE,          // photo / scanned image — OCR overlay
    SCANNED_PDF,    // PDF with no embedded text — OCR overlay
    DIGITAL_PDF,    // PDF with selectable native text — text viewer
    DOCX,           // Word document
    TXT,            // plain text
    CSV,            // spreadsheet
    UNKNOWN,
}

/**
 * Detect file type from MIME + extension.
 * Digital vs. scanned PDF detection is deferred to the PDF viewer itself
 * (it tries text extraction and routes accordingly).
 */
private fun resolveDocType(context: Context, imageUri: String): DocType {
    if (imageUri.isEmpty()) return DocType.UNKNOWN
    val mime = context.contentResolver.getType(Uri.parse(imageUri))
        ?: run {
            val ext = imageUri.substringAfterLast('.', "").lowercase()
            when (ext) {
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic" -> "image/jpeg"
                "pdf"  -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "txt"  -> "text/plain"
                "csv"  -> "text/csv"
                else   -> null
            }
        }
    return when {
        mime == null                                  -> DocType.UNKNOWN
        mime.startsWith("image/")                     -> DocType.IMAGE
        mime == "application/pdf"                     -> DocType.DIGITAL_PDF  // refined inside PdfRouter
        mime.contains("wordprocessingml")             -> DocType.DOCX
        mime == "text/plain"                          -> DocType.TXT
        mime == "text/csv"
            || imageUri.endsWith(".csv", true)        -> DocType.CSV
        else                                          -> DocType.UNKNOWN
    }
}

// ─── Router ───────────────────────────────────────────────────────────────────

@Composable
fun DocumentEditScreen(
    docId: String,
    imageUri: String,
    onBack: () -> Unit,
    onExtractText: () -> Unit = {},
) {
    val context = LocalContext.current
    val docType = remember(imageUri) { resolveDocType(context, imageUri) }
    val mimeType = remember(imageUri) {
        context.contentResolver.getType(Uri.parse(imageUri))
    }

    when (docType) {
        DocType.IMAGE ->
            OcrOverlayEditorScreen(
                docId    = docId,
                imageUri = imageUri,
                mimeType = mimeType,
                onBack   = onBack,
            )
        DocType.DIGITAL_PDF, DocType.SCANNED_PDF ->
            // All PDFs in this app are scanned — route to OCR overlay
            OcrOverlayEditorScreen(
                docId    = docId,
                imageUri = imageUri,
                mimeType = mimeType ?: "application/pdf",
                onBack   = onBack,
            )
        DocType.DOCX ->
            DocxEditScreen(imageUri = imageUri, onBack = onBack)
        DocType.TXT  ->
            TxtEditScreen(imageUri = imageUri, onBack = onBack)
        DocType.CSV  ->
            CsvEditScreen(imageUri = imageUri, onBack = onBack)
        DocType.UNKNOWN ->
            UnsupportedDocScreen(onBack = onBack)
    }
}

// ─── Unsupported document screen ─────────────────────────────────────────────

@Composable
private fun UnsupportedDocScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Document", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(
                    Icons.Default.InsertDriveFile, null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
                Text(
                    "Unsupported file type",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "This file format cannot be edited in DocScan AI. Try opening it in the native app for this file type.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                OutlinedButton(onClick = onBack) { Text("Go Back") }
            }
        }
    }
}

// ─── Top bar helper ───────────────────────────────────────────────────────────

@Composable
private fun EditorTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(content = actions)
    }
}

// ─── DOCX editor ─────────────────────────────────────────────────────────────

@Composable
private fun DocxEditScreen(imageUri: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var text    by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving  by remember { mutableStateOf(false) }
    var wordCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(imageUri) {
        text = withContext(Dispatchers.IO) { extractDocxText(context, imageUri) }
        wordCount = text.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        EditorTopBar(
            title    = "Word Document",
            subtitle = if (!loading) "$wordCount words" else "Loading…",
            onBack   = onBack,
        ) {
            // DOCX toolbar actions
            IconButton(
                onClick = {
                    saving = true
                    scope.launch {
                        val ok = saveTxtToDownloads(context, text, "DocScan_docx_${System.currentTimeMillis()}.txt")
                        saving = false
                        Toast.makeText(context, if (ok) "Saved to Downloads/DocScan AI" else "Save failed", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !saving && !loading && text.isNotEmpty(),
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Save, "Save", tint = MaterialTheme.colorScheme.primary)
            }
        }

        DocxToolbar()

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Reading Word document…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            OutlinedTextField(
                value         = text,
                onValueChange = {
                    text = it
                    wordCount = it.trim().split("\\s+".toRegex()).count { w -> w.isNotEmpty() }
                },
                modifier      = Modifier.fillMaxSize().padding(12.dp),
                textStyle     = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                placeholder   = { Text("No text could be extracted from this Word document", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
    }
}

@Composable
private fun DocxToolbar() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Pair(Icons.Default.FormatBold, "Bold"),
                Pair(Icons.Default.FormatItalic, "Italic"),
                Pair(Icons.Default.FormatUnderlined, "Underline"),
                Pair(Icons.Default.FormatListBulleted, "List"),
                Pair(Icons.Default.FormatAlignLeft, "Align"),
                Pair(Icons.Default.FindReplace, "Find"),
            ).forEach { (icon, label) ->
                FilterChip(
                    selected = false,
                    onClick  = {},
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(14.dp)) },
                )
            }
        }
    }
}

// ─── TXT editor ──────────────────────────────────────────────────────────────

@Composable
private fun TxtEditScreen(imageUri: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var text    by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving  by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        text = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(imageUri))
                    ?.bufferedReader()?.readText() ?: ""
            } catch (_: Exception) { "" }
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        EditorTopBar(
            title    = "Text File",
            subtitle = ".txt editor",
            onBack   = onBack,
        ) {
            IconButton(
                onClick = {
                    saving = true
                    scope.launch {
                        val ok = saveTxtToDownloads(context, text, "DocScan_txt_${System.currentTimeMillis()}.txt")
                        saving = false
                        Toast.makeText(context, if (ok) "Saved to Downloads/DocScan AI" else "Save failed", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !saving && !loading && text.isNotEmpty(),
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Save, "Save", tint = MaterialTheme.colorScheme.primary)
            }
        }

        TxtToolbar()

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    modifier      = Modifier.fillMaxWidth().weight(1f).padding(12.dp),
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(
                        color      = MaterialTheme.colorScheme.onBackground,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                    placeholder = { Text("Empty file", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors      = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val words = text.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
                    Text("$words words", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${text.length} chars", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TxtToolbar() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Pair(Icons.Default.FindReplace, "Find & Replace"),
                Pair(Icons.Default.ContentCopy, "Copy All"),
                Pair(Icons.Default.WrapText, "Wrap"),
            ).forEach { (icon, label) ->
                FilterChip(
                    selected = false,
                    onClick  = {},
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(14.dp)) },
                )
            }
        }
    }
}

// ─── CSV editor ──────────────────────────────────────────────────────────────

@Composable
private fun CsvEditScreen(imageUri: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var rows    by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving  by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        rows = withContext(Dispatchers.IO) {
            try {
                val raw = context.contentResolver.openInputStream(Uri.parse(imageUri))
                    ?.bufferedReader()?.readText() ?: ""
                parseCsv(raw)
            } catch (_: Exception) { emptyList() }
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        EditorTopBar(
            title    = "CSV Spreadsheet",
            subtitle = if (rows.isNotEmpty()) "${rows.size} rows × ${rows.firstOrNull()?.size ?: 0} cols" else "Loading…",
            onBack   = onBack,
        ) {
            IconButton(
                onClick = {
                    saving = true
                    scope.launch {
                        val csv = rows.joinToString("\n") { row -> row.joinToString(",") { "\"$it\"" } }
                        val ok = saveTxtToDownloads(context, csv, "DocScan_csv_${System.currentTimeMillis()}.csv")
                        saving = false
                        Toast.makeText(context, if (ok) "Saved to Downloads/DocScan AI" else "Save failed", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !saving && rows.isNotEmpty(),
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Save, "Save", tint = MaterialTheme.colorScheme.primary)
            }
        }

        CsvToolbar(
            rowCount = rows.size,
            colCount = rows.firstOrNull()?.size ?: 0,
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Parsing CSV…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data found in CSV file", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                val colCount     = rows.maxOf { it.size }
                val colWidth     = 140.dp
                val hScrollState = rememberScrollState()

                LazyColumn(
                    modifier       = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    itemsIndexed(rows) { rowIdx, row ->
                        Row(
                            modifier = Modifier
                                .horizontalScroll(hScrollState)
                                .background(
                                    when {
                                        rowIdx == 0   -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        rowIdx % 2 == 0 -> MaterialTheme.colorScheme.surfaceContainerLow
                                        else          -> MaterialTheme.colorScheme.surface
                                    }
                                )
                        ) {
                            // Row number
                            Box(
                                modifier = Modifier
                                    .width(36.dp).height(40.dp)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (rowIdx == 0) "#" else "$rowIdx",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // Cells
                            for (colIdx in 0 until colCount) {
                                val cell = row.getOrElse(colIdx) { "" }
                                var editing   by remember(rowIdx, colIdx) { mutableStateOf(false) }
                                var cellValue by remember(rowIdx, colIdx) { mutableStateOf(cell) }

                                Box(
                                    modifier = Modifier
                                        .width(colWidth).height(40.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .clickable(enabled = !editing) { editing = true },
                                ) {
                                    if (editing) {
                                        OutlinedTextField(
                                            value         = cellValue,
                                            onValueChange = { v ->
                                                cellValue = v
                                                val m = rows.map { it.toMutableList() }.toMutableList()
                                                while (m[rowIdx].size <= colIdx) m[rowIdx].add("")
                                                m[rowIdx][colIdx] = v
                                                rows = m
                                            },
                                            singleLine    = true,
                                            textStyle     = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color    = MaterialTheme.colorScheme.onSurface,
                                            ),
                                            modifier = Modifier.fillMaxSize(),
                                            colors   = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            ),
                                        )
                                    } else {
                                        Text(
                                            text  = cellValue,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color    = if (rowIdx == 0) MaterialTheme.colorScheme.onPrimaryContainer
                                                           else MaterialTheme.colorScheme.onSurface,
                                            ),
                                            maxLines = 1,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CsvToolbar(rowCount: Int, colCount: Int) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                "$rowCount rows × $colCount cols",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Pair(Icons.Default.FilterList, "Filter"),
                    Pair(Icons.Default.Sort, "Sort"),
                ).forEach { (icon, label) ->
                    FilterChip(
                        selected = false,
                        onClick  = {},
                        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(icon, null, modifier = Modifier.size(14.dp)) },
                    )
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun extractDocxText(context: Context, imageUri: String): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(Uri.parse(imageUri)) ?: return ""
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val xml = zip.readBytes().toString(Charsets.UTF_8)
                zip.close(); inputStream.close()
                return xml
                    .replace(Regex("<w:p[ />][^>]*>|<w:p>"), "\n")
                    .replace(Regex("<w:br[^>]*/?>"), "\n")
                    .replace(Regex("<[^>]+>"), "")
                    .replace(Regex("[ \t]+"), " ")
                    .trim()
            }
            entry = zip.nextEntry
        }
        zip.close(); inputStream.close(); ""
    } catch (_: Exception) { "" }
}

private fun parseCsv(raw: String): List<List<String>> {
    if (raw.isBlank()) return emptyList()
    return raw.lines().filter { it.isNotBlank() }.map { line ->
        val cells   = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        for (ch in line) {
            when {
                ch == '"'           -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { cells.add(current.toString().trim()); current.clear() }
                else                -> current.append(ch)
            }
        }
        cells.add(current.toString().trim())
        cells
    }
}

private suspend fun saveTxtToDownloads(context: Context, text: String, filename: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/DocScan AI")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            } else {
                val dir = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DocScan AI"
                ).also { it.mkdirs() }
                java.io.File(dir, filename).writeText(text)
            }
            true
        } catch (_: Exception) { false }
    }
