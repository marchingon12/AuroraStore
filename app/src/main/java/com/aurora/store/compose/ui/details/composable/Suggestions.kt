/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.aurora.extensions.shimmer
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.compose.composable.Header
import com.aurora.store.compose.composable.app.AppListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val PLACEHOLDER_COUNT = 6
private const val LOAD_MORE_THRESHOLD = 3

/**
 * Composable to display a single stream cluster as a horizontal-scrollable list.
 * @param cluster The cluster to display, or `null` while still loading
 * @param onLoadMore Invoked when the user scrolls near the end and the cluster has more pages
 * @param onNavigateToAppDetails Callback when an app is clicked
 */
@Composable
fun Suggestions(
    cluster: StreamCluster?,
    onLoadMore: () -> Unit = {},
    onNavigateToAppDetails: (packageName: String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val title = cluster?.clusterTitle?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.pref_ui_similar_apps)

    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xsmall))
    ) {
        Header(title = title)

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(
                horizontal = dimensionResource(R.dimen.padding_medium)
            ),
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.margin_medium)
            )
        ) {
            if (cluster == null) {
                items(count = PLACEHOLDER_COUNT) {
                    SuggestionPlaceholder()
                }
            } else {
                items(items = cluster.clusterAppList, key = { it.id }) { suggestedApp ->
                    AppListItem(
                        app = suggestedApp,
                        onClick = { onNavigateToAppDetails(suggestedApp.packageName) }
                    )
                }
            }
        }
    }

    if (cluster != null && cluster.hasNext()) {
        LaunchedEffect(listState, cluster.id) {
            snapshotFlow {
                val info = listState.layoutInfo
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                val total = info.totalItemsCount
                total > 0 && lastVisible >= total - LOAD_MORE_THRESHOLD
            }
                .distinctUntilChanged()
                .filter { it }
                .collect { onLoadMore() }
        }
    }
}

@Composable
private fun SuggestionPlaceholder() {
    Column(
        modifier = Modifier
            .width(dimensionResource(R.dimen.icon_size_cluster))
            .padding(dimensionResource(R.dimen.padding_xsmall)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xsmall))
    ) {
        Row(
            modifier = Modifier
                .requiredSize(dimensionResource(R.dimen.icon_size_cluster))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
                .shimmer(toShow = true)
        ) {}
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                .shimmer(toShow = true)
        ) {}
        Row(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.6f)
                .height(12.dp)
                .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                .shimmer(toShow = true)
        ) {}
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun SuggestionsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Suggestions(
        cluster = StreamCluster(
            clusterTitle = "Similar apps",
            clusterAppList = List(10) { app.copy(id = it) }
        )
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun SuggestionsLoadingPreview() {
    Suggestions(cluster = null)
}
