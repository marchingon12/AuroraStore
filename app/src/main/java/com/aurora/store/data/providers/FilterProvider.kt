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

package com.aurora.store.data.providers

import android.content.Context
import com.aurora.store.data.model.Filter
import com.aurora.store.util.Preferences
import com.aurora.store.util.remove
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterProvider @Inject constructor(
    private val json: Json,
    @ApplicationContext private val context: Context
) {

    companion object {
        const val PREFERENCE_FILTER = "PREFERENCE_FILTER"
    }

    init {
        // Clean any last saved filter
        context.remove(PREFERENCE_FILTER)
    }

    fun getSavedFilter(): Filter {
        val rawFilter = Preferences.getString(context, PREFERENCE_FILTER)
        return json.decodeFromString<Filter>(rawFilter.ifEmpty { "{}" })
    }

    fun saveFilter(filter: Filter) {
        Preferences.putString(context, PREFERENCE_FILTER, json.encodeToString(filter))
    }
}
