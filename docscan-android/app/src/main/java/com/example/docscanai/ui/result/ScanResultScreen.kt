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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.docscanai.data.ScanHistoryRepository
import com.example.docscanai.data.ScanRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val scanDateFmt = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    scanId: String,
    imageUri: String,
    onBack: () -> Unit,
    onViewDocument: () -> Unit,
) {
    val context      = LocalContext.current
    var selectedTab  by remember { mutableIntStateOf(0) }
    var showMenu     by remember { mutableStateOf(false) }
    val tabs         = listOf("Preview", "Extracted Text", "AI Summary")

    // Save this scan to history so it appears in Recent Scans
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

    fun shareImage() {
        if (imageUri.isEmpty()) return
        val uri = Uri.parse(imageUri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share scan"))
    }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Scan ID", scanId))
        Toast.makeText(context, "Scan ID copied", Toast.LENGTH_SHORT).show()
    }

    fun exportPdf() {
        // Share the image — the user can save/export via their chosen app
        shareImage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Scan Result",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            scanId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { shareImage() }) {
                        Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded         = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; shareImage() }
                            )
                            DropdownMenuItem(
                                text    = { Text("Copy Scan ID") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; copyToClipboard() }
                            )
                            DropdownMenuItem(
                                text    = { Text("View Full Screen") },
                                leadingIcon = { Icon(Icons.Default.Fullscreen, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onViewDocument() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color          = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = { copyToClipboard() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape    = MaterialTheme.shapes.large,
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Text", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick  = { exportPdf() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape    = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export / Share", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color    = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> PreviewTab(imageUri = imageUri, onViewDocument = onViewDocument)
                1 -> TextTab()
                2 -> SummaryTab()
            }
        }
    }
}

// ── Preview tab ───────────────────────────────────────────────────────────────

@Composable
private fun PreviewTab(imageUri: String, onViewDocument: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Badge
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                Text(
                    "Scan captured — tap to view full screen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Actual scanned image with loading/error states
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val docWidth = (maxWidth * 0.85f).coerceAtMost(360.dp)

            Box(
                modifier = Modifier
                    .width(docWidth)
                    .align(Alignment.TopCenter)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = onViewDocument)
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
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color     = MaterialTheme.colorScheme.primary,
                                modifier  = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.BrokenImage, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Unable to load image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                )

                // Tap overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, null,
                            tint = Color.White.copy(alpha = .85f), modifier = Modifier.size(14.dp))
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
private fun TextTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "OCR Result",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(
            modifier         = Modifier.fillMaxWidth().padding(vertical = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TextFields, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Text Extraction Ready",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Extracted text from your scan will appear here once connected to the DocScan AI backend.",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── AI summary tab ────────────────────────────────────────────────────────────

@Composable
private fun SummaryTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI Extracted Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground)

        Box(
            modifier         = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("AI Analysis Ready",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Document type, key fields, totals, and AI summary will appear here after backend processing.",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                listOf(
                    Pair(Icons.Default.Description,   "Document Type"),
                    Pair(Icons.Default.CalendarToday, "Date & Due Date"),
                    Pair(Icons.Default.AttachMoney,   "Amounts & Totals"),
                    Pair(Icons.Default.Business,      "Vendor & Parties"),
                ).forEach { (icon, label) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape    = MaterialTheme.shapes.medium,
                        color    = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier              = Modifier.padding(12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
    }
}
