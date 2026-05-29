package com.example.docscanai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE isTrashed = 0 ORDER BY timestamp DESC")
    fun getAllActiveDocuments(): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM documents 
        WHERE isTrashed = 0 
        AND (name LIKE '%' || :query || '%' OR ocrText LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE isFavorite = 1 AND isTrashed = 0 ORDER BY timestamp DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrashed = 1 ORDER BY trashTimestamp DESC")
    fun getTrashedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE folderId = :folderId AND isTrashed = 0 ORDER BY timestamp DESC")
    fun getDocumentsInFolder(folderId: String): Flow<List<DocumentEntity>>

    @Query("UPDATE documents SET folderId = :folderId WHERE id IN (:docIds)")
    suspend fun moveDocumentsToFolder(docIds: List<String>, folderId: String?)

    @Query("UPDATE documents SET isTrashed = :isTrashed, trashTimestamp = :timestamp WHERE id IN (:docIds)")
    suspend fun setTrashStatus(docIds: List<String>, isTrashed: Boolean, timestamp: Long?)

    @Query("DELETE FROM documents WHERE id IN (:docIds)")
    suspend fun permanentlyDeleteDocuments(docIds: List<String>)
    
    // Tag related operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDocumentTagCrossRef(crossRef: DocumentTagCrossRef)

    @Delete
    suspend fun deleteDocumentTagCrossRef(crossRef: DocumentTagCrossRef)

    @Query("""
        SELECT d.* FROM documents d 
        INNER JOIN document_tag_cross_ref ref ON d.id = ref.docId 
        WHERE ref.tagId = :tagId AND d.isTrashed = 0
        ORDER BY d.timestamp DESC
    """)
    fun getDocumentsByTag(tagId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE lastSyncedAt IS NULL OR timestamp > lastSyncedAt")
    suspend fun getUnsyncedDocuments(): List<DocumentEntity>

    @Query("UPDATE documents SET lastSyncedAt = :timestamp WHERE id IN (:docIds)")
    suspend fun markAsSynced(docIds: List<String>, timestamp: Long)
}

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY name ASC")
    fun getRootFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY name ASC")
    fun getSubFolders(parentId: String): Flow<List<FolderEntity>>
    
    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): FolderEntity?
    
    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    fun getFolderByIdFlow(id: String): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE lastSyncedAt IS NULL OR timestamp > lastSyncedAt")
    suspend fun getUnsyncedFolders(): List<FolderEntity>

    @Query("UPDATE folders SET lastSyncedAt = :timestamp WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, timestamp: Long)
}

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>
    
    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getTagById(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE lastSyncedAt IS NULL")
    suspend fun getUnsyncedTags(): List<TagEntity>

    @Query("UPDATE tags SET lastSyncedAt = :timestamp WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>, timestamp: Long)
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
}

@Dao
interface SignatureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureEntity)

    @Update
    suspend fun updateSignature(signature: SignatureEntity)

    @Delete
    suspend fun deleteSignature(signature: SignatureEntity)

    @Query("SELECT * FROM signatures ORDER BY createdAt DESC")
    fun getAllSignatures(): Flow<List<SignatureEntity>>

    @Query("SELECT * FROM signatures WHERE id = :id LIMIT 1")
    suspend fun getSignatureById(id: String): SignatureEntity?
}

@Dao
interface AnnotationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity)

    @Update
    suspend fun updateAnnotation(annotation: AnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)

    @Query("SELECT * FROM annotations WHERE documentId = :documentId AND pageNumber = :pageNumber")
    fun getAnnotationsForPage(documentId: String, pageNumber: Int): Flow<List<AnnotationEntity>>
}
