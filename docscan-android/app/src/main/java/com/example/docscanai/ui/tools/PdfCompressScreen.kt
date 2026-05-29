package com.example.docscanai.ui.tools

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.docscanai.utils.PdfEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfCompressScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isCompressing by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
        }
    }

    fun compressPdf() {
        val uriToCompress = selectedUri
        if (uriToCompress == null) {
            Toast.makeText(context, "Select a PDF first", Toast.LENGTH_SHORT).show()
            return
        }
        
        isCompressing = true
        scope.launch {
            val fileName = "DocScan_Compressed_${System.currentTimeMillis()}.pdf"
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/DocScan AI")
                }
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } else {
                val dir = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "DocScan AI"
                ).also { it.mkdirs() }
                Uri.fromFile(java.io.File(dir, fileName))
            }

            if (uri != null) {
                val result = PdfEngine.compressPdf(context, uriToCompress, uri)
                if (result.isSuccess) {
                    Toast.makeText(context, "Compressed PDF saved to Downloads/DocScan AI", Toast.LENGTH_LONG).show()
                    selectedUri = null
                } else {
                    Toast.makeText(context, "Compression failed", Toast.LENGTH_SHORT).show()
                }
            }
            isCompressing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compress PDF", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (selectedUri == null) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                    Text("Select PDF to Compress")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("PDF Selected", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { selectedUri = null }) {
                            Text("Change")
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Basic Optimization", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "This compression method restructures the PDF and removes unused metadata/objects to reduce file size without losing text or vector quality. It is safe for text-heavy documents.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = ::compressPdf,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedUri != null && !isCompressing
                ) {
                    if (isCompressing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Compress PDF")
                    }
                }
            }
        }
    }
}
