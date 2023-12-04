package com.aurora.store.util

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.download.DownloadDao
import com.aurora.store.data.work.DownloadWorker
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

class DownloadWorkerUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val gson: Gson
) {

    companion object {
        const val DOWNLOAD_WORKER = "DOWNLOAD_WORKER"
        const val DOWNLOAD_DATA = "DOWNLOAD_DATA"

        const val DOWNLOAD_PROGRESS = "DOWNLOAD_PROGRESS"
        const val DOWNLOAD_TIME = "DOWNLOAD_TIME"
        const val DOWNLOAD_SPEED = "DOWNLOAD_SPEED"

        const val NOTIFICATION_ID = 200
    }

    private val TAG = DownloadWorkerUtil::class.java.simpleName

    private val DOWNLOAD_APP = "DOWNLOAD_APP"
    private val DOWNLOAD_UPDATE = "DOWNLOAD_UPDATE"
    private val PACKAGE_NAME = "PACKAGE_NAME"
    private val VERSION_CODE = "VERSION_CODE"

    private val cleanupStates = listOf(
        WorkInfo.State.CANCELLED,
        WorkInfo.State.FAILED
    )

    @OptIn(DelicateCoroutinesApi::class)
    fun init(applicationContext: Context) {
        // Run cleanup for last finished download and drop it database
        GlobalScope.launch {
            WorkManager.getInstance(applicationContext)
                .getWorkInfosByTagFlow(DOWNLOAD_WORKER)
                .collect { downloadsList->
                    try {
                        if (downloadsList.all { it.state.isFinished }) {
                            // Do cleanup for last download if required
                            downloadsList.lastOrNull()?.let { workInfo ->
                                val pkgTag = workInfo.tags.find { it.contains(PACKAGE_NAME) }
                                val versionTag = workInfo.tags.find { it.contains(VERSION_CODE) }

                                val pkgName = pkgTag?.split(":")?.get(1)
                                val version = versionTag?.split(":")?.get(1)?.toInt()

                                if (pkgName != null) {
                                    // Do cleanup if required
                                    if (version != null && workInfo.state in cleanupStates) {
                                        onFailure(applicationContext, pkgName, version)
                                    }

                                    Log.i(TAG, "Removing $pkgName from queue")
                                    downloadDao.delete(pkgName)
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        Log.i(DOWNLOAD_WORKER, "Failed to cleanup!", exception)
                    }
                }
        }

        // Trigger downloads if list is not empty
        GlobalScope.launch {
            downloadDao.downloads().collectLatest { dbList ->
                if (dbList.isNotEmpty()) {
                    try {
                        Log.i(DOWNLOAD_WORKER, "Downloading ${dbList.first().packageName}")
                        downloadApp(applicationContext, dbList.first())
                    } catch (exception: Exception) {
                        Log.i(DOWNLOAD_WORKER, "Failed to download enqueued apps", exception)
                    }
                }
            }
        }
    }

    suspend fun isEnqueued(packageName: String): Boolean {
        return downloadDao.getDownload(packageName) != null
    }

    suspend fun enqueueApp(app: App) {
        downloadDao.insert(Download.fromApp(app))
    }

    fun cancelDownload(packageName: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(packageName)
    }

    fun cancelAll(context: Context, downloads: Boolean = true, updates: Boolean = true) {
        val workManager = WorkManager.getInstance(context)

        if (downloads) workManager.cancelAllWorkByTag(DOWNLOAD_APP)
        if (updates) workManager.cancelAllWorkByTag(DOWNLOAD_UPDATE)
    }

    private fun downloadApp(context: Context, download: Download) {
        val inputData = Data.Builder()
            .putString(DOWNLOAD_DATA, gson.toJson(download))
            .build()

        val work = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag(DOWNLOAD_WORKER)
            .addTag("${PACKAGE_NAME}:${download.packageName}")
            .addTag("${VERSION_CODE}:${download.versionCode}")
            .addTag(if (download.isInstalled) DOWNLOAD_UPDATE else DOWNLOAD_APP)
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .setInputData(inputData)
            .build()

        // Ensure all app downloads are unique to preserve individual records
        WorkManager.getInstance(context)
            .enqueueUniqueWork("${DOWNLOAD_WORKER}/${download.packageName}",
                ExistingWorkPolicy.KEEP, work)
    }

    @OptIn(ExperimentalPathApi::class)
    private fun onFailure(context: Context, packageName: String, versionCode: Int) {
        Log.i(DOWNLOAD_WORKER, "Cleaning up!")
        PathUtil.getAppDownloadDir(context, packageName, versionCode)
            .deleteRecursively()
        with (context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager) {
            cancel(NOTIFICATION_ID)
        }
    }
}
