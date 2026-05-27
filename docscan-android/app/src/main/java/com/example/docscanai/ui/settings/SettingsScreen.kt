package com.example.docscanai.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.docscanai.data.AppThemeRepository
import com.example.docscanai.data.AuthRepository
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.DeepNavy
import com.example.docscanai.ui.theme.IntelligentBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onViewOnboarding: () -> Unit,
    onSignOut: () -> Unit = {},
) {
    val context = LocalContext.current
    var highQuality      by remember { mutableStateOf(true) }
    var autoDetect       by remember { mutableStateOf(true) }
    var saveMetadata     by remember { mutableStateOf(false) }
    val darkMode         by AppThemeRepository.isDarkTheme.collectAsStateWithLifecycle()
    var notifications    by remember { mutableStateOf(true) }
    // AI & OCR
    var aiSuggestions    by remember { mutableStateOf(true) }
    var autoCleanup      by remember { mutableStateOf(true) }
    var smartTagging     by remember { mutableStateOf(true) }
    // Cloud & Sync
    var autoSync         by remember { mutableStateOf(true) }
    var wifiOnlySync     by remember { mutableStateOf(false) }

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
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Center content on tablets; match phone padding on small screens
            val sidePad = if (maxWidth > 600.dp) ((maxWidth - 600.dp) / 2).coerceAtLeast(0.dp) else 0.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = sidePad)
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

            // AI & OCR
            item {
                SettingsSection(title = "AI & OCR") {
                    SettingsToggleRow(
                        icon     = Icons.Default.AutoAwesome,
                        label    = "AI Suggestions",
                        subtitle = "Smart field detection and document categorisation",
                        checked  = aiSuggestions,
                        onCheckedChange = { aiSuggestions = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon     = Icons.Default.AutoFixHigh,
                        label    = "Auto-Cleanup OCR",
                        subtitle = "Fix common OCR errors automatically after scanning",
                        checked  = autoCleanup,
                        onCheckedChange = { autoCleanup = it }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon     = Icons.Default.Language,
                        label    = "OCR Language",
                        subtitle = "English",
                        onClick  = {}
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon     = Icons.Default.Label,
                        label    = "Smart Tagging",
                        subtitle = "Automatically tag documents by content type",
                        checked  = smartTagging,
                        onCheckedChange = { smartTagging = it }
                    )
                }
            }

            // Cloud & Sync
            item {
                SettingsSection(title = "Cloud & Sync") {
                    SettingsToggleRow(
                        icon     = Icons.Default.CloudSync,
                        label    = "Auto-Sync",
                        subtitle = "Upload scans to Supabase storage automatically",
                        checked  = autoSync,
                        onCheckedChange = { autoSync = it }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon     = Icons.Default.Wifi,
                        label    = "Wi-Fi Only",
                        subtitle = "Only sync when connected to Wi-Fi",
                        checked  = wifiOnlySync,
                        onCheckedChange = { wifiOnlySync = it }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon     = Icons.Default.Storage,
                        label    = "Storage Used",
                        subtitle = "Tap to view usage details",
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
                        onCheckedChange = { AppThemeRepository.setDarkTheme(context, it) }
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
                        onClick  = onSignOut,
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
        } // LazyColumn
        } // BoxWithConstraints
    }
}

@Composable
private fun ProfileCard(
    name: String  = AuthRepository.getName(),
    email: String = AuthRepository.getEmail(),
) {
    val initials = name.split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "DS" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(DeepNavy, IntelligentBlue))),
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials,
                    style     = MaterialTheme.typography.titleMedium,
                    color     = Color.White,
                    textAlign = TextAlign.Center,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        name.ifBlank { "DocScan User" },
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                    )
                    // Pro badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                null,
                                tint     = AIGlow,
                                modifier = Modifier.size(10.dp),
                            )
                            Text(
                                "PRO",
                                fontSize      = 9.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = Color.White,
                                fontFamily    = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }
                Text(
                    email.ifBlank { "guest@docscanai.com" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f),
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint     = Color.White.copy(alpha = 0.60f),
                modifier = Modifier.size(20.dp),
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
