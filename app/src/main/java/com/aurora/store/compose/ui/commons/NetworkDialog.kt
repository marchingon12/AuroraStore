/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aurora.extensions.isQAndAbove
import com.aurora.store.R

@Composable
fun NetworkDialog(onDismiss: () -> Unit = {}) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_network),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.title_no_network)) },
        text = { Text(stringResource(R.string.check_connectivity)) },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        if (isQAndAbove) {
                            context.startActivity(
                                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                            )
                        } else {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        }
                    } catch (e: ActivityNotFoundException) {
                        Log.i("NetworkDialog", "Unable to launch network settings")
                        try {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        } catch (_: Exception) { }
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.check_connectivity))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
