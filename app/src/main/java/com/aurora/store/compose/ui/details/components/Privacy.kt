/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R
import com.aurora.store.compose.composable.Header
import com.aurora.store.compose.composable.Info
import com.aurora.store.compose.theme.successColor
import com.aurora.store.compose.theme.warningColor
import com.aurora.store.data.model.Report

/**
 * Composable to display app's privacy report from ExodusPrivacy, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param report Report from Exodus Privacy for the app
 * @param onNavigateToDetailsExodus Callback when the user navigates
 */
@Composable
fun Privacy(report: Report?, onNavigateToDetailsExodus: (() -> Unit)? = null) {
    Header(
        title = stringResource(R.string.details_privacy),
        subtitle = stringResource(R.string.exodus_powered),
        onClick = onNavigateToDetailsExodus
    )

    val reportStatus = when {
        report == null -> stringResource(R.string.failed_to_fetch_report)
        report.id == -1 -> stringResource(R.string.exodus_progress)
        else -> if (report.trackers.isEmpty()) {
            stringResource(R.string.exodus_no_tracker)
        } else {
            stringResource(R.string.exodus_report_trackers, report.trackers.size, report.version)
        }
    }

    Info(
        painter = painterResource(R.drawable.ic_visibility),
        tint = when {
            report != null && report.trackers.isEmpty() -> successColor
            else -> warningColor
        },
        title = AnnotatedString(text = reportStatus),
        description = AnnotatedString(text = stringResource(R.string.exodus_tracker_desc))
    )
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))) {
        Privacy(report = Report(), onNavigateToDetailsExodus = {})
    }
}
