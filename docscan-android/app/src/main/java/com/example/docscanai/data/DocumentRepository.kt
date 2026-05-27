package com.example.docscanai.data

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.time.Duration.Companion.hours

// ── Data classes ──────────────────────────────────────────────────────────────

@Serializable
data class DocumentRecord(
    val id: String = "",
    val ownerId: String = "",
    val filename: String = "",
    val s3Key: String = "",
    val mimeType: String = "",
    val sizeBytes: Int? = null,
    val status: String = "PENDING",
    val ocrBlocks: JsonElement = JsonArray(emptyList()),
    val originalDimensions: JsonElement = buildJsonObject { put("width", 0); put("height", 0) },
    val documentCategory: String? = null,
    val createdAt: String = "",
)

@Serializable
private data class NewDocument(
    val id: String,
    val ownerId: String,
    val filename: String,
    val s3Key: String,
    val mimeType: String,
    val sizeBytes: Int?,
    val status: String = "UPLOADED",
    val ocrBlocks: JsonElement = JsonArray(emptyList()),
    val originalDimensions: JsonElement = buildJsonObject { put("width", 0); put("height", 0) },
)

@Serializable
private data class OcrUpdate(
    val ocrBlocks: JsonElement,
    val originalDimensions: JsonElement,
    val documentCategory: String,
    val status: String,
)

// ── Repository ────────────────────────────────────────────────────────────────

object DocumentRepository {

    // ── Upload + OCR ─────────────────────────────────────────────────────────

    /**
     * Full pipeline:
     * 1. Upload file bytes to Supabase Storage
     * 2. Insert document record to `documents` table
     * 3. Run on-device ML Kit OCR
     * 4. Update document with OCR blocks + status COMPLETED
     * Returns the new document ID on success.
     */
    suspend fun uploadAndProcess(
        context: Context,
        uri: Uri,
        filename: String,
        mimeType: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        val userId = AuthRepository.getUserId()
        if (userId.isEmpty()) return@withContext Result.failure(Exception("Not authenticated"))

        try {
            // Read file bytes
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: return@withContext Result.failure(Exception("Cannot read file"))

            // 1. Upload to Supabase Storage
            val docId   = UUID.randomUUID().toString()
            val s3Key   = "users/$userId/$docId-$filename"
            SupabaseModule.client.storage
                .from(SupabaseConfig.DOCUMENTS_BUCKET)
                .upload(s3Key, bytes) { upsert = false }

            // 2. Insert document record
            SupabaseModule.client
                .from("documents")
                .insert(
                    NewDocument(
                        id       = docId,
                        ownerId  = userId,
                        filename = filename,
                        s3Key    = s3Key,
                        mimeType = mimeType,
                        sizeBytes = bytes.size,
                    )
                )

            // 3. Run on-device ML Kit OCR
            val (blocks, imgW, imgH) = OcrRepository.runLocalOcr(context, uri.toString(), mimeType)

            val dims = buildJsonObject {
                put("width",  imgW)
                put("height", imgH)
            }
            val category = when {
                mimeType.startsWith("image/")        -> if (blocks.size >= 5) "SCANNED_IMAGE" else "IMAGE"
                mimeType == "application/pdf"        -> "SCANNED_PDF"
                mimeType.contains("wordprocessing")  -> "DOCX"
                mimeType == "text/plain"             -> "TXT"
                else                                 -> "UNKNOWN"
            }

            // 4. Update with OCR results
            SupabaseModule.client
                .from("documents")
                .update(
                    OcrUpdate(
                        ocrBlocks          = Json.encodeToJsonElement(blocks),
                        originalDimensions = dims,
                        documentCategory   = category,
                        status             = "COMPLETED",
                    )
                ) {
                    filter { eq("id", docId) }
                }

            Result.success(docId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Library queries ───────────────────────────────────────────────────────

    suspend fun getUserDocuments(): Result<List<DocumentRecord>> = withContext(Dispatchers.IO) {
        try {
            val docs = SupabaseModule.client
                .from("documents")
                .select {
                    filter { eq("ownerId", AuthRepository.getUserId()) }
                    order("createdAt", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<DocumentRecord>()
            Result.success(docs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocument(docId: String): Result<DocumentRecord> = withContext(Dispatchers.IO) {
        try {
            val doc = SupabaseModule.client
                .from("documents")
                .select { filter { eq("id", docId) } }
                .decodeSingle<DocumentRecord>()
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(docId: String, s3Key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Remove file from Storage
            SupabaseModule.client.storage
                .from(SupabaseConfig.DOCUMENTS_BUCKET)
                .delete(listOf(s3Key))

            // Remove DB record (RLS ensures only the owner can delete)
            SupabaseModule.client
                .from("documents")
                .delete { filter { eq("id", docId) } }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Download URL ──────────────────────────────────────────────────────────

    /**
     * Returns a time-limited (1 hour) signed download URL for a document.
     * Call this right before the user taps "Download" or "Open".
     */
    suspend fun getDownloadUrl(s3Key: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = SupabaseModule.client.storage
                .from(SupabaseConfig.DOCUMENTS_BUCKET)
                .createSignedUrl(s3Key, expiresIn = 1.hours)
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Converted documents ───────────────────────────────────────────────────

    @Serializable
    data class ConversionRecord(
        val id: String = "",
        val ownerId: String = "",
        val originalDocumentId: String = "",
        val originalFilename: String = "",
        val convertedS3Key: String = "",
        val convertedMimeType: String = "",
        val convertedFormat: String = "",
        val convertedFilename: String = "",
        val status: String = "PENDING",
        val downloadUrl: String? = null,
        val editable: Boolean = false,
        val createdAt: String = "",
    )

    @Serializable
    private data class NewConversion(
        val id: String,
        val ownerId: String,
        val originalDocumentId: String,
        val originalFilename: String,
        val convertedS3Key: String,
        val convertedMimeType: String,
        val convertedFormat: String,
        val convertedFilename: String,
        val status: String = "PENDING",
        val editable: Boolean,
    )

    /**
     * Registers a conversion in the DB, uploads the converted file to Storage,
     * and marks the conversion as COMPLETED — all in one call.
     */
    suspend fun uploadConvertedDocument(
        context: Context,
        convertedUri: Uri,
        originalDocId: String,
        originalFilename: String,
        convertedFilename: String,
        convertedMimeType: String,
        convertedFormat: String,
    ): Result<ConversionRecord> = withContext(Dispatchers.IO) {
        val userId = AuthRepository.getUserId()
        if (userId.isEmpty()) return@withContext Result.failure(Exception("Not authenticated"))

        try {
            val bytes = context.contentResolver.openInputStream(convertedUri)?.readBytes()
                ?: return@withContext Result.failure(Exception("Cannot read converted file"))

            val convId = UUID.randomUUID().toString()
            val s3Key  = "users/$userId/converted/$convId-$convertedFilename"

            // Upload to Storage
            SupabaseModule.client.storage
                .from(SupabaseConfig.DOCUMENTS_BUCKET)
                .upload(s3Key, bytes) { upsert = false }

            // Generate download URL
            val downloadUrl = SupabaseModule.client.storage
                .from(SupabaseConfig.DOCUMENTS_BUCKET)
                .createSignedUrl(s3Key, expiresIn = 1.hours)

            // Insert + immediately mark COMPLETED
            val editable = listOf("pdf", "docx", "txt")
                .contains(convertedFormat.lowercase())

            SupabaseModule.client
                .from("converted_documents")
                .insert(
                    NewConversion(
                        id                 = convId,
                        ownerId            = userId,
                        originalDocumentId = originalDocId,
                        originalFilename   = originalFilename,
                        convertedS3Key     = s3Key,
                        convertedMimeType  = convertedMimeType,
                        convertedFormat    = convertedFormat.uppercase(),
                        convertedFilename  = convertedFilename,
                        status             = "COMPLETED",
                        editable           = editable,
                    )
                )

            Result.success(
                ConversionRecord(
                    id                 = convId,
                    ownerId            = userId,
                    originalDocumentId = originalDocId,
                    originalFilename   = originalFilename,
                    convertedS3Key     = s3Key,
                    convertedMimeType  = convertedMimeType,
                    convertedFormat    = convertedFormat.uppercase(),
                    convertedFilename  = convertedFilename,
                    status             = "COMPLETED",
                    downloadUrl        = downloadUrl,
                    editable           = editable,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserConversions(): Result<List<ConversionRecord>> = withContext(Dispatchers.IO) {
        try {
            val conversions = SupabaseModule.client
                .from("converted_documents")
                .select {
                    filter {
                        eq("ownerId", AuthRepository.getUserId())
                        eq("status", "COMPLETED")
                    }
                    order("createdAt", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<ConversionRecord>()
            Result.success(conversions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConversion(convId: String, s3Key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseModule.client.storage
                .from(SupabaseConfig.DOCUMENTS_BUCKET)
                .delete(listOf(s3Key))
            SupabaseModule.client
                .from("converted_documents")
                .delete { filter { eq("id", convId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
