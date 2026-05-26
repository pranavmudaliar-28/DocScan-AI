package com.example.docscanai.ui.viewer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DocumentEditScreen(
    docId: String,
    imageUri: String,
    onBack: () -> Unit,
    onExtractText: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rotationStep by remember { mutableIntStateOf(0) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saving by remember { mutableStateOf(false) }

    val rotationDegrees = (rotationStep * 90).toFloat()
    val isEdited = rotationStep != 0 || brightness != 0f || contrast != 0f

    val colorFilter = remember(brightness, contrast) {
        val scale = 1f + contrast
        val totalOffset = (-0.5f * scale + 0.5f) * 255f + brightness * 255f
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    scale, 0f,    0f,    0f, totalOffset,
                    0f,    scale, 0f,    0f, totalOffset,
                    0f,    0f,    scale, 0f, totalOffset,
                    0f,    0f,    0f,    1f, 0f
                )
            )
        )
    }

    val painter = rememberAsyncImagePainter(imageUri)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Discard", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Edit Document",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(
                onClick = {
                    if (isEdited) {
                        saving = true
                        scope.launch {
                            val success = saveEditedImage(
                                context, imageUri, rotationDegrees, brightness, contrast
                            )
                            saving = false
                            Toast.makeText(
                                context,
                                if (success) "Saved to Pictures/DocScan AI" else "Save failed",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (success) onBack()
                        }
                    } else {
                        onBack()
                    }
                },
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        if (isEdited) "Save" else "Done",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Image preview — square container handles 90°/270° rotation without clipping
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            val previewSize = maxWidth * 0.85f
            Image(
                painter = painter,
                contentDescription = "Document preview",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier
                    .size(previewSize)
                    .rotate(rotationDegrees)
            )
        }

        // Edit controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Rotate
            EditSection("Rotate") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(onClick = { rotationStep = (rotationStep - 1).mod(4) }) {
                        Icon(Icons.Default.RotateLeft, "Rotate left")
                    }
                    Text(
                        "${rotationDegrees.toInt()}°",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedIconButton(onClick = { rotationStep = (rotationStep + 1) % 4 }) {
                        Icon(Icons.Default.RotateRight, "Rotate right")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Brightness
            EditSection("Brightness") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.WbSunny, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = -0.5f..0.5f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Icon(
                        Icons.Default.WbSunny, null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Contrast
            EditSection("Contrast") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Contrast, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = contrast,
                        onValueChange = { contrast = it },
                        valueRange = -0.5f..0.5f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Icon(
                        Icons.Default.Contrast, null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reset — only shown when something has changed
            if (isEdited) {
                TextButton(
                    onClick = { rotationStep = 0; brightness = 0f; contrast = 0f },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Reset All",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Extract & Edit Text
            OutlinedButton(
                onClick  = onExtractText,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.TextFields, null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Extract & Edit Text")
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun EditSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

private suspend fun saveEditedImage(
    context: Context,
    imageUri: String,
    rotation: Float,
    brightness: Float,
    contrast: Float,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val original = BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(Uri.parse(imageUri))
        ) ?: return@withContext false

        // Apply rotation
        val rotMatrix = android.graphics.Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, rotMatrix, true)

        // Apply brightness + contrast via Android ColorMatrix
        val scale = 1f + contrast
        val totalOffset = (-0.5f * scale + 0.5f) * 255f + brightness * 255f
        val cm = android.graphics.ColorMatrix(floatArrayOf(
            scale, 0f,    0f,    0f, totalOffset,
            0f,    scale, 0f,    0f, totalOffset,
            0f,    0f,    scale, 0f, totalOffset,
            0f,    0f,    0f,    1f, 0f
        ))
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        }
        val result = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(result).drawBitmap(rotated, 0f, 0f, paint)

        // Write to MediaStore (Pictures/DocScan AI)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "DocScan_edit_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DocScan AI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        resolver.openOutputStream(destUri)?.use { result.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(destUri, values, null, null)
        }
        true
    } catch (e: Exception) {
        false
    }
}
