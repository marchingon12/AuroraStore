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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.web.WebAppDetailsHelper
import com.aurora.store.data.PageResult
import com.aurora.store.data.paging.GenericPagingSource.Companion.manualPager
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.util.PackageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class InstalledViewModel @Inject constructor(
    blacklistProvider: BlacklistProvider,
    @ApplicationContext private val context: Context,
    private val webAppDetailsHelper: WebAppDetailsHelper
) : ViewModel() {

    private val blacklist = blacklistProvider.blacklist

    private val _apps = MutableStateFlow<PagingData<App>>(PagingData.empty())
    val apps = _apps.asStateFlow()

    /**
     * Enumerating installed packages calls into PackageManager and resolves a label
     * per app for sorting. Done lazily on [Dispatchers.IO] so the VM constructor and
     * the main thread stay snappy; paging awaits this before fetching the first page.
     */
    private val pagedPackages = viewModelScope.async(
        context = Dispatchers.IO,
        start = CoroutineStart.LAZY
    ) {
        PackageUtil.getAllValidPackages(context)
            .filterNot { it.packageName in blacklist }
            .chunked(PAGE_SIZE)
    }

    init {
        fetchApps()
    }

    private fun fetchApps() {
        manualPager(pageSize = PAGE_SIZE) { page ->
            // page is 1-indexed, but list is 0-indexed
            val chunks = pagedPackages.await()
            val chunk = chunks.getOrNull(page - 1)
                ?: return@manualPager PageResult(emptyList<App>(), hasMore = false)
            val items = webAppDetailsHelper.getAppDetails(chunk.map { it.packageName })
            PageResult(items, hasMore = page < chunks.size)
        }.flow.distinctUntilChanged()
            .cachedIn(viewModelScope)
            .onEach { _apps.value = it }
            .launchIn(viewModelScope)
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
