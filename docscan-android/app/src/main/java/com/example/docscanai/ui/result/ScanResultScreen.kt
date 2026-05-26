package com.example.docscanai.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.unit.dp

private val sampleOcrText = """
INVOICE #INV-2024-0392

Date: March 15, 2024
Due Date: April 15, 2024

Bill To:
Acme Corporation
123 Business Ave, Suite 400
New York, NY 10001

Services Rendered:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AI Consulting Services       ${'$'}4,500.00
Document Processing Setup    ${'$'}1,200.00
Monthly License (x3)         ${'$'}  897.00
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Subtotal                     ${'$'}6,597.00
Tax (8.5%)                   ${'$'}  560.74
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL DUE                    ${'$'}7,157.74

Payment Terms: Net 30
Bank: First National Bank
Account: ****4892
Routing: 021000021

Thank you for your business!
""".trimIndent()

private val aiSummaryItems = listOf(
    Triple("Document Type",  "Invoice",                  Icons.Default.Description),
    Triple("Total Amount",   "\$7,157.74",               Icons.Default.AttachMoney),
    Triple("Date Issued",    "March 15, 2024",           Icons.Default.CalendarToday),
    Triple("Due Date",       "April 15, 2024",           Icons.Default.Schedule),
    Triple("Vendor",         "Acme Corporation",         Icons.Default.Business),
    Triple("Status",         "Payment Pending",          Icons.Default.HourglassEmpty),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    scanId: String,
    onBack: () -> Unit,
    onViewDocument: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Preview", "Extracted Text", "AI Summary")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Scan Result",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            scanId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color  = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape  = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Text", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export PDF", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset( tabPositions[selectedTab]),
                        color    = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> PreviewTab(onViewDocument = onViewDocument)
                1 -> TextTab()
                2 -> SummaryTab()
            }
        }
    }
}

@Composable
private fun PreviewTab(onViewDocument: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scan quality badge
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                Text("Excellent scan quality · 98% confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Document preview card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.70f)
                .aspectRatio(0.75f)
                .clip(MaterialTheme.shapes.large)
                .clickable(onClick = onViewDocument)
        ) {
            // Document canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(surface, cornerRadius = CornerRadius(14.dp.toPx()))
                drawRoundRect(outline.copy(alpha = .4f), cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(1.5.dp.toPx()))

                val pad = 20.dp.toPx()
                // Header block
                drawRoundRect(primary.copy(alpha = .15f), Offset(pad, pad), Size(size.width - pad * 2, 22.dp.toPx()), CornerRadius(4.dp.toPx()))

                // Text lines
                val lx1 = pad; val lx2 = size.width - pad
                val lc  = primary.copy(alpha = .20f); val lw = 2.dp.toPx()
                val lineYs = listOf(.25f, .33f, .41f, .50f, .58f, .67f, .75f, .83f)
                lineYs.forEachIndexed { i, y ->
                    val endX = if (i % 3 == 2) lx2 - 30.dp.toPx() else lx2
                    drawLine(lc, Offset(lx1, size.height * y), Offset(endX, size.height * y), lw, StrokeCap.Round)
                }

                // Amount highlight
                drawRoundRect(primary.copy(alpha = .18f),
                    Offset(pad, size.height * .87f), Size(size.width * .45f, 14.dp.toPx()), CornerRadius(4.dp.toPx()))
            }

            // Tap to view label
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, null,
                        tint = Color.White.copy(alpha = .8f), modifier = Modifier.size(14.dp))
                    Text("Tap to view full document",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = .8f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Page info chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1 page", "JPEG · 2.4 MB", "Portrait").forEach { label ->
                Surface(
                    shape  = MaterialTheme.shapes.extraSmall,
                    color  = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun TextTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "OCR Result",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    "98% accuracy",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // OCR text
        SelectionContainer {
            Text(
                text     = sampleOcrText,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun SummaryTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "AI Extracted Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Key info cards
        aiSummaryItems.forEach { (label, value, icon) ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = MaterialTheme.shapes.medium,
                color    = MaterialTheme.colorScheme.surfaceContainerHigh,
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Summary paragraph
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = MaterialTheme.shapes.medium,
            color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text("AI Summary", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "This is an invoice from Acme Corporation dated March 15, 2024. " +
                    "The total amount due is \$7,157.74 (including 8.5% tax) for AI Consulting Services, " +
                    "Document Processing Setup, and a 3-month software license. Payment is due April 15, 2024.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Allows text selection in Compose
@Composable
private fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer { content() }
}
