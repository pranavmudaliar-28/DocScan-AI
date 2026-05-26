package com.example.docscanai.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    // Capture theme colors before entering non-composable scopes
    val bg          = MaterialTheme.colorScheme.background
    val surface     = MaterialTheme.colorScheme.surface
    val primary     = MaterialTheme.colorScheme.primary
    val secondary   = MaterialTheme.colorScheme.secondary
    val outline     = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "scan")

    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_pos"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(350)
        textAlpha.animateTo(1f, animationSpec = tween(700))
        delay(1500)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow orb behind the icon
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.08f * glowPulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.65f
                ),
                radius = size.minDimension * 0.65f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Document scan animation
            Canvas(modifier = Modifier.size(160.dp)) {
                val dw = size.width  * 0.50f
                val dh = size.height * 0.68f
                val dl = (size.width  - dw) / 2f
                val dt = (size.height - dh) / 2f
                val cr = CornerRadius(12.dp.toPx())

                // Outer glow ring
                drawRoundRect(
                    color      = secondary.copy(alpha = 0.12f * glowPulse),
                    topLeft    = Offset(dl - 9.dp.toPx(), dt - 9.dp.toPx()),
                    size       = Size(dw + 18.dp.toPx(), dh + 18.dp.toPx()),
                    cornerRadius = CornerRadius(20.dp.toPx())
                )

                // Document card
                drawRoundRect(
                    color    = surface,
                    topLeft  = Offset(dl, dt),
                    size     = Size(dw, dh),
                    cornerRadius = cr
                )

                // Document border
                drawRoundRect(
                    color    = outline.copy(alpha = 0.8f + 0.2f * glowPulse),
                    topLeft  = Offset(dl, dt),
                    size     = Size(dw, dh),
                    cornerRadius = cr,
                    style    = Stroke(2.dp.toPx())
                )

                // Text placeholder lines
                val lx1    = dl + 10.dp.toPx()
                val lx2    = dl + dw - 10.dp.toPx()
                val lineC  = primary.copy(alpha = 0.28f)
                val lw     = 2.5f.dp.toPx()
                drawLine(lineC, Offset(lx1, dt + dh * 0.30f), Offset(lx2,                        dt + dh * 0.30f), lw, StrokeCap.Round)
                drawLine(lineC, Offset(lx1, dt + dh * 0.44f), Offset(lx2 - 12.dp.toPx(), dt + dh * 0.44f), lw, StrokeCap.Round)
                drawLine(lineC, Offset(lx1, dt + dh * 0.58f), Offset(lx2 - 22.dp.toPx(), dt + dh * 0.58f), lw, StrokeCap.Round)

                // Animated scan line
                val scanY = dt + (dh * scanProgress)
                val edgeFade = when {
                    scanProgress < 0.08f -> scanProgress / 0.08f
                    scanProgress > 0.92f -> (1f - scanProgress) / 0.08f
                    else -> 1f
                }

                // Glow beam under scan line
                drawRect(
                    brush = Brush.verticalGradient(
                        colors  = listOf(Color.Transparent, primary.copy(alpha = 0.15f * glowPulse * edgeFade), Color.Transparent),
                        startY  = (scanY - 18.dp.toPx()).coerceAtLeast(dt),
                        endY    = (scanY + 18.dp.toPx()).coerceAtMost(dt + dh)
                    ),
                    topLeft = Offset(dl + 2.dp.toPx(), (scanY - 18.dp.toPx()).coerceAtLeast(dt)),
                    size    = Size(dw - 4.dp.toPx(), 36.dp.toPx())
                )

                drawLine(
                    color = primary.copy(alpha = edgeFade * glowPulse),
                    start = Offset(dl + 2.dp.toPx(), scanY),
                    end   = Offset(dl + dw - 2.dp.toPx(), scanY),
                    strokeWidth = 2.dp.toPx(),
                    cap   = StrokeCap.Round
                )

                // Corner scan markers
                val m  = 8.dp.toPx()
                val ms = 3.dp.toPx()
                val mx = dl - 7.dp.toPx()
                val my = dt - 7.dp.toPx()
                val mw = dw + 14.dp.toPx()
                val mh = dh + 14.dp.toPx()
                val mc = primary.copy(alpha = 0.5f + 0.5f * glowPulse)
                // TL
                drawLine(mc, Offset(mx,      my + m), Offset(mx,      my), ms, StrokeCap.Round)
                drawLine(mc, Offset(mx,      my),     Offset(mx + m,  my), ms, StrokeCap.Round)
                // TR
                drawLine(mc, Offset(mx + mw - m, my), Offset(mx + mw, my),      ms, StrokeCap.Round)
                drawLine(mc, Offset(mx + mw,     my), Offset(mx + mw, my + m),  ms, StrokeCap.Round)
                // BL
                drawLine(mc, Offset(mx,      my + mh - m), Offset(mx,     my + mh), ms, StrokeCap.Round)
                drawLine(mc, Offset(mx,      my + mh),     Offset(mx + m, my + mh), ms, StrokeCap.Round)
                // BR
                drawLine(mc, Offset(mx + mw - m, my + mh), Offset(mx + mw, my + mh),     ms, StrokeCap.Round)
                drawLine(mc, Offset(mx + mw,     my + mh - m), Offset(mx + mw, my + mh), ms, StrokeCap.Round)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // App name — fade in
            Row(
                modifier = Modifier.alpha(textAlpha.value),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text  = "DocScan",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text  = "AI",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Light),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alignByBaseline()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text      = "SCAN · ANALYZE · EXTRACT",
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha.value),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
