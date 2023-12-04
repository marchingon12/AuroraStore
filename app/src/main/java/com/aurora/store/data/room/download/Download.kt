package com.aurora.store.data.room.download

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.data.models.App
import com.aurora.store.data.model.DownloadStatus

@Entity
data class Download(
    @PrimaryKey val packageName: String,
    val versionCode: Int,
    val offerType: Int,
    val isInstalled: Boolean,
    val displayName: String,
    val iconURL: String,
    val size: Long,
    val id: Int,
    val status: DownloadStatus
) {
    companion object {
        fun fromApp(app: App): Download {
            return Download(
                app.packageName,
                app.versionCode,
                app.offerType,
                app.isInstalled,
                app.displayName,
                app.iconArtwork.url,
                app.size,
                app.id,
                DownloadStatus.UNAVAILABLE
            )
        }
    }
}
