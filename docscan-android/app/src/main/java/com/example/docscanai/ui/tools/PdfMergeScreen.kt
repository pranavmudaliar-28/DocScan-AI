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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
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
fun PdfMergeScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isMerging by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = selectedUris + uris
        }
    }

    fun mergePdfs() {
        if (selectedUris.size < 2) {
            Toast.makeText(context, "Select at least 2 PDFs", Toast.LENGTH_SHORT).show()
            return
        }
        
        isMerging = true
        scope.launch {
            val fileName = "DocScan_Merged_${System.currentTimeMillis()}.pdf"
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
                val result = PdfEngine.mergePdfs(context, selectedUris, uri)
                if (result.isSuccess) {
                    Toast.makeText(context, "Merged PDF saved to Downloads/DocScan AI", Toast.LENGTH_LONG).show()
                    selectedUris = emptyList()
                } else {
                    Toast.makeText(context, "Merge failed", Toast.LENGTH_SHORT).show()
                }
            }
            isMerging = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge PDFs", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                Icon(Icons.Default.Add, "Add PDF")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedUris.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Tap + to add PDFs for merging")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(selectedUris) { index, uri ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "PDF ${index + 1}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val newList = selectedUris.toMutableList()
                                            val temp = newList[index - 1]
                                            newList[index - 1] = newList[index]
                                            newList[index] = temp
                                            selectedUris = newList
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, "Move Up")
                                }
                                IconButton(
                                    onClick = {
                                        if (index < selectedUris.size - 1) {
                                            val newList = selectedUris.toMutableList()
                                            val temp = newList[index + 1]
                                            newList[index + 1] = newList[index]
                                            newList[index] = temp
                                            selectedUris = newList
                                        }
                                    },
                                    enabled = index < selectedUris.size - 1
                                ) {
                                    Icon(Icons.Default.ArrowDownward, "Move Down")
                                }
                                IconButton(
                                    onClick = {
                                        val newList = selectedUris.toMutableList()
                                        newList.removeAt(index)
                                        selectedUris = newList
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, "Remove")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = ::mergePdfs,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedUris.size >= 2 && !isMerging
            ) {
                if (isMerging) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Merge ${selectedUris.size} PDFs")
                }
            }
        }
    }
}
