package com.aurora.extensions

import android.content.pm.PackageInfo
import android.os.Process

fun PackageInfo.isApp(): Boolean {
    if (this.applicationInfo == null) return false

    return when {
        isQAndAbove() -> {
            Process.isApplicationUid(this.applicationInfo.uid) &&
                    !this.applicationInfo.isResourceOverlay && !this.isApex
        }
        isNAndAbove() -> Process.isApplicationUid(this.applicationInfo.uid)
        else -> this.packageName != null && this.versionName != null
    }
}
