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

package com.aurora.store.data.model

import androidx.annotation.StringRes

/**
 * Class holding information on a supported installer
 */
data class InstallerInfo(
    val id: Int,
    val installer: Installer,
    val packageNames: List<String>,
    val installerPackageNames: List<String>,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @StringRes val description: Int
) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is InstallerInfo -> other.id == id
            else -> false
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
