package com.example.docscanai.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.docscanai.data.DefaultDataRepository

private val DeepNavy = Color(0xFF0F172A)
private val IntelligentBlue = Color(0xFF3B82F6)
private val SlateLight = Color(0xFFCBD5E1)

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentScans = if (state is MainScreenUiState.Success) {
        (state as MainScreenUiState.Success).data
    } else emptyList()
    HomeScreen(recentScans = recentScans)
}

@Composable
internal fun HomeScreen(recentScans: List<String>, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { AppHeader() }
            item {
                Spacer(Modifier.height(4.dp))
                ScanButton()
            }
            item {
                Spacer(Modifier.height(14.dp))
                QuickActionsRow()
            }
            item {
                Spacer(Modifier.height(24.dp))
                RecentScansSection(recentScans)
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.Baseline) {
                Text("DocScan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("AI", fontSize = 22.sp, fontWeight = FontWeight.Light, color = IntelligentBlue)
            }
            Text(
                "Scan & analyze documents",
                fontSize = 12.sp,
                color = SlateLight.copy(alpha = 0.6f)
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .clickable {},
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, "Settings", tint = SlateLight, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ScanButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    listOf(IntelligentBlue.copy(alpha = 0.25f), Color(0xFF112240)),
                    radius = 520f
                )
            )
            .clickable {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            border = BorderStroke(1.5.dp, IntelligentBlue.copy(alpha = 0.35f))
        ) {}
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(IntelligentBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Scan",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Tap to Scan Document",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Camera · Auto-detect · AI extraction",
                fontSize = 12.sp,
                color = SlateLight.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun QuickActionsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(Icons.Default.Image, "Gallery", "Import image", Modifier.weight(1f))
        QuickActionCard(Icons.Default.Description, "PDF", "Open file", Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable {}
            .padding(16.dp)
    ) {
        Column {
            Icon(icon, label, tint = IntelligentBlue, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = SlateLight.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun RecentScansSection(scans: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text("Recent Scans", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        if (scans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No scans yet — tap the button above to start",
                    fontSize = 13.sp,
                    color = SlateLight.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        } else {
            scans.forEach { scan ->
                ScanItem(scan)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ScanItem(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable {}
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(IntelligentBlue.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, null, tint = IntelligentBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(name, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
