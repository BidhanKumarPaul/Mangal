package com.bkpit.mangal.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Real, working resumable download: writes to a .part file, uses an HTTP
 * Range header to resume from wherever a previous attempt left off (Wi-Fi
 * drop, app killed, etc — WorkManager's own constraint system decides when
 * to retry), verifies SHA-256 against ModelCatalogEntry.sha256, and only
 * then renames .part -> final filename. A failed checksum deletes the
 * partial file rather than leaving a corrupt model silently in place.
 */
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_ERROR = "error"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?: return@withContext Result.failure(workDataOf(KEY_ERROR to "missing model_id"))
        val entry = ModelCatalog.entries.firstOrNull { it.id == modelId }
            ?: return@withContext Result.failure(workDataOf(KEY_ERROR to "unknown model_id $modelId"))

        val modelsDir = File(applicationContext.filesDir, "models").apply { mkdirs() }
        val finalFile = File(modelsDir, "${entry.id}.bin")
        val partFile = File(modelsDir, "${entry.id}.bin.part")

        if (finalFile.exists()) return@withContext Result.success()

        return@withContext try {
            downloadWithResume(entry, partFile)
            val actualHash = sha256Of(partFile)
            if (entry.sha256 == "REPLACE_WITH_REAL_SHA256") {
                // Catalog placeholder — see ModelCatalog.kt. Can't verify against
                // a checksum that was never filled in; fail loudly instead of
                // pretending this passed.
                partFile.delete()
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "No real checksum configured for ${entry.id} in ModelCatalog — fill it in before this can pass verification")
                )
            }
            if (!actualHash.equals(entry.sha256, ignoreCase = true)) {
                partFile.delete()
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "Checksum mismatch for ${entry.id}: expected ${entry.sha256}, got $actualHash")
                )
            }
            partFile.renameTo(finalFile)
            Result.success()
        } catch (e: Exception) {
            // Leave the .part file in place — next run resumes from where
            // this attempt stopped, instead of restarting the whole download.
            Result.retry()
        }
    }

    private suspend fun downloadWithResume(entry: ModelCatalogEntry, partFile: File) {
        val existingBytes = if (partFile.exists()) partFile.length() else 0L

        val request = Request.Builder()
            .url(entry.downloadUrl)
            .header("Range", "bytes=$existingBytes-")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("HTTP ${response.code} for ${entry.downloadUrl}")
            }
            // Server must honor the Range header (206) for a true resume. If
            // it ignores Range and sends the whole file again (200), we must
            // start the .part file over or we'd duplicate the first bytes.
            val serverHonoredRange = response.code == 206
            val startOffset = if (serverHonoredRange) existingBytes else 0L
            val body = response.body ?: throw java.io.IOException("Empty response body")
            val totalBytes = startOffset + (body.contentLength().takeIf { it > 0 } ?: 0L)

            RandomAccessFile(partFile, "rw").use { raf ->
                if (!serverHonoredRange) raf.setLength(0)
                raf.seek(startOffset)
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = startOffset
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        raf.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val percent = ((downloaded * 100) / totalBytes).toInt()
                            setProgressAsync(workDataOf(KEY_PROGRESS_PERCENT to percent))
                        }
                    }
                }
            }
        }
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
