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
import android.util.Log
import androidx.core.content.IntentCompat
import com.aurora.extensions.runOnUiThread
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.installer.AppInstaller.Companion.ACTION_INSTALL_STATUS
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_DISPLAY_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_PACKAGE_NAME
import com.aurora.store.data.installer.AppInstaller.Companion.EXTRA_VERSION_CODE
import com.aurora.store.util.CommonUtil.inForeground
import com.aurora.store.util.NotificationUtil
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
class InstallerStatusReceiver : BroadcastReceiver() {

    private val TAG = InstallerStatusReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == ACTION_INSTALL_STATUS) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)!!
            val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME)!!
            val versionCode = intent.getIntExtra(EXTRA_VERSION_CODE, -1)

            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            val extra = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            // If package was successfully installed, exit after notifying user and doing cleanup
            if (status == PackageInstaller.STATUS_SUCCESS) {
                // No post-install steps for shared libraries
                if (PackageUtil.isSharedLibrary(context, packageName)) return

                AppInstaller.notifyInstallation(context, displayName, packageName)
                if (Preferences.getBoolean(context, PREFERENCE_AUTO_DELETE)) {
                    PathUtil.getAppDownloadDir(context, packageName, versionCode)
                        .deleteRecursively()
                }
                return
            }

            if (inForeground() && status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                promptUser(intent, context)
            } else {
                postStatus(status, packageName, extra, context)
                notifyUser(context, packageName, displayName, status)
            }
        }
    }

    private fun notifyUser(context: Context, packageName: String, displayName: String, status: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationUtil.getInstallerStatusNotification(
            context,
            packageName,
            displayName,
            AppInstaller.getErrorString(context, status)
        )
        notificationManager.notify(packageName.hashCode(), notification)
    }

    private fun promptUser(intent: Intent, context: Context) {
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let {
            it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                runOnUiThread { context.startActivity(it) }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to trigger installation!", exception)
            }
        }
    }

    private fun postStatus(status: Int, packageName: String?, extra: String?, context: Context) {
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                EventBus.getDefault().post(
                    InstallerEvent.Success(
                        packageName,
                        context.getString(R.string.installer_status_success)
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                EventBus.getDefault().post(
                    InstallerEvent.Cancelled(
                        packageName,
                        AppInstaller.getErrorString(context, status)
                    )
                )
            }

            else -> {
                EventBus.getDefault().post(
                    InstallerEvent.Failed(
                        packageName,
                        AppInstaller.getErrorString(context, status),
                        extra
                    )
                )
            }
        }
    }
}
