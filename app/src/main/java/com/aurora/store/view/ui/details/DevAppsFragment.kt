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

package com.aurora.store.view.ui.details

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.SearchBundle
import com.aurora.store.R
import com.aurora.store.databinding.ActivityGenericRecyclerBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.app.AppListViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.search.SearchResultViewModel
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.filterList

@AndroidEntryPoint
class DevAppsFragment : BaseFragment(R.layout.activity_generic_recycler) {

    private var _binding: ActivityGenericRecyclerBinding? = null
    private val binding: ActivityGenericRecyclerBinding
        get() = _binding!!

    private val args: DevAppsFragmentArgs by navArgs()

    private lateinit var VM: SearchResultViewModel

    private lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityGenericRecyclerBinding.bind(view)
        VM = ViewModelProvider(this)[SearchResultViewModel::class.java]

        VM.liveData.observe(viewLifecycleOwner) {
            updateController(it)
        }

        // Toolbar
        binding.layoutToolbarAction.apply {
            txtTitle.text = args.developerName
            toolbar.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        // Recycler View
        endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
            override fun onLoadMore(currentPage: Int) {
                VM.liveData.value?.let { VM.next(it.subBundles) }
            }
        }
        binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)

        VM.observeSearchResults("pub:${args.developerName}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateController(searchBundle: SearchBundle) {
        binding.recycler
            .withModels {
                setFilterDuplicates(true)
                searchBundle.appList
                    .filter { it.displayName.isNotEmpty() }
                    .forEach { app ->
                    add(
                        AppListViewModel_()
                            .id(app.id)
                            .app(app)
                            .click(View.OnClickListener {
                                openDetailsFragment(app.packageName, app)
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
    }
}
