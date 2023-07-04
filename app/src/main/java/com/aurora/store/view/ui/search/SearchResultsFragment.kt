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

package com.aurora.store.view.ui.search

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aurora.Constants
import com.aurora.extensions.open
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.store.R
import com.aurora.store.data.Filter
import com.aurora.store.data.providers.FilterProvider
import com.aurora.store.databinding.ActivitySearchResultBinding
import com.aurora.store.util.Preferences
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseActivity
import com.aurora.store.view.ui.downloads.DownloadActivity
import com.aurora.store.view.ui.sheets.FilterSheet
import com.aurora.store.viewmodel.search.SearchResultViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch


class SearchResultsActivity : BaseActivity(), OnSharedPreferenceChangeListener {

    lateinit var B: ActivitySearchResultBinding
    lateinit var VM: SearchResultViewModel

    lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener
    lateinit var searchView: TextInputEditText

    lateinit var sharedPreferences: SharedPreferences
    lateinit var filter: Filter

    var query: String? = null
    var searchBundle: SearchBundle = SearchBundle()
    private var shimmerAnimationVisible = false
    private var snackbar: Snackbar? = null

    override fun onConnected() {
        hideNetworkConnectivitySheet()
    }

    override fun onDisconnected() {
        showNetworkConnectivitySheet()
    }

    override fun onReconnected() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        B = ActivitySearchResultBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this)[SearchResultViewModel::class.java]

        sharedPreferences = Preferences.getPrefs(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        filter = FilterProvider.with(this).getSavedFilter()

        setContentView(B.root)

        attachToolbar()
        attachSearch()
        attachRecycler()
        attachFilter()

        VM.liveData.observe(this) {
            if (shimmerAnimationVisible) {
                endlessRecyclerOnScrollListener.resetPageCount()
                B.recycler.clear()
                shimmerAnimationVisible = false
            }
            searchBundle = it
            updateController(searchBundle)
        }

        query = intent.getStringExtra(Constants.STRING_EXTRA)
        query?.let {
            updateQuery(it)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                VM.responseCode.collect {
                    when (it) {
                        429 -> {
                            if (VM.liveData.value?.appList?.isEmpty() == true) {
                                attachErrorLayout(getString(R.string.rate_limited))
                            } else {
                                snackbar = Snackbar.make(
                                    B.root,
                                    getString(R.string.rate_limited),
                                    Snackbar.LENGTH_INDEFINITE
                                )
                                snackbar?.show()
                            }
                        }
                        else -> {
                            if (B.errorLayout.isVisible) detachErrorLayout()
                            if (snackbar != null && snackbar?.isShown == true) snackbar?.dismiss()
                        }
                    }
                }
            }
        }

    }

    override fun onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        if (Preferences.getBoolean(this, Preferences.PREFERENCE_FILTER_SEARCH))
            FilterProvider.with(this).saveFilter(Filter())
        super.onDestroy()
    }

    private fun attachErrorLayout(message: String) {
        B.recycler.visibility = View.GONE
        B.filterFab.visibility = View.GONE
        B.errorLayout.visibility = View.VISIBLE
        B.errorMessage.text = message
    }

    private fun detachErrorLayout() {
        B.recycler.visibility = View.VISIBLE
        B.filterFab.visibility = View.VISIBLE
        B.errorLayout.visibility = View.GONE
    }

    private fun attachToolbar() {
        searchView = B.layoutViewToolbar.inputSearch

        B.layoutViewToolbar.imgActionPrimary.setOnClickListener {
            finishAfterTransition()
        }
        B.layoutViewToolbar.imgActionSecondary.setOnClickListener {
            open(DownloadActivity::class.java)
        }
    }

    private fun attachFilter() {
        B.filterFab.setOnClickListener {
            FilterSheet.newInstance().show(supportFragmentManager, FilterSheet.TAG)
        }
    }

    private fun attachRecycler() {
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.next(searchBundle.subBundles)
            }
        }
        B.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
    }

    private fun updateController(searchBundle: SearchBundle?) {
        if (searchBundle == null) {
            shimmerAnimationVisible = true
            B.recycler.withModels {
                for (i in 1..10) {
                    add(AppListViewShimmerModel_().id(i))
                }
            }
            return
        }

        val filteredAppList = filter(searchBundle.appList)

        if (filteredAppList.isEmpty()) {
            if (searchBundle.subBundles.isNotEmpty()) {
                VM.next(searchBundle.subBundles)
                B.recycler.withModels {
                    setFilterDuplicates(true)
                    add(
                        AppProgressViewModel_()
                            .id("progress")
                    )
                }
            } else {
                B.recycler.adapter?.let {
                    /*Show empty search list if nothing found or no app matches filter criterion*/
                    if (it.itemCount == 1 && searchBundle.subBundles.isEmpty()) {
                        B.recycler.withModels {
                            add(
                                NoAppViewModel_()
                                    .id("no_app")
                                    .message(getString(R.string.details_no_app_match))
                                    .icon(R.drawable.ic_round_search)
                            )
                        }
                    }
                }
            }
        } else {
            B.recycler
                .withModels {
                    setFilterDuplicates(true)

                    filteredAppList.forEach { app ->
                        add(
                            AppListViewModel_()
                                .id(app.id)
                                .app(app)
                                .click(View.OnClickListener {
                                    openDetailsActivity(app)
                                })
                        )
                    }

                    if (searchBundle.subBundles.isNotEmpty()) {
                        add(
                            AppProgressViewModel_()
                                .id("progress")
                        )
                    }
                }

            B.recycler.adapter?.let {
                if (it.itemCount < 10) {
                    VM.next(searchBundle.subBundles)
                }
            }
        }
    }

    private fun attachSearch() {
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {}
        })

        searchView.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                query = searchView.text.toString()
                query?.let {
                    queryViewModel(it)
                    return@setOnEditorActionListener true
                }
            }
            false
        }
    }

    private fun updateQuery(query: String) {
        searchView.text = Editable.Factory.getInstance().newEditable(query)
        searchView.setSelection(query.length)
        queryViewModel(query)
    }

    private fun queryViewModel(query: String) {
        updateController(null)
        VM.observeSearchResults(query)
    }

    private fun filter(appList: MutableList<App>): List<App> {
        filter = FilterProvider.with(this).getSavedFilter()
        return appList
            .asSequence()
            .filter { if (!filter.paidApps) it.isFree else true }
            .filter { if (!filter.appsWithAds) !it.containsAds else true }
            .filter { if (!filter.gsfDependentApps) it.dependencies.dependentPackages.isEmpty() else true }
            .filter { if (filter.rating > 0) it.rating.average >= filter.rating else true }
            .filter { if (filter.downloads > 0) it.installs >= filter.downloads else true }
            .toList()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == FilterProvider.PREFERENCE_FILTER) {
            filter = FilterProvider.with(this).getSavedFilter()
            query?.let {
                queryViewModel(it)
            }
        }
    }
}
