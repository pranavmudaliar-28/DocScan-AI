package com.example.docscanai.ui.tools

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.docscanai.data.local.DatabaseModule
import com.example.docscanai.data.local.SignatureEntity
import com.example.docscanai.utils.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PlacedSignature(
    val signature: SignatureEntity,
    var offset: Offset,
    var scale: Float = 1f,
    var rotation: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureOverlayScreen(
    fileUri: String,
    onBack: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { DatabaseModule.signatureRepository }
    val signatures by repository.allSignatures.collectAsState(initial = emptyList())
    
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var isProcessing by remember { mutableStateOf(false) }
    
    // PdfRenderer components
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var currentPage by remember { mutableStateOf<PdfRenderer.Page?>(null) }
    
    // Canvas dimensions for relative mapping
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Placed signatures on the current page
    var placedSignatures by remember { mutableStateOf(listOf<PlacedSignature>()) }
    var showSignatureSelector by remember { mutableStateOf(false) }

    // Init PdfRenderer
    LaunchedEffect(fileUri) {
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(fileUri)
                val fd = context.contentResolver.openFileDescriptor(uri, "r")
                if (fd != null) {
                    fileDescriptor = fd
                    val renderer = PdfRenderer(fd)
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                    
                    if (pageCount > 0) {
                        val page = renderer.openPage(0)
                        currentPage = page
                        
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2, // higher res preview
                            page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pageBitmap = bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            currentPage?.close()
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }
    
    fun renderPage(index: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            currentPage?.close()
            val renderer = pdfRenderer ?: return@launch
            val page = renderer.openPage(index)
            currentPage = page
            
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            // Need white background
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            withContext(Dispatchers.Main) {
                currentPageIndex = index
                pageBitmap = bitmap
                placedSignatures = emptyList() // clear signatures on page turn
            }
        }
    }

    fun saveSignedPdf() {
        if (placedSignatures.isEmpty()) {
            Toast.makeText(context, "Please place a signature first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val activeSig = placedSignatures.first() // for now, save the first one
        val pageW = currentPage?.width?.toFloat() ?: 1f
        val pageH = currentPage?.height?.toFloat() ?: 1f
        val cvsW = canvasSize.width.toFloat()
        val cvsH = canvasSize.height.toFloat()
        
        if (cvsW == 0f || cvsH == 0f) return

        isProcessing = true
        coroutineScope.launch {
            // Map offset from Canvas UI to PDF dimensions
            // signature width in UI is fixed at 150.dp -> we estimate 150px (actually depends on density, but let's approximate)
            val density = context.resources.displayMetrics.density
            val sigUiWidth = 150f * density * activeSig.scale
            val sigUiHeight = 80f * density * activeSig.scale // rough estimate
            
            val xRatio = activeSig.offset.x / cvsW
            val yRatio = activeSig.offset.y / cvsH
            
            val pdfX = xRatio * pageW
            val pdfY = yRatio * pageH
            
            val pdfSigW = (sigUiWidth / cvsW) * pageW
            val pdfSigH = (sigUiHeight / cvsH) * pageH

            val fileName = "Signed_${System.currentTimeMillis()}.pdf"
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/DocScan AI")
            }

            val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val result = PdfEngine.insertSignature(
                    context = context,
                    pdfUri = Uri.parse(fileUri),
                    signatureUri = Uri.fromFile(File(activeSig.signature.imagePath)),
                    pageIndex = currentPageIndex,
                    x = pdfX,
                    y = pdfY,
                    width = pdfSigW,
                    height = pdfSigH,
                    outputUri = uri
                )

                if (result.isSuccess) {
                    Toast.makeText(context, "Signed PDF saved to Downloads!", Toast.LENGTH_LONG).show()
                    onBack()
                } else {
                    Toast.makeText(context, "Error saving: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
            isProcessing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = { saveSignedPdf() }) {
                        Text("Apply & Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentPageIndex > 0) renderPage(currentPageIndex - 1) },
                        enabled = currentPageIndex > 0
                    ) {
                        Text("Previous")
                    }
                    Text("Page ${currentPageIndex + 1} of $pageCount")
                    TextButton(
                        onClick = { if (currentPageIndex < pageCount - 1) renderPage(currentPageIndex + 1) },
                        enabled = currentPageIndex < pageCount - 1
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.LightGray)
        ) {
            if (pageBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .onGloballyPositioned { canvasSize = it.size },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Render Signatures
                    placedSignatures.forEachIndexed { index, placedSig ->
                        var isVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(placedSig.signature.id) {
                            isVisible = true
                        }
                        
                        val animatedScale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (isVisible) placedSig.scale else 0f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            ),
                            label = "sig_fly_in_scale"
                        )

                        AsyncImage(
                            model = File(placedSig.signature.imagePath),
                            contentDescription = "Signature",
                            modifier = Modifier
                                .offset(
                                    x = with(androidx.compose.ui.platform.LocalDensity.current) { placedSig.offset.x.toDp() },
                                    y = with(androidx.compose.ui.platform.LocalDensity.current) { placedSig.offset.y.toDp() }
                                )
                                .size(150.dp) // Base UI size
                                .graphicsLayer(
                                    scaleX = animatedScale,
                                    scaleY = animatedScale,
                                    rotationZ = placedSig.rotation
                                )
                                .pointerInput(Unit) {
                                    androidx.compose.foundation.gestures.detectTransformGestures { _, pan, zoom, rotation ->
                                        val currentList = placedSignatures.toMutableList()
                                        val cur = currentList[index]
                                        currentList[index] = cur.copy(
                                            offset = cur.offset + pan,
                                            scale = (cur.scale * zoom).coerceIn(0.5f, 3f),
                                            rotation = cur.rotation + rotation
                                        )
                                        placedSignatures = currentList
                                    }
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            
            // Signature Selector FAB or Overlay
            if (!showSignatureSelector) {
                FloatingActionButton(
                    onClick = { showSignatureSelector = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, "Add Signature")
                }
            } else {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Signature", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { showSignatureSelector = false }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                OutlinedButton(onClick = { 
                                    showSignatureSelector = false
                                    onNavigateToLibrary() 
                                }) {
                                    Text("Manage")
                                }
                            }
                            items(signatures.size) { index ->
                                val sig = signatures[index]
                                Box(
                                    modifier = Modifier
                                        .size(100.dp, 60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF3F4F6))
                                        .clickable {
                                            placedSignatures = placedSignatures + PlacedSignature(
                                                signature = sig,
                                                offset = Offset((canvasSize.width / 2f) - 200f, (canvasSize.height / 2f) - 100f)
                                            )
                                            showSignatureSelector = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = File(sig.imagePath),
                                        contentDescription = sig.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}
