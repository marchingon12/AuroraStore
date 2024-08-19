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

package com.aurora.store.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.SharedLibraryInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import com.aurora.extensions.getInstallerPackageNameCompat
import com.aurora.extensions.isApp
import com.aurora.extensions.isOAndAbove
import com.aurora.extensions.isPAndAbove
import com.aurora.extensions.isTAndAbove
import com.aurora.store.BuildConfig

object PackageUtil {

    private const val TAG = "PackageUtil"

    fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            getPackageInfo(context, packageName, PackageManager.GET_META_DATA)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isInstalled(context: Context, packageName: String, versionCode: Int): Boolean {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            return PackageInfoCompat.getLongVersionCode(packageInfo) >= versionCode.toLong()
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isSharedLibrary(context: Context, packageName: String): Boolean {
        return if (isOAndAbove()) {
            getAllSharedLibraries(context).any { it.name == packageName }
        } else {
            false
        }
    }

    fun isSharedLibraryInstalled(context: Context, packageName: String, versionCode: Int): Boolean {
        return if (isOAndAbove()) {
            val sharedLibraries = getAllSharedLibraries(context)
            if (isPAndAbove()) {
                sharedLibraries.any {
                    it.name == packageName && it.longVersion == versionCode.toLong()
                }
            } else {
                sharedLibraries.any {
                    @Suppress("DEPRECATION")
                    it.name == packageName && it.version == versionCode
                }
            }
        } else {
            false
        }
    }

    fun isUpdatable(context: Context, packageName: String, versionCode: Long): Boolean {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            return versionCode > PackageInfoCompat.getLongVersionCode(packageInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledVersion(context: Context, packageName: String): String {
        return try {
            val packageInfo = getPackageInfo(context, packageName)
            "${packageInfo.versionName} (${PackageInfoCompat.getLongVersionCode(packageInfo)})"
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun isTv(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    fun getLaunchIntent(context: Context, packageName: String?): Intent? {
        val intent = if (isTv(context)) {
            context.packageManager.getLeanbackLaunchIntentForPackage(packageName!!)
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName!!)
        }

        return if (intent == null) {
            null
        } else {
            intent.addCategory(if (isTv(context)) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
            intent
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getStorageManagerIntent(context: Context): Intent {
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        )

        // Check if the intent can be resolved
        val packageManager = context.packageManager
        val isIntentAvailable = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        ).isNotEmpty()

        return if (isIntentAvailable) {
            intent
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    }

    fun getInstallUnknownAppsIntent(): Intent {
        return if (isOAndAbove()) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (isOAndAbove()) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            @Suppress("DEPRECATION")
            val secureResult = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS, 0
            )

            return secureResult == 1
        }
    }

    @Throws(Exception::class)
    fun getPackageInfo(context: Context, packageName: String, flags: Int = 0): PackageInfo {
        return if (isTAndAbove()) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageInfoFlags.of(flags.toLong())
            )
        } else {
            context.packageManager.getPackageInfo(packageName, flags)
        }
    }

    fun getIconForPackage(context: Context, packageName: String): Bitmap? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val icon = packageInfo.applicationInfo!!.loadIcon(context.packageManager)
            if (icon.intrinsicWidth > 0 && icon.intrinsicHeight > 0) {
                icon.toBitmap(96, 96)
            } else {
                context.packageManager.defaultActivityIcon.toBitmap(96, 96)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get icon for package!", exception)
            null
        }
    }

    private fun getAllSharedLibraries(context: Context, flags: Int = 0): List<SharedLibraryInfo> {
        return if (isTAndAbove()) {
            context.packageManager.getSharedLibraries(PackageInfoFlags.of(flags.toLong()))
        } else if (isOAndAbove()) {
            context.packageManager.getSharedLibraries(flags)
        } else {
            emptyList()
        }
    }

    fun getPackageInfoMap(context: Context): MutableMap<String, PackageInfo> {
        val packageInfoSet: MutableMap<String, PackageInfo> = mutableMapOf()
        val packageManager: PackageManager = context.packageManager
        val flags: Int = PackageManager.GET_META_DATA
        var packageInfoList: List<PackageInfo> = packageManager.getInstalledPackages(flags)

        val isGoogleFilterEnabled = Preferences.getBoolean(
            context,
            Preferences.PREFERENCE_FILTER_GOOGLE
        )

        val isAuroraOnlyUpdateEnabled = Preferences.getBoolean(
            context,
            Preferences.PREFERENCE_FILTER_AURORA_ONLY,
            false
        )

        val isFDroidFilterEnabled = Preferences.getBoolean(
            context,
            Preferences.PREFERENCE_FILTER_FDROID
        )

        val isExtendedUpdateEnabled = Preferences.getBoolean(
            context,
            Preferences.PREFERENCE_UPDATES_EXTENDED
        )

        packageInfoList = packageInfoList.filter { it.isApp() }

        /*Filter google apps*/
        if (isGoogleFilterEnabled) {
            packageInfoList = packageInfoList
                .filter {
                    !listOf(
                        "com.chrome.beta",
                        "com.chrome.canary",
                        "com.chrome.dev",
                        "com.android.chrome",
                        "com.niksoftware.snapseed",
                        "com.google.toontastic",
                    ).contains(it.packageName)
                }.filter {
                    !it.packageName.contains("com.google")
                }
        }

        /*Select only Aurora STore installed apps*/
        if (isAuroraOnlyUpdateEnabled) {
            packageInfoList = packageInfoList
                .filter {
                    val packageInstaller =
                        packageManager.getInstallerPackageNameCompat(it.packageName)
                    listOf(
                        "com.aurora.store",
                        "com.aurora.store.debug",
                        "com.aurora.store.nightly",
                        "com.aurora.services"
                    ).contains(packageInstaller)
                }
        }

        if (!isExtendedUpdateEnabled) {
            packageInfoList = packageInfoList.filter { it.applicationInfo!!.enabled }
        }

        /*Filter F-Droid apps*/
        if (isFDroidFilterEnabled) {
            packageInfoList = packageInfoList.filter {
                !CertUtil.isFDroidApp(context, it.packageName)
            }
        }

        packageInfoList.forEach {
            packageInfoSet[it.packageName] = it
        }

        return packageInfoSet
    }


    fun getFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addDataScheme("package")
        @Suppress("DEPRECATION")
        filter.addAction(Intent.ACTION_PACKAGE_INSTALL)
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        return filter
    }
}
