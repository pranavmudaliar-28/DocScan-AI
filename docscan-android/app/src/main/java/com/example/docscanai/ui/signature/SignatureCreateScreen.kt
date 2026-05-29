package com.example.docscanai.ui.signature

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.docscanai.data.local.DatabaseModule
import com.example.docscanai.data.local.SignatureEntity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureCreateScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { DatabaseModule.signatureRepository }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("DRAW", "TYPE", "UPLOAD")

    var signatureName by remember { mutableStateOf("My Signature") }
    
    // Draw state
    data class StrokeData(val path: Path, val color: Color, val width: Float)
    var paths by remember { mutableStateOf(listOf<StrokeData>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    val graphicsLayer = rememberGraphicsLayer()

    // Type state
    var typedSignature by remember { mutableStateOf("") }
    
    // Upload state
    var uploadedUri by remember { mutableStateOf<String?>(null) }
    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            uploadedUri = result.uriContent?.toString()
        }
    }

    fun saveSignature() {
        coroutineScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val fileName = "signature_$timestamp.png"
                val file = File(context.filesDir, fileName)
                
                val bitmap: Bitmap? = when (selectedTabIndex) {
                    0 -> { // DRAW
                        if (paths.isEmpty() && currentPath == null) {
                            Toast.makeText(context, "Please draw a signature", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        graphicsLayer.toImageBitmap().asAndroidBitmap()
                    }
                    1 -> { // TYPE
                        if (typedSignature.isEmpty()) {
                            Toast.makeText(context, "Please type a signature", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        // Render text to bitmap using graphicsLayer
                        graphicsLayer.toImageBitmap().asAndroidBitmap()
                    }
                    2 -> { // UPLOAD
                        if (uploadedUri == null) {
                            Toast.makeText(context, "Please upload a signature", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        // Read bitmap from URI (In a real app, you'd apply a transparency filter here)
                        val uri = android.net.Uri.parse(uploadedUri)
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    }
                    else -> null
                }

                if (bitmap != null) {
                    // Make it transparent if needed (simple approach: assumed transparent background)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    val entity = SignatureEntity(
                        name = signatureName,
                        type = tabs[selectedTabIndex],
                        imagePath = file.absolutePath
                    )
                    repository.insertSignature(entity)
                    Toast.makeText(context, "Signature Saved", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Signature") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = { saveSignature() }) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = signatureName,
                onValueChange = { signatureName = it },
                label = { Text("Signature Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            TabRow(
                selectedTabIndex = selectedTabIndex,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                when (selectedTabIndex) {
                    0 -> { // DRAW
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithContent {
                                    graphicsLayer.record {
                                        this@drawWithContent.drawContent()
                                    }
                                    drawLayer(graphicsLayer)
                                }
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentPath?.lineTo(change.position.x, change.position.y)
                                            },
                                            onDragEnd = {
                                                currentPath?.let { paths = paths + StrokeData(it, Color.Black, 8f) }
                                                currentPath = null
                                            }
                                        )
                                    }
                            ) {
                                paths.forEach { stroke ->
                                    drawPath(
                                        path = stroke.path,
                                        color = stroke.color,
                                        style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                                currentPath?.let { path ->
                                    drawPath(
                                        path = path,
                                        color = Color.Black,
                                        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                            
                            // Clear button
                            IconButton(
                                onClick = { paths = emptyList(); currentPath = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    1 -> { // TYPE
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithContent {
                                    graphicsLayer.record {
                                        this@drawWithContent.drawContent()
                                    }
                                    drawLayer(graphicsLayer)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            TextField(
                                value = typedSignature,
                                onValueChange = { typedSignature = it },
                                textStyle = TextStyle(
                                    fontSize = 48.sp,
                                    fontFamily = FontFamily.Cursive,
                                    color = Color.Black
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                placeholder = { Text("Type here...", style = TextStyle(fontFamily = FontFamily.Cursive, fontSize = 48.sp, color = Color.Gray)) }
                            )
                        }
                    }
                    2 -> { // UPLOAD
                        if (uploadedUri == null) {
                            Button(onClick = {
                                cropLauncher.launch(
                                    CropImageContractOptions(
                                        uri = null,
                                        cropImageOptions = CropImageOptions(
                                            imageSourceIncludeCamera = true,
                                            imageSourceIncludeGallery = true
                                        )
                                    )
                                )
                            }) {
                                Text("Upload Image")
                            }
                        } else {
                            AsyncImage(
                                model = uploadedUri,
                                contentDescription = "Uploaded Signature",
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                            IconButton(
                                onClick = { uploadedUri = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
