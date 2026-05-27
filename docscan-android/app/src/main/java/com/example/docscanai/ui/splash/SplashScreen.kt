package com.example.docscanai.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.IntelligentBlue
import kotlinx.coroutines.delay

private val NavyDark  = Color(0xFF0F2240)
private val NavyLight = Color(0xFF1E3A5F)

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val scanProgress by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label         = "scan_pos"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "glow"
    )

    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(400)
        textAlpha.animateTo(1f, animationSpec = tween(700))
        delay(1400)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyLight, NavyDark))),
        contentAlignment = Alignment.Center,
    ) {
        // Particle dots
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val dots = listOf(
                Pair(0.08f, 0.05f), Pair(0.88f, 0.08f), Pair(0.25f, 0.12f),
                Pair(0.65f, 0.04f), Pair(0.80f, 0.18f), Pair(0.05f, 0.28f),
                Pair(0.92f, 0.35f), Pair(0.18f, 0.48f), Pair(0.48f, 0.02f),
                Pair(0.72f, 0.55f), Pair(0.35f, 0.78f), Pair(0.90f, 0.70f),
                Pair(0.12f, 0.85f), Pair(0.55f, 0.92f), Pair(0.78f, 0.88f),
            )
            dots.forEach { (x, y) ->
                drawCircle(
                    color  = AIGlow.copy(alpha = 0.22f),
                    radius = 2.5f.dp.toPx(),
                    center = Offset(size.width * x, size.height * y),
                )
            }
        }

        // Concentric rings
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.05f),
                radius = 130.dp.toPx(),
                center = center,
                style  = Stroke(1.dp.toPx()),
            )
            drawCircle(
                color  = Color.White.copy(alpha = 0.03f),
                radius = 210.dp.toPx(),
                center = center,
                style  = Stroke(1.dp.toPx()),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Glass logo box
            Surface(
                modifier        = Modifier.size(140.dp),
                shape           = RoundedCornerShape(28.dp),
                color           = Color.White.copy(alpha = 0.06f),
                border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(88.dp)) {
                        val dw = size.width  * 0.52f
                        val dh = size.height * 0.70f
                        val dl = (size.width  - dw) / 2f
                        val dt = (size.height - dh) / 2f
                        val cr = CornerRadius(10.dp.toPx())

                        // Document card
                        drawRoundRect(
                            color        = Color.White.copy(alpha = 0.08f),
                            topLeft      = Offset(dl, dt),
                            size         = Size(dw, dh),
                            cornerRadius = cr,
                        )
                        drawRoundRect(
                            color        = Color.White.copy(alpha = 0.20f + 0.10f * glowPulse),
                            topLeft      = Offset(dl, dt),
                            size         = Size(dw, dh),
                            cornerRadius = cr,
                            style        = Stroke(1.5.dp.toPx()),
                        )

                        // Text lines
                        val lx1 = dl + 8.dp.toPx()
                        val lx2 = dl + dw - 8.dp.toPx()
                        val lc  = AIGlow.copy(alpha = 0.50f)
                        val lw  = 2.dp.toPx()
                        drawLine(lc, Offset(lx1, dt + dh * 0.30f), Offset(lx2,                       dt + dh * 0.30f), lw, StrokeCap.Round)
                        drawLine(lc, Offset(lx1, dt + dh * 0.46f), Offset(lx2 - 8.dp.toPx(),  dt + dh * 0.46f), lw, StrokeCap.Round)
                        drawLine(lc, Offset(lx1, dt + dh * 0.62f), Offset(lx2 - 14.dp.toPx(), dt + dh * 0.62f), lw, StrokeCap.Round)

                        // Scan beam
                        val scanY = dt + dh * scanProgress
                        val edge  = when {
                            scanProgress < 0.08f -> scanProgress / 0.08f
                            scanProgress > 0.92f -> (1f - scanProgress) / 0.08f
                            else -> 1f
                        }

                        // Glow under beam
                        drawRect(
                            brush   = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, AIGlow.copy(alpha = 0.25f * glowPulse * edge), Color.Transparent),
                                startY = (scanY - 12.dp.toPx()).coerceAtLeast(dt),
                                endY   = (scanY + 12.dp.toPx()).coerceAtMost(dt + dh),
                            ),
                            topLeft = Offset(dl + 1.dp.toPx(), (scanY - 12.dp.toPx()).coerceAtLeast(dt)),
                            size    = Size(dw - 2.dp.toPx(), 24.dp.toPx()),
                        )

                        drawLine(
                            color       = AIGlow.copy(alpha = edge * glowPulse),
                            start       = Offset(dl + 2.dp.toPx(), scanY),
                            end         = Offset(dl + dw - 2.dp.toPx(), scanY),
                            strokeWidth = 2.dp.toPx(),
                            cap         = StrokeCap.Round,
                        )

                        // Corner markers
                        val m  = 7.dp.toPx(); val ms = 2.5f.dp.toPx()
                        val mx = dl - 5.dp.toPx(); val my = dt - 5.dp.toPx()
                        val mw = dw + 10.dp.toPx(); val mh = dh + 10.dp.toPx()
                        val mc = AIGlow.copy(alpha = 0.5f + 0.5f * glowPulse)
                        drawLine(mc, Offset(mx, my + m), Offset(mx, my), ms, StrokeCap.Round)
                        drawLine(mc, Offset(mx, my), Offset(mx + m, my), ms, StrokeCap.Round)
                        drawLine(mc, Offset(mx + mw - m, my), Offset(mx + mw, my), ms, StrokeCap.Round)
                        drawLine(mc, Offset(mx + mw, my), Offset(mx + mw, my + m), ms, StrokeCap.Round)
                        drawLine(mc, Offset(mx, my + mh - m), Offset(mx, my + mh), ms, StrokeCap.Round)
                        drawLine(mc, Offset(mx, my + mh), Offset(mx + m, my + mh), ms, StrokeCap.Round)
                        drawLine(mc, Offset(mx + mw - m, my + mh), Offset(mx + mw, my + mh), ms, StrokeCap.Round)
                        drawLine(mc, Offset(mx + mw, my + mh - m), Offset(mx + mw, my + mh), ms, StrokeCap.Round)
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            // Wordmark
            Row(
                modifier          = Modifier.alpha(textAlpha.value),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "DocScan",
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
                Text(
                    " AI",
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = AIGlow,
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                "Intelligent Document Scanner",
                fontSize      = 13.sp,
                color         = Color.White.copy(alpha = textAlpha.value * 0.60f),
                letterSpacing = 0.5.sp,
                textAlign     = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // Loading dots
            Row(
                modifier              = Modifier.alpha(textAlpha.value),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == 1) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == 1) AIGlow.copy(alpha = 0.8f + 0.2f * glowPulse)
                                else Color.White.copy(alpha = 0.35f)
                            ),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "INITIALIZING · OCR ENGINE v4.2",
                fontSize      = 10.sp,
                color         = Color.White.copy(alpha = textAlpha.value * 0.35f),
                letterSpacing = 1.sp,
                fontFamily    = FontFamily.Monospace,
                textAlign     = TextAlign.Center,
            )
        }
    }
}
