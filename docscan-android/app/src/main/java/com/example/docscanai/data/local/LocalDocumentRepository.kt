package com.example.docscanai.data.local

import kotlinx.coroutines.flow.Flow
class LocalDocumentRepository(
    private val documentDao: DocumentDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val categoryDao: CategoryDao
) {
    // ── Documents ────────────────────────────────────────────────────────────

    fun getAllActiveDocuments(): Flow<List<DocumentEntity>> = documentDao.getAllActiveDocuments()

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> = documentDao.searchDocuments(query)

    fun getFavoriteDocuments(): Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments()

    fun getTrashedDocuments(): Flow<List<DocumentEntity>> = documentDao.getTrashedDocuments()

    fun getDocumentsInFolder(folderId: String): Flow<List<DocumentEntity>> = documentDao.getDocumentsInFolder(folderId)
    
    fun getDocumentsByTag(tagId: String): Flow<List<DocumentEntity>> = documentDao.getDocumentsByTag(tagId)

    suspend fun getDocumentById(id: String): DocumentEntity? = documentDao.getDocumentById(id)

    suspend fun insertDocument(document: DocumentEntity) {
        documentDao.insertDocument(document)
    }

    suspend fun updateDocument(document: DocumentEntity) {
        documentDao.updateDocument(document)
    }

    suspend fun toggleFavorite(docId: String) {
        val doc = documentDao.getDocumentById(docId)
        if (doc != null) {
            documentDao.updateDocument(doc.copy(isFavorite = !doc.isFavorite))
        }
    }

    // ── Trash & Bulk Actions ─────────────────────────────────────────────────

    suspend fun moveToTrash(docIds: List<String>) {
        documentDao.setTrashStatus(docIds, isTrashed = true, timestamp = System.currentTimeMillis())
    }

    suspend fun restoreFromTrash(docIds: List<String>) {
        documentDao.setTrashStatus(docIds, isTrashed = false, timestamp = null)
    }

    suspend fun permanentlyDelete(docIds: List<String>) {
        documentDao.permanentlyDeleteDocuments(docIds)
    }
    
    suspend fun moveDocumentsToFolder(docIds: List<String>, folderId: String?) {
        documentDao.moveDocumentsToFolder(docIds, folderId)
    }

    // ── Folders ──────────────────────────────────────────────────────────────

    fun getRootFolders(): Flow<List<FolderEntity>> = folderDao.getRootFolders()

    fun getSubFolders(parentId: String): Flow<List<FolderEntity>> = folderDao.getSubFolders(parentId)
    
    fun getFolderById(id: String): Flow<FolderEntity?> = folderDao.getFolderByIdFlow(id)

    suspend fun insertFolder(folder: FolderEntity) {
        folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        folderDao.deleteFolder(folder)
    }

    // ── Tags ─────────────────────────────────────────────────────────────────

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun insertTag(tag: TagEntity) {
        tagDao.insertTag(tag)
    }
    
    suspend fun addTagToDocument(docId: String, tagId: String) {
        documentDao.insertDocumentTagCrossRef(DocumentTagCrossRef(docId, tagId))
    }
    
    suspend fun removeTagFromDocument(docId: String, tagId: String) {
        documentDao.deleteDocumentTagCrossRef(DocumentTagCrossRef(docId, tagId))
    }

    // ── Categories ───────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }
}
