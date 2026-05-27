package com.example.docscanai.ui.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.docscanai.ui.permission.PermissionDeniedDialog
import com.example.docscanai.ui.permission.PermissionRationaleDialog
import com.example.docscanai.ui.permission.PermissionStatus
import com.example.docscanai.ui.permission.galleryPermission
import com.example.docscanai.ui.permission.rememberAppPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Data model ────────────────────────────────────────────────────────────────

private data class FileEntry(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val isImage: Boolean,
)

private enum class FileFilter(val label: String) {
    ALL("All"), IMAGES("Images"), PDF("PDF"),
    DOCS("Docs"), TEXT("Text"), OTHER("Other")
}

// ── MediaStore helpers ────────────────────────────────────────────────────────

private suspend fun queryAllRecentFiles(context: Context, limit: Int = 60): List<FileEntry> =
    withContext(Dispatchers.IO) {
        val results = mutableListOf<FileEntry>()

        // ── Images ────────────────────────────────────────────────────────────
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE),
            null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            while (cursor.moveToNext() && results.size < limit / 2) {
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol))
                results.add(FileEntry(uri, cursor.getString(nameCol) ?: "Image", cursor.getString(mimeCol) ?: "image/jpeg", true))
            }
        }

        // ── Downloads (PDFs, DOCX, TXT, CSV, etc.) ───────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.MIME_TYPE),
                null, null, "${MediaStore.Downloads.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                while (cursor.moveToNext() && results.count { !it.isImage } < limit / 2) {
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val uri  = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(idCol))
                    results.add(FileEntry(uri, cursor.getString(nameCol) ?: "File", mime, mime.startsWith("image/")))
                }
            }
        }

        // ── Files / Documents (API < Q fallback via Files table) ─────────────
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val sel = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?,?,?,?,?,?)"
            val args = arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword",
                "text/plain",
                "text/csv",
                "application/vnd.ms-excel",
            )
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.MIME_TYPE),
                sel, args, "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                while (cursor.moveToNext() && results.size < limit) {
                    val mime = cursor.getString(mimeCol) ?: ""
                    val uri  = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), cursor.getLong(idCol))
                    results.add(FileEntry(uri, cursor.getString(nameCol) ?: "File", mime, false))
                }
            }
        }

        results.sortedByDescending { it.uri.toString() }
    }

private fun FileEntry.matchesFilter(filter: FileFilter) = when (filter) {
    FileFilter.ALL    -> true
    FileFilter.IMAGES -> isImage
    FileFilter.PDF    -> mimeType == "application/pdf"
    FileFilter.DOCS   -> mimeType.contains("word") || mimeType.contains("document")
    FileFilter.TEXT   -> mimeType == "text/plain" || mimeType == "text/csv" || mimeType.contains("spreadsheet") || mimeType.contains("excel")
    FileFilter.OTHER  -> !isImage && mimeType != "application/pdf" && !mimeType.contains("word") && !mimeType.contains("document") && mimeType != "text/plain" && mimeType != "text/csv"
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryImportScreen(
    onBack: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    onCamera: () -> Unit,
) {
    val context   = LocalContext.current
    val permState = rememberAppPermissionState(galleryPermission())

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showDeniedDialog    by remember { mutableStateOf(false) }
    var allFiles            by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var activeFilter        by remember { mutableStateOf(FileFilter.ALL) }

    // System photo picker
    val pickMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onImageSelected(it) } }

    // Full file manager — ALL types
    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onImageSelected(it) } }

    LaunchedEffect(permState.status) {
        if (permState.status == PermissionStatus.Granted) {
            allFiles = queryAllRecentFiles(context)
        }
    }

    val filtered = remember(allFiles, activeFilter) {
        allFiles.filter { it.matchesFilter(activeFilter) }
    }

    // ── Permission dialogs ────────────────────────────────────────────────────
    if (showRationaleDialog) {
        PermissionRationaleDialog(
            icon      = Icons.Default.PhotoLibrary,
            title     = "Storage Access",
            rationale = "DocScan AI needs access to your files to import documents, images, PDFs, and more.",
            onAllow   = { showRationaleDialog = false; permState.launchRequest() },
            onDismiss = { showRationaleDialog = false },
        )
    }
    if (showDeniedDialog) {
        PermissionDeniedDialog(
            icon      = Icons.Default.PhotoLibrary,
            title     = "Storage Access Blocked",
            message   = "Storage access was denied. Open Settings and allow storage access to import files.",
            onDismiss = { showDeniedDialog = false },
        )
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Import File", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {

            // ── Action row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Browse Gallery (images only)
                Button(
                    onClick  = {
                        when (permState.status) {
                            PermissionStatus.Granted ->
                                pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            PermissionStatus.ShowRationale -> showRationaleDialog = true
                            PermissionStatus.PermanentlyDenied -> showDeniedDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = MaterialTheme.shapes.large,
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery", style = MaterialTheme.typography.labelLarge)
                }

                // Camera shortcut
                OutlinedButton(
                    onClick  = onCamera,
                    modifier = Modifier.height(50.dp),
                    shape    = MaterialTheme.shapes.large,
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(Icons.Default.CameraAlt, "Camera", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }

                // Browse all files
                OutlinedButton(
                    onClick  = {
                        pickFileLauncher.launch(
                            arrayOf(
                                "*/*",   // all — file manager shows everything
                            )
                        )
                    },
                    modifier = Modifier.height(50.dp),
                    shape    = MaterialTheme.shapes.large,
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(Icons.Default.FolderOpen, "Files", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            // ── Supported formats chips ───────────────────────────────────────
            LazyRow(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding      = PaddingValues(end = 8.dp),
            ) {
                val formats = listOf("JPG/PNG" to Icons.Default.Image, "PDF" to Icons.Default.PictureAsPdf,
                    "DOCX" to Icons.Default.Description, "TXT" to Icons.Default.TextFields,
                    "CSV" to Icons.Default.TableChart)
                items(formats) { (label, icon) ->
                    Surface(
                        shape  = MaterialTheme.shapes.extraSmall,
                        color  = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(icon, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Filter tabs ───────────────────────────────────────────────────
            LazyRow(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding    = PaddingValues(end = 8.dp),
            ) {
                items(FileFilter.entries) { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick  = { activeFilter = filter },
                        label    = {
                            val count = allFiles.count { it.matchesFilter(filter) }
                            Text(if (filter == FileFilter.ALL) filter.label else "${filter.label} ($count)",
                                style = MaterialTheme.typography.labelSmall)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── File grid ─────────────────────────────────────────────────────
            when {
                permState.status != PermissionStatus.Granted -> {
                    PermissionPromptGrid(
                        onGrantPermission = {
                            when (permState.status) {
                                PermissionStatus.ShowRationale     -> showRationaleDialog = true
                                PermissionStatus.PermanentlyDenied -> showDeniedDialog = true
                                else -> permState.launchRequest()
                            }
                        }
                    )
                }
                filtered.isEmpty() && allFiles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("No files found on this device", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Use the folder icon above to browse manually", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        }
                    }
                }
                filtered.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No ${activeFilter.label.lowercase()} files found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(3),
                        modifier              = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding        = PaddingValues(bottom = 24.dp),
                    ) {
                        items(filtered, key = { it.uri.toString() }) { entry ->
                            FileTile(entry = entry, onClick = { onImageSelected(entry.uri) })
                        }
                    }
                }
            }
        }
    }
}

// ── File tile ─────────────────────────────────────────────────────────────────

@Composable
private fun FileTile(entry: FileEntry, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        if (entry.isImage) {
            // Real thumbnail for images
            AsyncImage(
                model              = entry.uri,
                contentDescription = entry.name,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        } else {
            // Icon tile for documents
            val (icon, tint, bg) = fileVisuals(entry.mimeType)
            Box(
                modifier = Modifier.fillMaxSize().background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(8.dp),
                ) {
                    Icon(icon, null, modifier = Modifier.size(36.dp), tint = tint)
                    Text(
                        text     = entry.name,
                        style    = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // File type badge (always shown)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(
                text  = mimeToExt(entry.mimeType),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color.White,
            )
        }
    }
}

// ── Visual helpers ────────────────────────────────────────────────────────────

@Composable
private fun fileVisuals(mime: String): Triple<ImageVector, Color, Color> {
    val primary   = MaterialTheme.colorScheme.primary
    val error     = MaterialTheme.colorScheme.error
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    return when {
        mime == "application/pdf"         -> Triple(Icons.Default.PictureAsPdf, error,     MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
        mime.contains("word") ||
            mime.contains("document")     -> Triple(Icons.Default.Description,  primary,   MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        mime == "text/plain"              -> Triple(Icons.Default.TextFields,    secondary, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        mime == "text/csv" ||
            mime.contains("spreadsheet") ||
            mime.contains("excel")        -> Triple(Icons.Default.TableChart,   tertiary,  MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
        mime.startsWith("image/")         -> Triple(Icons.Default.Image,         primary,   MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        else                              -> Triple(Icons.Default.InsertDriveFile, secondary, MaterialTheme.colorScheme.surfaceContainerHigh)
    }
}

private fun mimeToExt(mime: String) = when {
    mime == "application/pdf"                          -> "PDF"
    mime.contains("wordprocessingml") || mime.contains("msword") -> "DOCX"
    mime == "text/plain"                               -> "TXT"
    mime == "text/csv"                                 -> "CSV"
    mime.contains("spreadsheet") || mime.contains("excel") -> "XLS"
    mime.startsWith("image/jpeg") || mime.startsWith("image/jpg") -> "JPG"
    mime.startsWith("image/png")                       -> "PNG"
    mime.startsWith("image/")                          -> "IMG"
    else -> "FILE"
}

// ── Permission prompt ─────────────────────────────────────────────────────────

@Composable
private fun PermissionPromptGrid(onGrantPermission: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
            }
            Text("Allow File Access", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Grant storage access to browse photos, PDFs, Word documents, text files, and more.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onGrantPermission, shape = MaterialTheme.shapes.large) {
                Text("Grant Access")
            }
        }
    }
}
