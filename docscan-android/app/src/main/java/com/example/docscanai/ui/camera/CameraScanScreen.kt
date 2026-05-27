package com.example.docscanai.ui.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.IntelligentBlue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.docscanai.data.MultiPageScanManager
import com.example.docscanai.ui.permission.AppPermissionState
import com.example.docscanai.ui.permission.PermissionStatus
import com.example.docscanai.ui.permission.rememberAppPermissionState
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

// ── Camera provider helper ────────────────────────────────────────────────────

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                // future.get() is non-blocking here — listener fires only after the future completes
                @Suppress("BlockingMethodInNonBlockingContext")
                if (cont.isActive) cont.resume(future.get())
            },
            ContextCompat.getMainExecutor(this),
        )
        cont.invokeOnCancellation { future.cancel(true) }
    }

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun CameraScanScreen(
    onBack: () -> Unit,
    onGallery: () -> Unit,
    onCapture: (Uri) -> Unit,
) {
    val permState = rememberAppPermissionState(android.Manifest.permission.CAMERA)

    when (permState.status) {
        PermissionStatus.Granted           -> CameraViewfinder(onBack = onBack, onGallery = onGallery, onCapture = onCapture)
        PermissionStatus.ShowRationale     -> CameraPermissionRationale(permState = permState, onBack = onBack)
        PermissionStatus.PermanentlyDenied -> CameraPermissionDenied(onBack = onBack)
    }
}

// ── Live camera preview + capture ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraViewfinder(onBack: () -> Unit, onGallery: () -> Unit, onCapture: (Uri) -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val primary        = MaterialTheme.colorScheme.primary
    val scope          = rememberCoroutineScope()

    var lensFacing   by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isCapturing  by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var camera       by remember { mutableStateOf<Camera?>(null) }

    // Multi-page state
    val pageCount        = MultiPageScanManager.pages.size
    val showPostCapture  = remember { mutableStateOf<Uri?>(null) }
    var buildingPdf      by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType          = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lensFacing) {
        runCatching {
            val provider = context.getCameraProvider()
            val preview  = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            provider.unbindAll()
            camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        }.onFailure { captureError = "Camera unavailable" }
    }

    LaunchedEffect(flashEnabled, camera) {
        val torch = flashEnabled && lensFacing == CameraSelector.LENS_FACING_BACK
        camera?.cameraControl?.enableTorch(torch)
    }

    // ── Animations ────────────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan_y",
    )
    val cornerGlow by infiniteTransition.animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "corner",
    )

    // ── Shutter callback ──────────────────────────────────────────────────────
    fun doCapture() {
        isCapturing = true
        captureError = null
        val photoFile = File(context.cacheDir, "docscan_${System.currentTimeMillis()}.jpg")
        val options   = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", photoFile
                    )
                    // Show post-capture choice instead of navigating immediately
                    showPostCapture.value = uri
                }
                override fun onError(exc: ImageCaptureException) {
                    isCapturing = false
                    captureError = "Capture failed: ${exc.message}"
                }
            },
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Camera preview ────────────────────────────────────────────────────
        androidx.compose.ui.viewinterop.AndroidView(
            factory  = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Document frame + scan beam ────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameW = size.width * 0.80f
            val frameH = frameW * 1.35f
            val frameL = (size.width  - frameW) / 2f
            val frameT = (size.height - frameH) / 2f - 28.dp.toPx()
            val frameR = frameL + frameW
            val frameB = frameT + frameH
            val overlay = Color.Black.copy(alpha = 0.50f)

            drawRect(overlay, Offset.Zero,            Size(size.width, frameT))
            drawRect(overlay, Offset(0f, frameB),     Size(size.width, size.height - frameB))
            drawRect(overlay, Offset(0f, frameT),     Size(frameL, frameH))
            drawRect(overlay, Offset(frameR, frameT), Size(size.width - frameR, frameH))

            drawRoundRect(
                primary.copy(alpha = 0.55f),
                Offset(frameL, frameT), Size(frameW, frameH),
                CornerRadius(14.dp.toPx()), style = Stroke(1.5.dp.toPx()),
            )

            val sy       = frameT + frameH * scanY
            val edgeFade = when {
                scanY < .06f -> scanY / .06f
                scanY > .94f -> (1f - scanY) / .06f
                else         -> 1f
            }
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, primary.copy(alpha = .20f * edgeFade), Color.Transparent),
                    startY = (sy - 20.dp.toPx()).coerceAtLeast(frameT),
                    endY   = (sy + 20.dp.toPx()).coerceAtMost(frameB),
                ),
                Offset(frameL, (sy - 20.dp.toPx()).coerceAtLeast(frameT)),
                Size(frameW, 40.dp.toPx()),
            )
            drawLine(
                primary.copy(alpha = edgeFade * cornerGlow),
                Offset(frameL + 2.dp.toPx(), sy),
                Offset(frameR - 2.dp.toPx(), sy),
                2.dp.toPx(), StrokeCap.Round,
            )

            val m = 18.dp.toPx(); val ms = 3.5f.dp.toPx()
            val mc = primary.copy(alpha = cornerGlow)
            drawLine(mc, Offset(frameL, frameT + m), Offset(frameL, frameT), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameL, frameT), Offset(frameL + m, frameT), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameR - m, frameT), Offset(frameR, frameT), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameR, frameT), Offset(frameR, frameT + m), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameL, frameB - m), Offset(frameL, frameB), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameL, frameB), Offset(frameL + m, frameB), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameR - m, frameB), Offset(frameR, frameB), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameR, frameB - m), Offset(frameR, frameB), ms, StrokeCap.Round)
        }

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            CamIconBtn(onClick = {
                MultiPageScanManager.clearPages()
                onBack()
            }) {
                Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            // Auto-detect status pill
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(100.dp),
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AIGlow),
                    )
                    Text(
                        if (pageCount > 0) "${pageCount}p accumulated" else "Auto-detect",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CamIconBtn(onClick = {
                    if (lensFacing == CameraSelector.LENS_FACING_BACK) flashEnabled = !flashEnabled
                }) {
                    Icon(
                        if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        "Flash",
                        tint     = if (flashEnabled) Color.Yellow else Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                CamIconBtn(onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    flashEnabled = false
                }) {
                    Icon(Icons.Default.FlipCameraAndroid, "Flip", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        // ── AI detect pill ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-200).dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(100.dp))
                .background(Brush.linearGradient(listOf(IntelligentBlue, AIGlow)))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "Document detected — hold steady",
                    fontSize   = 12.sp,
                    color      = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
            }
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        captureError?.let { msg ->
            Surface(
                modifier  = Modifier.align(Alignment.TopCenter).padding(top = 100.dp, start = 16.dp, end = 16.dp),
                shape     = MaterialTheme.shapes.medium,
                color     = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
            LaunchedEffect(msg) { kotlinx.coroutines.delay(2500); captureError = null }
        }

        // ── Scan mode tabs ────────────────────────────────────────────────────
        val scanModes = listOf("Single", "Multi-page", "ID Card", "Book")
        var activeScanMode by remember { mutableIntStateOf(if (pageCount > 0) 1 else 0) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 140.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            scanModes.forEachIndexed { index, mode ->
                val isActive = index == activeScanMode
                Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(100.dp))
                        .clickable { activeScanMode = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            mode,
                            fontSize   = 13.sp,
                            fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.SemiBold
                                         else androidx.compose.ui.text.font.FontWeight.Normal,
                            color      = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                        )
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp).height(2.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
                                    .background(AIGlow),
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom bar ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
        ) {
            // Left: Gallery or Page Count
            if (pageCount > 0) {
                // Show "Finish PDF" button on left when pages accumulated
                Column(
                    modifier             = Modifier.align(Alignment.CenterStart).padding(start = 28.dp),
                    horizontalAlignment  = Alignment.CenterHorizontally,
                    verticalArrangement  = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (buildingPdf) Color.Black.copy(0.45f)
                                else primary.copy(0.85f)
                            )
                            .clickable(enabled = !buildingPdf) {
                                buildingPdf = true
                                scope.launch {
                                    val pdfUri = MultiPageScanManager.buildPdf(context)
                                    buildingPdf = false
                                    if (pdfUri != null) {
                                        MultiPageScanManager.clearPages()
                                        onCapture(pdfUri)
                                    } else {
                                        captureError = "PDF creation failed"
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (buildingPdf) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, "Build PDF", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                    Text(
                        "Finish\n($pageCount p.)",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                // Original gallery shortcut
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 40.dp)
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { onGallery() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }

            // Centre: Shutter button
            Box(modifier = Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(82.dp).clip(CircleShape).background(primary.copy(alpha = 0.25f)))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCapturing) Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
                            else Brush.linearGradient(listOf(primary, MaterialTheme.colorScheme.secondary))
                        )
                        .clickable(enabled = !isCapturing && !buildingPdf) { doCapture() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    } else {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
                    }
                }
            }

            // Right: Gallery when in multi-page mode, or empty
            if (pageCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 40.dp)
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { onGallery() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }

    // ── Post-capture bottom sheet ─────────────────────────────────────────────
    showPostCapture.value?.let { capturedUri ->
        PostCaptureSheet(
            capturedUri  = capturedUri,
            pageCount    = pageCount,
            onAddPage    = {
                MultiPageScanManager.addPage(capturedUri)
                showPostCapture.value = null
            },
            onScanSingle = {
                showPostCapture.value = null
                onCapture(capturedUri)
            },
            onDismiss    = { showPostCapture.value = null },
        )
    }
}

// ── Post-capture choice sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCaptureSheet(
    capturedUri: Uri,
    pageCount: Int,
    onAddPage: () -> Unit,
    onScanSingle: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Page captured",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Thumbnail
            AsyncImage(
                model              = capturedUri,
                contentDescription = "Captured page",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.medium),
            )

            if (pageCount > 0) {
                Text(
                    "Session already has $pageCount page${if (pageCount != 1) "s" else ""}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Add to multi-page session
            Button(
                onClick  = onAddPage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add as Page ${pageCount + 1}")
            }

            // Single-page direct scan
            OutlinedButton(
                onClick  = onScanSingle,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Done, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan This Page Only")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Reusable icon button ──────────────────────────────────────────────────────

@Composable
private fun CamIconBtn(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content          = content,
    )
}

// ── Permission screens ────────────────────────────────────────────────────────

@Composable
private fun CameraPermissionRationale(permState: AppPermissionState, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF0D1E38), Color(0xFF050C1C)), radius = 1400f))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.Default.Close, "Close", tint = Color.White.copy(alpha = 0.7f))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 40.dp),
        ) {
            Box(
                modifier         = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("Camera Access Required", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "DocScan AI needs your camera to capture and scan documents into searchable, structured data.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.65f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))
            Button(onClick = { permState.launchRequest() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.large) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Allow Camera Access", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) { Text("Go Back", color = Color.White.copy(0.55f)) }
        }
    }
}

@Composable
private fun CameraPermissionDenied(onBack: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF1C0A0A), Color(0xFF050C1C)), radius = 1400f))
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.Default.Close, "Close", tint = Color.White.copy(alpha = 0.7f))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 40.dp),
        ) {
            Box(
                modifier         = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer.copy(0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("Camera Access Blocked", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "Camera permission was denied. Open Settings and allow Camera access to use the scanner.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.65f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.large,
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Settings", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) { Text("Go Back", color = Color.White.copy(0.55f)) }
        }
    }
}
