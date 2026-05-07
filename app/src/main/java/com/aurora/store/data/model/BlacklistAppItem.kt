/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import android.graphics.Bitmap

data class BlacklistAppItem(
    val packageName: String,
    val displayName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Bitmap,
    val isFiltered: Boolean
)
