package com.example.docscanai.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class OnboardingPage(val title: String, val description: String)

private val pages = listOf(
    OnboardingPage(
        title = "Scan Any Document",
        description = "Point your camera at any document — receipts, contracts, IDs, books — and DocScan AI captures it perfectly."
    ),
    OnboardingPage(
        title = "AI Extracts Everything",
        description = "Powerful OCR and AI models extract text, tables, and key data from your scans with remarkable accuracy."
    ),
    OnboardingPage(
        title = "Export & Share Instantly",
        description = "Save as PDF, copy extracted text, or share directly. Your documents, organised and ready everywhere."
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState   = rememberPagerState(pageCount = { pages.size })
    val scope        = rememberCoroutineScope()
    val isLast       = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 200.dp)
        ) { pageIndex ->
            PageContent(page = pages[pageIndex], pageIndex = pageIndex)
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (selected) 28.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text  = if (isLast) "Get Started" else "Next",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onFinish) {
                Text(
                    text  = "Skip",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage, pageIndex: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            when (pageIndex) {
                0 -> ScanIllustration()
                1 -> AiIllustration()
                else -> ExportIllustration()
            }
        }

        Spacer(Modifier.height(52.dp))

        Text(
            text      = page.title,
            style     = MaterialTheme.typography.headlineSmall,
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text      = page.description,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Illustrations ─────────────────────────────────────────────────────────────

@Composable
private fun ScanIllustration() {
    val primary   = MaterialTheme.colorScheme.primary
    val surface   = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline   = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan_y"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        val dw = size.width * 0.52f; val dh = size.height * 0.70f
        val dl = (size.width - dw) / 2f; val dt = (size.height - dh) / 2f
        val cr = CornerRadius(14.dp.toPx())

        // Glow ring
        drawRoundRect(primary.copy(alpha = 0.10f * glow),
            Offset(dl - 10.dp.toPx(), dt - 10.dp.toPx()),
            Size(dw + 20.dp.toPx(), dh + 20.dp.toPx()), CornerRadius(22.dp.toPx()))

        // Document
        drawRoundRect(surface, Offset(dl, dt), Size(dw, dh), cr)
        drawRoundRect(outline.copy(alpha = 0.6f), Offset(dl, dt), Size(dw, dh), cr, style = Stroke(2.dp.toPx()))

        // Text lines
        val lx1 = dl + 10.dp.toPx(); val lx2 = dl + dw - 10.dp.toPx()
        val lc  = primary.copy(alpha = 0.30f); val lw = 2.5f.dp.toPx()
        drawLine(lc, Offset(lx1, dt + dh * .30f), Offset(lx2,               dt + dh * .30f), lw, StrokeCap.Round)
        drawLine(lc, Offset(lx1, dt + dh * .44f), Offset(lx2 - 14.dp.toPx(), dt + dh * .44f), lw, StrokeCap.Round)
        drawLine(lc, Offset(lx1, dt + dh * .58f), Offset(lx2 - 22.dp.toPx(), dt + dh * .58f), lw, StrokeCap.Round)

        // Scan line
        val sy = dt + dh * scanY
        val fe = if (scanY < .08f) scanY / .08f else if (scanY > .92f) (1f - scanY) / .08f else 1f
        drawLine(primary.copy(alpha = fe * glow), Offset(dl + 2.dp.toPx(), sy), Offset(dl + dw - 2.dp.toPx(), sy), 2.dp.toPx(), StrokeCap.Round)

        // Corner markers
        val m = 9.dp.toPx(); val ms = 3.5f.dp.toPx()
        val mx = dl - 7.dp.toPx(); val my = dt - 7.dp.toPx()
        val mw = dw + 14.dp.toPx(); val mh = dh + 14.dp.toPx()
        val mc = primary.copy(alpha = .5f + .5f * glow)
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

@Composable
private fun AiIllustration() {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface   = MaterialTheme.colorScheme.surfaceContainerHigh

    val infiniteTransition = rememberInfiniteTransition(label = "ai")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val r  = 30.dp.toPx()
        val orbitR = 68.dp.toPx()
        val nodeR  = 10.dp.toPx()
        val nodeCount = 6

        // Outer pulse ring
        drawCircle(primary.copy(alpha = (1f - pulse) * 0.3f), radius = orbitR + pulse * 40.dp.toPx(), center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))

        // Orbit nodes + connecting lines
        for (i in 0 until nodeCount) {
            val angle = (i * 360.0 / nodeCount - 90.0) * Math.PI / 180.0
            val nx = cx + orbitR * cos(angle).toFloat()
            val ny = cy + orbitR * sin(angle).toFloat()

            // Line from center to node
            drawLine(
                Brush.linearGradient(listOf(primary.copy(alpha = .4f), secondary.copy(alpha = .15f)), Offset(cx, cy), Offset(nx, ny)),
                Offset(cx, cy), Offset(nx, ny), 1.5.dp.toPx()
            )

            // Node
            val activeFactor = if ((pulse * nodeCount).toInt() % nodeCount == i) glow else 0.4f
            drawCircle(secondary.copy(alpha = activeFactor), nodeR, Offset(nx, ny))
            drawCircle(secondary.copy(alpha = .2f * activeFactor), nodeR * 1.8f, Offset(nx, ny))
        }

        // Centre hub
        drawCircle(Brush.radialGradient(listOf(primary, secondary), radius = r, center = Offset(cx, cy)), r, Offset(cx, cy))
        drawCircle(primary.copy(alpha = .25f * glow), r * 1.6f, Offset(cx, cy))

        // AI text represented as 3 dots in centre
        val dotSpacing = 6.dp.toPx(); val dotR = 2.5f.dp.toPx()
        for (j in -1..1) {
            drawCircle(Color.White.copy(alpha = .9f), dotR, Offset(cx + j * dotSpacing, cy))
        }
    }
}

@Composable
private fun ExportIllustration() {
    val primary   = MaterialTheme.colorScheme.primary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val surface   = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline   = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "export")
    val arrowY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "arrow"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow"
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        val dw = size.width * 0.48f; val dh = size.height * 0.62f
        val dl = (size.width - dw) / 2f; val dt = (size.height - dh) / 2f + 12.dp.toPx()
        val cr = CornerRadius(12.dp.toPx())

        // Glow beneath
        drawRoundRect(tertiary.copy(alpha = .12f * glow),
            Offset(dl - 8.dp.toPx(), dt - 8.dp.toPx()), Size(dw + 16.dp.toPx(), dh + 16.dp.toPx()), CornerRadius(20.dp.toPx()))

        // Document
        drawRoundRect(surface, Offset(dl, dt), Size(dw, dh), cr)
        drawRoundRect(outline.copy(alpha = .5f), Offset(dl, dt), Size(dw, dh), cr, style = Stroke(2.dp.toPx()))

        // Lines in document
        val lx1 = dl + 10.dp.toPx(); val lx2 = dl + dw - 10.dp.toPx()
        val lc  = primary.copy(alpha = .25f); val lw = 2.dp.toPx()
        drawLine(lc, Offset(lx1, dt + dh * .30f), Offset(lx2,               dt + dh * .30f), lw, StrokeCap.Round)
        drawLine(lc, Offset(lx1, dt + dh * .44f), Offset(lx2 - 16.dp.toPx(), dt + dh * .44f), lw, StrokeCap.Round)
        drawLine(lc, Offset(lx1, dt + dh * .58f), Offset(lx2 - 24.dp.toPx(), dt + dh * .58f), lw, StrokeCap.Round)

        // Check mark badge (bottom-right of doc)
        val bx = dl + dw - 4.dp.toPx(); val by = dt + dh - 4.dp.toPx()
        drawCircle(tertiary.copy(alpha = glow), 12.dp.toPx(), Offset(bx, by))
        val ck = 5.dp.toPx()
        drawLine(Color.White, Offset(bx - ck * .6f, by), Offset(bx - ck * .1f, by + ck * .5f), 2.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White, Offset(bx - ck * .1f, by + ck * .5f), Offset(bx + ck * .6f, by - ck * .4f), 2.dp.toPx(), StrokeCap.Round)

        // Arrow above doc (animated)
        val ax = size.width / 2f; val ay = dt - 20.dp.toPx() + arrowY
        val arrowLen = 18.dp.toPx(); val arrowW = 10.dp.toPx()
        // Glow
        drawCircle(primary.copy(alpha = .15f * glow), 18.dp.toPx(), Offset(ax, ay))
        // Vertical shaft
        drawLine(primary.copy(alpha = glow), Offset(ax, ay + arrowLen / 2f), Offset(ax, ay - arrowLen / 2f), 3.dp.toPx(), StrokeCap.Round)
        // Arrowhead
        drawLine(primary.copy(alpha = glow), Offset(ax - arrowW / 2f, ay - arrowLen / 2f + arrowW / 2f), Offset(ax, ay - arrowLen / 2f), 3.dp.toPx(), StrokeCap.Round)
        drawLine(primary.copy(alpha = glow), Offset(ax + arrowW / 2f, ay - arrowLen / 2f + arrowW / 2f), Offset(ax, ay - arrowLen / 2f), 3.dp.toPx(), StrokeCap.Round)
    }
}
