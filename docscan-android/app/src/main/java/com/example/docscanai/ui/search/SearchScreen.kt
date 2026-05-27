package com.example.docscanai.ui.search

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.docscanai.data.ScanHistoryRepository
import com.example.docscanai.data.ScanRecord
import com.example.docscanai.ui.theme.IntelligentBlue

// ── Design tokens ─────────────────────────────────────────────────────────────

private val SearchBg: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.background
private val CardBg: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.surface
private val CardBorder: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
private val LabelGray: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
private val TextPrimary: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
private val TextSecondary: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
private val HighlightBg: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
private val HighlightText: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.primary
private val AiCardBg: androidx.compose.ui.graphics.Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
private val PdfBadgeBg    = Color(0xFFFEE2E2)
private val PdfBadgeText  = Color(0xFFDC2626)
private val ScanBadgeBg   = Color(0xFFCCFBF1)
private val ScanBadgeText = Color(0xFF0D9488)

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onResultClick: (ScanRecord) -> Unit,
) {
    val context        = LocalContext.current
    val keyboard       = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var query        by remember { mutableStateOf("") }
    var hasSearched  by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf("Smart match") }

    val filterChips = listOf("Smart match", "PDF", "Past 7 days", "OCR text")

    val allScans by ScanHistoryRepository.scans.collectAsStateWithLifecycle()

    val results = remember(query, allScans) {
        if (query.length < 2) emptyList()
        else allScans.filter { it.name.contains(query, ignoreCase = true) }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── Search bar row ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBg)
                    .border(1.5.dp, IntelligentBlue, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Search, null,
                        tint     = IntelligentBlue,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Search documents, OCR text…",
                                color    = LabelGray,
                                fontSize = 15.sp,
                            )
                        }
                        BasicTextField(
                            value         = query,
                            onValueChange = { query = it; hasSearched = it.length >= 2 },
                            modifier      = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine    = true,
                            textStyle     = TextStyle(fontSize = 15.sp, color = TextPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboard?.hide()
                                hasSearched = true
                            }),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.width(1.dp).height(18.dp).background(Color(0xFFD1D5DB)))
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Default.Mic, "Voice",
                        tint     = IntelligentBlue,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                Toast.makeText(context, "Voice search coming soon", Toast.LENGTH_SHORT).show()
                            },
                    )
                }
            }
        }

        // ── Filter chips ──────────────────────────────────────────────────────
        LazyRow(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filterChips) { chip ->
                SearchFilterChip(
                    label    = chip,
                    selected = chip == activeFilter,
                    onClick  = { activeFilter = chip },
                )
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        when {
            !hasSearched || query.length < 2 -> SearchIdleState()
            results.isEmpty()                -> SearchEmptyState(query)
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item {
                        AiAnswerCard(query = query, count = results.size)
                        Spacer(Modifier.height(18.dp))
                    }
                    item {
                        Text(
                            "MATCHES IN TEXT  ·  ${results.size}",
                            fontSize      = 10.sp,
                            color         = LabelGray,
                            fontFamily    = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            modifier      = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    items(results, key = { it.id }) { record ->
                        SearchResultCard(
                            record  = record,
                            query   = query,
                            onClick = { onResultClick(record) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// ── Filter chip (pill style) ──────────────────────────────────────────────────

@Composable
private fun SearchFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selected) IntelligentBlue else CardBg)
            .border(1.dp, if (selected) IntelligentBlue else Color(0xFFD1D5DB), RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (label == "Smart match") {
                Icon(
                    Icons.Default.AutoAwesome, null,
                    tint     = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.surface else IntelligentBlue,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                label,
                fontSize   = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.surface else TextSecondary,
            )
        }
    }
}

// ── AI Answer card ────────────────────────────────────────────────────────────

@Composable
private fun AiAnswerCard(query: String, count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        color    = AiCardBg,
        border   = BorderStroke(1.dp, IntelligentBlue.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(CardBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Default.AutoAwesome, null,
                        tint     = IntelligentBlue,
                        modifier = Modifier.size(11.dp))
                    Text(
                        "AI ANSWER",
                        fontSize      = 10.sp,
                        color         = IntelligentBlue,
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
            // Body with bold highlights
            val noun = if (count == 1) "document" else "documents"
            val body = buildAnnotatedString {
                append("Found ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                    append("$count $noun")
                }
                append(" matching ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                    append("\"$query\"")
                }
                append(". Tap any result to open and view the matched content in context.")
            }
            Text(body, fontSize = 14.sp, color = TextSecondary, lineHeight = 21.sp)
        }
    }
}

// ── Search result card ────────────────────────────────────────────────────────

@Composable
private fun SearchResultCard(record: ScanRecord, query: String, onClick: () -> Unit) {
    val isPdf     = record.name.endsWith(".pdf", ignoreCase = true)
    val isGallery = record.id.startsWith("gallery")

    val (badgeBg, badgeText, badgeLabel) = when {
        isPdf     -> Triple(PdfBadgeBg,  PdfBadgeText,  "PDF")
        isGallery -> Triple(ScanBadgeBg, ScanBadgeText, "IMG")
        else      -> Triple(ScanBadgeBg, ScanBadgeText, "SCAN")
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(14.dp),
        color    = CardBg,
        border   = BorderStroke(1.dp, CardBorder),
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Type badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    badgeLabel,
                    fontSize      = if (badgeLabel.length > 3) 7.sp else 9.sp,
                    color         = badgeText,
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                )
            }

            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    record.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    maxLines   = 1,
                )
                Text(
                    "PAGE 1  ·  OCR MATCH",
                    fontSize      = 10.sp,
                    color         = LabelGray,
                    fontFamily    = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(2.dp))
                SnippetText(
                    name    = record.name,
                    keyword = query,
                )
            }
        }
    }
}

// ── Inline keyword highlight ──────────────────────────────────────────────────

@Composable
private fun SnippetText(name: String, keyword: String) {
    val lower  = name.lowercase()
    val kLower = keyword.lowercase()
    val idx    = lower.indexOf(kLower)

    // Build "…prefix keyword suffix…" context window
    val fullSnippet = "…${name}…"
    val snipIdx     = fullSnippet.lowercase().indexOf(kLower)

    if (snipIdx < 0 || keyword.isBlank()) {
        Text(fullSnippet, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        return
    }

    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = TextSecondary)) {
            append(fullSnippet.substring(0, snipIdx))
        }
        withStyle(SpanStyle(
            color      = HighlightText,
            background = HighlightBg,
            fontWeight = FontWeight.Medium,
        )) {
            append(fullSnippet.substring(snipIdx, snipIdx + keyword.length))
        }
        withStyle(SpanStyle(color = TextSecondary)) {
            append(fullSnippet.substring(snipIdx + keyword.length))
        }
    }
    Text(text, fontSize = 13.sp, lineHeight = 18.sp)
}

// ── Idle state ────────────────────────────────────────────────────────────────

@Composable
private fun SearchIdleState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(IntelligentBlue.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Search, null,
                    tint     = IntelligentBlue,
                    modifier = Modifier.size(32.dp))
            }
            Text(
                "Search your documents",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
            )
            Text(
                "Search by filename, OCR-extracted text, or AI summaries",
                fontSize  = 13.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun SearchEmptyState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier            = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Default.SearchOff, null,
                tint     = LabelGray,
                modifier = Modifier.size(52.dp))
            Text(
                "No results for \"$query\"",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
            )
            Text(
                "Try different keywords or check your OCR text filters",
                fontSize  = 13.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )
        }
    }
}



