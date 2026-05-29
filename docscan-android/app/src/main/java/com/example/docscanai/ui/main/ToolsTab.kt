package com.example.docscanai.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docscanai.ui.theme.AIGlow
import com.example.docscanai.ui.theme.IntelligentBlue

data class ToolItem(val name: String, val icon: ImageVector, val color: Color, val isNew: Boolean = false, val route: String? = null)
data class ToolCategory(val title: String, val items: List<ToolItem>)

val toolCategories = listOf(
    ToolCategory("Scanner", listOf(
        ToolItem("Scan Document", Icons.Default.DocumentScanner, Color(0xFF3B82F6), route = "camera_scanner"),
        ToolItem("Smart Search", Icons.Default.Search, Color(0xFF3B82F6), route = "search")
    )),
    ToolCategory("OCR", listOf(
        ToolItem("OCR Text", Icons.Default.TextSnippet, Color(0xFFA855F7), route = "camera_scanner") // Route to camera for OCR first
    )),
    ToolCategory("PDF Utilities", listOf(
        ToolItem("PDF Editor", Icons.Default.PictureAsPdf, Color(0xFFEF4444)),
        ToolItem("Merge PDF", Icons.Default.MergeType, Color(0xFFEF4444), route = "pdf_merge"),
        ToolItem("Compress PDF", Icons.Default.Compress, Color(0xFFEF4444), route = "pdf_compress"),
        ToolItem("Split PDF", Icons.Default.CallSplit, Color(0xFFEF4444), route = "pdf_split")
    )),
    ToolCategory("File Tools", listOf(
        ToolItem("Convert Files", Icons.Default.Transform, Color(0xFF10B981), route = "convert"),
        ToolItem("Folder System", Icons.Default.Folder, Color(0xFF10B981)),
        ToolItem("Cloud Sync", Icons.Default.CloudSync, Color(0xFF10B981), route = "app_settings")
    )),
    ToolCategory("Signatures", listOf(
        ToolItem("Create Signature", Icons.Default.Create, Color(0xFFF59E0B), route = "signature_create"),
        ToolItem("Manage Signatures", Icons.Default.CollectionsBookmark, Color(0xFFF59E0B), route = "signature_library"),
        ToolItem("Sign PDF", Icons.Default.PictureAsPdf, Color(0xFFF59E0B), route = "pdf_sign"),
        ToolItem("Sign Image", Icons.Default.Image, Color(0xFFF59E0B), route = "image_sign")
    )),
    ToolCategory("AI Magic", listOf(
        ToolItem("AI Summary", Icons.Default.AutoAwesome, IntelligentBlue, isNew = true),
        ToolItem("AI Rename", Icons.Default.EditNote, IntelligentBlue, isNew = true)
    ))
)

@Composable
fun ToolsTab(
    modifier: Modifier = Modifier,
    onToolClick: (ToolItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                "All Tools",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        toolCategories.forEach { category ->
            item {
                Text(
                    category.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Display in rows of 3
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val chunked = category.items.chunked(3)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { tool ->
                                ToolCard(
                                    tool = tool,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onToolClick(tool) }
                                )
                            }
                            // Fill empty spaces if the row has less than 3 items
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool: ToolItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = tool.color.copy(alpha = 0.1f))
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Surface(shape = CircleShape, color = tool.color.copy(alpha = 0.15f), modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(tool.icon, tool.name, tint = tool.color, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
            if (tool.isNew) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-6).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(IntelligentBlue, AIGlow)))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("NEW", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Text(
            tool.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
