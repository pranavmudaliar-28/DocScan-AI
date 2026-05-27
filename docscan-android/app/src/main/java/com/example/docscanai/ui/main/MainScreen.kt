package com.example.docscanai.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.docscanai.data.AuthRepository
import com.example.docscanai.data.DefaultDataRepository
import com.example.docscanai.data.ScanRecord
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.DeepNavy
import com.example.docscanai.ui.theme.IntelligentBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
private val ScanCyan   = Color(0xFF06B6D4)

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else      -> "Good evening"
    }
}

@Composable
fun MainScreen(
    onScan: () -> Unit,
    onGallery: () -> Unit,
    onSettings: () -> Unit,
    onConvert: () -> Unit,
    onSearch: () -> Unit = {},
    onScanItem: (ScanRecord) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentScans = if (state is MainScreenUiState.Success) {
        (state as MainScreenUiState.Success).data
    } else emptyList()

    HomeScreen(
        recentScans = recentScans,
        onScan      = onScan,
        onGallery   = onGallery,
        onSettings  = onSettings,
        onConvert   = onConvert,
        onSearch    = onSearch,
        onScanItem  = onScanItem,
    )
}

@Composable
internal fun HomeScreen(
    recentScans: List<ScanRecord>,
    onScan: () -> Unit = {},
    onGallery: () -> Unit = {},
    onSettings: () -> Unit = {},
    onConvert: () -> Unit = {},
    onSearch: () -> Unit = {},
    onScanItem: (ScanRecord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab  by remember { mutableIntStateOf(0) }
    var showHeroCard by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            Column {
                com.example.docscanai.ui.ads.AdMobBanner()
                MainTabBar(
                    selectedTab = selectedTab,
                    onTabSelect = { tab ->
                        when (tab) {
                            0 -> selectedTab = 0
                            1 -> selectedTab = 1
                            3 -> onSearch()
                            4 -> onSettings()
                        }
                    },
                    onScan = onScan
                )
            }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            val sidePad = if (maxWidth > 600.dp) ((maxWidth - 600.dp) / 2).coerceAtLeast(0.dp) else 0.dp

            if (selectedTab == 1) {
                FilesTab(
                    scans = recentScans,
                    onScanItem = onScanItem,
                    modifier = Modifier.fillMaxSize().padding(horizontal = sidePad)
                )
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(horizontal = sidePad),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    item { AvatarHeader(onSettings = onSettings) }

                if (showHeroCard) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        AiHeroCard(onDismiss = { showHeroCard = false })
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    QuickActionsGrid(
                        onScan    = onScan,
                        onGallery = onGallery,
                        onConvert = onConvert,
                    )
                }

                if (recentScans.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        ContinueEditingSection(
                            scans      = recentScans.take(5),
                            onScanItem = onScanItem,
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    RecentScansSection(
                        scans      = recentScans,
                        onScanItem = onScanItem,
                        onScan     = onScan,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun FilesTab(
    scans: List<ScanRecord>,
    onScanItem: (ScanRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Files",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp).clickable { }
                    ) {
                        Icon(Icons.Default.Sort, null, modifier = Modifier.padding(10.dp), tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp).clickable { }
                    ) {
                        Icon(Icons.Default.GridView, null, modifier = Modifier.padding(10.dp), tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                placeholder = { 
                    Text("Search files, text inside docs...", 
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    Icon(Icons.Default.Mic, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = IntelligentBlue
                ),
                singleLine = true
            )
        }

        // Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable { }
                    ) {
                        Text("All", color = androidx.compose.material3.MaterialTheme.colorScheme.surface, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable { }
                    ) {
                        Text("Recent", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable { }
                    ) {
                        Text("Favorites", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = IntelligentBlue,
                        modifier = Modifier.clickable { }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.surface, modifier = Modifier.size(14.dp))
                            Text("AI tagged", color = androidx.compose.material3.MaterialTheme.colorScheme.surface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Folders Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "FOLDERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "+ New",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = IntelligentBlue,
                        modifier = Modifier.clickable { }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FolderCard(title = "Q4 Expenses", items = 23, color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                    FolderCard(title = "Contracts", items = 12, color = IntelligentBlue, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FolderCard(title = "Receipts · 2025", items = 87, color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                    FolderCard(title = "Personal", items = 9, color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                }
            }
        }

        // Files Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FILES · ${scans.size.takeIf { it > 0 } ?: 142}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (scans.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilesTabScanItem(
                        type = "PDF", typeColor = Color(0xFFEF4444),
                        title = "Acme Co — MSA v3", subtitle = "24 pages · 4.2 MB",
                        date = "Today", isStarred = true, hasAi = false
                    )
                    FilesTabScanItem(
                        type = "SCAN", typeColor = ScanCyan,
                        title = "Costco — receipt 1124", subtitle = "1 page · OCR done",
                        date = "9:12 AM", isStarred = false, hasAi = true
                    )
                    FilesTabScanItem(
                        type = "DOCX", typeColor = IntelligentBlue,
                        title = "Proposal — Northwind", subtitle = "8 pages · 248 KB",
                        date = "Yesterday", isStarred = true, hasAi = true
                    )
                }
            }
        } else {
            items(scans, key = { it.id }) { record ->
                val isGallery = record.id.startsWith("gallery")
                val typeColor = if (isGallery) Color(0xFF06B6D4) else IntelligentBlue
                val fileType = if (isGallery) "IMG" else "SCAN"
                
                FilesTabScanItem(
                    type = fileType, typeColor = typeColor,
                    title = record.name, subtitle = "1 page · 2.1 MB",
                    date = dateFormat.format(Date(record.timestamp)), isStarred = false, hasAi = !isGallery,
                    onClick = { onScanItem(record) }
                )
            }
        }
    }
}

@Composable
private fun FolderCard(title: String, items: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.clickable { }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.FolderOpen, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$items ITEMS", fontSize = 10.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun FilesTabScanItem(
    type: String, typeColor: Color,
    title: String, subtitle: String,
    date: String, isStarred: Boolean, hasAi: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    type,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isStarred) {
                        Icon(Icons.Default.StarBorder, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    }
                }
                Text(subtitle, fontSize = 13.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasAi) {
                    Icon(Icons.Default.AutoAwesome, null, tint = IntelligentBlue, modifier = Modifier.size(14.dp))
                } else {
                    Spacer(Modifier.size(14.dp))
                }
                Text(date, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Avatar header ─────────────────────────────────────────────────────────────

@Composable
private fun AvatarHeader(onSettings: () -> Unit) {
    val name     = AuthRepository.getName().ifBlank { "Maya Chen" }
    val initials = name.split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "MC" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF3B82F6), CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.surface)
            }
            Column {
                Text(
                    greeting(),
                    fontSize      = 13.sp,
                    color         = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    name,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        
        Surface(
            shape = CircleShape,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Outlined.Notifications, "Notifications", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
                Box(modifier = Modifier.padding(top = 8.dp, end = 10.dp).size(8.dp).background(Color(0xFFEF4444), CircleShape).align(Alignment.TopEnd).border(1.5.dp, androidx.compose.material3.MaterialTheme.colorScheme.surface, CircleShape))
            }
        }
    }
}

// ── AI Hero Card ──────────────────────────────────────────────────────────────

@Composable
private fun AiHeroCard(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), androidx.compose.material3.MaterialTheme.colorScheme.primary))),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF93C5FD), modifier = Modifier.size(12.dp))
                    Text(
                        "AI ASSISTANT",
                        fontSize      = 10.sp,
                        color         = Color(0xFFE0E7FF),
                        fontFamily    = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight    = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "You have 3 receipts ready to\norganize into your Q4 expense\nfolder.",
                fontSize   = 19.sp,
                fontWeight = FontWeight.Bold,
                color      = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                lineHeight = 26.sp
            )

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clickable {},
                ) {
                    Text(
                        "Review",
                        modifier   = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    )
                }
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                    border   = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable(onClick = onDismiss),
                ) {
                    Text(
                        "Dismiss",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color    = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }
    }
}

// ── Quick Actions Grid ────────────────────────────────────────────────────────

@Composable
private fun QuickActionsGrid(
    onScan: () -> Unit,
    onGallery: () -> Unit,
    onConvert: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Quick actions",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            modifier   = Modifier.padding(bottom = 16.dp),
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionItem(
                icon     = Icons.Default.CropFree,
                label    = "Scan",
                color    = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f),
                onClick  = onScan,
            )
            QuickActionItem(
                icon     = Icons.Default.Image,
                label    = "Import",
                color    = Color(0xFFA855F7),
                modifier = Modifier.weight(1f),
                onClick  = onGallery,
            )
            QuickActionItem(
                icon     = Icons.AutoMirrored.Filled.CompareArrows,
                label    = "Convert",
                color    = Color(0xFF22C55E),
                modifier = Modifier.weight(1f),
                onClick  = onConvert,
            )
            QuickActionItem(
                icon     = Icons.Default.AutoFixHigh,
                label    = "Cleanup",
                color    = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                onClick  = {},
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier              = modifier,
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
            shape    = RoundedCornerShape(16.dp),
            color    = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            border   = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        Text(
            label,
            fontSize    = 13.sp,
            fontWeight  = FontWeight.Medium,
            color       = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            textAlign   = TextAlign.Center,
        )
    }
}

// ── Storage card ──────────────────────────────────────────────────────────────



// ── Continue Editing ─────────────────────────────────────────────────────────

@Composable
private fun ContinueEditingSection(
    scans: List<ScanRecord>,
    onScanItem: (ScanRecord) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Continue editing",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "See all",
                fontSize = 14.sp,
                color = Color(0xFF3B82F6),
                fontWeight = FontWeight.Medium
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(scans, key = { it.id }) { record ->
                ContinueEditCard(record = record, onClick = { onScanItem(record) })
            }
        }
    }
}

@Composable
private fun ContinueEditCard(record: ScanRecord, onClick: () -> Unit) {
    val progress = remember { (30..80).random() / 100f }
    val isPdf = record.id.startsWith("gallery_")
    val fileType = if (isPdf) "PDF" else "DOCX"
    val typeColor = if (isPdf) Color(0xFFEF4444) else Color(0xFF3B82F6)

    Surface(
        modifier = Modifier.width(160.dp).clickable(onClick = onClick),
        shape    = RoundedCornerShape(16.dp),
        color    = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Document Placeholder Graphic
            Box(modifier = Modifier.fillMaxWidth().height(80.dp).border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(4.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(6.dp).background(Color(0xFFBFDBFE), RoundedCornerShape(3.dp)))
                }
                
                // Badge
                Text(
                    fileType,
                    fontSize = 9.sp,
                    color = typeColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                
                // Progress Bar overlay on bottom of document
                Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant)) {
                    Box(modifier = Modifier.fillMaxWidth(progress).height(3.dp).background(Color(0xFF3B82F6)))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                if (isPdf) "MSA v3" else "Northwind",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isPdf) "OCR REVIEW · ${(progress*100).toInt()}%" else "DRAFTING · ${(progress*100).toInt()}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color    = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isPdf) "4 min ago" else "32 min ago",
                    fontSize = 12.sp,
                    color    = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Surface(shape = CircleShape, color = Color(0xFF3B82F6), modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.surface, modifier = Modifier.padding(6.dp))
                }
            }
        }
    }
}

// ── Recent scans ──────────────────────────────────────────────────────────────

@Composable
private fun RecentScansSection(
    scans: List<ScanRecord>,
    onScanItem: (ScanRecord) -> Unit,
    onScan: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                Text(
                    "This week",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                "Insights",
                fontSize   = 14.sp,
                color      = Color(0xFF3B82F6),
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(12.dp))

        if (scans.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.surfaceContainerLow,
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.FileCopy, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No scans yet",
                        fontSize = 14.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap the scan button to get started",
                        fontSize  = 12.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        } else {
            scans.forEach { record ->
                ScanItem(record = record, onClick = { onScanItem(record) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ScanItem(record: ScanRecord, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IntelligentBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Description, null, tint = IntelligentBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.name,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    dateFormat.format(Date(record.timestamp)),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.width(8.dp))
            // FileTag badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ScanCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    "SCAN",
                    fontSize      = 9.sp,
                    color         = ScanCyan,
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

// ── Bottom Tab Bar ────────────────────────────────────────────────────────────

private val TabSelected: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
private val TabUnselected: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
private val TabDivider: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant

@Composable
private fun MainTabBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onScan: () -> Unit,
) {
    val dividerColor = TabDivider
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color       = dividerColor,
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            TabBarItem(
                icon     = Icons.Outlined.Home,
                label    = "Home",
                selected = selectedTab == 0,
                onClick  = { onTabSelect(0) },
            )
            TabBarItem(
                icon     = Icons.Outlined.FolderOpen,
                label    = "Files",
                selected = selectedTab == 1,
                onClick  = { onTabSelect(1) },
            )

            // Elevated center scan button — rounded square with blue glow
            Box(
                modifier = Modifier
                    .offset(y = (-14).dp)
                    .shadow(
                        elevation    = 14.dp,
                        shape        = RoundedCornerShape(18.dp),
                        ambientColor = IntelligentBlue.copy(alpha = 0.30f),
                        spotColor    = IntelligentBlue.copy(alpha = 0.55f),
                    )
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(IntelligentBlue, AIGlow)))
                    .clickable(onClick = onScan),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.CropFree,
                    contentDescription = "Scan",
                    tint     = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(28.dp),
                )
            }

            TabBarItem(
                icon     = Icons.Outlined.Search,
                label    = "Search",
                selected = selectedTab == 3,
                onClick  = { onTabSelect(3) },
            )
            TabBarItem(
                icon     = Icons.Outlined.Person,
                label    = "Account",
                selected = selectedTab == 4,
                onClick  = { onTabSelect(4) },
            )
        }
    }
}

@Composable
private fun TabBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            icon, label,
            tint     = if (selected) TabSelected else TabUnselected,
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            fontSize   = 10.sp,
            color      = if (selected) TabSelected else TabUnselected,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}



