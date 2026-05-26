package com.example.docscanai.ui.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val placeholderImages = listOf(
    Pair("Receipt_Jan.jpg",  listOf(Color(0xFF1A2B4A), Color(0xFF0F1F38))),
    Pair("Contract_Q1.pdf",  listOf(Color(0xFF1E1050), Color(0xFF0F0A30))),
    Pair("Invoice_032.jpg",  listOf(Color(0xFF0A2830), Color(0xFF051820))),
    Pair("ID_Card.jpg",      listOf(Color(0xFF301020), Color(0xFF200A18))),
    Pair("License.jpg",      listOf(Color(0xFF102818), Color(0xFF081A10))),
    Pair("Passport.jpg",     listOf(Color(0xFF2A1A08), Color(0xFF1A1005))),
    Pair("Medical_Rec.pdf",  listOf(Color(0xFF1A1A3A), Color(0xFF101030))),
    Pair("Tax_2024.jpg",     listOf(Color(0xFF0A2820), Color(0xFF051A14))),
    Pair("Bank_Stmt.pdf",    listOf(Color(0xFF281010), Color(0xFF180808))),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryImportScreen(
    onBack: () -> Unit,
    onImageSelected: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Import from Gallery",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            // Primary action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Browse gallery
                Button(
                    onClick = onImageSelected,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Browse Gallery", style = MaterialTheme.typography.labelLarge)
                }

                // Camera shortcut
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        Icons.Default.CameraAlt, "Camera",
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Divider + label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "  Recent Photos  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Image grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(placeholderImages) { (name, colors) ->
                    ImageTile(name = name, gradient = colors, onClick = onImageSelected)
                }
            }
        }
    }
}

@Composable
private fun ImageTile(name: String, gradient: List<Color>, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.78f)
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
    ) {
        // Simulated document lines inside the tile
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index % 3 == 2) 0.65f else 1f)
                        .height(3.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
        }

        // File name label
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text     = name,
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
    }
}
