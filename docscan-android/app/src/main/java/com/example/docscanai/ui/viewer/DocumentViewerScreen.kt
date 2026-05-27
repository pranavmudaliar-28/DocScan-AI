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
import android.app.Activity
import com.example.docscanai.ui.ads.AdMobInterstitial

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
        // Zoomable + pannable image area (Paper mockup)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, bottom = 120.dp)
                .clip(RectangleShape)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 4f)
                        offsetX += pan.x
                        offsetY += pan.y
                        if (scale <= 1.05f) { offsetX = 0f; offsetY = 0f }
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .scale(clampedScale)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                if (imageUri.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = imageUri,
                        contentDescription = "Scanned document",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        loading = {
                            Box(modifier = Modifier.height(400.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            }
                        },
                        error = {
                            Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.BrokenImage, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Could not load image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                        Text("No image available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Floating Scrollbar
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                Text("4", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                Text("12", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
            }
        }

        // Top bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Column {
                        Text("Q4 Vendor Agreement", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                        Text("PDF · 12 PAGES · 4.2 MB", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                    }
                }
                Row {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.StarBorder, "Favorite", tint = Color(0xFFF59E0B))
                    }
                    IconButton(onClick = { shareImage(context, imageUri) }) {
                        Icon(Icons.Default.Upload, "Share", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        // Bottom controls + AI summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        ) {
            // AI Summary card
            var showAiCard by remember { mutableStateOf(true) }
            if (showAiCard) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(4.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(12.dp))
                                    Text("AI SUMMARY", fontSize = 10.sp, color = Color(0xFF3B82F6), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                        Text(
                            "12-page MSA between Acme Co. and Northwind. Key terms: Net 30 payment, 5-year confidentiality, scope defined in Exhibit A.\nAction: requires signature on page 11.",
                            fontSize = 14.sp,
                            color = Color(0xFF334155),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Bottom Navigation Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toolbar items
                    val items = listOf(
                        Icons.Default.ZoomIn to "Zoom",
                        Icons.Default.TextFields to "Annotate",
                        Icons.Default.AutoFixHigh to "Cleanup",
                        Icons.Default.SwapHoriz to "Convert"
                    )
                    
                    items.forEach { (icon, label) ->
                        val isSelected = label == "Cleanup"
                        val color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF64748B)
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                            Box {
                                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                                if (isSelected) {
                                    Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)).align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(label, fontSize = 11.sp, color = color, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                    
                    // Edit button
                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
        Toast.makeText(context, "No file to save", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val resolver = context.contentResolver
        val mime     = resolver.getType(imageUri.toUri()) ?: "image/jpeg"
        val isImage  = mime.startsWith("image/")
        val ext      = when {
            mime == "application/pdf"          -> "pdf"
            mime.contains("wordprocessingml")
                || mime == "application/msword" -> "docx"
            mime == "text/plain"               -> "txt"
            mime == "text/csv"
                || mime.contains("excel")
                || mime.contains("spreadsheet") -> "csv"
            mime.startsWith("image/png")       -> "png"
            else                               -> "jpg"
        }
        val fileName = "DocScan_${System.currentTimeMillis()}.$ext"

        if (isImage) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DocScan AI")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Insert failed")
            resolver.openInputStream(imageUri.toUri())?.use { input ->
                resolver.openOutputStream(destUri)?.use { output -> input.copyTo(output) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(destUri, values, null, null)
            }
            Toast.makeText(context, "Saved to Pictures/DocScan AI", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/DocScan AI")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val destUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("Insert failed")
                resolver.openInputStream(imageUri.toUri())?.use { input ->
                    resolver.openOutputStream(destUri)?.use { output -> input.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(destUri, values, null, null)
            } else {
                val dir = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "DocScan AI"
                ).also { it.mkdirs() }
                resolver.openInputStream(imageUri.toUri())?.use { input ->
                    java.io.File(dir, fileName).outputStream().use { output -> input.copyTo(output) }
                }
            }
            Toast.makeText(context, "Saved to Downloads/DocScan AI", Toast.LENGTH_SHORT).show()
        }
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
