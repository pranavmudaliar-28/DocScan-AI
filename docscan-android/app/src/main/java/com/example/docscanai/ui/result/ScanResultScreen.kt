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
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu    by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val tabs        = listOf("Preview", "Extracted Text", "AI Summary")

    val fileMime = remember(imageUri) {
        if (imageUri.isEmpty()) "" else context.contentResolver.getType(Uri.parse(imageUri)) ?: ""
    }
    val isImageFile = fileMime.startsWith("image/") || 
                      imageUri.endsWith(".jpg", true) || 
                      imageUri.endsWith(".jpeg", true) || 
                      imageUri.endsWith(".png", true)

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

    fun getShareableUri(uriStr: String): Uri {
        var uri = Uri.parse(uriStr)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Costco — receipt 1124",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "SCAN · 3 PAGES · 2 MIN AGO",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
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
                        Icon(Icons.Default.Upload, "Share", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
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
                                "Save as PDF",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
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
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("Export Format") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Choose a file format to export this scan:", modifier = Modifier.padding(bottom = 8.dp))
                            
                            OutlinedButton(
                                onClick = {
                                    showExportDialog = false
                                    coroutineScope.launch {
                                        com.example.docscanai.data.MultiPageScanManager.clearPages()
                                        com.example.docscanai.data.MultiPageScanManager.addPage(Uri.parse(imageUri))
                                        val pdfUri = com.example.docscanai.data.MultiPageScanManager.buildPdf(context)
                                        if (pdfUri != null) {
                                            shareSpecificUri(pdfUri.toString(), "application/pdf")
                                        } else {
                                            Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("PDF Document (.pdf)") }

                            OutlinedButton(
                                onClick = {
                                    showExportDialog = false
                                    shareSpecificUri(imageUri, "image/jpeg")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("JPEG Image (.jpg)") }

                            OutlinedButton(
                                onClick = {
                                    showExportDialog = false
                                    shareSpecificUri(imageUri, "image/png")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("PNG Image (.png)") }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showExportDialog = false }) { Text("Cancel") }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // OCR Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "OCR complete · 98% confidence",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                    Text(
                        "EXTRACTED 247 WORDS · 2 TABLES",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF166534).copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                }
                Text("Details", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SuccessGreen, modifier = Modifier.clickable { })
            }
        }

        // Receipt Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Costco Wholesale", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                Text("1234 INDUSTRIAL WAY · SAN MATEO, CA", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Receipt", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("#1124-882", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                    Column {
                        Text("Date", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("2025-11-14", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Cashier", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("K-072", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE2E8F0))
                
                val items = listOf(
                    "KIRKLAND PAPER 12PK" to "$24.99",
                    "ORG. STRAWBERRIES 2LB" to "$11.98",
                    "COFFEE BEANS 2.5LB" to "$18.49",
                    "POST-IT NOTES 12PK" to "$14.99"
                )
                
                items.forEachIndexed { index, pair ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index+1}×", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8), modifier = Modifier.width(24.dp))
                        
                        if (index == 1) { // Blue highlight line
                            Box(modifier = Modifier.width(2.dp).height(16.dp).background(Color(0xFF3B82F6)))
                            Spacer(Modifier.width(6.dp))
                        }
                        
                        Text(pair.first, fontSize = 14.sp, color = Color(0xFF334155), modifier = Modifier.weight(1f))
                        Text(pair.second, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE2E8F0))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color(0xFF0F172A))
                    Text("$70.45", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                }
            }
        }

        // Category Suggestion Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, Color(0xFFDBEAFE))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFF3B82F6), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Category suggestion", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text("This looks like an Office Supplies receipt. Save to Q4 Expenses?", fontSize = 14.sp, color = Color(0xFF334155), modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Save to Q4", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Pick another", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
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
