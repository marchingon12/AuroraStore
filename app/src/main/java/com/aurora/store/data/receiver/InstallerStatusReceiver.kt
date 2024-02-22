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
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.util.CommonUtil.inForeground
import com.aurora.store.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
class InstallerStatusReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PREAPPROVAL_STATUS =
            "com.aurora.store.data.receiver.ACTION_PREAPPROVAL_STATUS"
        const val ACTION_INSTALL_STATUS =
            "com.aurora.store.data.receiver.InstallReceiver.INSTALL_STATUS"
    }

    private val TAG = InstallerStatusReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)

        if (intent.action == ACTION_INSTALL_STATUS) {
            val extra = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            // Exit early if package was successfully installed, nothing to do
            if (status == PackageInstaller.STATUS_SUCCESS) return

            if (inForeground() && status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                promptUser(intent, context)
            } else {
                postStatus(status, packageName, extra, context)
                notifyUser(context, packageName!!, status)
            }
        } else if (intent.action == ACTION_PREAPPROVAL_STATUS) {
            Log.i(TAG, "Pre-approval status for $packageName: $status")

            if (inForeground() && status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                promptUser(intent, context)
            }
        }
    }

    private fun notifyUser(context: Context, packageName: String, status: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationUtil.getInstallerStatusNotification(
            context,
            App(packageName),
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
