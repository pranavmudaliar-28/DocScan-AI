package com.example.docscanai.ui.camera

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CameraScanScreen(
    onBack: () -> Unit,
    onCapture: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan_y"
    )
    val cornerGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "corner"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF0D1E38), Color(0xFF050C1C)),
                    radius = 1400f
                )
            )
    ) {
        // Viewfinder overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameW = size.width * 0.80f
            val frameH = frameW * 1.35f
            val frameL = (size.width  - frameW) / 2f
            val frameT = (size.height - frameH) / 2f - 28.dp.toPx()
            val frameR = frameL + frameW
            val frameB = frameT + frameH
            val overlay = Color.Black.copy(alpha = 0.62f)

            // 4-panel dark overlay around frame
            drawRect(overlay, Offset.Zero, Size(size.width, frameT))
            drawRect(overlay, Offset(0f, frameB), Size(size.width, size.height - frameB))
            drawRect(overlay, Offset(0f, frameT), Size(frameL, frameH))
            drawRect(overlay, Offset(frameR, frameT), Size(size.width - frameR, frameH))

            // Frame border
            drawRoundRect(
                primary.copy(alpha = 0.45f),
                Offset(frameL, frameT), Size(frameW, frameH),
                CornerRadius(14.dp.toPx()), style = Stroke(1.5.dp.toPx())
            )

            // Scan beam
            val sy      = frameT + frameH * scanY
            val edgeFade = when {
                scanY < .06f -> scanY / .06f
                scanY > .94f -> (1f - scanY) / .06f
                else         -> 1f
            }
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, primary.copy(alpha = .18f * edgeFade), Color.Transparent),
                    startY = (sy - 20.dp.toPx()).coerceAtLeast(frameT),
                    endY   = (sy + 20.dp.toPx()).coerceAtMost(frameB)
                ),
                Offset(frameL, (sy - 20.dp.toPx()).coerceAtLeast(frameT)),
                Size(frameW, 40.dp.toPx())
            )
            drawLine(
                primary.copy(alpha = edgeFade * cornerGlow),
                Offset(frameL + 2.dp.toPx(), sy),
                Offset(frameR - 2.dp.toPx(), sy),
                2.dp.toPx(), StrokeCap.Round
            )

            // Corner markers
            val m  = 18.dp.toPx(); val ms = 3.5f.dp.toPx()
            val mc = primary.copy(alpha = cornerGlow)
            drawLine(mc, Offset(frameL, frameT + m), Offset(frameL, frameT), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameL, frameT), Offset(frameL + m, frameT), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameR - m, frameT), Offset(frameR, frameT), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameR, frameT), Offset(frameR, frameT + m), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameL, frameB - m), Offset(frameL, frameB), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameL, frameB), Offset(frameL + m, frameB), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameR - m, frameB), Offset(frameR, frameB), ms, StrokeCap.Round)
            drawLine(mc, Offset(frameR, frameB - m), Offset(frameR, frameB), ms, StrokeCap.Round)
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CamIconBtn(onClick = onBack) {
                Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
            }
            Text("Scan Document", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CamIconBtn(onClick = {}) {
                    Icon(Icons.Default.FlashOn, "Flash", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                }
                CamIconBtn(onClick = {}) {
                    Icon(Icons.Default.FlipCameraAndroid, "Flip", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Hint label (below centre of screen)
        Surface(
            modifier = Modifier.align(Alignment.Center).offset(y = 220.dp),
            shape = MaterialTheme.shapes.extraSmall,
            color = Color.Black.copy(alpha = 0.52f)
        ) {
            Text(
                "Align document within the frame",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }

        // Bottom bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
        ) {
            // Gallery shortcut
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 40.dp)
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {},
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhotoLibrary, "Gallery", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
            }

            // Capture button
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
                // Inner shutter
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        )
                        .clickable(onClick = onCapture),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                }
            }
        }
    }
}

@Composable
private fun CamIconBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
