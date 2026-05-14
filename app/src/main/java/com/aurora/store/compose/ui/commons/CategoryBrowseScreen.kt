/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Error
import com.aurora.store.compose.composable.Header
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.AppListItem
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
import com.aurora.store.viewmodel.subcategory.CategoryStreamViewModel

@Composable
fun CategoryBrowseScreen(
    title: String,
    browseUrl: String,
    onNavigateTo: (Destination) -> Unit,
    viewModel: CategoryStreamViewModel = hiltViewModel(
        creationCallback = { factory: CategoryStreamViewModel.Factory ->
            factory.create(browseUrl)
        }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = title)
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ViewState.Loading -> ContainedLoadingIndicator()

            is ViewState.Error -> {
                Error(
                    modifier = Modifier.padding(paddingValues),
                    painter = painterResource(R.drawable.ic_disclaimer),
                    message = stringResource(R.string.error)
                )
            }

            is ViewState.Success<*> -> {
                val bundle = state.getDataAs<StreamBundle>()
                val clusters = bundle.streamClusters.values.toList()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    clusters.forEach { cluster ->
                        item(key = "header_${cluster.id}") {
                            Header(
                                title = cluster.clusterTitle,
                                onClick = if (cluster.clusterBrowseUrl.isNotBlank()) {
                                    {
                                        onNavigateTo(
                                            Destination.ExpandedStreamBrowse(
                                                cluster.clusterTitle,
                                                cluster.clusterBrowseUrl
                                            )
                                        )
                                    }
                                } else {
                                    null
                                }
                            )
                        }

                        item(key = "row_${cluster.id}") {
                            LazyRow(modifier = Modifier.fillMaxWidth()) {
                                items(
                                    items = cluster.clusterAppList,
                                    key = { it.packageName }
                                ) { app ->
                                    AppListItem(
                                        app = app,
                                        onClick = {
                                            onNavigateTo(Destination.AppDetails(app.packageName))
                                        }
                                    )
                                }
                            }

                            if (cluster.hasNext()) {
                                LaunchedEffect(cluster.clusterNextPageUrl) {
                                    viewModel.fetchNextCluster(cluster)
                                }
                            }
                        }
                    }

                    if (bundle.hasNext()) {
                        item(key = "load_more") {
                            LaunchedEffect(bundle.streamNextPageUrl) {
                                viewModel.fetchNextPage()
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }
}
