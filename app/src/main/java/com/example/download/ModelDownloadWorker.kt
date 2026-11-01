package com.example.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class ModelDownloadTarget(
    val filename: String,
    val downloadUrl: String,
    val expectedSizeBytes: Long
)

class ModelDownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ModelDownloadWorker"
        const val CHANNEL_ID = "lfm3_model_download_channel"
        const val NOTIFICATION_ID = 2048

        const val KEY_FILE_NAME = "current_file"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_STATUS_MESSAGE = "status_message"

        val DOWNLOAD_TARGETS = listOf(
            ModelDownloadTarget(
                filename = "genai_config.json",
                downloadUrl = "https://huggingface.co/LiquidAI/LFM3-1.7B-ONNX/resolve/main/genai_config.json",
                expectedSizeBytes = 2048L
            ),
            ModelDownloadTarget(
                filename = "tokenizer.json",
                downloadUrl = "https://huggingface.co/LiquidAI/LFM3-1.7B-ONNX/resolve/main/tokenizer.json",
                expectedSizeBytes = 4_500_000L
            ),
            ModelDownloadTarget(
                filename = "model_q4f16.onnx",
                downloadUrl = "https://huggingface.co/LiquidAI/LFM3-1.7B-ONNX/resolve/main/model_q4f16.onnx",
                expectedSizeBytes = 1_480_000_000L
            )
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val targetDir = File(context.filesDir, "models/lfm3").apply { mkdirs() }

        createNotificationChannel()
        try {
            setForeground(createForegroundInfo("Starting LFM3-1.7B download...", 0))
        } catch (_: Exception) {
            // Foreground presentation optional on restricted profiles
        }

        val totalAllFilesBytes = DOWNLOAD_TARGETS.sumOf { it.expectedSizeBytes }
        var cumulativeDownloadedBytes = 0L

        for (target in DOWNLOAD_TARGETS) {
            val destinationFile = File(targetDir, target.filename)
            val partFile = File(targetDir, "${target.filename}.part")

            if (destinationFile.exists() && destinationFile.length() > 0) {
                cumulativeDownloadedBytes += destinationFile.length()
                continue
            }

            var downloadSuccess = false
            var attempts = 0
            var lastError: String? = null

            while (!downloadSuccess && attempts < 3) {
                attempts++
                try {
                    setProgressAsync(
                        workDataOf(
                            KEY_FILE_NAME to target.filename,
                            KEY_PROGRESS_PERCENT to ((cumulativeDownloadedBytes.toDouble() / totalAllFilesBytes.toDouble()) * 100).toInt().coerceIn(0, 99),
                            KEY_BYTES_DOWNLOADED to cumulativeDownloadedBytes,
                            KEY_TOTAL_BYTES to totalAllFilesBytes,
                            KEY_STATUS_MESSAGE to "Connecting to ${target.filename}..."
                        )
                    )

                    downloadFileResumableWithRedirects(target, partFile) { fileBytes ->
                        val currentTotal = cumulativeDownloadedBytes + fileBytes
                        val overallPercent = ((currentTotal.toDouble() / totalAllFilesBytes.toDouble()) * 100).toInt().coerceIn(0, 99)

                        setProgressAsync(
                            workDataOf(
                                KEY_FILE_NAME to target.filename,
                                KEY_PROGRESS_PERCENT to overallPercent,
                                KEY_BYTES_DOWNLOADED to currentTotal,
                                KEY_TOTAL_BYTES to totalAllFilesBytes,
                                KEY_STATUS_MESSAGE to "Downloading ${target.filename} ($overallPercent%)"
                            )
                        )
                    }

                    if (partFile.exists() && partFile.length() > 0) {
                        partFile.renameTo(destinationFile)
                        cumulativeDownloadedBytes += destinationFile.length()
                        downloadSuccess = true
                        Log.i(TAG, "Successfully downloaded: ${target.filename} (${destinationFile.length()} bytes)")
                    } else {
                        throw IllegalStateException("Downloaded file is empty")
                    }
                } catch (e: Exception) {
                    lastError = e.message
                    Log.w(TAG, "Download attempt $attempts failed for ${target.filename}: ${e.message}")
                    if (attempts >= 3) {
                        return@withContext Result.failure(
                            workDataOf(
                                KEY_STATUS_MESSAGE to "Download error: $lastError. Tap Resume or Launch Simulation.",
                                KEY_FILE_NAME to target.filename
                            )
                        )
                    }
                }
            }
        }

        Result.success(
            workDataOf(
                KEY_STATUS_MESSAGE to "All LFM3-1.7B model assets verified.",
                KEY_PROGRESS_PERCENT to 100
            )
        )
    }

    private fun downloadFileResumableWithRedirects(
        target: ModelDownloadTarget,
        partFile: File,
        onProgress: (Long) -> Unit
    ) {
        var existingBytes = if (partFile.exists()) partFile.length() else 0L

        var currentUrl = target.downloadUrl
        var connection: HttpURLConnection? = null
        var redirectCount = 0

        // Handle up to 5 HTTP 301/302/307/308 redirects from HuggingFace to CDN
        while (redirectCount < 5) {
            val url = URL(currentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; Sovereign/1.0)")
            conn.setRequestProperty("Accept", "*/*")

            if (existingBytes > 0) {
                conn.setRequestProperty("Range", "bytes=$existingBytes-")
            }

            conn.connect()
            val responseCode = conn.responseCode

            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 || responseCode == 308) {
                val newLocation = conn.getHeaderField("Location")
                conn.disconnect()
                if (newLocation != null) {
                    currentUrl = newLocation
                    redirectCount++
                    continue
                }
            }

            connection = conn
            break
        }

        val activeConn = connection ?: throw IllegalStateException("Could not establish connection to ${target.downloadUrl}")

        try {
            val responseCode = activeConn.responseCode

            // If server returns HTTP 200, Range header was ignored; restart file
            if (responseCode == HttpURLConnection.HTTP_OK) {
                existingBytes = 0L
                partFile.delete()
            } else if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Server returned HTTP $responseCode")
            }

            val raf = RandomAccessFile(partFile, "rw")
            raf.seek(existingBytes)

            var totalBytesWritten = existingBytes
            val buffer = ByteArray(64 * 1024)

            activeConn.inputStream.use { input ->
                var bytesRead: Int
                var lastProgressUpdate = System.currentTimeMillis()
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        raf.close()
                        activeConn.disconnect()
                        return
                    }
                    raf.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate > 300) {
                        onProgress(totalBytesWritten)
                        lastProgressUpdate = now
                    }
                }
            }
            raf.close()
            onProgress(totalBytesWritten)
        } finally {
            activeConn.disconnect()
        }
    }

    private fun createForegroundInfo(message: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Sovereign: LFM3-1.7B Neural Model")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background downloading of on-device language models"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
