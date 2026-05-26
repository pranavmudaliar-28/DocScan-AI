package com.example.docscanai.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

private val DeepNavy = Color(0xFF0F172A)
private val MidNavy = Color(0xFF1E3A5F)
private val IntelligentBlue = Color(0xFF3B82F6)

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")

    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_pos"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(400)
        textAlpha.animateTo(1f, animationSpec = tween(600))
        delay(1500)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepNavy, Color(0xFF112240), DeepNavy)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated document + scan icon
            Canvas(modifier = Modifier.size(160.dp)) {
                val dw = size.width * 0.50f
                val dh = size.height * 0.68f
                val dl = (size.width - dw) / 2
                val dt = (size.height - dh) / 2
                val cr = CornerRadius(12.dp.toPx())

                // Outer glow ring
                drawRoundRect(
                    color = IntelligentBlue.copy(alpha = 0.12f * glowPulse),
                    topLeft = Offset(dl - 8.dp.toPx(), dt - 8.dp.toPx()),
                    size = Size(dw + 16.dp.toPx(), dh + 16.dp.toPx()),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )

                // Document background
                drawRoundRect(
                    color = MidNavy,
                    topLeft = Offset(dl, dt),
                    size = Size(dw, dh),
                    cornerRadius = cr
                )

                // Document border
                drawRoundRect(
                    color = IntelligentBlue.copy(alpha = 0.65f),
                    topLeft = Offset(dl, dt),
                    size = Size(dw, dh),
                    cornerRadius = cr,
                    style = Stroke(2.dp.toPx())
                )

                // Document text lines
                val lx1 = dl + 10.dp.toPx()
                val lx2 = dl + dw - 10.dp.toPx()
                val lineColor = IntelligentBlue.copy(alpha = 0.32f)
                val lw = 2.5f.dp.toPx()
                drawLine(lineColor, Offset(lx1, dt + dh * 0.30f), Offset(lx2, dt + dh * 0.30f), lw, StrokeCap.Round)
                drawLine(lineColor, Offset(lx1, dt + dh * 0.44f), Offset(lx2 - 12.dp.toPx(), dt + dh * 0.44f), lw, StrokeCap.Round)
                drawLine(lineColor, Offset(lx1, dt + dh * 0.58f), Offset(lx2 - 20.dp.toPx(), dt + dh * 0.58f), lw, StrokeCap.Round)

                // Scan line
                val scanY = dt + (dh * scanProgress)
                val scanAlpha = when {
                    scanProgress < 0.08f -> scanProgress / 0.08f
                    scanProgress > 0.92f -> (1f - scanProgress) / 0.08f
                    else -> 1f
                }
                drawLine(
                    color = IntelligentBlue.copy(alpha = scanAlpha * glowPulse),
                    start = Offset(dl + 2.dp.toPx(), scanY),
                    end = Offset(dl + dw - 2.dp.toPx(), scanY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Corner scan markers
                val m = 8.dp.toPx()
                val ms = 3.dp.toPx()
                val mx = dl - 7.dp.toPx()
                val my = dt - 7.dp.toPx()
                val mw = dw + 14.dp.toPx()
                val mh = dh + 14.dp.toPx()
                val mc = IntelligentBlue.copy(alpha = glowPulse)
                // TL
                drawLine(mc, Offset(mx, my + m), Offset(mx, my), ms, StrokeCap.Round)
                drawLine(mc, Offset(mx, my), Offset(mx + m, my), ms, StrokeCap.Round)
                // TR
                drawLine(mc, Offset(mx + mw - m, my), Offset(mx + mw, my), ms, StrokeCap.Round)
                drawLine(mc, Offset(mx + mw, my), Offset(mx + mw, my + m), ms, StrokeCap.Round)
                // BL
                drawLine(mc, Offset(mx, my + mh - m), Offset(mx, my + mh), ms, StrokeCap.Round)
                drawLine(mc, Offset(mx, my + mh), Offset(mx + m, my + mh), ms, StrokeCap.Round)
                // BR
                drawLine(mc, Offset(mx + mw - m, my + mh), Offset(mx + mw, my + mh), ms, StrokeCap.Round)
                drawLine(mc, Offset(mx + mw, my + mh - m), Offset(mx + mw, my + mh), ms, StrokeCap.Round)
            }

            Spacer(modifier = Modifier.height(36.dp))

            // App name fade-in
            Row(
                modifier = Modifier.alpha(textAlpha.value),
                verticalAlignment = Alignment.Baseline
            ) {
                Text(
                    text = "DocScan",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                    color = IntelligentBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SCAN · ANALYZE · EXTRACT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = textAlpha.value * 0.45f),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
