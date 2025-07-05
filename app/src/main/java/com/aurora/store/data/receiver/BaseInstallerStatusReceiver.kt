/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.aurora.store.data.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.EXTRA_SESSION_ID
import android.util.Log
import androidx.core.content.IntentCompat
import androidx.core.content.getSystemService
import com.aurora.extensions.runOnUiThread
import com.aurora.store.AuroraApp
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller.Companion.ACTION_INSTALL_STATUS
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_DISPLAY_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_PACKAGE_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_VERSION_CODE
import com.aurora.store.data.installer.base.InstallerBase
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE

abstract class BaseInstallerStatusReceiver : BroadcastReceiver() {

    private val TAG = BaseInstallerStatusReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == ACTION_INSTALL_STATUS) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
            val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: packageName

            val versionCode = intent.getLongExtra(EXTRA_VERSION_CODE, -1)
            val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            val extra = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            Log.i(
                TAG,
                "$packageName ($versionCode) sessionId=$sessionId, status=$status, extra=$extra"
            )

            // If package was successfully installed, exit after notifying user and doing cleanup
            if (status == PackageInstaller.STATUS_SUCCESS) {
                // No post-install steps for shared libraries
                if (PackageUtil.isSharedLibrary(context, packageName)) return

                AuroraApp.enqueuedInstalls.remove(packageName)
                InstallerBase.notifyInstallation(context, displayName, packageName)

                if (Preferences.getBoolean(context, PREFERENCE_AUTO_DELETE)) {
                    PathUtil.getAppDownloadDir(context, packageName, versionCode)
                        .deleteRecursively()
                }

                return postStatus(status, packageName, extra, context)
            }

            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                doAppropriatePrompt(context, intent, sessionId)
            } else {
                AuroraApp.enqueuedInstalls.remove(packageName)
                notifyUser(context, packageName, displayName, status)

                postStatus(status, packageName, extra, context)
            }
        }
    }

    private fun notifyUser(
        context: Context,
        packageName: String,
        displayName: String,
        status: Int
    ) {
        val notificationManager = context.getSystemService<NotificationManager>()
        val notification = NotificationUtil.getInstallerStatusNotification(
            context,
            packageName,
            displayName,
            InstallerBase.getErrorString(context, status)
        )
        notificationManager?.notify(packageName.hashCode(), notification)
    }

    internal fun promptUser(context: Context, intent: Intent) {
        runOnUiThread {
            val launchIntent = IntentCompat.getParcelableExtra(
                intent,
                Intent.EXTRA_INTENT,
                Intent::class.java
            )

            if (launchIntent != null) {
                launchIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                launchIntent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    context.startActivity(launchIntent)
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to launch intent!", exception)
                }
            } else {
                Log.w(TAG, "No launch intent found in the installation request.")
            }
        }
    }

    open fun doAppropriatePrompt(context: Context, intent: Intent, sessionId: Int) {
        promptUser(context, intent)
    }

    open fun postStatus(status: Int, packageName: String?, extra: String?, context: Context) {
        val event = when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                InstallerEvent.Installed(packageName!!).apply {
                    this.extra = context.getString(R.string.installer_status_success)
                }
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                InstallerEvent.Cancelled(packageName!!).apply {
                    this.extra = InstallerBase.getErrorString(context, status)
                }
            }

            else -> {
                InstallerEvent.Failed(packageName!!).apply {
                    this.error = InstallerBase.getErrorString(context, status)
                    this.extra = extra ?: ""
                }
            }
        }

        AuroraApp.events.send(event)
    }
}
