package com.example.docscanai.ui.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.docscanai.data.ScanHistoryRepository
import com.example.docscanai.data.local.DocumentEntity
import com.example.docscanai.data.local.DatabaseModule
import androidx.core.net.toUri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun getScanDateFmt() = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    scanId: String,
    imageUri: String,
    onBack: () -> Unit,
    onViewDocument: () -> Unit,
) {
    val context     = LocalContext.current
    var showMenu    by remember { mutableStateOf(false) }

    var scanName by remember { mutableStateOf("Processing...") }
    var scanTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isOcrProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(scanId) {
        val existing = DatabaseModule.localDocumentRepository.getDocumentById(scanId)
        if (existing != null) {
            scanName = existing.name
            scanTimestamp = existing.timestamp
        } else {
            val label = when {
                scanId.startsWith("scan_")    -> "Camera scan"
                scanId.startsWith("gallery_") -> "Gallery import"
                else -> "Document scan"
            }
            val ts = System.currentTimeMillis()
            scanTimestamp = ts
            scanName = "$label — ${getScanDateFmt().format(Date(ts))}"
            
            val newDoc = DocumentEntity(
                id        = scanId,
                name      = scanName,
                imageUri  = imageUri,
                timestamp = ts,
            )
            DatabaseModule.localDocumentRepository.insertDocument(newDoc)
            
            // Background OCR extraction for search
            isOcrProcessing = true
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val mime = if (imageUri.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
                    val (blocks, _, _) = com.example.docscanai.data.OcrRepository.runLocalOcr(context, imageUri, mime)
                    val extractedText = blocks.joinToString(" ") { it.text }
                    if (extractedText.isNotBlank()) {
                        DatabaseModule.localDocumentRepository.updateDocument(
                            newDoc.copy(ocrText = extractedText)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isOcrProcessing = false
                }
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    fun getShareableUri(uriStr: String): Uri {
        var uri = uriStr.toUri()
        if (uri.scheme == "file") {
            try {
                val file = java.io.File(uri.path!!)
                uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return uri
    }

    fun shareSpecificUri(uriStr: String, mimeOverride: String? = null) {
        if (uriStr.isEmpty()) return
        val uri = getShareableUri(uriStr)
        val mime = mimeOverride ?: context.contentResolver.getType(uri) ?: "image/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mime
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share document"))
    }

    fun shareDoc() {
        shareSpecificUri(imageUri, if (imageUri.endsWith(".pdf", true)) "application/pdf" else null)
    }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Scan ID", scanId))
        Toast.makeText(context, "Scan ID copied", Toast.LENGTH_SHORT).show()
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export As") },
            text = { Text("Choose a file format to export this scan.") },
            confirmButton = {
                TextButton(onClick = { 
                    showExportDialog = false
                    shareSpecificUri(imageUri, "application/pdf")
                }) {
                    Text("PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showExportDialog = false
                    shareSpecificUri(imageUri, "image/jpeg")
                }) {
                    Text("JPG")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            scanName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        val timeDiff = System.currentTimeMillis() - scanTimestamp
                        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(timeDiff)
                        val timeStr = if (minutes < 1) "JUST NOW" else if (minutes < 60) "$minutes MIN AGO" else getScanDateFmt().format(Date(scanTimestamp)).uppercase()
                        Text(
                            "SCAN · 1 PAGE · $timeStr",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded         = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text        = { Text("Copy Scan ID") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                                onClick     = { showMenu = false; copyToClipboard() },
                            )
                            DropdownMenuItem(
                                text        = { Text("View Full Screen") },
                                leadingIcon = { Icon(Icons.Default.Fullscreen, null, modifier = Modifier.size(18.dp)) },
                                onClick     = { showMenu = false; onViewDocument() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(
                color          = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Edit button
                    OutlinedButton(
                        onClick  = { onViewDocument() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape    = MaterialTheme.shapes.medium,
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color(0xFF0F172A))
                            .clickable { showExportDialog = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "Export",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Icon(Icons.Default.SaveAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PreviewTab(imageUri = imageUri, isOcrProcessing = isOcrProcessing, onViewDocument = onViewDocument)
        }
    }
}

// ── Preview tab ───────────────────────────────────────────────────────────────

@Composable
private fun PreviewTab(imageUri: String, isOcrProcessing: Boolean, onViewDocument: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Badge
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.CheckCircle, null,
                    tint     = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp))
                Text(
                    "Scan captured — tap to view full screen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val docWidth = (maxWidth * 0.85f).coerceAtMost(360.dp)

            Box(
                modifier = Modifier
                    .width(docWidth)
                    .align(Alignment.TopCenter)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = onViewDocument),
            ) {
                SubcomposeAsyncImage(
                    model              = imageUri,
                    contentDescription = "Scanned document",
                    contentScale       = ContentScale.FillWidth,
                    modifier           = Modifier.fillMaxWidth(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color       = MaterialTheme.colorScheme.primary,
                                modifier    = Modifier.size(36.dp),
                                strokeWidth = 3.dp,
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.BrokenImage, null,
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Unable to load image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                )
                
                if (isOcrProcessing) {
                    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "ocr_sweep")
                    val sweepY by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                        ),
                        label = "sweep_y"
                    )

                    BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                        val heightPx = constraints.maxHeight.toFloat()
                        val currentY = sweepY * heightPx
                        
                        // Scanner line and gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .androidx.compose.ui.graphics.graphicsLayer {
                                    translationY = currentY - 60.dp.toPx()
                                }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, com.example.docscanai.ui.theme.AIGlow.copy(alpha = 0.5f), com.example.docscanai.ui.theme.IntelligentBlue)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .androidx.compose.ui.graphics.graphicsLayer {
                                    translationY = currentY
                                }
                                .background(com.example.docscanai.ui.theme.IntelligentBlue)
                                .androidx.compose.ui.draw.shadow(8.dp, spotColor = com.example.docscanai.ui.theme.AIGlow)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Default.ZoomIn, null,
                            tint     = Color.White.copy(alpha = .85f),
                            modifier = Modifier.size(14.dp))
                        Text("Tap for full screen & zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = .85f))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1 page", "Portrait").forEach { label ->
                Surface(
                    shape  = MaterialTheme.shapes.extraSmall,
                    color  = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
    }
}

