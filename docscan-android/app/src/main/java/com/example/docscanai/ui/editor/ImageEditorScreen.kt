package com.example.docscanai.ui.editor

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.SubcomposeAsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.launch

data class Stroke(
    val path: androidx.compose.ui.graphics.Path,
    val color: Color,
    val width: Float = 8f,
    val isEraser: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    imageUri: String,
    onBack: () -> Unit,
    onExtractText: (String) -> Unit
) {
    val context = LocalContext.current
    var currentImageUri by remember { mutableStateOf(imageUri) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    // Draw state
    var isDrawingMode by remember { mutableStateOf(false) }
    var paths by remember { mutableStateOf(listOf<Stroke>()) }
    var currentPath by remember { mutableStateOf<androidx.compose.ui.graphics.Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Red) }
    var isEraser by remember { mutableStateOf(false) }
    
    // UI state
    var showDrawMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showTuneMenu by remember { mutableStateOf(false) }
    var showWatermarkMenu by remember { mutableStateOf(false) }
    var showSignatureMenu by remember { mutableStateOf(false) }
    
    // Additional overlays
    var watermarkText by remember { mutableStateOf("") }
    
    // Filter state
    var colorMatrix by remember { mutableStateOf(androidx.compose.ui.graphics.ColorMatrix()) }
    var brightness by remember { mutableFloatStateOf(0f) } // -1f to 1f
    var contrast by remember { mutableFloatStateOf(0f) } // -1f to 1f

    val combinedMatrix = remember(colorMatrix, brightness, contrast) {
        val c = contrast + 1f
        val t = (-0.5f * c + 0.5f) * 255f + (brightness * 255f)
        val tuneMatrix = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            c,  0f, 0f, 0f, t,
            0f, c,  0f, 0f, t,
            0f, 0f, c,  0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // Multiply colorMatrix with tuneMatrix
        val result = androidx.compose.ui.graphics.ColorMatrix()
        // Compose ColorMatrix doesn't have a multiply method exposed, so we just apply it sequentially if possible.
        // Actually, ColorMatrix has `setToMultiply(m1, m2)` or we can just apply both using `ColorFilter.colorMatrix(tuneMatrix)`... wait, we can only pass one ColorFilter.
        // We'll just do manual array multiplication or fallback to just using tuneMatrix.
        // For simplicity, let's just use `setToMultiply` if available or manual.
        val array1 = colorMatrix.values
        val array2 = tuneMatrix.values
        val resArray = FloatArray(20)
        for (i in 0..3) {
            for (j in 0..4) {
                var sum = 0f
                for (k in 0..3) {
                    sum += array1[i * 5 + k] * array2[k * 5 + j]
                }
                if (j == 4) sum += array1[i * 5 + 4]
                resArray[i * 5 + j] = sum
            }
        }
        androidx.compose.ui.graphics.ColorMatrix(resArray)
    }
    
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    
    val cropLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { currentImageUri = it.toString() }
        } else {
            result.error?.printStackTrace()
        }
    }

    fun shareImage() {
        if (imageUri.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, imageUri.toUri())
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Image"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Editor", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onExtractText(imageUri) }) {
                        Icon(Icons.Default.DocumentScanner, "Extract Text")
                    }
                    IconButton(onClick = ::shareImage) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Editing Tools
                if (showDrawMenu) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            IconButton(onClick = { showDrawMenu = false; isDrawingMode = false }) { Icon(Icons.Default.Close, "Close") }
                        }
                        item {
                            IconButton(onClick = { currentColor = Color.Red; isEraser = false }) { Icon(Icons.Default.Circle, "Red", tint = Color.Red) }
                        }
                        item {
                            IconButton(onClick = { currentColor = Color.Blue; isEraser = false }) { Icon(Icons.Default.Circle, "Blue", tint = Color.Blue) }
                        }
                        item {
                            IconButton(onClick = { currentColor = Color.Green; isEraser = false }) { Icon(Icons.Default.Circle, "Green", tint = Color.Green) }
                        }
                        item {
                            IconButton(onClick = { currentColor = Color.Black; isEraser = false }) { Icon(Icons.Default.Circle, "Black", tint = Color.Black) }
                        }
                        item {
                            IconButton(onClick = { isEraser = true }) { 
                                Icon(Icons.Default.CleaningServices, "Eraser", tint = if (isEraser) MaterialTheme.colorScheme.primary else LocalContentColor.current) 
                            }
                        }
                    }
                } else if (showFilterMenu) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            IconButton(onClick = { showFilterMenu = false }) { Icon(Icons.Default.Close, "Close") }
                        }
                        item {
                            TextButton(onClick = { colorMatrix = androidx.compose.ui.graphics.ColorMatrix() }) { Text("Original") }
                        }
                        item {
                            TextButton(onClick = { colorMatrix = androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) } }) { Text("Grayscale") }
                        }
                        item {
                            TextButton(onClick = { 
                                colorMatrix = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                                    0.393f, 0.769f, 0.189f, 0f, 0f,
                                    0.349f, 0.686f, 0.168f, 0f, 0f,
                                    0.272f, 0.534f, 0.131f, 0f, 0f,
                                    0f, 0f, 0f, 1f, 0f
                                ))
                            }) { Text("Sepia") }
                        }
                        item {
                            TextButton(onClick = { 
                                colorMatrix = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                                    -1f, 0f, 0f, 0f, 255f,
                                    0f, -1f, 0f, 0f, 255f,
                                    0f, 0f, -1f, 0f, 255f,
                                    0f, 0f, 0f, 1f, 0f
                                ))
                            }) { Text("Invert") }
                        }
                    }
                } else if (showTuneMenu) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showTuneMenu = false }) { Icon(Icons.Default.Close, "Close") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { brightness = 0f; contrast = 0f }) { Text("Reset") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Brightness", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = brightness,
                                onValueChange = { brightness = it },
                                valueRange = -1f..1f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Contrast", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = contrast,
                                onValueChange = { contrast = it },
                                valueRange = -1f..1f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else if (showWatermarkMenu) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showWatermarkMenu = false }) { Icon(Icons.Default.Close, "Close") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { watermarkText = "" }) { Text("Clear") }
                        }
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Watermark Text") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                } else if (showSignatureMenu) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { showSignatureMenu = false; isDrawingMode = false }) { Icon(Icons.Default.Close, "Close") }
                        Text("Draw Signature", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { isDrawingMode = false; showSignatureMenu = false }) { Text("Done") }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { rotation -= 90f }) { Icon(Icons.Default.RotateLeft, "Rotate") }
                                Text("Rotate", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { 
                                    cropLauncher.launch(CropImageContractOptions(uri = currentImageUri.toUri(), cropImageOptions = CropImageOptions(imageSourceIncludeGallery = false, imageSourceIncludeCamera = false)))
                                }) { Icon(Icons.Default.Crop, "Crop") }
                                Text("Crop", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { showFilterMenu = true }) { Icon(Icons.Default.FilterBAndW, "Filter") }
                                Text("Filters", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { showTuneMenu = true }) { Icon(Icons.Default.Tune, "Tune") }
                                Text("Tune", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { showWatermarkMenu = true }) { Icon(Icons.Default.BrandingWatermark, "Watermark") }
                                Text("Watermark", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { showSignatureMenu = true; isDrawingMode = true; currentColor = Color.Black; isEraser = false }) { Icon(Icons.Default.Create, "Sign") }
                                Text("Sign", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { showDrawMenu = true; isDrawingMode = true }) { 
                                    Icon(Icons.Default.Draw, "Draw", tint = if (isDrawingMode) MaterialTheme.colorScheme.primary else LocalContentColor.current) 
                                }
                                Text("Draw", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { 
                                    scale = 1f; offsetX = 0f; offsetY = 0f; rotation = 0f
                                    colorMatrix = androidx.compose.ui.graphics.ColorMatrix()
                                    brightness = 0f
                                    contrast = 0f
                                    watermarkText = ""
                                    paths = emptyList()
                                    isDrawingMode = false
                                    showDrawMenu = false
                                    showFilterMenu = false
                                    showTuneMenu = false
                                    showWatermarkMenu = false
                                    showSignatureMenu = false
                                    currentImageUri = imageUri
                                }) { Icon(Icons.Default.Undo, "Reset") }
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Divider()

                // Actions
                BottomAppBar {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { Toast.makeText(context, "Exporting...", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.PictureAsPdf, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Export")
                        }
                        TextButton(onClick = { Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Download")
                        }
                        TextButton(onClick = {
                            coroutineScope.launch {
                                try {
                                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                    val filename = "DocScan_${System.currentTimeMillis()}.png"
                                    val values = android.content.ContentValues().apply {
                                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/DocScan")
                                    }
                                    val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                    if (uri != null) {
                                        context.contentResolver.openOutputStream(uri)?.use { out ->
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error saving", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFE5E7EB))
                .clip(RectangleShape)
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }
                .pointerInput(isDrawingMode) {
                    if (!isDrawingMode) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                            if (scale <= 1.05f) { offsetX = 0f; offsetY = 0f }
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(offset.x, offset.y)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath?.lineTo(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                currentPath?.let { paths = paths + Stroke(it, currentColor, 8f, isEraser) }
                                currentPath = null
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = currentImageUri,
                contentDescription = "Image Editor",
                contentScale = ContentScale.Fit,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(combinedMatrix),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                        rotationZ = rotation
                    ),
                loading = { CircularProgressIndicator(modifier = Modifier.size(40.dp)) },
                error = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BrokenImage, null, modifier = Modifier.size(48.dp))
                        Text("Could not load image")
                    }
                }
            )
            
            if (paths.isNotEmpty() || currentPath != null) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize().padding(16.dp).graphicsLayer(
                        scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY, rotationZ = rotation,
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                    )
                ) {
                    paths.forEach { stroke ->
                        drawPath(
                            path = stroke.path, 
                            color = if (stroke.isEraser) Color.Transparent else stroke.color, 
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = stroke.width,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            ),
                            blendMode = if (stroke.isEraser) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver
                        )
                    }
                    currentPath?.let { path ->
                        drawPath(
                            path = path, 
                            color = if (isEraser) Color.Transparent else currentColor, 
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 8f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            ),
                            blendMode = if (isEraser) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver
                        )
                    }
                }
            }
            
            if (watermarkText.isNotEmpty()) {
                Text(
                    text = watermarkText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.graphicsLayer(
                        rotationZ = -45f,
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                )
            }
        }
    }
}
