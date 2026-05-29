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
fun PdfSplitScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pageRanges by remember { mutableStateOf("") }
    var isSplitting by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
        }
    }

    fun parsePageRanges(input: String): List<Int> {
        val pages = mutableSetOf<Int>()
        val parts = input.split(",")
        for (part in parts) {
            val range = part.trim()
            if (range.contains("-")) {
                val bounds = range.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].trim().toIntOrNull()
                    val end = bounds[1].trim().toIntOrNull()
                    if (start != null && end != null && start <= end) {
                        for (i in start..end) pages.add(i)
                    }
                }
            } else {
                val p = range.toIntOrNull()
                if (p != null) pages.add(p)
            }
        }
        return pages.sorted()
    }

    fun splitPdf() {
        val uriToSplit = selectedUri
        if (uriToSplit == null) {
            Toast.makeText(context, "Select a PDF first", Toast.LENGTH_SHORT).show()
            return
        }
        val pages = parsePageRanges(pageRanges)
        if (pages.isEmpty()) {
            Toast.makeText(context, "Enter a valid page range (e.g., 1, 3, 5-7)", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSplitting = true
        scope.launch {
            val fileName = "DocScan_Split_${System.currentTimeMillis()}.pdf"
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
                val result = PdfEngine.splitPdf(context, uriToSplit, pages, uri)
                if (result.isSuccess) {
                    Toast.makeText(context, "Split PDF saved to Downloads/DocScan AI", Toast.LENGTH_LONG).show()
                    selectedUri = null
                    pageRanges = ""
                } else {
                    Toast.makeText(context, "Split failed", Toast.LENGTH_SHORT).show()
                }
            }
            isSplitting = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split / Extract PDF", style = MaterialTheme.typography.titleMedium) },
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
                    Text("Select PDF to Split")
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

                OutlinedTextField(
                    value = pageRanges,
                    onValueChange = { pageRanges = it },
                    label = { Text("Pages to Extract") },
                    placeholder = { Text("e.g., 1, 3, 5-7") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Enter comma-separated page numbers or ranges. The specified pages will be extracted into a new PDF document.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = ::splitPdf,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedUri != null && pageRanges.isNotBlank() && !isSplitting
                ) {
                    if (isSplitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Split PDF")
                    }
                }
            }
        }
    }
}
