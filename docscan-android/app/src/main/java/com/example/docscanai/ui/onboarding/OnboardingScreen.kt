package com.example.docscanai.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docscanai.ui.theme.DeepNavy
import com.example.docscanai.ui.theme.IntelligentBlue
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class OnboardingPage(
    val stepLabel: String,
    val title: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPage(
        stepLabel   = "01 · CAPTURE",
        title       = "Scan Any Document",
        description = "Point your camera at any document — receipts, contracts, IDs, books — and DocScan AI captures it perfectly in seconds."
    ),
    OnboardingPage(
        stepLabel   = "02 · ANALYZE",
        title       = "AI Extracts Everything",
        description = "Powerful OCR reads text, tables, and key fields from your scans with remarkable accuracy — even on handwritten notes."
    ),
    OnboardingPage(
        stepLabel   = "03 · EXPORT",
        title       = "Export & Share Instantly",
        description = "Save as PDF, Word, TXT or CSV. Copy extracted text or share directly — your documents ready in any format."
    ),
    OnboardingPage(
        stepLabel   = "04 · SYNC",
        title       = "Cloud Storage & Sync",
        description = "All your scans are securely stored in the cloud. Access, search, and manage them from anywhere, anytime."
    ),
    OnboardingPage(
        stepLabel   = "05 · PRIVACY",
        title       = "Your Data, Fully Secure",
        description = "End-to-end encrypted storage. AES-256 at rest, TLS in transit. Your documents are private by default."
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()
    val isLast     = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            PageContent(page = pages[pageIndex], pageIndex = pageIndex)
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 8.dp),
        ) {
            // Dots + Continue button row
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Dot indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(pages.size) { index ->
                        val selected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue   = if (selected) 24.dp else 7.dp,
                            animationSpec = tween(300),
                            label         = "dot",
                        )
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (selected) IntelligentBlue
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                // Continue / Get Started pill button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(DeepNavy)
                        .clickable {
                            if (isLast) onFinish()
                            else scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            if (isLast) "Get Started" else "Continue",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = androidx.compose.ui.graphics.Color.White,
                        )
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint     = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // Skip
            if (!isLast) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = onFinish) {
                        Text(
                            "Skip",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage, pageIndex: Int) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
    ) {
        val ilSize = (maxWidth * 0.60f).coerceIn(200.dp, 280.dp)

        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Step label
            Text(
                page.stepLabel,
                fontSize      = 11.sp,
                color         = IntelligentBlue,
                fontFamily    = FontFamily.Monospace,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier      = Modifier.padding(bottom = 16.dp),
            )

            // Illustration card
            Surface(
                modifier = Modifier.size(ilSize),
                shape    = RoundedCornerShape(24.dp),
                color    = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (pageIndex) {
                        0    -> ScanIllustration()
                        1    -> AiIllustration()
                        2    -> ExportIllustration()
                        3    -> CloudIllustration()
                        else -> SecurityIllustration()
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text      = page.title,
                fontSize  = 24.sp,
                fontWeight = FontWeight.Bold,
                color     = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text      = page.description,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Illustrations — unchanged logic, fills its given size ─────────────────────

@Composable
private fun ScanIllustration() {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.background
    val outline = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label         = "scan_y"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue  = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val dw = size.width * 0.52f; val dh = size.height * 0.70f
        val dl = (size.width - dw) / 2f; val dt = (size.height - dh) / 2f
        val cr = CornerRadius(14.dp.toPx())

        drawRoundRect(primary.copy(alpha = 0.10f * glow),
            Offset(dl - 10.dp.toPx(), dt - 10.dp.toPx()),
            Size(dw + 20.dp.toPx(), dh + 20.dp.toPx()), CornerRadius(22.dp.toPx()))

        drawRoundRect(surface, Offset(dl, dt), Size(dw, dh), cr)
        drawRoundRect(outline.copy(alpha = 0.6f), Offset(dl, dt), Size(dw, dh), cr, style = Stroke(2.dp.toPx()))

        val lx1 = dl + 10.dp.toPx(); val lx2 = dl + dw - 10.dp.toPx()
        val lc  = primary.copy(alpha = 0.30f); val lw = 2.5f.dp.toPx()
        drawLine(lc, Offset(lx1, dt + dh * .30f), Offset(lx2,                 dt + dh * .30f), lw, StrokeCap.Round)
        drawLine(lc, Offset(lx1, dt + dh * .44f), Offset(lx2 - 14.dp.toPx(), dt + dh * .44f), lw, StrokeCap.Round)
        drawLine(lc, Offset(lx1, dt + dh * .58f), Offset(lx2 - 22.dp.toPx(), dt + dh * .58f), lw, StrokeCap.Round)

        val sy = dt + dh * scanY
        val fe = if (scanY < .08f) scanY / .08f else if (scanY > .92f) (1f - scanY) / .08f else 1f
        drawLine(primary.copy(alpha = fe * glow), Offset(dl + 2.dp.toPx(), sy), Offset(dl + dw - 2.dp.toPx(), sy), 2.dp.toPx(), StrokeCap.Round)

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

    val infiniteTransition = rememberInfiniteTransition(label = "ai")
    val pulse by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label         = "pulse"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val r      = size.minDimension * 0.136f
        val orbitR = size.minDimension * 0.309f
        val nodeR  = size.minDimension * 0.045f
        val nodeCount = 6

        drawCircle(primary.copy(alpha = (1f - pulse) * 0.3f), radius = orbitR + pulse * size.minDimension * 0.182f, center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))

        for (i in 0 until nodeCount) {
            val angle = (i * 360.0 / nodeCount - 90.0) * Math.PI / 180.0
            val nx = cx + orbitR * cos(angle).toFloat()
            val ny = cy + orbitR * sin(angle).toFloat()

            drawLine(
                Brush.linearGradient(listOf(primary.copy(alpha = .4f), secondary.copy(alpha = .15f)), Offset(cx, cy), Offset(nx, ny)),
                Offset(cx, cy), Offset(nx, ny), 1.5.dp.toPx()
            )

            val activeFactor = if ((pulse * nodeCount).toInt() % nodeCount == i) glow else 0.4f
            drawCircle(secondary.copy(alpha = activeFactor), nodeR, Offset(nx, ny))
            drawCircle(secondary.copy(alpha = .2f * activeFactor), nodeR * 1.8f, Offset(nx, ny))
        }

        drawCircle(Brush.radialGradient(listOf(primary, secondary), radius = r, center = Offset(cx, cy)), r, Offset(cx, cy))
        drawCircle(primary.copy(alpha = .25f * glow), r * 1.6f, Offset(cx, cy))

        val dotSpacing = size.minDimension * 0.027f; val dotR = size.minDimension * 0.011f
        for (j in -1..1) {
            drawCircle(Color.White.copy(alpha = .9f), dotR, Offset(cx + j * dotSpacing, cy))
        }
    }
}

@Composable
private fun ExportIllustration() {
    val primary  = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface  = MaterialTheme.colorScheme.background
    val outline  = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "export")
    val arrowY by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "arrow"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val dw = size.width * 0.48f; val dh = size.height * 0.62f
        val dl = (size.width - dw) / 2f; val dt = (size.height - dh) / 2f + 12.dp.toPx()
        val cr = CornerRadius(12.dp.toPx())

        drawRoundRect(tertiary.copy(alpha = .12f * glow),
            Offset(dl - 8.dp.toPx(), dt - 8.dp.toPx()), Size(dw + 16.dp.toPx(), dh + 16.dp.toPx()), CornerRadius(20.dp.toPx()))

        drawRoundRect(surface, Offset(dl, dt), Size(dw, dh), cr)
        drawRoundRect(outline.copy(alpha = .5f), Offset(dl, dt), Size(dw, dh), cr, style = Stroke(2.dp.toPx()))

        val lx1 = dl + 10.dp.toPx(); val lx2 = dl + dw - 10.dp.toPx()
        val lc  = primary.copy(alpha = .25f); val lw = 2.dp.toPx()
        drawLine(lc, Offset(lx1, dt + dh * .30f), Offset(lx2,                 dt + dh * .30f), lw, StrokeCap.Round)
        drawLine(lc, Offset(lx1, dt + dh * .44f), Offset(lx2 - 16.dp.toPx(), dt + dh * .44f), lw, StrokeCap.Round)
        drawLine(lc, Offset(lx1, dt + dh * .58f), Offset(lx2 - 24.dp.toPx(), dt + dh * .58f), lw, StrokeCap.Round)

        val bx = dl + dw - 4.dp.toPx(); val by = dt + dh - 4.dp.toPx()
        drawCircle(tertiary.copy(alpha = glow), 12.dp.toPx(), Offset(bx, by))
        val ck = 5.dp.toPx()
        drawLine(Color.White, Offset(bx - ck * .6f, by), Offset(bx - ck * .1f, by + ck * .5f), 2.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White, Offset(bx - ck * .1f, by + ck * .5f), Offset(bx + ck * .6f, by - ck * .4f), 2.dp.toPx(), StrokeCap.Round)

        val ax = size.width / 2f; val ay = dt - 20.dp.toPx() + arrowY
        val arrowLen = 18.dp.toPx(); val arrowW = 10.dp.toPx()
        drawCircle(primary.copy(alpha = .15f * glow), 18.dp.toPx(), Offset(ax, ay))
        drawLine(primary.copy(alpha = glow), Offset(ax, ay + arrowLen / 2f), Offset(ax, ay - arrowLen / 2f), 3.dp.toPx(), StrokeCap.Round)
        drawLine(primary.copy(alpha = glow), Offset(ax - arrowW / 2f, ay - arrowLen / 2f + arrowW / 2f), Offset(ax, ay - arrowLen / 2f), 3.dp.toPx(), StrokeCap.Round)
        drawLine(primary.copy(alpha = glow), Offset(ax + arrowW / 2f, ay - arrowLen / 2f + arrowW / 2f), Offset(ax, ay - arrowLen / 2f), 3.dp.toPx(), StrokeCap.Round)
    }
}

// ── Page 4: Cloud Sync illustration ──────────────────────────────────────────

@Composable
private fun CloudIllustration() {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface   = MaterialTheme.colorScheme.background

    val infiniteTransition = rememberInfiniteTransition(label = "cloud")
    val pulse by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val glow by infiniteTransition.animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f; val cy = size.height / 2f

        // Cloud shape (3 overlapping circles + rectangle base)
        val cloudR = size.minDimension * 0.18f
        val baseW  = cloudR * 2.6f; val baseH = cloudR * 0.9f
        val baseL  = cx - baseW / 2f; val baseT  = cy - cloudR * 0.1f

        drawCircle(primary.copy(alpha = 0.15f * glow), cloudR * 2.5f, Offset(cx, cy - cloudR * 0.3f))
        drawRect(primary.copy(alpha = 0.75f), Offset(baseL, baseT), Size(baseW, baseH))
        drawCircle(primary.copy(alpha = 0.75f), cloudR, Offset(cx, baseT))
        drawCircle(primary.copy(alpha = 0.75f), cloudR * 0.75f, Offset(cx - cloudR * 0.9f, baseT + cloudR * 0.1f))
        drawCircle(primary.copy(alpha = 0.75f), cloudR * 0.75f, Offset(cx + cloudR * 0.9f, baseT + cloudR * 0.1f))

        // White cloud shape fill
        drawRect(surface, Offset(baseL + 2.dp.toPx(), baseT + 2.dp.toPx()), Size(baseW - 4.dp.toPx(), baseH - 2.dp.toPx()))
        drawCircle(surface, cloudR - 2.dp.toPx(), Offset(cx, baseT))
        drawCircle(surface, cloudR * 0.75f - 2.dp.toPx(), Offset(cx - cloudR * 0.9f, baseT + cloudR * 0.1f))
        drawCircle(surface, cloudR * 0.75f - 2.dp.toPx(), Offset(cx + cloudR * 0.9f, baseT + cloudR * 0.1f))

        // Animated upload dots
        val dotCount = 3
        for (i in 0 until dotCount) {
            val progress = ((pulse + i.toFloat() / dotCount) % 1f)
            val dotY = baseT + baseH + cloudR * 0.6f + progress * cloudR * 1.4f
            val alpha = if (progress < 0.2f) progress / 0.2f else if (progress > 0.8f) (1f - progress) / 0.2f else 1f
            drawCircle(secondary.copy(alpha = alpha.coerceIn(0f, 1f) * glow), 4.dp.toPx(), Offset(cx + (i - 1) * 16.dp.toPx(), dotY))
        }

        // Upload arrow inside cloud
        val arrowY1 = baseT + cloudR * 0.5f; val arrowY2 = baseT + cloudR * -0.2f
        val arrowW2 = 8.dp.toPx()
        drawLine(primary, Offset(cx, arrowY1), Offset(cx, arrowY2), 2.5.dp.toPx(), StrokeCap.Round)
        drawLine(primary, Offset(cx - arrowW2, arrowY2 + arrowW2), Offset(cx, arrowY2), 2.5.dp.toPx(), StrokeCap.Round)
        drawLine(primary, Offset(cx + arrowW2, arrowY2 + arrowW2), Offset(cx, arrowY2), 2.5.dp.toPx(), StrokeCap.Round)
    }
}

// ── Page 5: Security illustration ────────────────────────────────────────────

@Composable
private fun SecurityIllustration() {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "shield")
    val glow by infiniteTransition.animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val ring by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val shieldW = size.minDimension * 0.38f
        val shieldH = shieldW * 1.2f
        val sl = cx - shieldW / 2f; val st = cy - shieldH * 0.55f

        // Outer pulse ring
        drawCircle(primary.copy(alpha = (1f - ring) * 0.25f * glow), radius = shieldW * 0.8f + ring * shieldW * 0.5f, center = Offset(cx, cy), style = Stroke(2.dp.toPx()))

        // Shield body — simplified as rounded rect
        drawRoundRect(
            primary.copy(alpha = 0.15f),
            Offset(sl, st), Size(shieldW, shieldH),
            CornerRadius(shieldW * 0.35f, shieldW * 0.2f),
        )
        drawRoundRect(
            primary.copy(alpha = 0.6f + 0.4f * glow),
            Offset(sl, st), Size(shieldW, shieldH),
            CornerRadius(shieldW * 0.35f, shieldW * 0.2f),
            style = Stroke(2.5.dp.toPx()),
        )

        // Checkmark inside shield
        val ckCx = cx; val ckCy = cy + shieldH * 0.05f
        val ckSize = shieldW * 0.22f
        drawLine(tertiary.copy(alpha = glow), Offset(ckCx - ckSize, ckCy), Offset(ckCx - ckSize * 0.2f, ckCy + ckSize * 0.7f), 3.dp.toPx(), StrokeCap.Round)
        drawLine(tertiary.copy(alpha = glow), Offset(ckCx - ckSize * 0.2f, ckCy + ckSize * 0.7f), Offset(ckCx + ckSize, ckCy - ckSize * 0.5f), 3.dp.toPx(), StrokeCap.Round)

        // Small lock icon at top of shield
        val lockCx = cx; val lockCy = st + shieldH * 0.28f
        val lockW = shieldW * 0.2f; val lockH = lockW * 0.8f
        drawRoundRect(primary, Offset(lockCx - lockW / 2f, lockCy), Size(lockW, lockH), CornerRadius(3.dp.toPx()))
        drawArc(primary, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(lockCx - lockW * 0.35f, lockCy - lockW * 0.45f),
            size = Size(lockW * 0.7f, lockW * 0.55f),
            style = Stroke(2.dp.toPx()))
    }
}
