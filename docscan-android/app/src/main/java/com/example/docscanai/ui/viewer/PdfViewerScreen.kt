package com.example.docscanai.ui.viewer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    fileUri: String,
    onBack: () -> Unit,
    onExtractText: (String) -> Unit,
    onSignPdf: (String) -> Unit
) {
    val context = LocalContext.current
    val uri = fileUri.toUri()
    
    var pdfBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fileUri) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    val bitmaps = mutableListOf<Bitmap>()
                    val pageCount = renderer.pageCount
                    
                    for (i in 0 until pageCount) {
                        val page = renderer.openPage(i)
                        // Scale up for better quality
                        val width = context.resources.displayMetrics.widthPixels
                        val height = (width.toFloat() / page.width * page.height).toInt()
                        
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // Fill white background
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)
                        page.close()
                    }
                    renderer.close()
                    pfd.close()
                    
                    withContext(Dispatchers.Main) {
                        pdfBitmaps = bitmaps
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorMsg = "Could not open PDF file."
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMsg = "Error rendering PDF: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    fun sharePdf() {
        if (fileUri.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Viewer", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onExtractText(fileUri) }) {
                        Icon(Icons.Default.DocumentScanner, "Extract Text")
                    }
                    IconButton(onClick = { onSignPdf(fileUri) }) {
                        Icon(Icons.Default.Create, "Sign PDF")
                    }
                    IconButton(onClick = ::sharePdf) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        },
        bottomBar = {
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
                    TextButton(onClick = { Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
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
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pdfBitmaps.size) { index ->
                        Box {
                            Surface(
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    bitmap = pdfBitmaps[index].asImageBitmap(),
                                    contentDescription = "PDF Page ${index + 1}",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                            
                            // Delete Page Button
                            IconButton(
                                onClick = { 
                                    pdfBitmaps = pdfBitmaps.toMutableList().apply { removeAt(index) }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
                                    .size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, "Delete Page", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            
                            // Page number badge
                            Text(
                                "${index + 1}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
