package com.example.docscanai.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.docscanai.data.local.DatabaseModule
import com.example.docscanai.data.SupabaseModule
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncWorker", "Starting SyncWorker...")
            val db = DatabaseModule.database
            
            val timestamp = System.currentTimeMillis()

            // 1. Sync Folders
            val unsyncedFolders = db.folderDao().getUnsyncedFolders()
            if (unsyncedFolders.isNotEmpty()) {
                Log.d("SyncWorker", "Syncing ${unsyncedFolders.size} folders")
                SupabaseModule.client.postgrest["folders"].upsert(unsyncedFolders)
                db.folderDao().markAsSynced(unsyncedFolders.map { it.id }, timestamp)
            }

            // 2. Sync Tags
            val unsyncedTags = db.tagDao().getUnsyncedTags()
            if (unsyncedTags.isNotEmpty()) {
                Log.d("SyncWorker", "Syncing ${unsyncedTags.size} tags")
                SupabaseModule.client.postgrest["tags"].upsert(unsyncedTags)
                db.tagDao().markAsSynced(unsyncedTags.map { it.id }, timestamp)
            }

            // 3. Sync Documents
            val unsyncedDocs = db.documentDao().getUnsyncedDocuments()
            if (unsyncedDocs.isNotEmpty()) {
                Log.d("SyncWorker", "Syncing ${unsyncedDocs.size} documents metadata")
                // Note: physical file upload to Supabase Storage would happen here
                // For now, we sync the metadata to PostgreSQL
                SupabaseModule.client.postgrest["documents"].upsert(unsyncedDocs)
                db.documentDao().markAsSynced(unsyncedDocs.map { it.id }, timestamp)
            }

            Log.d("SyncWorker", "SyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error during sync: ${e.message}", e)
            Result.retry()
        }
    }
}
