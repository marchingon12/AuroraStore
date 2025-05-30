package com.aurora.store.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ProxyInfo(
    var protocol: String,
    var host: String,
    var port: Int,
    var proxyUser: String?,
    var proxyPassword: String?
) : Parcelable
