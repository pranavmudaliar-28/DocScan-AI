package com.example.docscanai.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Sort
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
import com.example.docscanai.ui.theme.IntelligentBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
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
    onScanItem: (ScanRecord) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentScans = if (state is MainScreenUiState.Success) {
        (state as MainScreenUiState.Success).data
    } else emptyList()

    HomeScreen(
        recentScans = recentScans,
        modifier    = modifier,
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
    modifier: Modifier = Modifier,
    onScan: () -> Unit = {},
    onGallery: () -> Unit = {},
    onSettings: () -> Unit = {},
    onConvert: () -> Unit = {},
    onSearch: () -> Unit = {},
    onScanItem: (ScanRecord) -> Unit = {},
) {
    var selectedTab  by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
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
        containerColor = MaterialTheme.colorScheme.background
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

                item {
                    Spacer(Modifier.height(20.dp))
                    QuickActionsGrid(
                        onScan    = onScan,
                        onGallery = onGallery,
                        onConvert = onConvert,
                    )
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    RecentScansSection(
                        scans      = recentScans,
                        onScanItem = onScanItem,
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
    var searchQuery by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp).clickable { }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp).clickable { }
                    ) {
                        Icon(Icons.Default.GridView, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSurface)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = IntelligentBlue
                ),
                singleLine = true
            )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (scans.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    Text("No scans yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap the scan or import button below to get started.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
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
private fun FilesTabScanItem(
    type: String, typeColor: Color,
    title: String, subtitle: String,
    date: String, isStarred: Boolean, hasAi: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
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
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isStarred) {
                        Icon(Icons.Default.StarBorder, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    }
                }
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasAi) {
                    Icon(Icons.Default.AutoAwesome, null, tint = IntelligentBlue, modifier = Modifier.size(14.dp))
                } else {
                    Spacer(Modifier.size(14.dp))
                }
                Text(date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Avatar header ─────────────────────────────────────────────────────────────

@Composable
private fun AvatarHeader(onSettings: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(Color(0xFF3B82F6), Color(0xFFA855F7))),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.surface)
            }
            Column {
                Text(
                    greeting(),
                    fontSize      = 13.sp,
                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    name,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(44.dp).clickable {
                android.widget.Toast.makeText(context, "No new notifications", android.widget.Toast.LENGTH_SHORT).show()
            }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Outlined.Notifications, "Notifications", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
                Box(modifier = Modifier.padding(top = 8.dp, end = 10.dp).size(8.dp).background(Color(0xFFEF4444), CircleShape).align(Alignment.TopEnd).border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape))
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
            color      = MaterialTheme.colorScheme.onBackground,
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
                color    = Color(0xFF10B981),
                modifier = Modifier.weight(1f),
                onClick  = onConvert,
            )
            Spacer(Modifier.weight(1f)) // keep spacing equal by filling empty space
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
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).shadow(8.dp, RoundedCornerShape(20.dp), spotColor = color.copy(alpha = 0.2f)).clickable(onClick = onClick),
            shape    = RoundedCornerShape(20.dp),
            color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            border   = BorderStroke(1.dp, color.copy(alpha = 0.1f)),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, color.copy(alpha = 0.05f))))) {
                Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, label, tint = color, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
        Text(
            label,
            fontSize    = 13.sp,
            fontWeight  = FontWeight.Medium,
            color       = MaterialTheme.colorScheme.onBackground,
            textAlign   = TextAlign.Center,
        )
    }
}


// ── Recent scans ──────────────────────────────────────────────────────────────

@Composable
private fun RecentScansSection(
    scans: List<ScanRecord>,
    onScanItem: (ScanRecord) -> Unit,
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
                    color      = MaterialTheme.colorScheme.onBackground,
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
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape    = RoundedCornerShape(20.dp),
                color    = MaterialTheme.colorScheme.surface,
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.03f)),
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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

private val TabSelected: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground
private val TabUnselected: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
private val TabDivider: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

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
            .background(MaterialTheme.colorScheme.surface)
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
                    tint     = MaterialTheme.colorScheme.surface,
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



