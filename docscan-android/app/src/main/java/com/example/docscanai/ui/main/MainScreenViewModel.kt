package com.example.docscanai.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docscanai.data.local.DocumentEntity
import com.example.docscanai.data.local.LocalDocumentRepository
import com.example.docscanai.ui.main.MainScreenUiState.Success
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import com.example.docscanai.data.local.FolderEntity
import com.example.docscanai.data.local.TagEntity

enum class SortOrder {
    NAME_ASC, NAME_DESC, DATE_CREATED_ASC, DATE_CREATED_DESC, DATE_MODIFIED_ASC, DATE_MODIFIED_DESC, SIZE_ASC, SIZE_DESC
}

enum class FilterType {
    ALL, PDF, IMAGE, DOCX, TXT, CSV
}

class MainScreenViewModel(private val repository: LocalDocumentRepository) : ViewModel() {
    private val _sortOrder = kotlinx.coroutines.flow.MutableStateFlow(SortOrder.DATE_MODIFIED_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder
    
    private val _filterType = kotlinx.coroutines.flow.MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType

    val uiState: StateFlow<MainScreenUiState> = combine(
        repository.getAllActiveDocuments(),
        repository.getRootFolders(),
        repository.getAllTags(),
        _sortOrder,
        _filterType
    ) { docs, folders, tags, sort, filter ->
        var filteredDocs = docs
        if (filter != FilterType.ALL) {
            filteredDocs = filteredDocs.filter { doc -> 
                val isImage = doc.id.startsWith("gallery")
                when(filter) {
                    FilterType.IMAGE -> isImage
                    FilterType.PDF -> !isImage // Assuming scans are PDF logic initially
                    else -> true
                }
            }
        }
        
        val sortedDocs = when (sort) {
            SortOrder.NAME_ASC -> filteredDocs.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> filteredDocs.sortedByDescending { it.name.lowercase() }
            SortOrder.DATE_CREATED_ASC -> filteredDocs.sortedBy { it.timestamp }
            SortOrder.DATE_CREATED_DESC -> filteredDocs.sortedByDescending { it.timestamp }
            SortOrder.DATE_MODIFIED_ASC -> filteredDocs.sortedBy { it.timestamp }
            SortOrder.DATE_MODIFIED_DESC -> filteredDocs.sortedByDescending { it.timestamp }
            SortOrder.SIZE_ASC -> filteredDocs.sortedBy { it.fileSizeBytes }
            SortOrder.SIZE_DESC -> filteredDocs.sortedByDescending { it.fileSizeBytes }
        }
        
        MainScreenUiState.Success(sortedDocs, folders, tags) as MainScreenUiState
    }
    .catch { emit(MainScreenUiState.Error(it)) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)
    
    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }
    fun setFilterType(type: FilterType) { _filterType.value = type }

    private val _selectedDocIds = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    val selectedDocIds: StateFlow<Set<String>> = _selectedDocIds

    fun toggleSelection(docId: String) {
        val current = _selectedDocIds.value
        if (current.contains(docId)) {
            _selectedDocIds.value = current - docId
        } else {
            _selectedDocIds.value = current + docId
        }
    }

    fun clearSelection() {
        _selectedDocIds.value = emptySet()
    }

    fun deleteSelectedDocuments() {
        val ids = _selectedDocIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveToTrash(ids)
            clearSelection()
        }
    }

    fun moveSelectedToFolder(folderId: String?) {
        val ids = _selectedDocIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveDocumentsToFolder(ids, folderId)
            clearSelection()
        }
    }

    suspend fun createFolder(name: String) {
        val folderId = "folder_" + java.util.UUID.randomUUID().toString()
        repository.insertFolder(
            FolderEntity(
                id = folderId,
                name = name,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun createTag(name: String, colorHex: String = "#3B82F6") {
        val tagId = "tag_" + java.util.UUID.randomUUID().toString()
        repository.insertTag(
            TagEntity(
                id = tagId,
                name = name,
                colorCode = colorHex
            )
        )
    }
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(
        val data: List<DocumentEntity>,
        val folders: List<FolderEntity> = emptyList(),
        val tags: List<TagEntity> = emptyList()
    ) : MainScreenUiState
}
