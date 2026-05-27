package com.example.docscanai.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.IntelligentBlue

private data class SearchResult(
    val fileName: String,
    val fileType: String,
    val snippet: String,
    val matchWord: String,
    val date: String,
)

private val SampleResults = listOf(
    SearchResult("Invoice_Q2_2025.pdf", "PDF", "Total amount due: \$4,200.00 — payment", "payment", "May 14"),
    SearchResult("Contract_NDA.pdf", "PDF", "This agreement is subject to renewal upon", "renewal", "May 10"),
    SearchResult("Meeting_Notes.scan", "SCAN", "Action items: follow up on budget approval", "budget", "May 8"),
    SearchResult("Report_Draft.docx", "DOCX", "Quarterly performance exceeded expectations by", "performance", "May 6"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit) {
    val keyboard      = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var query         by remember { mutableStateOf("") }
    var hasSearched   by remember { mutableStateOf(false) }
    var activeFilters by remember { mutableStateOf(setOf<String>()) }

    val filterChips = listOf("Smart match", "PDF", "Past 7 days", "OCR text")

    val results = remember(query) {
        if (query.length < 2) emptyList()
        else SampleResults.filter {
            it.fileName.contains(query, ignoreCase = true) ||
            it.snippet.contains(query, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            // Search bar
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value           = query,
                    onValueChange   = { query = it; hasSearched = it.length >= 2 },
                    placeholder     = { Text("Search documents, OCR text…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)) },
                    leadingIcon     = {
                        Icon(Icons.Default.Search, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp))
                    },
                    trailingIcon    = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = ""; hasSearched = false }) {
                                Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Mic, "Voice",
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine      = true,
                    modifier        = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    shape           = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        hasSearched = true
                    }),
                    colors          = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor      = IntelligentBlue.copy(alpha = 0.60f),
                    ),
                )
            }

            // Filter chips
            LazyRow(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filterChips) { chip ->
                    val isActive = activeFilters.contains(chip)
                    FilterChip(
                        selected = isActive,
                        onClick  = {
                            activeFilters = if (isActive) activeFilters - chip else activeFilters + chip
                        },
                        label    = { Text(chip, fontSize = 12.sp) },
                        leadingIcon = if (isActive) ({
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                        }) else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IntelligentBlue,
                            selectedLabelColor     = Color.White,
                            selectedLeadingIconColor = Color.White,
                        ),
                    )
                }
                item {
                    if (activeFilters.isNotEmpty()) {
                        TextButton(onClick = { activeFilters = emptySet() }) {
                            Text("Clear filters", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (!hasSearched || query.length < 2) {
                // Empty / idle state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Search, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp))
                        }
                        Text("Search your documents", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            "Search by filename, OCR-extracted text, or AI summaries",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        // AI suggestion chip
                        Surface(
                            shape  = RoundedCornerShape(100.dp),
                            color  = IntelligentBlue.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, IntelligentBlue.copy(alpha = 0.20f)),
                            modifier = Modifier.clickable { query = "invoice"; hasSearched = true },
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.AutoAwesome, null,
                                    tint     = IntelligentBlue,
                                    modifier = Modifier.size(13.dp))
                                Text("Try: \"invoice\" or \"contract\"",
                                    fontSize = 12.sp, color = IntelligentBlue)
                            }
                        }
                    }
                }
            } else if (results.isEmpty()) {
                // No results
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(Icons.Default.SearchOff, null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(52.dp))
                        Text("No results for \"$query\"",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground)
                        Text("Try different keywords or check OCR text filters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // AI Answer card
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(16.dp),
                            color    = IntelligentBlue.copy(alpha = 0.06f),
                            border   = BorderStroke(1.dp, IntelligentBlue.copy(alpha = 0.25f)),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null,
                                        tint     = IntelligentBlue,
                                        modifier = Modifier.size(14.dp))
                                    Text(
                                        "AI ANSWER",
                                        fontSize      = 10.sp,
                                        color         = IntelligentBlue,
                                        fontFamily    = FontFamily.Monospace,
                                        fontWeight    = FontWeight.SemiBold,
                                        letterSpacing = 1.sp,
                                    )
                                }
                                Text(
                                    "Found ${results.size} document${if (results.size != 1) "s" else ""} matching \"$query\". Most recent: ${results.first().fileName}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    // Match count label
                    item {
                        Text(
                            "MATCHES IN TEXT · ${results.size}",
                            fontSize      = 10.sp,
                            color         = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontFamily    = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            modifier      = Modifier.padding(top = 4.dp),
                        )
                    }

                    // Results
                    items(results) { result ->
                        SearchResultCard(result = result, query = query)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: SearchResult, query: String) {
    val typeColor = when (result.fileType) {
        "PDF"  -> Color(0xFFEF4444)
        "SCAN" -> Color(0xFF06B6D4)
        "DOCX" -> IntelligentBlue
        else   -> Color(0xFF8B5CF6)
    }
    val onSurface = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    result.fileType,
                    fontSize      = 8.sp,
                    color         = typeColor,
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        result.fileName,
                        style     = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color     = MaterialTheme.colorScheme.onSurface,
                        modifier  = Modifier.weight(1f),
                    )
                    Text(
                        result.date,
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }

                // Snippet with highlighted match
                val lowerSnippet = result.snippet.lowercase()
                val lowerQuery   = query.lowercase()
                val matchIdx     = lowerSnippet.indexOf(lowerQuery)
                if (matchIdx >= 0) {
                    Text(
                        buildAnnotatedString {
                            append(result.snippet.take(matchIdx))
                            withStyle(SpanStyle(
                                background = AIGlow.copy(alpha = 0.25f),
                                color      = onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )) {
                                append(result.snippet.substring(matchIdx, matchIdx + query.length))
                            }
                            append(result.snippet.drop(matchIdx + query.length))
                        },
                        style   = MaterialTheme.typography.bodySmall,
                        color   = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                } else {
                    Text(
                        result.snippet,
                        style   = MaterialTheme.typography.bodySmall,
                        color   = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }

                // OCR badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Text(
                        "OCR TEXT MATCH",
                        modifier      = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize      = 8.sp,
                        color         = MaterialTheme.colorScheme.primary,
                        fontFamily    = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}
