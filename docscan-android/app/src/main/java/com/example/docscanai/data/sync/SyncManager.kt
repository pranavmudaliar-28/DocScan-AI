package com.example.docscanai.data.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncManager {
    private const val SYNC_WORK_NAME = "CloudSyncWork"

    fun setupPeriodicSync(context: Context, autoSync: Boolean, wifiOnly: Boolean) {
        val workManager = WorkManager.getInstance(context)
        
        if (!autoSync) {
            workManager.cancelUniqueWork(SYNC_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    fun triggerOneTimeSync(context: Context, wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "OneTimeSyncWork",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
