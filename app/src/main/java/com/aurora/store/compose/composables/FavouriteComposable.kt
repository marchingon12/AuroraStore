/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composables

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.App
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.data.room.favourite.Favourite

/**
 * Composable to display a favourite app in a list
 * @param favourite A [Favourite] app to display
 * @param onClick Callback when this composable is clicked
 * @param onClear Callback when the favourite button is clicked to remove the app from favourites
 */
@Composable
fun FavouriteComposable(
    favourite: Favourite,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.padding_xsmall)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1F)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(favourite.iconURL)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_android),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
            )
            Column(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.margin_small))
            ) {
                Text(
                    text = favourite.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = favourite.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DateUtils.formatDateTime(
                        LocalContext.current,
                        favourite.added,
                        DateUtils.FORMAT_SHOW_DATE
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
                )
            }
        }
        IconButton(onClick = onClear) {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_checked),
                contentDescription = stringResource(R.string.action_favourite)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FavouriteComposablePreview() {
    val app = App(
        packageName = BuildConfig.APPLICATION_ID,
        displayName = LocalContext.current.getString(R.string.app_name)
    )
    FavouriteComposable(favourite = Favourite.fromApp(app, Favourite.Mode.MANUAL))
}
