/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2023, grrfe <grrfe@420blaze.it>
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

package com.aurora.store.data.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.PACKAGE_SOURCE_STORE
import android.content.pm.PackageInstaller.PreapprovalDetails
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.icu.util.ULocale
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import com.aurora.extensions.isNAndAbove
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isSAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.extensions.isUAndAbove
import com.aurora.store.data.receiver.InstallerStatusReceiver
import com.aurora.store.data.receiver.InstallerStatusReceiver.Companion.ACTION_INSTALL_STATUS
import com.aurora.store.data.receiver.InstallerStatusReceiver.Companion.ACTION_PREAPPROVAL_STATUS
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.Log
import com.aurora.store.util.PackageUtil.isSharedLibraryInstalled
import java.net.URL
import kotlin.properties.Delegates

class SessionInstaller(context: Context) : InstallerBase(context) {

    companion object {

        fun createSession(context: Context, packageName: String): Int {
            val sessionParams = getSessionParams(context, packageName)
            return context.packageManager.packageInstaller.createSession(sessionParams)
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun requestUserPreapproval(
            context: Context,
            packageName: String,
            displayName: String,
            iconURL: String,
            sessionId: Int
        ) {
            val packageInstaller = context.packageManager.packageInstaller.openSession(sessionId)
            val preapprovalDetails = PreapprovalDetails.Builder()
                .setIcon(BitmapFactory.decodeStream(URL(iconURL).openStream()))
                .setLabel(displayName)
                .setLocale(ULocale.getDefault())
                .setPackageName(packageName)
                .build()
            packageInstaller.requestUserPreapproval(
                preapprovalDetails,
                getCallBackIntent(context, packageName, ACTION_PREAPPROVAL_STATUS).intentSender
            )
        }

        private fun getSessionParams(context: Context, packageName: String): SessionParams {
            return SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(packageName)
                if (isOAndAbove()) {
                    setInstallReason(PackageManager.INSTALL_REASON_USER)
                }
                if (isNAndAbove()) {
                    setOriginatingUid(Process.myUid())
                }
                if (isSAndAbove()) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                if (isTAndAbove()) {
                    setPackageSource(PACKAGE_SOURCE_STORE)
                }
                if (isUAndAbove()) {
                    setInstallerPackageName(context.packageName)
                    setRequestUpdateOwnership(true)
                }
            }
        }

        private fun getCallBackIntent(
            context: Context,
            packageName: String,
            installAction: String,
            parentSessionId: Int = -1
        ): PendingIntent {
            val callBackIntent = Intent(context, InstallerStatusReceiver::class.java).apply {
                action = installAction
                setPackage(context.packageName)
                putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, packageName)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            val flags = if (isSAndAbove()) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            return PendingIntent.getBroadcast(context, parentSessionId, callBackIntent, flags)
        }
    }

    var parentSessionId by Delegates.notNull<Int>()

    private val packageInstaller = context.packageManager.packageInstaller
    private val sessionIdMap = mutableMapOf<Int, String>()

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i("${download.packageName} already queued")
        } else {
            Log.i("Received session install request for ${download.packageName}")

            val callback = object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {}

                override fun onBadgingChanged(sessionId: Int) {}

                override fun onActiveChanged(sessionId: Int, active: Boolean) {}

                override fun onProgressChanged(sessionId: Int, progress: Float) {}

                override fun onFinished(sessionId: Int, success: Boolean) {
                    if (sessionId in sessionIdMap.keys && success) {
                        sessionIdMap.remove(sessionId)
                        if (sessionIdMap.isNotEmpty()) {
                            val nextSession = sessionIdMap.keys.first()
                            commitInstall(sessionIdMap.getValue(nextSession), nextSession)
                        } else {
                            packageInstaller.unregisterSessionCallback(this)
                        }
                    }
                }
            }

            download.sharedLibs.forEach {
                // Shared library packages cannot be updated
                if (!isSharedLibraryInstalled(context, it.packageName, it.versionCode)) {
                    stageInstall(
                        download.packageName,
                        download.versionCode,
                        it.sessionId,
                        it.packageName
                    )
                }
            }
            stageInstall(download.packageName, download.versionCode, download.sessionId)

            if (sessionIdMap.size > 1) packageInstaller.registerSessionCallback(callback)
            commitInstall(
                sessionIdMap.getValue(sessionIdMap.keys.first()),
                sessionIdMap.keys.first()
            )
        }
    }

    private fun stageInstall(
        packageName: String,
        versionCode: Int,
        existingSessionId: Int?,
        sharedLibPkgName: String = "",
    ) {
        val packageInstaller = context.packageManager.packageInstaller

        // Verify if there is already an existing session for the package
        val sessionId = if (existingSessionId != null && packageInstaller.getSessionInfo(existingSessionId) != null) {
            existingSessionId
        } else {
            packageInstaller.createSession(
                getSessionParams(context, sharedLibPkgName.ifBlank { packageName })
            )
        }
        val session = packageInstaller.openSession(sessionId)

        try {
            Log.i("Writing splits to session for $packageName")
            getFiles(packageName, versionCode, sharedLibPkgName).forEach {
                it.inputStream().use { input ->
                    session.openWrite("${sharedLibPkgName.ifBlank { packageName }}_${System.currentTimeMillis()}", 0, -1).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
            }

            // Add session to list of staged sessions
            sessionIdMap[sessionId] = packageName
            if (sharedLibPkgName.isBlank()) parentSessionId = sessionId
        } catch (exception: Exception) {
            session.abandon()
            removeFromInstallQueue(packageName)
            postError(packageName, exception.localizedMessage, exception.stackTraceToString())
        }
    }

    private fun commitInstall(packageName: String, sessionId: Int) {
        Log.i("Starting install session for $packageName")
        val session = packageInstaller.openSession(sessionId)
        session.commit(
            getCallBackIntent(
                context,
                packageName,
                ACTION_INSTALL_STATUS,
                parentSessionId
            ).intentSender
        )
        session.close()
    }
}
