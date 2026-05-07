/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data

data class PageResult<T>(
    val items: List<T>,
    val hasMore: Boolean = items.isNotEmpty()
)
