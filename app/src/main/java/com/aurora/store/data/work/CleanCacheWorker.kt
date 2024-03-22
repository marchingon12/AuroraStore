package com.aurora.store.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aurora.extensions.isMAndAbove
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltWorker
class CleanCacheWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "CleanCacheWorker"
        private const val CLEAN_CACHE_WORKER = "CLEAN_CACHE_WORKER"

        fun scheduleAutomatedCacheCleanup(context: Context) {
            Log.i(TAG, "Scheduling periodic app updates!")
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(CLEAN_CACHE_WORKER, KEEP, buildCleanCacheWork())
        }

        private fun buildCleanCacheWork(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)

            if (isMAndAbove()) constraints.setRequiresDeviceIdle(true)

            return PeriodicWorkRequestBuilder<CleanCacheWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = MINUTES
            ).setConstraints(constraints.build()).build()
        }
    }

    private val cacheDuration = Preferences.getInteger(appContext, PREFERENCE_UPDATES_CHECK_INTERVAL, 12)
        .toLong()
        .toDuration(DurationUnit.HOURS)

    override suspend fun doWork(): Result {
        Log.i(TAG, "Cleaning cache")

        PathUtil.getDownloadDirectory(appContext).listFiles()?.forEach { download -> // com.example.app
            // Delete if the download directory is empty
            if (download.listFiles().isNullOrEmpty()) {
                Log.i(TAG, "Removing empty download directory for ${download.name}")
                download.deleteRecursively(); return@forEach
            }

            download.listFiles()!!.forEach { versionCode -> // 20240325
                if (versionCode.listFiles().isNullOrEmpty()) {
                    // Purge empty non-accessible directory
                    Log.i(TAG, "Removing empty directory for ${download.name}, ${versionCode.name}")
                    versionCode.deleteRecursively()
                } else {
                    Log.i(TAG, "Removing old directory for ${download.name}, ${versionCode.name}")
                    versionCode.deleteIfOld()
                }
            }
        }

        return Result.success()
    }

    private fun File.deleteIfOld() {
        val elapsedTime = Calendar.getInstance().timeInMillis - this.lastModified()
        if (elapsedTime.toDuration(DurationUnit.HOURS) > cacheDuration) {
            this.deleteRecursively()
        }
    }
}
