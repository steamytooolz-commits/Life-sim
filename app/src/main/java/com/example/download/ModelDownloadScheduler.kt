package com.example.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import java.io.File

object ModelDownloadScheduler {
    const val UNIQUE_WORK_NAME = "lfm3_model_download_work"

    fun hasCompleteModel(context: Context): Boolean {
        val dir = File(context.filesDir, "models/lfm3")
        val onnx = File(dir, "model_q4f16.onnx")
        val config = File(dir, "genai_config.json")
        val tokenizer = File(dir, "tokenizer.json")
        return onnx.exists() && onnx.length() > 0 &&
               config.exists() && config.length() > 0 &&
               tokenizer.exists() && tokenizer.length() > 0
    }

    fun startDownload(context: Context, requireUnmetered: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requireUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .addTag("LFM3_DOWNLOAD")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun retryDownload(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .addTag("LFM3_DOWNLOAD")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun observeDownloadWork(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)
    }

    fun cancelDownload(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
