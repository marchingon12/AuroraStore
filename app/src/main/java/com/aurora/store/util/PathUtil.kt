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
import android.os.Environment
import com.aurora.extensions.isRAndAbove
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.File
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.Path

fun Context.getInternalBaseDirectory(): String {
    return (getExternalFilesDir(null) ?: filesDir).path
}

object PathUtil {

    private fun getDownloadDirectory(context: Context): String {
        return if (context.isExternalStorageEnable()) {
            getExternalPath(context)
        } else {
            context.getInternalBaseDirectory()
        }
    }

    fun getPackageDirectory(context: Context, packageName: String): String {
        return getDownloadDirectory(context) + "/Downloads/$packageName"
    }

    private fun getVersionDirectory(
        context: Context,
        packageName: String,
        versionCode: Int
    ): String {
        return getPackageDirectory(context, packageName) + "/$versionCode"
    }

    fun getAppDownloadDir(context: Context, packageName: String, versionCode: Int): Path {
        return Path(getPackageDirectory(context, packageName), versionCode.toString())
    }

    fun getApkDownloadFile(context: Context, app: App, file: File): String {
        return getVersionDirectory(context, app.packageName, app.versionCode) + "/${file.name}"
    }

    fun getApkDownloadFile(context: Context, packageName: String, versionCode: Int): String {
        return getVersionDirectory(context, packageName, versionCode)
    }

    fun getExternalPath(context: Context): String {
        val defaultDir =
            java.io.File("${Environment.getExternalStorageDirectory().absolutePath}/Aurora/Store")

        if (!defaultDir.exists())
            defaultDir.mkdirs()

        return Preferences.getString(
            context,
            Preferences.PREFERENCE_DOWNLOAD_DIRECTORY,
            defaultDir.absolutePath
        )
    }

    fun getBaseCopyDirectory(context: Context): String {
        return "${getExternalPath(context)}/Exports/"
    }

    private fun getObbDownloadPath(app: App): String {
        return Environment.getExternalStorageDirectory()
            .toString() + "/Android/obb/" + app.packageName
    }

    fun getObbDownloadDir(packageName: String): Path {
        return Path(
            Environment.getExternalStorageDirectory().absolutePath,
            "/Android/obb/",
            packageName
        )
    }

    fun getObbDownloadFile(app: App, file: File): String {
        val obbDir = getObbDownloadPath(app)
        return "$obbDir/${file.name}"
    }

    fun needsStorageManagerPerm(fileList: List<File>): Boolean {
        return fileList.any { it.type == File.FileType.OBB || it.type == File.FileType.PATCH }
    }

    fun getSpoofDirectory(context: Context): String {
        return "${context.getInternalBaseDirectory()}/SpoofConfigs"
    }

    fun getNewEmptySpoofConfig(context: Context): java.io.File {
        val file = java.io.File("${getSpoofDirectory(context)}/${UUID.randomUUID()}.properties")
        file.parentFile?.mkdirs()
        file.createNewFile()
        return file
    }

    fun canWriteToDirectory(context: Context, directoryPath: String): Boolean {
        val directory = if (directoryPath.startsWith("/")) {
            java.io.File(directoryPath)
        } else {
            java.io.File(context.getExternalFilesDir(null), directoryPath)
        }

        return if (isRAndAbove()) {
            Environment.isExternalStorageManager() && directory.exists() && directory.canWrite()
        } else {
            isExternalStorageAccessible(context) && directory.exists() && directory.canWrite()
        }
    }
}

fun Context.isExternalStorageEnable(): Boolean {
    return Preferences.getBoolean(this, Preferences.PREFERENCE_DOWNLOAD_EXTERNAL)
}
