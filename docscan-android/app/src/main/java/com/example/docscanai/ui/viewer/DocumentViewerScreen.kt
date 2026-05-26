package com.example.docscanai.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    docId: String,
    onBack: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    val clampedScale = scale.coerceIn(0.5f, 3f)

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Pinch-to-zoom document
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 100.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(0.74f)
                    .scale(clampedScale)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(surface, cornerRadius = CornerRadius(14.dp.toPx()))
                    drawRoundRect(outline.copy(alpha = .35f), cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(1.5.dp.toPx()))

                    val pad = 22.dp.toPx()
                    val lc  = primary.copy(alpha = .20f)
                    val lw  = 2.dp.toPx()

                    // Header bar
                    drawRoundRect(primary.copy(alpha = .12f), Offset(pad, pad), Size(size.width - pad * 2, 24.dp.toPx()), CornerRadius(5.dp.toPx()))
                    drawRoundRect(primary.copy(alpha = .08f), Offset(pad, pad + 34.dp.toPx()), Size(size.width * .4f, 14.dp.toPx()), CornerRadius(3.dp.toPx()))

                    // Divider
                    drawLine(outline.copy(alpha = .3f), Offset(pad, size.height * .15f), Offset(size.width - pad, size.height * .15f), 1.dp.toPx())
                    drawRoundRect(primary.copy(alpha = .08f), Offset(pad, size.height * .17f), Size(size.width * .25f, 10.dp.toPx()), CornerRadius(3.dp.toPx()))

                    // Content rows
                    val rows = listOf(.22f, .28f, .34f, .40f, .46f, .52f, .58f, .64f, .70f)
                    rows.forEachIndexed { i, y ->
                        val w = when (i % 4) { 0 -> 1.0f; 1 -> 0.88f; 2 -> 0.95f; else -> 0.72f }
                        drawLine(lc, Offset(pad, size.height * y), Offset(pad + (size.width - pad * 2) * w, size.height * y), lw, StrokeCap.Round)
                    }

                    // Divider + total row
                    drawLine(outline.copy(alpha = .25f), Offset(pad, size.height * .76f), Offset(size.width - pad, size.height * .76f), 1.dp.toPx())
                    drawRoundRect(primary.copy(alpha = .14f), Offset(pad, size.height * .80f), Size(size.width - pad * 2, 18.dp.toPx()), CornerRadius(5.dp.toPx()))
                    drawRoundRect(primary.copy(alpha = .22f), Offset(size.width * .60f, size.height * .80f), Size(size.width * .32f, 18.dp.toPx()), CornerRadius(5.dp.toPx()))

                    // Footer
                    drawLine(lc, Offset(pad, size.height * .91f), Offset(size.width - pad, size.height * .91f), lw, StrokeCap.Round)
                }
            }
        }

        // Top gradient bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background.copy(alpha = 0f)),
                        endY = 200f
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(docId, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text("1 of 1 page", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onBackground) }
                    IconButton(onClick = {}) { Icon(Icons.Default.Edit,  "Edit",  tint = MaterialTheme.colorScheme.onBackground) }
                }
            }
        }

        // Bottom controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background.copy(alpha = 0f), MaterialTheme.colorScheme.background),
                        startY = 0f, endY = 160f
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zoom controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerIconBtn(onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f) }) {
                        Icon(Icons.Default.ZoomOut, "Out", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "${(clampedScale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(46.dp)
                    )
                    ViewerIconBtn(onClick = { scale = (scale + 0.25f).coerceAtMost(3f) }) {
                        Icon(Icons.Default.ZoomIn, "In", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }

                if (abs(clampedScale - 1f) > 0.05f) {
                    TextButton(onClick = { scale = 1f }) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ViewerIconBtn(onClick = {}) {
                        Icon(Icons.Default.FileDownload, "Download", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    ViewerIconBtn(onClick = {}) {
                        Icon(Icons.Default.Print, "Print", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerIconBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
