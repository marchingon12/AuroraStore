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

package com.aurora.store.viewmodel.all

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.PackageUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlacklistViewModel @Inject constructor(
    val blacklistProvider: BlacklistProvider,
    val gson: Gson,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = BlacklistViewModel::class.java.simpleName

    private val _packages = MutableStateFlow<List<PackageInfo>?>(null)
    val packages = _packages.asStateFlow()

    var selected: MutableSet<String> = blacklistProvider.blacklist

    init {
        fetchApps()
    }

    private fun fetchApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _packages.value = PackageUtil.getAllValidPackages(context)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch apps", exception)
            }
        }
    }

    fun selectAll() {
        selected.addAll(packages.value?.map { it.packageName } ?: emptyList())
    }

    fun removeAll() {
        selected.clear()
    }

    fun importBlacklist(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    val importedSet: MutableSet<String> = gson.fromJson(
                        it.bufferedReader().readText(),
                        object : TypeToken<MutableSet<String?>?>() {}.type
                    )

                    val knownSet = blacklistProvider.blacklist
                    knownSet.addAll(importedSet)

                    selected = knownSet
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to import blacklist", exception)
            }
        }
    }

    fun exportBlacklist(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(gson.toJson(blacklistProvider.blacklist).encodeToByteArray())
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to export blacklist", exception)
            }
        }
    }
}
