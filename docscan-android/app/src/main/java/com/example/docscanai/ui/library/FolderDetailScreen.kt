package com.example.docscanai.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.docscanai.data.local.DatabaseModule
import com.example.docscanai.data.local.DocumentEntity
import com.example.docscanai.data.local.FolderEntity
import com.example.docscanai.data.local.LocalDocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FolderDetailViewModel(
    private val folderId: String,
    private val repository: LocalDocumentRepository
) : ViewModel() {

    private val folderFlow: Flow<FolderEntity?> = repository.getFolderById(folderId)
    private val docsFlow: Flow<List<DocumentEntity>> = repository.getDocumentsInFolder(folderId)

    val uiState: StateFlow<FolderDetailUiState> = combine(folderFlow, docsFlow) { folder, docs ->
        if (folder == null) {
            FolderDetailUiState.Error("Folder not found")
        } else {
            FolderDetailUiState.Success(folder, docs)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FolderDetailUiState.Loading)
}

class FolderDetailViewModelFactory(
    private val folderId: String,
    private val repository: LocalDocumentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FolderDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FolderDetailViewModel(folderId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed interface FolderDetailUiState {
    object Loading : FolderDetailUiState
    data class Error(val message: String) : FolderDetailUiState
    data class Success(val folder: FolderEntity, val docs: List<DocumentEntity>) : FolderDetailUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: String,
    onBack: () -> Unit,
    onDocumentClick: (DocumentEntity) -> Unit
) {
    val factory = remember { FolderDetailViewModelFactory(folderId, DatabaseModule.localDocumentRepository) }
    val viewModel: FolderDetailViewModel = viewModel(factory = factory)
    
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (state is FolderDetailUiState.Success) {
                        Text((state as FolderDetailUiState.Success).folder.name)
                    } else {
                        Text("Folder")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val s = state) {
                is FolderDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FolderDetailUiState.Error -> {
                    Text(s.message, modifier = Modifier.align(Alignment.Center))
                }
                is FolderDetailUiState.Success -> {
                    if (s.docs.isEmpty()) {
                        com.example.docscanai.ui.components.AnimatedEmptyState(
                            icon = Icons.Outlined.FolderOpen,
                            title = "No documents in this folder",
                            subtitle = "Add documents to organize your scans",
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(s.docs, key = { _, it -> it.id }) { index, doc ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(doc.id) {
                                    kotlinx.coroutines.delay(index * 50L)
                                    visible = true
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = visible,
                                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 4 }
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { onDocumentClick(doc) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(doc.name, fontWeight = FontWeight.Bold)
                                                Text(if (doc.id.startsWith("gallery")) "Image" else "Scan", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
