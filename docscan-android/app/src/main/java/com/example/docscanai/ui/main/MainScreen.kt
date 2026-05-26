package com.example.docscanai.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.docscanai.data.DefaultDataRepository
import com.example.docscanai.data.ScanRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

@Composable
fun MainScreen(
    onScan: () -> Unit,
    onGallery: () -> Unit,
    onSettings: () -> Unit,
    onConvert: () -> Unit,
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
    onScanItem: (ScanRecord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWide  = maxWidth > 600.dp
        val sidePad = if (isWide) ((maxWidth - 600.dp) / 2).coerceAtLeast(0.dp) else 0.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(primary.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.14f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.82f, size.height * 0.14f)
            )
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(secondary.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.60f),
                    radius = size.width * 0.44f
                ),
                radius = size.width * 0.44f,
                center = Offset(size.width * 0.10f, size.height * 0.60f)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = sidePad)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { AppHeader(onSettings = onSettings) }
            item {
                Spacer(Modifier.height(8.dp))
                ScanCta(onScan = onScan)
            }
            item {
                Spacer(Modifier.height(12.dp))
                FeaturePills()
            }
            item {
                Spacer(Modifier.height(20.dp))
                QuickActionsRow(onGallery = onGallery, onConvert = onConvert)
            }
            item {
                Spacer(Modifier.height(28.dp))
                RecentScansSection(
                    scans      = recentScans,
                    onScanItem = onScanItem,
                    onScan     = onScan,
                )
            }
        }
    }
}

@Composable
private fun AppHeader(onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DocScan", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.width(4.dp))
                Text("AI", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Scan & analyze documents",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ScanCta(onScan: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        val ctaHeight = (maxWidth * 0.46f).coerceIn(160.dp, 220.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ctaHeight)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
                        )
                    )
                )
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape    = MaterialTheme.shapes.extraLarge,
                color    = Color.Transparent,
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {}
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, "Scan", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Tap to Scan Document", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(4.dp))
                Text("Camera · Auto-detect · AI extraction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FeaturePills() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("OCR Text", "AI Summary", "PDF Export", "Multi-page").forEach { label ->
            Surface(
                shape  = MaterialTheme.shapes.extraSmall,
                color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text(
                    label,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(onGallery: () -> Unit, onConvert: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            icon     = Icons.Default.Image,
            label    = "Gallery",
            subtitle = "Import image",
            modifier = Modifier.weight(1f),
            onClick  = onGallery
        )
        QuickActionCard(
            icon     = Icons.Default.CompareArrows,
            label    = "Convert",
            subtitle = "Change format",
            modifier = Modifier.weight(1f),
            onClick  = onConvert
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier,
        shape    = MaterialTheme.shapes.large,
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)) {
            Column {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(label,    style = MaterialTheme.typography.labelLarge,  color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,   color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RecentScansSection(
    scans: List<ScanRecord>,
    onScanItem: (ScanRecord) -> Unit,
    onScan: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Scans", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            if (scans.isNotEmpty()) {
                Text("${scans.size} scans", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (scans.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = MaterialTheme.shapes.large,
                color    = MaterialTheme.colorScheme.surfaceContainerLow,
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier         = Modifier.padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No scans yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Tap the scan button above to get started",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
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
        shape    = MaterialTheme.shapes.medium,
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(
                    dateFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
