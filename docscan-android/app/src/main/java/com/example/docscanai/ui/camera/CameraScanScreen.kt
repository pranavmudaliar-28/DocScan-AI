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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.docscanai.ui.permission.PermissionDeniedDialog
import com.example.docscanai.ui.permission.PermissionRationaleDialog
import com.example.docscanai.ui.permission.PermissionStatus
import com.example.docscanai.ui.permission.cameraPermission
import com.example.docscanai.ui.permission.rememberAppPermissionState
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

    val permState = rememberAppPermissionState(cameraPermission())
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(permState.status) {
        if (!hasLaunched && permState.status == PermissionStatus.Granted) {
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

    if (showRationaleDialog) {
        PermissionRationaleDialog(
            icon = Icons.Default.CameraAlt,
            title = "Camera Access",
            rationale = "DocScan AI needs access to your camera to scan documents.",
            onAllow = { showRationaleDialog = false; permState.launchRequest() },
            onDismiss = { showRationaleDialog = false; onBack() }
        )
    }

    if (showDeniedDialog) {
        PermissionDeniedDialog(
            icon = Icons.Default.CameraAlt,
            title = "Camera Access Blocked",
            message = "Camera access was denied. Open Settings and allow camera access to scan documents.",
            onDismiss = { showDeniedDialog = false; onBack() }
        )
    }

    if (permState.status != PermissionStatus.Granted) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Camera Permission Required", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Text("Please grant camera permission to scan documents.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    when (permState.status) {
                        PermissionStatus.ShowRationale -> showRationaleDialog = true
                        PermissionStatus.PermanentlyDenied -> showDeniedDialog = true
                        else -> permState.launchRequest()
                    }
                }) {
                    Text("Grant Permission")
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
