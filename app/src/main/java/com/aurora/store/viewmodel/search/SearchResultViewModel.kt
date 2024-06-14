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

package com.aurora.store.viewmodel.search

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.flushAndAdd
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.WebSearchHelper
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.FilterProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class SearchResultViewModel @Inject constructor(
    val filterProvider: FilterProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = SearchResultViewModel::class.java.simpleName
    private val authData: AuthData = AuthProvider
        .with(context)
        .getAuthData()

    private val _searchBundle: MutableStateFlow<SearchBundle?> = MutableStateFlow(null)
    val searchBundle = _searchBundle.asStateFlow()

    val liveData: MutableLiveData<SearchBundle> = MutableLiveData()

    private val helper: SearchHelper
        get() = if (authData.isAnonymous) {
            WebSearchHelper(authData)
        } else {
            SearchHelper(authData).using(HttpClient.getPreferredClient(context))
        }

    fun observeSearchResults(query: String, force: Boolean = false) {
        // Nothing to do if query and filters are same
        if (_searchBundle.value?.query == query && !force) return

        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    _searchBundle.value = search(query)
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to search", exception)
                    _searchBundle.value = SearchBundle()
                }
            }
        }
    }

    private fun search(query: String): SearchBundle {
        return helper.searchResults(query)
    }

    @Synchronized
    fun next() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val nextSubBundleSet = _searchBundle.value?.subBundles
                    if (!nextSubBundleSet.isNullOrEmpty()) {
                        val nextSearchBundle = helper.next(nextSubBundleSet)
                        if (nextSearchBundle.appList.isNotEmpty()) {
                            val newSearchBundle = _searchBundle.value?.apply {
                                subBundles.flushAndAdd(nextSearchBundle.subBundles)
                                appList.addAll(nextSearchBundle.appList)
                            }
                            _searchBundle.value = newSearchBundle
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to get next bundle", e)
                }
            }
        }
    }
}
