package com.example.docscanai.ui.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.docscanai.ui.permission.PermissionDeniedDialog
import com.example.docscanai.ui.permission.PermissionRationaleDialog
import com.example.docscanai.ui.permission.PermissionStatus
import com.example.docscanai.ui.permission.galleryPermission
import com.example.docscanai.ui.permission.rememberAppPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── MediaStore helper ─────────────────────────────────────────────────────────

private fun queryRecentImages(context: Context, limit: Int = 30): List<Uri> {
    val images     = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder  = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection, null, null, sortOrder,
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext() && images.size < limit) {
            images.add(
                ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idCol),
                )
            )
        }
    }
    return images
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
    var shouldOpenPicker    by remember { mutableStateOf(false) }
    var galleryImages       by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // System photo picker — no manual permission needed on API 33+
    val pickMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onImageSelected(it) } }

    // System file / document picker — no permission required (SAF)
    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onImageSelected(it) } }

    // Load real images when permission is granted; auto-open picker if pending
    LaunchedEffect(permState.status) {
        if (permState.status == PermissionStatus.Granted) {
            galleryImages = withContext(Dispatchers.IO) { queryRecentImages(context) }
        }
    }

    LaunchedEffect(permState.status, shouldOpenPicker) {
        if (permState.status == PermissionStatus.Granted && shouldOpenPicker) {
            shouldOpenPicker = false
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else if (permState.status != PermissionStatus.Granted && shouldOpenPicker) {
            shouldOpenPicker = false
        }
    }

    // Centralised pick action with permission gate
    fun tryPickImage() {
        when (permState.status) {
            PermissionStatus.Granted -> pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            PermissionStatus.ShowRationale     -> { shouldOpenPicker = true; showRationaleDialog = true }
            PermissionStatus.PermanentlyDenied -> showDeniedDialog = true
        }
    }

    // ── Permission dialogs ────────────────────────────────────────────────────

    if (showRationaleDialog) {
        PermissionRationaleDialog(
            icon      = Icons.Default.PhotoLibrary,
            title     = "Photo Library Access",
            rationale = "DocScan AI needs access to your photos to import documents for scanning.",
            onAllow   = {
                showRationaleDialog = false
                permState.launchRequest()
            },
            onDismiss = {
                showRationaleDialog = false
                shouldOpenPicker    = false
            },
        )
    }

    if (showDeniedDialog) {
        PermissionDeniedDialog(
            icon      = Icons.Default.PhotoLibrary,
            title     = "Photo Access Blocked",
            message   = "Photo library access was denied. Open Settings and allow Photos access to import documents.",
            onDismiss = { showDeniedDialog = false },
        )
    }

    // ── Screen content ────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Import from Gallery",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
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
            // Action buttons — row 1: gallery + camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick  = { tryPickImage() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = MaterialTheme.shapes.large,
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Browse Gallery", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick  = onCamera,
                    modifier = Modifier.height(52.dp),
                    shape    = MaterialTheme.shapes.large,
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        "Camera",
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Action button — row 2: file manager
            OutlinedButton(
                onClick  = {
                    pickFileLauncher.launch(
                        arrayOf("image/*", "image/jpeg", "image/png", "application/pdf")
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp),
                shape  = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Browse Files  (Images, PDF)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "  Recent Photos  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            when {
                permState.status != PermissionStatus.Granted -> {
                    PermissionPromptGrid(onGrantPermission = { showRationaleDialog = true })
                }
                galleryImages.isEmpty() -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No photos found on this device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(3),
                        modifier              = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement   = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding        = PaddingValues(bottom = 24.dp),
                    ) {
                        items(galleryImages, key = { it.toString() }) { uri ->
                            RealImageTile(uri = uri, onClick = { onImageSelected(uri) })
                        }
                    }
                }
            }
        }
    }
}

// ── Image tile ────────────────────────────────────────────────────────────────

@Composable
private fun RealImageTile(uri: Uri, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.78f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model              = uri,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
        )
    }
}

// ── Placeholder shown before permission is granted ────────────────────────────

@Composable
private fun PermissionPromptGrid(onGrantPermission: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Allow access to your photos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Grant photo library access to see your recent images here.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onGrantPermission,
                shape   = MaterialTheme.shapes.large,
            ) {
                Text("Grant Access")
            }
        }
    }
}
