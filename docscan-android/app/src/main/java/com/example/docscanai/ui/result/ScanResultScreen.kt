package com.example.docscanai.ui.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.docscanai.data.ScanHistoryRepository
import com.example.docscanai.data.ScanRecord
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.IntelligentBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val scanDateFmt = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
private val SuccessGreen = Color(0xFF16A34A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    scanId: String,
    imageUri: String,
    onBack: () -> Unit,
    onViewDocument: () -> Unit,
) {
    val context     = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu    by remember { mutableStateOf(false) }
    val tabs        = listOf("Preview", "Extracted Text", "AI Summary")

    val fileMime    = remember(imageUri) {
        if (imageUri.isEmpty()) "" else context.contentResolver.getType(Uri.parse(imageUri)) ?: ""
    }
    val isImageFile = fileMime.startsWith("image/")

    var ocrText    by remember { mutableStateOf<String?>(null) }
    var ocrLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && ocrText == null && !ocrLoading && isImageFile && imageUri.isNotEmpty()) {
            ocrLoading = true
            ocrText    = runScanOcr(context, imageUri)
            ocrLoading = false
        }
    }

    LaunchedEffect(scanId) {
        val label = when {
            scanId.startsWith("scan_")    -> "Camera scan"
            scanId.startsWith("gallery_") -> "Gallery import"
            else -> "Document scan"
        }
        val ts = System.currentTimeMillis()
        ScanHistoryRepository.addScan(
            context,
            ScanRecord(
                id        = scanId,
                name      = "$label — ${scanDateFmt.format(Date(ts))}",
                imageUri  = imageUri,
                timestamp = ts,
            )
        )
    }

    fun shareDoc() {
        if (imageUri.isEmpty()) return
        val uri    = Uri.parse(imageUri)
        val mime   = context.contentResolver.getType(uri) ?: "image/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mime
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share document"))
    }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Scan ID", scanId))
        Toast.makeText(context, "Scan ID copied", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Scan Result",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            scanId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { shareDoc() }) {
                        Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded         = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text        = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)) },
                                onClick     = { showMenu = false; shareDoc() },
                            )
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
                        shape    = MaterialTheme.shapes.large,
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }

                    // Export / Share button — uses correct MIME for the actual file type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(Brush.linearGradient(listOf(IntelligentBlue, AIGlow)))
                            .clickable { shareDoc() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                if (isImageFile) Icons.Default.PictureAsPdf else Icons.Default.Share,
                                null, tint = Color.White, modifier = Modifier.size(16.dp)
                            )
                            Text(
                                if (isImageFile) "Save as PDF" else "Export",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
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
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color    = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (index == 2) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        null,
                                        tint     = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }

            // OCR confidence banner — only shown for image scans on non-preview tabs
            if (selectedTab != 0 && isImageFile) {
                val bannerColor = if (ocrLoading) MaterialTheme.colorScheme.primary else SuccessGreen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerColor.copy(alpha = 0.10f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (ocrLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint     = SuccessGreen,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        if (ocrLoading) "Running OCR…"
                        else if (ocrText.isNullOrEmpty()) "No text detected"
                        else "OCR complete · ${ocrText!!.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }} words extracted",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = bannerColor,
                    )
                }
            }

            when (selectedTab) {
                0    -> PreviewTab(imageUri = imageUri, onViewDocument = onViewDocument)
                1    -> TextTab(imageUri = imageUri, ocrText = ocrText, loading = ocrLoading)
                2    -> SummaryTab()
            }
        }
    }
}

// ── Preview tab ───────────────────────────────────────────────────────────────

@Composable
private fun PreviewTab(imageUri: String, onViewDocument: () -> Unit) {
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

// ── Extracted text tab ────────────────────────────────────────────────────────

@Composable
private fun TextTab(imageUri: String, ocrText: String?, loading: Boolean) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Extracting text…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            ocrText != null && ocrText.isNotEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    color    = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Text(
                                "EXTRACTED TEXT",
                                fontSize      = 10.sp,
                                color         = MaterialTheme.colorScheme.primary,
                                fontFamily    = FontFamily.Monospace,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                            )
                            val wordCount = ocrText.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SuccessGreen.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    "$wordCount words",
                                    modifier      = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize      = 10.sp,
                                    color         = SuccessGreen,
                                    fontFamily    = FontFamily.Monospace,
                                    fontWeight    = FontWeight.Bold,
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(
                            ocrText,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 4.dp),
                        )
                    }
                }
                // Copy button
                OutlinedButton(
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Extracted text", ocrText))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy All Text")
                }
            }
            ocrText != null && ocrText.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier            = Modifier.padding(32.dp),
                    ) {
                        Icon(Icons.Default.TextFields, null,
                            modifier = Modifier.size(48.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("No text detected",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground)
                        Text("OCR found no readable text in this scan.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }
            else -> {
                // imageUri is non-image file (PDF/DOCX/etc) — direct user to Edit flow
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier            = Modifier.padding(32.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.TextFields, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Open in Editor to extract text",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground)
                        Text("Tap Edit to open this document in the full OCR editor.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ── AI summary tab ────────────────────────────────────────────────────────────

private val SoftBlue = Color(0xFFEEF4FF)

@Composable
private fun SummaryTab() {
    var suggestionDismissed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "AI Extracted Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // AI suggestion card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            color    = if (MaterialTheme.colorScheme.background.red < 0.5f)
                           IntelligentBlue.copy(alpha = 0.10f)
                       else SoftBlue,
            border   = BorderStroke(1.dp, IntelligentBlue.copy(alpha = 0.20f)),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.AutoAwesome, null,
                        tint     = IntelligentBlue,
                        modifier = Modifier.size(16.dp))
                    Text(
                        "AI SUGGESTION",
                        fontSize      = 10.sp,
                        color         = IntelligentBlue,
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                }
                Text(
                    "Document categorised as a scanned image. Connect to DocScan AI backend to unlock full text extraction, AI summary, and smart field detection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!suggestionDismissed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val context2 = androidx.compose.ui.platform.LocalContext.current
                        Surface(
                            modifier = Modifier.clickable {
                                Toast.makeText(context2, "Document saved to history", Toast.LENGTH_SHORT).show()
                            },
                            shape    = RoundedCornerShape(8.dp),
                            color    = IntelligentBlue,
                        ) {
                            Text(
                                "Saved",
                                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color.White,
                            )
                        }
                        Surface(
                            modifier = Modifier.clickable { suggestionDismissed = true },
                            shape    = RoundedCornerShape(8.dp),
                            color    = Color.Transparent,
                            border   = BorderStroke(1.dp, IntelligentBlue.copy(alpha = 0.40f)),
                        ) {
                            Text(
                                "Dismiss",
                                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize   = 12.sp,
                                color      = IntelligentBlue,
                            )
                        }
                    }
                }
            }
        }

        // Placeholder field cards
        listOf(
            Pair(Icons.Default.Description,   "Document Type"),
            Pair(Icons.Default.CalendarToday, "Date & Due Date"),
            Pair(Icons.Default.AttachMoney,   "Amounts & Totals"),
            Pair(Icons.Default.Business,      "Vendor & Parties"),
        ).forEach { (icon, label) ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = MaterialTheme.shapes.medium,
                color    = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier              = Modifier.padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(icon, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp))
                    Text(label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ── OCR helper ────────────────────────────────────────────────────────────────

private suspend fun runScanOcr(context: Context, imageUri: String): String =
    withContext(Dispatchers.Default) {
        try {
            val uri = Uri.parse(imageUri)
            suspendCancellableCoroutine { cont ->
                val image      = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result -> if (cont.isActive) cont.resume(result.text) }
                    .addOnFailureListener { if (cont.isActive) cont.resume("") }
            }
        } catch (_: Exception) { "" }
    }
