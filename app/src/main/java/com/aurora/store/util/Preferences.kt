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
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager


object Preferences {

    const val PREFERENCE_DEFAULT = "PREFERENCE_DEFAULT"

    const val PREFERENCE_AUTH_DATA = "PREFERENCE_AUTH_DATA"
    const val PREFERENCE_INSTALLER_ID = "PREFERENCE_INSTALLER_ID"
    const val PREFERENCE_THEME_TYPE = "PREFERENCE_THEME_TYPE"
    const val PREFERENCE_THEME_ACCENT = "PREFERENCE_THEME_ACCENT"
    const val PREFERENCE_FOR_YOU = "PREFERENCE_FOR_YOU"
    const val PREFERENCE_DEFAULT_SELECTED_TAB = "PREFERENCE_DEFAULT_SELECTED_TAB"
    const val PREFERENCE_SIMILAR = "PREFERENCE_SIMILAR"
    const val PREFERENCE_INTRO = "PREFERENCE_INTRO"

    const val PREFERENCE_FILTER_GOOGLE = "PREFERENCE_FILTER_GOOGLE"
    const val PREFERENCE_FILTER_FDROID = "PREFERENCE_FILTER_FDROID"
    const val PREFERENCE_FILTER_SEARCH = "PREFERENCE_FILTER_SEARCH"
    const val PREFERENCE_FILTER_AURORA_ONLY = "PREFERENCE_FILTER_AURORA_ONLY"

    const val PREFERENCE_DOWNLOADS_CACHE_TIME = "PREFERENCE_DOWNLOADS_CACHE_TIME"
    const val PREFERENCE_AUTO_DELETE = "PREFERENCE_AUTO_DELETE"

    const val PREFERENCE_INSTALLATION_DEVICE_OWNER = "PREFERENCE_INSTALLATION_DEVICE_OWNER"
    const val INSTALLATION_ABANDON_SESSION = "INSTALLATION_ABANDON_SESSION"

    const val PREFERENCE_INSECURE_ANONYMOUS = "PREFERENCE_INSECURE_ANONYMOUS"
    const val PREFERENCE_PROXY_URL = "PREFERENCE_PROXY_URL"
    const val PREFERENCE_PROXY_INFO = "PREFERENCE_PROXY_INFO"
    const val PREFERENCE_PROXY_ENABLED = "PREFERENCE_PROXY_ENABLED"

    const val PREFERENCE_VENDING_VERSION = "PREFERENCE_VENDING_VERSION"

    const val PREFERENCE_UPDATES_EXTENDED = "PREFERENCE_UPDATES_EXTENDED"
    const val PREFERENCE_UPDATES_AUTO = "PREFERENCE_UPDATES_AUTO"
    const val PREFERENCE_UPDATES_CHECK_INTERVAL = "PREFERENCE_UPDATES_CHECK_INTERVAL"

    const val PREFERENCE_MIGRATION_VERSION = "PREFERENCE_MIGRATION_VERSION"

    private var prefs: SharedPreferences? = null

    fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(context)
        }
        return prefs!!
    }

    fun putString(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun putInteger(context: Context, key: String, value: Int) {
        getPrefs(context).edit().putInt(key, value).apply()
    }

    fun putFloat(context: Context, key: String, value: Float) {
        getPrefs(context).edit().putFloat(key, value).apply()
    }

    fun putLong(context: Context, key: String, value: Long) {
        getPrefs(context).edit().putLong(key, value).apply()
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getString(context: Context, key: String, default: String = ""): String {
        return getPrefs(context).getString(key, default).toString()
    }

    fun getInteger(context: Context, key: String, default: Int = 0): Int {
        return getPrefs(context).getInt(key, default)
    }

    fun getFloat(context: Context, key: String): Float {
        return getPrefs(context).getFloat(key, 0.0f)
    }

    fun getLong(context: Context, key: String): Long {
        return getPrefs(context).getLong(key, 0L)
    }

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean {
        return getPrefs(context).getBoolean(key, default)
    }
}

/*Preference Extensions*/

fun Context.save(key: String, value: Int) = Preferences.putInteger(this, key, value)

fun Context.save(key: String, value: Boolean) = Preferences.putBoolean(this, key, value)

fun Context.save(key: String, value: String) = Preferences.putString(this, key, value)


fun Fragment.save(key: String, value: Int) = requireContext().save(key, value)

fun Fragment.save(key: String, value: Boolean) = requireContext().save(key, value)

fun Fragment.save(key: String, value: String) = requireContext().save(key, value)
