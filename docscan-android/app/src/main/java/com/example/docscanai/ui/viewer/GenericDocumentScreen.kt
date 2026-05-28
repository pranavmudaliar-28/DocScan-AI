package com.example.docscanai.ui.viewer

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericDocumentScreen(
    fileUri: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uri = fileUri.toUri()
    
    var extractedText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(fileUri) {
        withContext(Dispatchers.IO) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val fileName = uri.path ?: ""
                
                val isText = mimeType.startsWith("text/") || fileName.endsWith(".txt") || fileName.endsWith(".csv")
                val isDocx = mimeType.contains("wordprocessingml") || fileName.endsWith(".docx")
                val isXlsx = mimeType.contains("spreadsheetml") || fileName.endsWith(".xlsx")
                
                if (isText) {
                    val stream = context.contentResolver.openInputStream(uri)
                    extractedText = stream?.bufferedReader()?.use { it.readText() }
                } else if (isDocx) {
                    val stream = context.contentResolver.openInputStream(uri)
                    val zis = ZipInputStream(stream)
                    var text = ""
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val xml = BufferedReader(InputStreamReader(zis)).readText()
                            // Extract text from <w:t> tags
                            val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
                            val matches = regex.findAll(xml)
                            text = matches.joinToString(" ") { it.groupValues[1] }
                            break
                        }
                        entry = zis.nextEntry
                    }
                    zis.close()
                    extractedText = text.ifEmpty { "No text found in document." }
                } else if (isXlsx) {
                    val stream = context.contentResolver.openInputStream(uri)
                    val zis = ZipInputStream(stream)
                    var text = ""
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "xl/sharedStrings.xml") {
                            val xml = BufferedReader(InputStreamReader(zis)).readText()
                            val regex = Regex("<t[^>]*>(.*?)</t>")
                            val matches = regex.findAll(xml)
                            text = matches.joinToString("\\n") { it.groupValues[1] }
                            break
                        }
                        entry = zis.nextEntry
                    }
                    zis.close()
                    extractedText = text.ifEmpty { "No text found in spreadsheet." }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isLoading = false
        }
    }

    fun shareFile() {
        if (fileUri.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = context.contentResolver.getType(uri) ?: "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Document"))
    }

    fun openInExternalApp() {
        if (fileUri.isEmpty()) return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Viewer", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::shareFile) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (extractedText != null) {
                // Show Extracted Text
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Preview (Plain Text Extracted)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = extractedText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = ::openInExternalApp, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in External App for Full Formatting")
                    }
                }
            } else {
                // Unsupported format entirely (e.g. PPT or unknown)
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Unsupported File Format",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This file cannot be previewed natively in DocScan AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = ::openInExternalApp) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in External App")
                    }
                }
            }
        }
    }
}
