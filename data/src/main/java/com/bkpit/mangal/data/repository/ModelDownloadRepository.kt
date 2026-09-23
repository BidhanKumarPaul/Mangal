package com.bkpit.mangal.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bkpit.mangal.data.download.ModelCatalog
import com.bkpit.mangal.data.download.ModelDownloadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class ModelDownloadRepository(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun enqueueDownload(modelId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi only, per spec
            .build()

        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(ModelDownloadWorker.KEY_MODEL_ID to modelId))
            .setConstraints(constraints)
            .addTag(tagFor(modelId))
            .build()

        // Unique work per model id: re-enqueuing (e.g. user re-opens Model
        // Manager mid-download) attaches to the same job instead of starting
        // a duplicate download.
        workManager.enqueueUniqueWork(modelId, androidx.work.ExistingWorkPolicy.KEEP, request)
    }

    fun cancelDownload(modelId: String) = workManager.cancelUniqueWork(modelId)

    fun observeProgress(modelId: String): Flow<DownloadState> =
        workManager.getWorkInfosForUniqueWorkFlow(modelId).map { infos ->
            val info = infos.firstOrNull() ?: return@map DownloadState.NotStarted
            when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.Queued
                WorkInfo.State.RUNNING -> {
                    val percent = info.progress.getInt(ModelDownloadWorker.KEY_PROGRESS_PERCENT, 0)
                    DownloadState.Downloading(percent)
                }
                WorkInfo.State.SUCCEEDED -> DownloadState.Complete
                WorkInfo.State.FAILED -> DownloadState.Failed(
                    info.outputData.getString(ModelDownloadWorker.KEY_ERROR) ?: "Unknown error"
                )
                WorkInfo.State.CANCELLED -> DownloadState.NotStarted
            }
        }

    fun localFileFor(modelId: String): File? {
        val entry = ModelCatalog.entries.firstOrNull { it.id == modelId } ?: return null
        val file = File(File(context.filesDir, "models"), "${entry.id}.bin")
        return file.takeIf { it.exists() }
    }

    fun delete(modelId: String): Boolean {
        val entry = ModelCatalog.entries.firstOrNull { it.id == modelId } ?: return false
        val file = File(File(context.filesDir, "models"), "${entry.id}.bin")
        return !file.exists() || file.delete()
    }

    private fun tagFor(modelId: String) = "model_download_$modelId"
}

sealed class DownloadState {
    object NotStarted : DownloadState()
    object Queued : DownloadState()
    data class Downloading(val percent: Int) : DownloadState()
    object Complete : DownloadState()
    data class Failed(val reason: String) : DownloadState()
}
