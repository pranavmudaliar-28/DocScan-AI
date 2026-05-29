package com.example.docscanai.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Sort
import kotlinx.coroutines.launch
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
// removed import com.example.docscanai.data.ScanRecord
import com.example.docscanai.data.local.DocumentEntity
import com.example.docscanai.data.local.FolderEntity
import com.example.docscanai.data.local.TagEntity
import com.example.docscanai.data.local.DatabaseModule
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
    onScanItem: (DocumentEntity) -> Unit,
    onFolderClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    onPdfMerge: () -> Unit = {},
    onPdfSplit: () -> Unit = {},
    onPdfCompress: () -> Unit = {},
    onRoute: (String) -> Unit = {},
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DatabaseModule.localDocumentRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentScans = if (state is MainScreenUiState.Success) {
        (state as MainScreenUiState.Success).data
    } else emptyList()
    val folders = if (state is MainScreenUiState.Success) {
        (state as MainScreenUiState.Success).folders
    } else emptyList()
    val tags = if (state is MainScreenUiState.Success) {
        (state as MainScreenUiState.Success).tags
    } else emptyList()

    val selectedDocIds by viewModel.selectedDocIds.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    HomeScreen(
        recentScans = recentScans,
        folders     = folders,
        tags        = tags,
        modifier    = modifier,
        onScan      = onScan,
        onGallery   = onGallery,
        onSettings  = onSettings,
        onConvert   = onConvert,
        onSearch    = onSearch,
        onPdfMerge  = onPdfMerge,
        onPdfSplit  = onPdfSplit,
        onPdfCompress = onPdfCompress,
        onRoute = onRoute,
        onScanItem  = onScanItem,
        onFolderClick = onFolderClick,
        onCreateFolder = { name ->
            coroutineScope.launch {
                viewModel.createFolder(name)
            }
        },
        onCreateTag = { name ->
            coroutineScope.launch {
                viewModel.createTag(name)
            }
        },
        selectedDocIds = selectedDocIds,
        onToggleSelect = { viewModel.toggleSelection(it) },
        onClearSelect = { viewModel.clearSelection() },
        onDeleteSelected = { viewModel.deleteSelectedDocuments() },
        onMoveSelected = { folderId -> viewModel.moveSelectedToFolder(folderId) },
        onSetSortOrder = { viewModel.setSortOrder(it) }
    )
}

@Composable
internal fun HomeScreen(
    recentScans: List<DocumentEntity>,
    folders: List<FolderEntity> = emptyList(),
    tags: List<TagEntity> = emptyList(),
    modifier: Modifier = Modifier,
    onScan: () -> Unit = {},
    onGallery: () -> Unit = {},
    onSettings: () -> Unit = {},
    onConvert: () -> Unit = {},
    onSearch: () -> Unit = {},
    onPdfMerge: () -> Unit = {},
    onPdfSplit: () -> Unit = {},
    onPdfCompress: () -> Unit = {},
    onRoute: (String) -> Unit = {},
    onScanItem: (DocumentEntity) -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    onCreateTag: (String) -> Unit = {},
    selectedDocIds: Set<String> = emptySet(),
    onToggleSelect: (String) -> Unit = {},
    onClearSelect: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onMoveSelected: (String?) -> Unit = {},
    onSetSortOrder: (SortOrder) -> Unit = {}
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
                            3 -> selectedTab = 3
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

            when (selectedTab) {
                1 -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    ToolsTab(
                        modifier = Modifier.fillMaxSize().padding(horizontal = sidePad),
                        onToolClick = { tool ->
                            when (tool.route) {
                                "camera_scanner" -> onScan()
                                "search" -> onSearch()
                                "convert" -> onConvert()
                                "app_settings" -> onSettings()
                                "pdf_merge" -> onPdfMerge()
                                "pdf_split" -> onPdfSplit()
                                "pdf_compress" -> onPdfCompress()
                                "signature_create" -> onRoute("signature_create")
                                "signature_library" -> onRoute("signature_library")
                                "pdf_sign" -> onRoute("pdf_sign")
                                "image_sign" -> onRoute("image_sign")
                                else -> android.widget.Toast.makeText(context, "${tool.name} coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                3 -> {
                    FilesTab(
                        scans = recentScans,
                        folders = folders,
                        tags = tags,
                        selectedDocIds = selectedDocIds,
                        onToggleSelect = onToggleSelect,
                        onClearSelect = onClearSelect,
                        onDeleteSelected = onDeleteSelected,
                        onMoveSelected = onMoveSelected,
                        onScanItem = onScanItem,
                        onFolderClick = onFolderClick,
                        onCreateFolder = onCreateFolder,
                        onCreateTag = onCreateTag,
                        onSetSortOrder = onSetSortOrder,
                        modifier = Modifier.fillMaxSize().padding(horizontal = sidePad)
                    )
                }
                else -> {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }

                    LazyColumn(
                        modifier       = Modifier.fillMaxSize().padding(horizontal = sidePad),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        item { 
                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                                        androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(300)) { it / 4 }
                            ) {
                                AvatarHeader(onSettings = onSettings) 
                            }
                        }

                        item {
                            Spacer(Modifier.height(20.dp))
                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300, delayMillis = 100)) + 
                                        androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(300, delayMillis = 100)) { it / 4 }
                            ) {
                                QuickActionsGrid(
                                    onScan    = onScan,
                                    onGallery = onGallery,
                                    onConvert = onConvert,
                                )
                            }
                        }

                        item {
                            Spacer(Modifier.height(20.dp))
                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300, delayMillis = 200)) + 
                                        androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(300, delayMillis = 200)) { it / 4 }
                            ) {
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
    }
}

@Composable
private fun FilesTab(
    scans: List<DocumentEntity>,
    folders: List<FolderEntity> = emptyList(),
    tags: List<TagEntity> = emptyList(),
    selectedDocIds: Set<String> = emptySet(),
    onToggleSelect: (String) -> Unit = {},
    onClearSelect: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onMoveSelected: (String?) -> Unit = {},
    onScanItem: (DocumentEntity) -> Unit,
    onFolderClick: (String) -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    onCreateTag: (String) -> Unit = {},
    onSetSortOrder: (SortOrder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newTagName by remember { mutableStateOf("") }
    val viewMode by com.example.docscanai.data.AppSettingsRepository.viewMode.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        onCreateFolder(newFolderName)
                    }
                    showCreateFolderDialog = false
                    newFolderName = ""
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateTagDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTagDialog = false },
            title = { Text("Create Tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTagName.isNotBlank()) {
                        onCreateTag(newTagName)
                    }
                    showCreateTagDialog = false
                    newTagName = ""
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    var showMoveToFolderDialog by remember { mutableStateOf(false) }

    if (showMoveToFolderDialog) {
        AlertDialog(
            onDismissRequest = { showMoveToFolderDialog = false },
            title = { Text("Move to Folder") },
            text = {
                LazyColumn {
                    item {
                        Text(
                            "Remove from Folder (Root)",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMoveSelected(null)
                                    showMoveToFolderDialog = false
                                }
                                .padding(16.dp)
                        )
                    }
                    items(folders) { folder ->
                        Text(
                            folder.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMoveSelected(folder.id)
                                    showMoveToFolderDialog = false
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveToFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Bar
        item {
            if (selectedDocIds.isNotEmpty()) {
                // Contextual Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = onClearSelect) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                        Text(
                            "${selectedDocIds.size} selected",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { showMoveToFolderDialog = true }) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = "Move to Folder")
                        }
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.Red)
                        }
                    }
                }
            } else {
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
                            modifier = Modifier.size(40.dp).clickable { showCreateFolderDialog = true }
                        ) {
                            Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(40.dp).clickable { showSortMenu = true }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Date Modified (Newest)") },
                                    onClick = { onSetSortOrder(SortOrder.DATE_MODIFIED_DESC); showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date Created (Newest)") },
                                    onClick = { onSetSortOrder(SortOrder.DATE_CREATED_DESC); showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A-Z)") },
                                    onClick = { onSetSortOrder(SortOrder.NAME_ASC); showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (Z-A)") },
                                    onClick = { onSetSortOrder(SortOrder.NAME_DESC); showSortMenu = false }
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (viewMode == "LIST") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(40.dp).clickable { com.example.docscanai.data.AppSettingsRepository.setViewMode(context, "LIST") }
                        ) {
                            Icon(Icons.Default.List, null, modifier = Modifier.padding(10.dp), tint = if (viewMode == "LIST") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (viewMode == "GRID") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(40.dp).clickable { com.example.docscanai.data.AppSettingsRepository.setViewMode(context, "GRID") }
                        ) {
                            Icon(Icons.Default.GridView, null, modifier = Modifier.padding(10.dp), tint = if (viewMode == "GRID") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        }
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
        // Tags Section
        item {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable { showCreateTagDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Tag", modifier = Modifier.size(16.dp))
                            Text("New Tag", fontSize = 13.sp)
                        }
                    }
                }
                items(tags, key = { it.id }) { tag ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable { /* TODO filter by tag */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(
                                try { Color(android.graphics.Color.parseColor(tag.colorCode)) } catch (e: Exception) { IntelligentBlue }
                            ))
                            Text(tag.name, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        // Folders Section
        if (folders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "FOLDERS · ${folders.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            item {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(folders, key = { it.id }) { folder ->
                        FolderItem(folder = folder, onClick = { onFolderClick(folder.id) })
                    }
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
            item {
                androidx.compose.animation.AnimatedContent(
                    targetState = viewMode,
                    label = "files_view_mode",
                    transitionSpec = {
                        androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                    }
                ) { mode ->
                    if (mode == "LIST") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            scans.forEach { record ->
                                val isGallery = record.id.startsWith("gallery")
                                val typeColor = if (isGallery) Color(0xFF06B6D4) else IntelligentBlue
                                val fileType = if (isGallery) "IMG" else "SCAN"
                                
                                FilesTabScanItem(
                                    type = fileType, typeColor = typeColor,
                                    title = record.name, subtitle = "1 page · 2.1 MB",
                                    date = dateFormat.format(Date(record.timestamp)), isStarred = false, hasAi = !isGallery,
                                    isSelected = selectedDocIds.contains(record.id),
                                    selectionModeEnabled = selectedDocIds.isNotEmpty(),
                                    onClick = {
                                        if (selectedDocIds.isNotEmpty()) {
                                            onToggleSelect(record.id)
                                        } else {
                                            onScanItem(record)
                                        }
                                    },
                                    onLongClick = {
                                        onToggleSelect(record.id)
                                    }
                                )
                            }
                        }
                    } else {
                        // Grid Mode using simple Grid emulation with rows
                        val chunked = scans.chunked(2)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunked.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { record ->
                                        val isGallery = record.id.startsWith("gallery")
                                        val typeColor = if (isGallery) Color(0xFF06B6D4) else IntelligentBlue
                                        val fileType = if (isGallery) "IMG" else "SCAN"

                                        FilesTabGridScanItem(
                                            modifier = Modifier.weight(1f),
                                            type = fileType, typeColor = typeColor,
                                            title = record.name, subtitle = "2.1 MB",
                                            date = dateFormat.format(Date(record.timestamp)), isStarred = false, hasAi = !isGallery,
                                            isSelected = selectedDocIds.contains(record.id),
                                            selectionModeEnabled = selectedDocIds.isNotEmpty(),
                                            onClick = {
                                                if (selectedDocIds.isNotEmpty()) {
                                                    onToggleSelect(record.id)
                                                } else {
                                                    onScanItem(record)
                                                }
                                            },
                                            onLongClick = {
                                                onToggleSelect(record.id)
                                            }
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FilesTabGridScanItem(
    modifier: Modifier = Modifier,
    type: String, typeColor: Color,
    title: String, subtitle: String,
    date: String, isStarred: Boolean, hasAi: Boolean,
    isSelected: Boolean = false,
    selectionModeEnabled: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val backgroundColor = if (isSelected) typeColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) typeColor else MaterialTheme.colorScheme.surfaceVariant
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier.fillMaxWidth().aspectRatio(0.85f).combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = typeColor, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            type,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                if (hasAi) {
                    Icon(Icons.Default.AutoAwesome, null, tint = IntelligentBlue, modifier = Modifier.size(16.dp))
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}



@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FilesTabScanItem(
    type: String, typeColor: Color,
    title: String, subtitle: String,
    date: String, isStarred: Boolean, hasAi: Boolean,
    isSelected: Boolean = false,
    selectionModeEnabled: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val backgroundColor = if (isSelected) typeColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) typeColor else MaterialTheme.colorScheme.surfaceVariant
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
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
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = typeColor, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        type,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
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

@Composable
private fun FolderItem(
    folder: FolderEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(140.dp).clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = IntelligentBlue,
                modifier = Modifier.size(32.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    folder.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "0 files", // TODO count files
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    scans: List<DocumentEntity>,
    onScanItem: (DocumentEntity) -> Unit,
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
private fun ScanItem(record: DocumentEntity, onClick: () -> Unit) {
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
        val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
            targetValue = when (selectedTab) {
                0 -> (-80).dp
                1 -> (-30).dp
                3 -> 30.dp
                4 -> 80.dp
                else -> 0.dp
            },
            animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
            label = "indicator_offset"
        )
        val intelligentBlue = IntelligentBlue
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(68.dp)
                    .drawBehind {
                        val indicatorWidth = 40.dp.toPx()
                        val centerX = size.width / 2 + indicatorOffset.toPx()
                        drawLine(
                            color = intelligentBlue,
                            start = Offset(centerX - indicatorWidth / 2, 0f),
                            end = Offset(centerX + indicatorWidth / 2, 0f),
                            strokeWidth = 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
            TabBarItem(
                icon     = Icons.Outlined.Home,
                label    = "Home",
                selected = selectedTab == 0,
                onClick  = { onTabSelect(0) },
            )
            TabBarItem(
                icon     = Icons.Default.Apps,
                label    = "Tools",
                selected = selectedTab == 1,
                onClick  = { onTabSelect(1) },
            )

            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "fab_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "fab_pulse_scale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.55f,
                targetValue = 0.25f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "fab_pulse_alpha"
            )

            // Elevated center scan button — rounded square with blue glow
            Box(
                modifier = Modifier
                    .offset(y = (-14).dp)
                    .shadow(
                        elevation    = 14.dp,
                        shape        = RoundedCornerShape(18.dp),
                        ambientColor = IntelligentBlue.copy(alpha = 0.30f),
                        spotColor    = IntelligentBlue.copy(alpha = pulseAlpha),
                    )
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .androidx.compose.ui.graphics.graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
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
                icon     = Icons.Outlined.FolderOpen,
                label    = "Docs",
                selected = selectedTab == 3,
                onClick  = { onTabSelect(3) },
            )
            TabBarItem(
                icon     = Icons.Outlined.Person,
                label    = "Profile",
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



