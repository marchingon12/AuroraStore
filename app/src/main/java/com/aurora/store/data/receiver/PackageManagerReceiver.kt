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
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.aurora.store.BuildConfig
import com.aurora.store.data.downloader.RequestGroupIdBuilder
import com.aurora.store.data.event.BusEvent.InstallEvent
import com.aurora.store.data.event.BusEvent.UninstallEvent
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.util.PathUtil
import com.aurora.store.util.Preferences
import org.greenrobot.eventbus.EventBus
import java.io.File

open class PackageManagerReceiver : BroadcastReceiver() {

    private val TAG = PackageManagerReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.data != null) {
            val packageName = intent.data!!.encodedSchemeSpecificPart

            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    EventBus.getDefault().post(InstallEvent(packageName, ""))

                    // Create app icon if user requests it
                    if (Preferences.getBoolean(context, Preferences.PREFERENCE_AUTO_INSTALL_ICON)) {
                        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                            requestShortcutCreation(context, packageName)
                        } else {
                            Log.i(TAG, "Launcher doesn't supports pinned shortcuts")
                        }
                    }
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    EventBus.getDefault().post(UninstallEvent(packageName, ""))
                }
            }

            //Clear installation queue
            AppInstaller.getInstance(context)
                .getPreferredInstaller()
                .removeFromInstallQueue(packageName)

            //clearNotification(context, packageName)

            val isAutoDeleteAPKEnabled = Preferences.getBoolean(
                context,
                Preferences.PREFERENCE_AUTO_DELETE
            )

            if (isAutoDeleteAPKEnabled)
                clearDownloads(context, packageName)

            //Clear self update apk
            if (packageName == BuildConfig.APPLICATION_ID)
                clearDownloads(context, packageName)
        }
    }

    private fun clearNotification(context: Context, packageName: String) {
        val notificationManager = context.applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val groupIDsOfPackageName = RequestGroupIdBuilder.getGroupIDsForApp(context, packageName.hashCode())
        groupIDsOfPackageName.forEach {
            notificationManager.cancel(packageName, it)
        }
    }

    private fun clearDownloads(context: Context, packageName: String) {
        try {
            val rootDirPath = PathUtil.getPackageDirectory(context, packageName)
            val rootDir = File(rootDirPath)
            if (rootDir.exists())
                rootDir.deleteRecursively()
        } catch (e: Exception) {

        }
    }

    private fun requestShortcutCreation(context: Context, packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val shortcutInfo = ShortcutInfoCompat.Builder(context, packageName)
                .setShortLabel(appInfo.loadLabel(context.packageManager))
                .setIcon(
                    IconCompat.createWithBitmap(appInfo.loadIcon(context.packageManager).toBitmap())
                )
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to request shortcut pin!", exception)
        }
    }
}
