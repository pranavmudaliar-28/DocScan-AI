package com.example.docscanai.ui.viewer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

// ─── File-type detection ────────────────────────────────────────────────────────

private enum class DocType { IMAGE, PDF, DOCX, TXT, CSV, UNKNOWN }

private fun resolveDocType(context: Context, imageUri: String): DocType {
    if (imageUri.isEmpty()) return DocType.UNKNOWN
    val mime = context.contentResolver.getType(Uri.parse(imageUri))
        ?: imageUri.substringAfterLast('.', "").let { ext ->
            when (ext.lowercase()) {
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic" -> "image/jpeg"
                "pdf"  -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "txt"  -> "text/plain"
                "csv"  -> "text/csv"
                else   -> null
            }
        }
    return when {
        mime == null                             -> DocType.UNKNOWN
        mime.startsWith("image/")               -> DocType.IMAGE
        mime == "application/pdf"               -> DocType.PDF
        mime.contains("wordprocessingml")       -> DocType.DOCX
        mime == "text/plain"                    -> DocType.TXT
        mime == "text/csv" ||
            imageUri.endsWith(".csv", true)     -> DocType.CSV
        else                                    -> DocType.UNKNOWN
    }
}

// ─── Router ─────────────────────────────────────────────────────────────────────

@Composable
fun DocumentEditScreen(
    docId: String,
    imageUri: String,
    onBack: () -> Unit,
    onExtractText: () -> Unit = {},
) {
    val context = LocalContext.current
    val docType = remember(imageUri) { resolveDocType(context, imageUri) }

    when (docType) {
        DocType.IMAGE, DocType.UNKNOWN ->
            ImageEditScreen(docId = docId, imageUri = imageUri, onBack = onBack, onExtractText = onExtractText)
        DocType.PDF  ->
            PdfViewerScreen(imageUri = imageUri, onBack = onBack, onExtractText = onExtractText)
        DocType.DOCX ->
            DocxEditScreen(imageUri = imageUri, onBack = onBack)
        DocType.TXT  ->
            TxtEditScreen(imageUri = imageUri, onBack = onBack)
        DocType.CSV  ->
            CsvEditScreen(imageUri = imageUri, onBack = onBack)
    }
}

// ─── Top bar helper ──────────────────────────────────────────────────────────────

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
        verticalAlignment = Alignment.CenterVertically,
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

// ─── IMAGE editor ────────────────────────────────────────────────────────────────

@Composable
private fun ImageEditScreen(
    docId: String,
    imageUri: String,
    onBack: () -> Unit,
    onExtractText: () -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var rotationStep by remember { mutableIntStateOf(0) }
    var brightness   by remember { mutableFloatStateOf(0f) }
    var contrast     by remember { mutableFloatStateOf(0f) }
    var saturation   by remember { mutableFloatStateOf(1f) }
    var flipH        by remember { mutableStateOf(false) }
    var saving       by remember { mutableStateOf(false) }

    val rotationDegrees = (rotationStep * 90).toFloat()
    val isEdited = rotationStep != 0 || brightness != 0f || contrast != 0f || saturation != 1f || flipH

    val colorFilter = remember(brightness, contrast, saturation) {
        val scale = 1f + contrast
        val offset = (-0.5f * scale + 0.5f) * 255f + brightness * 255f
        val inv = 1f - saturation
        val lumR = 0.213f; val lumG = 0.715f; val lumB = 0.072f
        ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            (lumR * inv + saturation) * scale, lumG * inv * scale, lumB * inv * scale, 0f, offset,
            lumR * inv * scale, (lumG * inv + saturation) * scale, lumB * inv * scale, 0f, offset,
            lumR * inv * scale, lumG * inv * scale, (lumB * inv + saturation) * scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )))
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        EditorTopBar(
            title    = "Image Editor",
            subtitle = "Adjust · Rotate · Flip",
            onBack   = onBack,
        ) {
            TextButton(
                onClick = {
                    if (isEdited) {
                        saving = true
                        scope.launch {
                            val ok = saveEditedImage(context, imageUri, rotationDegrees, brightness, contrast, flipH)
                            saving = false
                            Toast.makeText(context, if (ok) "Saved to Pictures/DocScan AI" else "Save failed", Toast.LENGTH_SHORT).show()
                            if (ok) onBack()
                        }
                    } else { onBack() }
                },
                enabled = !saving,
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                else Text(if (isEdited) "Save" else "Done", color = MaterialTheme.colorScheme.primary)
            }
        }

        // Preview
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter          = rememberAsyncImagePainter(imageUri),
                contentDescription = "Preview",
                contentScale     = ContentScale.Fit,
                colorFilter      = colorFilter,
                modifier         = Modifier.size(maxWidth * 0.85f).rotate(rotationDegrees),
            )
        }

        // Controls
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            EditSection("Rotate") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedIconButton(onClick = { rotationStep = (rotationStep - 1).mod(4) }) {
                        Icon(Icons.Default.RotateLeft, "Left")
                    }
                    Text("${rotationDegrees.toInt()}°", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    OutlinedIconButton(onClick = { rotationStep = (rotationStep + 1) % 4 }) {
                        Icon(Icons.Default.RotateRight, "Right")
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            EditSection("Flip") {
                FilterChip(selected = flipH, onClick = { flipH = !flipH }, label = { Text("Horizontal") },
                    leadingIcon = { Icon(Icons.Default.Flip, null, modifier = Modifier.size(16.dp)) })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            EditSection("Brightness  ${formatPercent(brightness, -0.5f, 0.5f)}") {
                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -0.5f..0.5f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
            }
            EditSection("Contrast  ${formatPercent(contrast, -0.5f, 0.5f)}") {
                Slider(value = contrast, onValueChange = { contrast = it }, valueRange = -0.5f..0.5f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
            }
            EditSection("Saturation  ${(saturation * 100).toInt()}%") {
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..2f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
            }
            if (isEdited) {
                TextButton(
                    onClick = { rotationStep = 0; brightness = 0f; contrast = 0f; saturation = 1f; flipH = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Reset All", color = MaterialTheme.colorScheme.error) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OutlinedButton(onClick = onExtractText, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.TextFields, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Extract & Edit Text (OCR)")
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

// ─── PDF viewer ──────────────────────────────────────────────────────────────────

@Composable
private fun PdfViewerScreen(
    imageUri: String,
    onBack: () -> Unit,
    onExtractText: () -> Unit,
) {
    val context = LocalContext.current

    // Render all PDF pages to bitmaps
    var pages     by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var pageCount by remember { mutableIntStateOf(0) }
    var loading   by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(Uri.parse(imageUri), "r")
                    ?: throw Exception("Cannot open PDF")
                val renderer = PdfRenderer(fd)
                pageCount = renderer.pageCount
                val bitmaps = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val scale = (context.resources.displayMetrics.widthPixels.toFloat() / page.width).coerceAtMost(3f)
                    val bmp = Bitmap.createBitmap(
                        (page.width * scale).toInt(),
                        (page.height * scale).toInt(),
                        Bitmap.Config.ARGB_8888,
                    )
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bmp)
                }
                renderer.close()
                fd.close()
                pages = bitmaps
            } catch (e: Exception) {
                loadError = e.message ?: "Failed to render PDF"
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        EditorTopBar(
            title    = "PDF Viewer",
            subtitle = if (pageCount > 0) "$pageCount page${if (pageCount > 1) "s" else ""}" else "Loading…",
            onBack   = onBack,
        ) {
            IconButton(onClick = onExtractText) {
                Icon(Icons.Default.TextFields, "Extract text", tint = MaterialTheme.colorScheme.primary)
            }
        }

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Rendering PDF pages…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            loadError != null -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Text("Could not open PDF", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                        Text(loadError!!, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        Button(onClick = onExtractText) {
                            Icon(Icons.Default.TextFields, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Extract Text via OCR")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(pages) { index, bmp ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Page ${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                            Surface(
                                shape  = MaterialTheme.shapes.medium,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Image(
                                    bitmap             = bmp.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    contentScale       = ContentScale.FillWidth,
                                    modifier           = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    item {
                        // Extract-text footer button
                        OutlinedButton(
                            onClick  = onExtractText,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Icon(Icons.Default.TextFields, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Extract & Edit Text (OCR)")
                        }
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }
}

// ─── DOCX editor ─────────────────────────────────────────────────────────────────

@Composable
private fun DocxEditScreen(
    imageUri: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var text    by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving  by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        text = withContext(Dispatchers.IO) { extractDocxText(context, imageUri) }
        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        EditorTopBar(
            title    = "Word Document",
            subtitle = ".docx editor",
            onBack   = onBack,
        ) {
            IconButton(
                onClick = {
                    saving = true
                    scope.launch {
                        val ok = saveTxtToDownloads(context, text, "DocScan_docx_${System.currentTimeMillis()}.txt")
                        saving = false
                        Toast.makeText(context, if (ok) "Saved to Downloads/DocScan AI" else "Save failed", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !saving && text.isNotEmpty(),
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Save, "Save", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Reading Word document…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            OutlinedTextField(
                value            = text,
                onValueChange    = { text = it },
                modifier         = Modifier.fillMaxSize().padding(12.dp),
                textStyle        = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                placeholder      = { Text("No text could be extracted from this Word document", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors           = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
    }
}

// ─── TXT editor ──────────────────────────────────────────────────────────────────

@Composable
private fun TxtEditScreen(
    imageUri: String,
    onBack: () -> Unit,
) {
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

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
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
                enabled = !saving && text.isNotEmpty(),
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Save, "Save", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value            = text,
                    onValueChange    = { text = it },
                    modifier         = Modifier.fillMaxWidth().weight(1f).padding(12.dp),
                    textStyle        = MaterialTheme.typography.bodyMedium.copy(
                        color      = MaterialTheme.colorScheme.onBackground,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                    placeholder      = { Text("Empty file", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors           = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
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

// ─── CSV editor ──────────────────────────────────────────────────────────────────

@Composable
private fun CsvEditScreen(
    imageUri: String,
    onBack: () -> Unit,
) {
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

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        EditorTopBar(
            title    = "CSV Spreadsheet",
            subtitle = if (rows.isNotEmpty()) "${rows.size} rows × ${rows.first().size} cols" else "Loading…",
            onBack   = onBack,
        ) {
            IconButton(
                onClick = {
                    saving = true
                    scope.launch {
                        val csv = rows.joinToString("\n") { row -> row.joinToString(",") { cell -> "\"$cell\"" } }
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

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Parsing CSV…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            rows.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data found in CSV file", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> {
                val colCount = rows.maxOf { it.size }
                val colWidth = 140.dp
                // Single shared state so all rows scroll horizontally in sync
                val hScrollState = rememberScrollState()

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    itemsIndexed(rows) { rowIdx, row ->
                        Row(modifier = Modifier
                            .horizontalScroll(hScrollState)
                            .background(
                                if (rowIdx == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else if (rowIdx % 2 == 0) MaterialTheme.colorScheme.surfaceContainerLow
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            // Row number
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(40.dp)
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
                                var editing by remember(rowIdx, colIdx) { mutableStateOf(false) }
                                var cellValue by remember(rowIdx, colIdx) { mutableStateOf(cell) }

                                Box(
                                    modifier = Modifier
                                        .width(colWidth)
                                        .height(40.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .clickable(enabled = !editing) { editing = true },
                                ) {
                                    if (editing) {
                                        OutlinedTextField(
                                            value = cellValue,
                                            onValueChange = { v ->
                                                cellValue = v
                                                val mutableRows = rows.map { it.toMutableList() }.toMutableList()
                                                while (mutableRows[rowIdx].size <= colIdx) mutableRows[rowIdx].add("")
                                                mutableRows[rowIdx][colIdx] = v
                                                rows = mutableRows
                                            },
                                            singleLine = true,
                                            textStyle  = MaterialTheme.typography.bodySmall.copy(
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
                                            text = cellValue,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color    = if (rowIdx == 0) MaterialTheme.colorScheme.onPrimaryContainer
                                                           else MaterialTheme.colorScheme.onSurface,
                                            ),
                                            maxLines = 1,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
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

// ─── Section label helper ────────────────────────────────────────────────────────

@Composable
private fun EditSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

private fun formatPercent(value: Float, min: Float, max: Float): String {
    val pct = ((value - min) / (max - min) * 100).toInt() - 50
    return if (pct >= 0) "+$pct%" else "$pct%"
}

// ─── DOCX text extraction ────────────────────────────────────────────────────────

private fun extractDocxText(context: Context, imageUri: String): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(Uri.parse(imageUri)) ?: return ""
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val xml = zip.readBytes().toString(Charsets.UTF_8)
                zip.close()
                inputStream.close()
                // Preserve paragraph and line breaks
                return xml
                    .replace(Regex("<w:p[ />][^>]*>|<w:p>"), "\n")
                    .replace(Regex("<w:br[^>]*/?>"), "\n")
                    .replace(Regex("<[^>]+>"), "")
                    .replace(Regex("[ \t]+"), " ")
                    .trim()
            }
            entry = zip.nextEntry
        }
        zip.close()
        inputStream.close()
        ""
    } catch (_: Exception) { "" }
}

// ─── CSV parsing ─────────────────────────────────────────────────────────────────

private fun parseCsv(raw: String): List<List<String>> {
    if (raw.isBlank()) return emptyList()
    return raw.lines()
        .filter { it.isNotBlank() }
        .map { line ->
            val cells = mutableListOf<String>()
            var inQuotes = false
            val current = StringBuilder()
            for (ch in line) {
                when {
                    ch == '"'        -> inQuotes = !inQuotes
                    ch == ',' && !inQuotes -> { cells.add(current.toString().trim()); current.clear() }
                    else             -> current.append(ch)
                }
            }
            cells.add(current.toString().trim())
            cells
        }
}

// ─── Save to Downloads ───────────────────────────────────────────────────────────

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

// ─── Save edited image ───────────────────────────────────────────────────────────

private suspend fun saveEditedImage(
    context: Context,
    imageUri: String,
    rotation: Float,
    brightness: Float,
    contrast: Float,
    flipH: Boolean,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val original = BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(Uri.parse(imageUri))
        ) ?: return@withContext false

        val flipped = if (flipH) {
            val m = android.graphics.Matrix().apply { postScale(-1f, 1f, original.width / 2f, original.height / 2f) }
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, m, true)
        } else original

        val rotMatrix = android.graphics.Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(flipped, 0, 0, flipped.width, flipped.height, rotMatrix, true)

        val scale = 1f + contrast
        val totalOffset = (-0.5f * scale + 0.5f) * 255f + brightness * 255f
        val cm = android.graphics.ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, totalOffset,
            0f, scale, 0f, 0f, totalOffset,
            0f, 0f, scale, 0f, totalOffset,
            0f, 0f, 0f, 1f, 0f,
        ))
        val paint = android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(cm) }
        val result = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(result).drawBitmap(rotated, 0f, 0f, paint)

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "DocScan_edit_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DocScan AI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val destUri  = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        resolver.openOutputStream(destUri)?.use { result.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(destUri, values, null, null)
        }
        true
    } catch (_: Exception) { false }
}
