package com.example.docscanai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onViewOnboarding: () -> Unit,
) {
    var highQuality   by remember { mutableStateOf(true) }
    var autoDetect    by remember { mutableStateOf(true) }
    var saveMetadata  by remember { mutableStateOf(false) }
    var darkMode      by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Profile card
            item {
                ProfileCard()
            }

            // Scan Settings
            item {
                SettingsSection(title = "Scan Settings") {
                    SettingsToggleRow(
                        icon     = Icons.Default.HighQuality,
                        label    = "High Quality Mode",
                        subtitle = "Capture at maximum resolution",
                        checked  = highQuality,
                        onCheckedChange = { highQuality = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon     = Icons.Default.CenterFocusStrong,
                        label    = "Auto-detect Document",
                        subtitle = "Automatically find document edges",
                        checked  = autoDetect,
                        onCheckedChange = { autoDetect = it }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon     = Icons.Default.PhotoSizeSelectLarge,
                        label    = "Default Format",
                        subtitle = "JPEG · PDF · PNG",
                        onClick  = {}
                    )
                }
            }

            // Export Settings
            item {
                SettingsSection(title = "Export Settings") {
                    SettingsNavRow(
                        icon     = Icons.Default.FolderOpen,
                        label    = "Save Location",
                        subtitle = "Documents / DocScan AI",
                        onClick  = {}
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon     = Icons.Default.Info,
                        label    = "Include Metadata",
                        subtitle = "Embed scan date and settings in file",
                        checked  = saveMetadata,
                        onCheckedChange = { saveMetadata = it }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon     = Icons.Default.PictureAsPdf,
                        label    = "PDF Compression",
                        subtitle = "Balanced",
                        onClick  = {}
                    )
                }
            }

            // Appearance
            item {
                SettingsSection(title = "Appearance") {
                    SettingsToggleRow(
                        icon     = Icons.Default.DarkMode,
                        label    = "Dark Mode",
                        subtitle = "Use dark theme throughout the app",
                        checked  = darkMode,
                        onCheckedChange = { darkMode = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon     = Icons.Default.Notifications,
                        label    = "Notifications",
                        subtitle = "Processing complete alerts",
                        checked  = notifications,
                        onCheckedChange = { notifications = it }
                    )
                }
            }

            // Help & Support
            item {
                SettingsSection(title = "Help & Support") {
                    SettingsNavRow(
                        icon    = Icons.Default.School,
                        label   = "View Tutorial",
                        onClick = onViewOnboarding
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon    = Icons.Default.HelpOutline,
                        label   = "Help Center",
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon    = Icons.Default.Security,
                        label   = "Privacy Policy",
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon    = Icons.Default.Description,
                        label   = "Terms of Service",
                        onClick = {}
                    )
                }
            }

            // About
            item {
                SettingsSection(title = "About") {
                    SettingsNavRow(
                        icon     = Icons.Default.Info,
                        label    = "Version",
                        subtitle = "1.0.0 (build 1)",
                        onClick  = {}
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon     = Icons.Default.SystemUpdate,
                        label    = "Check for Updates",
                        onClick  = {}
                    )
                }
            }

            // Sign out
            item {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    OutlinedButton(
                        onClick  = {},
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = MaterialTheme.shapes.large,
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Logout, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape    = MaterialTheme.shapes.large,
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "DS",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "DocScan User",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "user@docscanai.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Text(
            title,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = MaterialTheme.shapes.large,
            color    = MaterialTheme.colorScheme.surfaceContainerHigh,
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor       = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor       = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor     = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor     = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 62.dp),
        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
