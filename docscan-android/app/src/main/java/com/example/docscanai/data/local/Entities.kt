package com.example.docscanai.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

import kotlinx.serialization.Serializable

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("folderId"), Index("categoryId")]
)
@Serializable
data class DocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val imageUri: String,
    val timestamp: Long = System.currentTimeMillis(),
    val documentType: String = "PDF", // PDF, JPG, PNG, DOCX, TXT, CSV
    val folderId: String? = null,
    val categoryId: String? = null,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val trashTimestamp: Long? = null,
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 1,
    val lastSyncedAt: Long? = null,
    val ocrText: String? = null
)

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
@Serializable
data class FolderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val colorCode: String? = null,
    val lastSyncedAt: Long? = null
)

@Entity(tableName = "tags")
@Serializable
data class TagEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorCode: String = "#3B82F6",
    val lastSyncedAt: Long? = null
)

@Entity(
    tableName = "document_tag_cross_ref",
    primaryKeys = ["docId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["docId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class DocumentTagCrossRef(
    val docId: String,
    val tagId: String
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g., Finance, Medical, Legal, Education
    val iconName: String? = null,
    val isPredefined: Boolean = false
)

@Entity(tableName = "signatures")
@Serializable
data class SignatureEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // DRAW, TYPE, UPLOAD
    val imagePath: String, // Path to transparent PNG
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
)

@Entity(
    tableName = "annotations",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
@Serializable
data class AnnotationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val pageNumber: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val annotationType: String, // TEXT, PEN, HIGHLIGHT, MARKER, RECTANGLE, CIRCLE, ARROW, LINE, STAMP
    val content: String? = null, // Text content for TEXT/STAMP
    val color: Int? = null,
    val strokeWidth: Float? = null,
    val pathData: String? = null // SVG path data for drawing tools
)
