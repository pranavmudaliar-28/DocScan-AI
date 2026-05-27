package com.example.docscanai.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.docscanai.data.ConfidenceLevel
import com.example.docscanai.data.OcrBlock
import com.example.docscanai.data.OcrRepository
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.IntelligentBlue
import com.example.docscanai.ui.theme.SoftBackground
import com.example.docscanai.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ─── State ───────────────────────────────────────────────────────────────────

private sealed interface OcrLoadState {
    object Loading : OcrLoadState
    data class Ready(val blocks: List<OcrBlock>, val imgW: Int, val imgH: Int) : OcrLoadState
    object Empty : OcrLoadState   // loaded but zero blocks
    data class Error(val msg: String) : OcrLoadState
}

// ─── Screen ───────────────────────────────────────────────────────────────────

/**
 * Full inline OCR overlay editor.
 *
 * Shows the document image with tappable, editable text overlays at the exact
 * coordinates returned by ML Kit / Tesseract. Each block can be tapped to edit,
 * auto-cleaned, copied, or exported as plain text.
 *
 * @param docId  Backend MongoDB ID — if a "scan_*" / "gallery_*" prefix or empty,
 *               the backend is skipped and ML Kit runs on-device.
 * @param imageUri  Content URI or file URI of the source document.
 * @param mimeType  MIME type hint, e.g. "application/pdf".
 * @param onBack  Navigation callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrOverlayEditorScreen(
    docId: String,
    imageUri: String,
    mimeType: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val density = LocalDensity.current

    // ── Core state ────────────────────────────────────────────────────────────
    var loadState     by remember { mutableStateOf<OcrLoadState>(OcrLoadState.Loading) }
    var blocks        by remember { mutableStateOf<List<OcrBlock>>(emptyList()) }
    var originalW     by remember { mutableIntStateOf(0) }
    var originalH     by remember { mutableIntStateOf(0) }
    var selectedBlock by remember { mutableStateOf<OcrBlock?>(null) }
    var showEditDialog  by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var saving        by remember { mutableStateOf(false) }

    // Undo / redo stacks
    val undoHistory = remember { ArrayDeque<List<OcrBlock>>() }
    val redoHistory = remember { ArrayDeque<List<OcrBlock>>() }

    fun pushUndo(snapshot: List<OcrBlock>) {
        undoHistory.addLast(snapshot)
        if (undoHistory.size > 40) undoHistory.removeFirst()
        redoHistory.clear()
    }

    fun undo() {
        if (undoHistory.isEmpty()) return
        redoHistory.addLast(blocks.toList())
        blocks = undoHistory.removeLast()
        selectedBlock = null
    }

    fun redo() {
        if (redoHistory.isEmpty()) return
        undoHistory.addLast(blocks.toList())
        blocks = redoHistory.removeLast()
        selectedBlock = null
    }

    // ── Load blocks (backend → ML Kit fallback) ───────────────────────────────
    LaunchedEffect(docId, imageUri) {
        loadState = OcrLoadState.Loading
        var loaded = false

        // Try backend first
        if (docId.isNotEmpty() && !docId.startsWith("scan_") && !docId.startsWith("gallery_")) {
            OcrRepository.fetchBlocks(docId).onSuccess { (serverBlocks, dims) ->
                if (serverBlocks.isNotEmpty() && dims.first > 0) {
                    blocks    = serverBlocks
                    originalW = dims.first
                    originalH = dims.second
                    loadState = if (serverBlocks.isEmpty()) OcrLoadState.Empty
                                else OcrLoadState.Ready(serverBlocks, dims.first, dims.second)
                    loaded = true
                }
            }
        }

        // Fall back to local ML Kit
        if (!loaded) {
            val (mlBlocks, w, h) = OcrRepository.runLocalOcr(context, imageUri, mimeType)
            blocks    = mlBlocks
            originalW = w
            originalH = h
            loadState = when {
                mlBlocks.isEmpty() -> OcrLoadState.Empty
                else               -> OcrLoadState.Ready(mlBlocks, w, h)
            }
        }
    }

    // ── AI auto-cleanup ───────────────────────────────────────────────────────
    fun runAutoCleanup() {
        pushUndo(blocks.toList())
        blocks = blocks.map { it.withEdit(OcrRepository.cleanupText(it.editedText)) }
        Toast.makeText(context, "Auto-cleaned ${blocks.size} blocks", Toast.LENGTH_SHORT).show()
    }

    // ── Save edits to backend ─────────────────────────────────────────────────
    fun saveToBackend() {
        saving = true
        scope.launch {
            OcrRepository.saveBlocks(docId, blocks)
            saving = false
            Toast.makeText(context, "Edits saved", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Export all text ───────────────────────────────────────────────────────
    fun exportText(): String = blocks
        .sortedWith(compareBy({ it.pageNum }, { it.y }, { it.x }))
        .joinToString("\n") { it.editedText }

    // ─────────────────────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Toolbar ───────────────────────────────────────────────────────────
        OcrToolbar(
            blockCount   = blocks.size,
            canUndo      = undoHistory.isNotEmpty(),
            canRedo      = redoHistory.isNotEmpty(),
            saving       = saving,
            onBack       = onBack,
            onUndo       = { undo() },
            onRedo       = { redo() },
            onCleanup    = { runAutoCleanup() },
            onExport     = { showExportSheet = true },
            onSave       = { saveToBackend() },
            hasBackendId = docId.isNotEmpty() && !docId.startsWith("scan_") && !docId.startsWith("gallery_"),
        )

        // ── AI status strip ───────────────────────────────────────────────────
        if (loadState is OcrLoadState.Ready) {
            val lowConfidenceCount = blocks.count { it.confidence < 80 }
            val avgConfidence = if (blocks.isNotEmpty())
                blocks.sumOf { it.confidence.toDouble() }.toInt() / blocks.size
            else 0
            Surface(
                color = if (lowConfidenceCount > 0) SoftBackground else Color(0xFFE7F4EC),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (lowConfidenceCount > 0) IntelligentBlue.copy(alpha = 0.3f)
                    else SuccessGreen.copy(alpha = 0.4f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint     = if (lowConfidenceCount > 0) IntelligentBlue else SuccessGreen,
                        modifier = Modifier.size(16.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (lowConfidenceCount > 0)
                                "$lowConfidenceCount low-confidence region${if (lowConfidenceCount != 1) "s" else ""} flagged"
                            else "OCR complete · all blocks high confidence",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (lowConfidenceCount > 0) IntelligentBlue else SuccessGreen,
                        )
                        Text(
                            "${blocks.size} BLOCKS · ${avgConfidence}% AVG CONFIDENCE",
                            fontSize      = 10.sp,
                            color         = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp,
                        )
                    }
                    if (lowConfidenceCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = IntelligentBlue,
                        ) {
                            Text(
                                "Fix",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color.White,
                                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── Body ──────────────────────────────────────────────────────────────
        when (loadState) {
            is OcrLoadState.Loading -> LoadingPanel()
            is OcrLoadState.Error   -> ErrorPanel((loadState as OcrLoadState.Error).msg)
            is OcrLoadState.Empty   -> EmptyOcrPanel(
                onRetry = {
                    scope.launch {
                        loadState = OcrLoadState.Loading
                        val (mlBlocks, w, h) = OcrRepository.runLocalOcr(context, imageUri, mimeType)
                        blocks    = mlBlocks
                        originalW = w
                        originalH = h
                        loadState = if (mlBlocks.isEmpty()) OcrLoadState.Empty
                                    else OcrLoadState.Ready(mlBlocks, w, h)
                    }
                }
            )
            is OcrLoadState.Ready -> {
                // ── Overlay canvas ─────────────────────────────────────────────
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    val containerW = with(density) { maxWidth.toPx() }
                    val containerH = with(density) { maxHeight.toPx() }
                    val imgRect    = computeImageRect(containerW, containerH, originalW, originalH)
                    val scaleX     = if (originalW > 0) imgRect.width / originalW else 1f
                    val scaleY     = if (originalH > 0) imgRect.height / originalH else 1f

                    // Base document image
                    AsyncImage(
                        model              = imageUri,
                        contentDescription = null,
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.fillMaxSize(),
                    )

                    // Highlight boxes
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        blocks.forEach { block ->
                            val l = imgRect.left + block.x * scaleX
                            val t = imgRect.top  + block.y * scaleY
                            val w = (block.width  * scaleX).coerceAtLeast(8f)
                            val h = (block.height * scaleY).coerceAtLeast(8f)
                            val isSelected = block.id == selectedBlock?.id
                            val fillAlpha  = if (isSelected) 0.30f else 0.18f
                            val strokeAlpha = if (isSelected) 0.90f else 0.55f
                            val boxColor = when {
                                isSelected                                    -> Color(0xFF2196F3)
                                block.confidence < 60                         -> Color(0xFFF44336)
                                block.confidence < 85                         -> Color(0xFFFF9800)
                                else                                          -> Color(0xFFFFEB3B)
                            }
                            drawRect(
                                color   = boxColor.copy(alpha = fillAlpha),
                                topLeft = Offset(l, t),
                                size    = Size(w, h),
                            )
                            drawRect(
                                color   = boxColor.copy(alpha = strokeAlpha),
                                topLeft = Offset(l, t),
                                size    = Size(w, h),
                                style   = Stroke(width = if (isSelected) 2.5f else 1.2f),
                            )
                        }
                    }

                    // Click-targets + text labels layered on top of Canvas boxes
                    blocks.forEach { block ->
                        val l = imgRect.left + block.x * scaleX
                        val t = imgRect.top  + block.y * scaleY
                        val w = (block.width  * scaleX).coerceAtLeast(32f)
                        val h = (block.height * scaleY).coerceAtLeast(24f)
                        val isSelected = block.id == selectedBlock?.id
                        val textSp = ((h * 0.52f) / density.density).sp.coerceIn(6.5.sp, 11.sp)

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(l.roundToInt(), t.roundToInt()) }
                                .size(
                                    with(density) { w.toDp() },
                                    with(density) { h.toDp() },
                                )
                                .background(
                                    if (isSelected) Color(0xFF2196F3).copy(alpha = 0.18f)
                                    else Color.Black.copy(alpha = 0.08f)
                                )
                                .clickable {
                                    selectedBlock = if (selectedBlock?.id == block.id) null else block
                                },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = block.editedText,
                                fontSize = textSp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White
                                        else Color(0xFFF0F4FF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                            )
                        }
                    }
                }

                // ── Selected-block bottom panel ────────────────────────────────
                selectedBlock?.let { block ->
                    BlockPanel(
                        block         = block,
                        onEdit        = { showEditDialog = true },
                        onCopy        = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("OCR text", block.editedText))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss     = { selectedBlock = null },
                    )
                }
            }
        }
    }

    // ── Edit text dialog ──────────────────────────────────────────────────────
    if (showEditDialog && selectedBlock != null) {
        var draft by remember(selectedBlock) { mutableStateOf(selectedBlock!!.editedText) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit OCR Block") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConfidenceBadge(selectedBlock!!.confidence)
                    if (selectedBlock!!.text != selectedBlock!!.editedText) {
                        Text(
                            "Original: ${selectedBlock!!.text}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value         = draft,
                        onValueChange = { draft = it },
                        label         = { Text("Corrected text") },
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 2,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    TextButton(
                        onClick = { draft = OcrRepository.cleanupText(draft) },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Auto-clean", style = MaterialTheme.typography.labelSmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pushUndo(blocks.toList())
                    val idx = blocks.indexOfFirst { it.id == selectedBlock!!.id }
                    if (idx >= 0) {
                        val updated = blocks.toMutableList()
                        updated[idx] = updated[idx].withEdit(draft)
                        blocks = updated
                        selectedBlock = updated[idx]
                    }
                    showEditDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            },
        )
    }

    // ── Export bottom sheet ────────────────────────────────────────────────────
    if (showExportSheet) {
        ModalBottomSheet(onDismissRequest = { showExportSheet = false }) {
            ExportPanel(
                text      = exportText(),
                blockCount = blocks.size,
                context   = context,
                scope     = scope,
                onDismiss = { showExportSheet = false },
            )
        }
    }
}

// ─── Toolbar ─────────────────────────────────────────────────────────────────

@Composable
private fun OcrToolbar(
    blockCount: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    saving: Boolean,
    hasBackendId: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCleanup: () -> Unit,
    onExport: () -> Unit,
    onSave: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "OCR Editor",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (blockCount > 0) {
                Text(
                    "$blockCount text blocks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(
                Icons.Default.Undo, "Undo",
                tint = if (canUndo) MaterialTheme.colorScheme.onBackground
                       else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f),
            )
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(
                Icons.Default.Redo, "Redo",
                tint = if (canRedo) MaterialTheme.colorScheme.onBackground
                       else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f),
            )
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onBackground)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text         = { Text("Auto-Clean All") },
                    leadingIcon  = { Icon(Icons.Default.AutoFixHigh, null) },
                    onClick      = { showMenu = false; onCleanup() },
                )
                DropdownMenuItem(
                    text         = { Text("Export Text") },
                    leadingIcon  = { Icon(Icons.Default.FileDownload, null) },
                    onClick      = { showMenu = false; onExport() },
                )
                if (hasBackendId) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text         = { if (saving) Text("Saving…") else Text("Save to Cloud") },
                        leadingIcon  = {
                            if (saving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.CloudUpload, null)
                        },
                        onClick      = { showMenu = false; if (!saving) onSave() },
                        enabled      = !saving,
                    )
                }
            }
        }
    }
}

// ─── Selected-block bottom panel ─────────────────────────────────────────────

@Composable
private fun BlockPanel(
    block: OcrBlock,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        tonalElevation  = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ConfidenceBadge(block.confidence)
                    Text(
                        "Text block",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close, "Deselect",
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Text(
                block.editedText,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick  = onCopy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─── Export panel ─────────────────────────────────────────────────────────────

@Composable
private fun ExportPanel(
    text: String,
    blockCount: Int,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
) {
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Export OCR Text",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "$blockCount blocks · ${text.length} characters",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape           = MaterialTheme.shapes.medium,
            color           = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier        = Modifier.fillMaxWidth().heightIn(max = 160.dp),
        ) {
            Text(
                text     = text.take(400) + if (text.length > 400) "…" else "",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Copy to clipboard
            OutlinedButton(
                onClick = {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("OCR export", text))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy All")
            }
            // Save to Downloads
            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        val ok = saveTextToDownloads(context, text, "OCR_Export_${System.currentTimeMillis()}.txt")
                        saving = false
                        Toast.makeText(context, if (ok) "Saved to Downloads/DocScan AI" else "Save failed", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                enabled  = !saving,
                modifier = Modifier.weight(1f),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save .txt")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ─── Utility composables ──────────────────────────────────────────────────────

@Composable
private fun ConfidenceBadge(confidence: Int) {
    val (color, label) = when {
        confidence >= 85 -> Pair(Color(0xFF4CAF50), "$confidence%")
        confidence >= 60 -> Pair(Color(0xFFFF9800), "$confidence%")
        else             -> Pair(MaterialTheme.colorScheme.error, "$confidence%")
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LoadingPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                "Running OCR…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyOcrPanel(onRetry: () -> Unit) {
    Box(
        Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Default.TextFields, null,
                modifier = Modifier.size(52.dp),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                "No text detected",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                "OCR found no readable text in this document. Try enhancing the image quality before extracting text.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Re-run OCR")
            }
        }
    }
}

@Composable
private fun ErrorPanel(msg: String) {
    Box(
        Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.ErrorOutline, null,
                modifier = Modifier.size(48.dp),
                tint     = MaterialTheme.colorScheme.error,
            )
            Text(
                "OCR failed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                msg,
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Image-rect math ─────────────────────────────────────────────────────────

private data class ImageRect(
    val left: Float, val top: Float, val width: Float, val height: Float,
)

/**
 * Calculates the actual rendered image region within a container, assuming
 * ContentScale.Fit (letterbox/pillarbox, no cropping).
 */
private fun computeImageRect(cW: Float, cH: Float, imgW: Int, imgH: Int): ImageRect {
    if (imgW == 0 || imgH == 0) return ImageRect(0f, 0f, cW, cH)
    val imgAspect  = imgW.toFloat() / imgH.toFloat()
    val contAspect = cW / cH
    return if (imgAspect > contAspect) {
        // Fit by width — horizontal bars top/bottom
        val rH = cW / imgAspect
        ImageRect(0f, (cH - rH) / 2f, cW, rH)
    } else {
        // Fit by height — vertical bars left/right
        val rW = cH * imgAspect
        ImageRect((cW - rW) / 2f, 0f, rW, cH)
    }
}

// ─── Save helper ─────────────────────────────────────────────────────────────

private suspend fun saveTextToDownloads(context: Context, text: String, filename: String): Boolean =
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
