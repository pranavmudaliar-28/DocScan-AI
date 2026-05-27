package com.example.docscanai.ui.camera

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun CameraScanScreen(
    onBack: () -> Unit,
    onGallery: () -> Unit,
    onCapture: (Uri) -> Unit,
) {
    val context = LocalContext.current
    var hasLaunched by remember { mutableStateOf(false) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scanResult != null) {
                val pdfUri = scanResult.pdf?.uri
                val pages = scanResult.pages

                if (!pages.isNullOrEmpty()) {
                    onCapture(pages.first().imageUri)
                } else if (pdfUri != null) {
                    onCapture(pdfUri)
                } else {
                    onBack()
                }
            } else {
                onBack()
            }
        } else {
            // Cancelled or failed
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(20)
                .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
                .setScannerMode(SCANNER_MODE_FULL)
                .build()

            val scanner = GmsDocumentScanning.getClient(options)
            val activity = context.findActivity()
            if (activity != null) {
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Scanner failed to start: ${e.message}", Toast.LENGTH_LONG).show()
                        onBack()
                    }
            } else {
                onBack()
            }
        }
    }

    // While the scanner activity is launching/running, show a loading placeholder
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
