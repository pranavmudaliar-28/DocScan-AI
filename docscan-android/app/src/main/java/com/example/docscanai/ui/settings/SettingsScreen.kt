package com.example.docscanai.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.docscanai.data.AppSettingsRepository
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
    onPrivacyPolicy: () -> Unit,
    onTermsAndConditions: () -> Unit,
    onSignOut: () -> Unit = {},
) {
    val context = LocalContext.current
    
    val highQuality      by AppSettingsRepository.highQuality.collectAsStateWithLifecycle()
    val autoDetect       by AppSettingsRepository.autoDetect.collectAsStateWithLifecycle()
    val saveMetadata     by AppSettingsRepository.saveMetadata.collectAsStateWithLifecycle()
    val notifications    by AppSettingsRepository.notifications.collectAsStateWithLifecycle()
    val aiSuggestions    by AppSettingsRepository.aiSuggestions.collectAsStateWithLifecycle()
    val autoCleanup      by AppSettingsRepository.autoCleanup.collectAsStateWithLifecycle()
    val smartTagging     by AppSettingsRepository.smartTagging.collectAsStateWithLifecycle()
    val autoSync         by AppSettingsRepository.autoSync.collectAsStateWithLifecycle()
    val wifiOnlySync     by AppSettingsRepository.wifiOnlySync.collectAsStateWithLifecycle()

    val themeMode by AppThemeRepository.themeMode.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            com.example.docscanai.ui.ads.AdMobBanner()
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val sidePad = if (maxWidth > 600.dp) ((maxWidth - 600.dp) / 2).coerceAtLeast(0.dp) else 0.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = sidePad)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Profile card
                item { ProfileCard() }

                // Scan Settings
                item {
                    SettingsSection(title = "Scan Settings") {
                        SettingsToggleRow(
                            icon     = Icons.Default.HighQuality,
                            label    = "High Quality Mode",
                            subtitle = "Capture at maximum resolution",
                            checked  = highQuality,
                            onCheckedChange = { AppSettingsRepository.setSetting(context, "highQuality", it) }
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon     = Icons.Default.CenterFocusStrong,
                            label    = "Auto-detect Document",
                            subtitle = "Automatically find document edges",
                            checked  = autoDetect,
                            onCheckedChange = { AppSettingsRepository.setSetting(context, "autoDetect", it) }
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon     = Icons.Default.Info,
                            label    = "Include Metadata",
                            subtitle = "Embed scan date and settings in file",
                            checked  = saveMetadata,
                            onCheckedChange = { AppSettingsRepository.setSetting(context, "saveMetadata", it) }
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
                            onCheckedChange = { AppSettingsRepository.setSetting(context, "aiSuggestions", it) }
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon     = Icons.Default.AutoFixHigh,
                            label    = "Auto-Cleanup OCR",
                            subtitle = "Fix common OCR errors automatically after scanning",
                            checked  = autoCleanup,
                            onCheckedChange = { AppSettingsRepository.setSetting(context, "autoCleanup", it) }
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon     = Icons.AutoMirrored.Filled.Label,
                            label    = "Smart Tagging",
                            subtitle = "Automatically tag documents by content type",
                            checked  = smartTagging,
                            onCheckedChange = { AppSettingsRepository.setSetting(context, "smartTagging", it) }
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
                            onCheckedChange = { AppSettingsRepository.setSetting(context, "autoSync", it) }
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon     = Icons.Default.Wifi,
                            label    = "Wi-Fi Only",
                            subtitle = "Only sync when connected to Wi-Fi",
                            checked  = wifiOnlySync,
                            onCheckedChange = { AppSettingsRepository.setSetting(context, "wifiOnlySync", it) }
                        )
                    }
                }

                // Appearance
                item {
                    SettingsSection(title = "Appearance") {
                        val subtitle = when(themeMode) {
                            com.example.docscanai.data.ThemeMode.SYSTEM_DEFAULT -> "System Default"
                            com.example.docscanai.data.ThemeMode.LIGHT -> "Light Mode"
                            com.example.docscanai.data.ThemeMode.DARK -> "Dark Mode"
                        }
                        SettingsNavRow(
                            icon     = Icons.Default.DarkMode,
                            label    = "Theme",
                            subtitle = subtitle,
                            onClick  = { showThemeDialog = true }
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon     = Icons.Default.Notifications,
                            label    = "Notifications",
                            subtitle = "Processing complete alerts",
                            checked  = notifications,
                            onCheckedChange = { AppSettingsRepository.setSetting(context, "notifications", it) }
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
                            icon    = Icons.Default.PrivacyTip,
                            label   = "Privacy Policy",
                            onClick = onPrivacyPolicy
                        )
                        SettingsDivider()
                        SettingsNavRow(
                            icon    = Icons.Default.Gavel,
                            label   = "Terms & Conditions",
                            onClick = onTermsAndConditions
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
                            Icon(Icons.AutoMirrored.Filled.Logout, null,
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sign Out", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            AppThemeRepository.setThemeMode(context, com.example.docscanai.data.ThemeMode.SYSTEM_DEFAULT)
                            showThemeDialog = false
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == com.example.docscanai.data.ThemeMode.SYSTEM_DEFAULT, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text("System Default", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            AppThemeRepository.setThemeMode(context, com.example.docscanai.data.ThemeMode.LIGHT)
                            showThemeDialog = false
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == com.example.docscanai.data.ThemeMode.LIGHT, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Light Mode", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            AppThemeRepository.setThemeMode(context, com.example.docscanai.data.ThemeMode.DARK)
                            showThemeDialog = false
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == com.example.docscanai.data.ThemeMode.DARK, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Dark Mode", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
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
