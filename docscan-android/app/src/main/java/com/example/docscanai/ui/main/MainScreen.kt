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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                        0    -> selectedTab = 0
                        1    -> { selectedTab = 1; onGallery() }
                        3    -> { selectedTab = 3; onSearch() }
                        4    -> onSettings()
                        else -> {}
                    }
                },
                onScan = onScan,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            val sidePad = if (maxWidth > 600.dp) ((maxWidth - 600.dp) / 2).coerceAtLeast(0.dp) else 0.dp

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
                    Spacer(Modifier.height(16.dp))
                    StorageCard()
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

// ── Avatar header ─────────────────────────────────────────────────────────────

@Composable
private fun AvatarHeader(onSettings: () -> Unit) {
    val name     = AuthRepository.getName().ifBlank { "DocScan User" }
    val initials = name.split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "DS" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DeepNavy, MaterialTheme.colorScheme.background)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    greeting(),
                    fontSize      = 11.sp,
                    color         = Color.White.copy(alpha = 0.65f),
                    fontFamily    = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    name,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Notifications, "Notifications", tint = Color.White.copy(alpha = 0.75f))
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Brush.linearGradient(listOf(IntelligentBlue, AIGlow)), CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
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
            .background(Brush.linearGradient(listOf(Color(0xFF1A3060), IntelligentBlue))),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = AIGlow, modifier = Modifier.size(12.dp))
                    Text(
                        "AI INSIGHTS",
                        fontSize      = 10.sp,
                        color         = AIGlow,
                        fontFamily    = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Your documents are ready to review",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Recent scans processed with 98% OCR confidence",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.75f),
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = Color.White.copy(alpha = 0.15f),
                    border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                    modifier = Modifier.clickable {},
                ) {
                    Text(
                        "Review",
                        modifier   = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White,
                    )
                }
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = Color.Transparent,
                    modifier = Modifier.clickable(onClick = onDismiss),
                ) {
                    Text(
                        "Dismiss",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.65f),
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
            "Quick Actions",
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
            modifier   = Modifier.padding(bottom = 12.dp),
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionItem(
                icon     = Icons.Default.CameraAlt,
                label    = "Scan",
                color    = IntelligentBlue,
                modifier = Modifier.weight(1f),
                onClick  = onScan,
            )
            QuickActionItem(
                icon     = Icons.Default.Image,
                label    = "Import",
                color    = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f),
                onClick  = onGallery,
            )
            QuickActionItem(
                icon     = Icons.AutoMirrored.Filled.CompareArrows,
                label    = "Convert",
                color    = Color(0xFFD97706),
                modifier = Modifier.weight(1f),
                onClick  = onConvert,
            )
            QuickActionItem(
                icon     = Icons.Default.AutoFixHigh,
                label    = "Cleanup",
                color    = Color(0xFF16A34A),
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
        verticalArrangement   = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.size(54.dp).clickable(onClick = onClick),
            shape    = RoundedCornerShape(16.dp),
            color    = color.copy(alpha = 0.12f),
            border   = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            label,
            fontSize    = 11.sp,
            fontWeight  = FontWeight.Medium,
            color       = MaterialTheme.colorScheme.onBackground,
            textAlign   = TextAlign.Center,
        )
    }
}

// ── Storage card ──────────────────────────────────────────────────────────────

@Composable
private fun StorageCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Storage, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                    Text(
                        "Storage Used",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    "2.4 / 10 GB",
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.24f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.linearGradient(listOf(IntelligentBlue, AIGlow))),
                )
            }
        }
    }
}

// ── Continue Editing ─────────────────────────────────────────────────────────

@Composable
private fun ContinueEditingSection(
    scans: List<ScanRecord>,
    onScanItem: (ScanRecord) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Continue editing",
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
            modifier   = Modifier.padding(bottom = 12.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(scans, key = { it.id }) { record ->
                ContinueEditCard(record = record, onClick = { onScanItem(record) })
            }
        }
    }
}

@Composable
private fun ContinueEditCard(record: ScanRecord, onClick: () -> Unit) {
    val progress = remember { (55..95).random() / 100f }
    val fileType = when {
        record.id.startsWith("gallery_") -> "PDF"
        else -> "SCAN"
    }
    val typeColor = if (fileType == "PDF") Color(0xFFEF4444) else ScanCyan

    Surface(
        modifier = Modifier.width(140.dp).clickable(onClick = onClick),
        shape    = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Type badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(typeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    fileType,
                    fontSize      = 9.sp,
                    color         = typeColor,
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
            Text(
                record.name.take(22),
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
            )
            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.linearGradient(listOf(IntelligentBlue, AIGlow))),
                    )
                }
                Text(
                    "${(progress * 100).toInt()}% done",
                    fontSize = 9.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                )
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
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                "This week",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Insights",
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
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

@Composable
private fun MainTabBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onScan: () -> Unit,
) {
    Surface(
        color           = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp,
        tonalElevation  = 2.dp,
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            TabBarItem(
                icon     = Icons.Default.Home,
                label    = "Home",
                selected = selectedTab == 0,
                onClick  = { onTabSelect(0) },
            )
            TabBarItem(
                icon     = Icons.Default.Folder,
                label    = "Files",
                selected = selectedTab == 1,
                onClick  = { onTabSelect(1) },
            )

            // Elevated center scan button
            Box(modifier = Modifier.offset(y = (-10).dp)) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(IntelligentBlue, AIGlow)))
                        .clickable(onClick = onScan),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CameraAlt, "Scan", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }

            TabBarItem(
                icon     = Icons.Default.Search,
                label    = "Search",
                selected = selectedTab == 3,
                onClick  = { onTabSelect(3) },
            )
            TabBarItem(
                icon     = Icons.Default.Person,
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
        modifier              = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            icon, label,
            tint     = if (selected) IntelligentBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            fontSize   = 10.sp,
            color      = if (selected) IntelligentBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
