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

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.FilterProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    val filterProvider: FilterProvider,
    private val authProvider: AuthProvider,
    private val searchHelper: SearchHelper,
    private val webSearchHelper: WebSearchHelper
) : ViewModel() {

    private val TAG = SearchResultViewModel::class.java.simpleName

    val liveData: MutableLiveData<SearchBundle> = MutableLiveData()

    private var searchBundle: SearchBundle = SearchBundle()

    private val helper: SearchContract
        get() = if (authProvider.isAnonymous) webSearchHelper else searchHelper

    fun observeSearchResults(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    searchBundle = search(query)
                    liveData.postValue(searchBundle)
                } catch (e: Exception) {

                }
            }
        }
    }

    private fun search(query: String): SearchBundle {
        return helper.searchResults(query)
    }

    @Synchronized
    fun next(nextSubBundleSet: Set<SearchBundle.SubBundle>) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (nextSubBundleSet.isNotEmpty()) {
                        val newSearchBundle = helper.next(nextSubBundleSet.toMutableSet())
                        if (newSearchBundle.appList.isNotEmpty()) {
                            searchBundle = searchBundle.copy(
                                subBundles = newSearchBundle.subBundles,
                                appList = searchBundle.appList + newSearchBundle.appList
                            )

                            liveData.postValue(searchBundle)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to get next bundle", e)
                }
            }
        }
    }
}
