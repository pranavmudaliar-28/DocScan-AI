package com.example.docscanai.ui.viewer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DocumentViewerScreen(
    docId: String,
    imageUri: String,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val clampedScale = scale.coerceIn(0.5f, 4f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Zoomable + pannable image area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, bottom = 100.dp)
                .clip(RectangleShape)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 4f)
                        offsetX += pan.x
                        offsetY += pan.y
                        if (scale <= 1.05f) { offsetX = 0f; offsetY = 0f }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri.isNotEmpty()) {
                SubcomposeAsyncImage(
                    model = imageUri,
                    contentDescription = "Scanned document",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .scale(clampedScale)
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
                    loading = {
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.BrokenImage, null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Could not load image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            } else {
                Text(
                    "No image available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Top gradient bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0f)
                        ),
                        endY = 200f
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(docId, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text("1 of 1 page", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = { shareImage(context, imageUri) }) {
                        Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        // Bottom controls + AI summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = 200f
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // AI Summary card
            var showAiCard by remember { mutableStateOf(true) }
            if (showAiCard) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape    = RoundedCornerShape(14.dp),
                    color    = Color(0xFF1A3060).copy(alpha = 0.92f),
                    border   = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.25f)),
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.AutoAwesome, null,
                            tint     = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "AI SUMMARY",
                                fontSize      = 9.sp,
                                color         = Color(0xFF60A5FA),
                                fontFamily    = FontFamily.Monospace,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                "Scanned document — tap Edit to extract and review text",
                                fontSize = 12.sp,
                                color    = Color.White.copy(alpha = 0.75f),
                            )
                        }
                        IconButton(onClick = { showAiCard = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Dismiss",
                                tint     = Color.White.copy(alpha = 0.50f),
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerIconBtn(onClick = {
                        scale = (scale - 0.25f).coerceAtLeast(0.5f)
                        if (scale <= 1.05f) { offsetX = 0f; offsetY = 0f }
                    }) {
                        Icon(Icons.Default.ZoomOut, "Out", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "${(clampedScale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(46.dp)
                    )
                    ViewerIconBtn(onClick = { scale = (scale + 0.25f).coerceAtMost(4f) }) {
                        Icon(Icons.Default.ZoomIn, "In", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }

                if (abs(clampedScale - 1f) > 0.05f || abs(offsetX) > 5f || abs(offsetY) > 5f) {
                    TextButton(onClick = { scale = 1f; offsetX = 0f; offsetY = 0f }) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ViewerIconBtn(onClick = { downloadImage(context, imageUri) }) {
                        Icon(Icons.Default.FileDownload, "Download", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    ViewerIconBtn(onClick = { printImage(context, imageUri) }) {
                        Icon(Icons.Default.Print, "Print", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private fun shareImage(context: Context, imageUri: String) {
    if (imageUri.isEmpty()) {
        Toast.makeText(context, "No image to share", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, imageUri.toUri())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share document"))
}

private fun downloadImage(context: Context, imageUri: String) {
    if (imageUri.isEmpty()) {
        Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "DocScan_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DocScan AI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("Insert failed")
        resolver.openInputStream(imageUri.toUri())?.use { input ->
            resolver.openOutputStream(destUri)?.use { output ->
                input.copyTo(output)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(destUri, values, null, null)
        }
        Toast.makeText(context, "Saved to Pictures/DocScan AI", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
    }
}

private fun printImage(context: Context, imageUri: String) {
    if (imageUri.isEmpty()) {
        Toast.makeText(context, "No image to print", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val bitmap = android.graphics.BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(imageUri.toUri())
        ) ?: throw Exception("Could not decode image")
        val printHelper = androidx.print.PrintHelper(context).apply {
            scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
        }
        printHelper.printBitmap("DocScan AI - Document", bitmap)
    } catch (_: Exception) {
        Toast.makeText(context, "Print not available", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ViewerIconBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
