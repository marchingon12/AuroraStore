/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.commons

import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastMap
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.Constants
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.compose.composables.BlackListComposable
import com.aurora.store.compose.composables.TopAppBarComposable
import com.aurora.store.compose.menu.BlacklistMenu
import com.aurora.store.compose.menu.MenuItem
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.all.BlacklistViewModel
import java.util.Calendar

private const val TAG = "BlacklistScreen"

@Composable
fun BlacklistScreen(onNavigateUp: () -> Unit, viewModel: BlacklistViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val packages by viewModel.packages.collectAsStateWithLifecycle()

    ScreenContent(
        packages = packages,
        onNavigateUp = onNavigateUp,
        isPackageBlacklisted = { pkgName -> viewModel.isBlacklisted(pkgName) },
        isPackageFiltered = { pkgInfo -> viewModel.isFiltered(pkgInfo) },
        onBlacklistImport = { uri ->
            viewModel.importBlacklist(ctx, uri)
            ctx.toast(R.string.toast_black_import_success)
        },
        onBlacklistExport = { uri ->
            viewModel.exportBlacklist(ctx, uri)
            ctx.toast(R.string.toast_black_export_success)
        }
    )
}

@Composable
private fun ScreenContent(
    packages: List<PackageInfo>? = null,
    onNavigateUp: () -> Unit = {},
    isPackageBlacklisted: (packageName: String) -> Boolean = { false },
    isPackageFiltered: (packageInfo: PackageInfo) -> Boolean = { false },
    onBlacklistImport: (uri: Uri) -> Unit = {},
    onBlacklistExport: (uri: Uri) -> Unit = {},
    onBlacklist: (packages: List<String>) -> Unit = {},
    onWhitelist: (packages: List<String>) -> Unit = {}
) {

    val ctx = LocalContext.current
    val docImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            if (it != null) onBlacklistImport(it) else ctx.toast(R.string.toast_black_import_failed)
        }
    )
    val docExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(Constants.JSON_MIME_TYPE),
        onResult = {
            if (it != null) onBlacklistExport(it) else ctx.toast(R.string.toast_black_export_failed)
        }
    )

    @Composable
    fun Menu() {
        BlacklistMenu { menuItem ->
            when (menuItem) {
                MenuItem.SELECT_ALL -> onBlacklist(packages!!.fastMap { it.packageName })
                MenuItem.REMOVE_ALL -> onWhitelist(packages!!.fastMap { it.packageName })
                MenuItem.IMPORT -> docImportLauncher.launch(arrayOf(Constants.JSON_MIME_TYPE))
                MenuItem.EXPORT -> docExportLauncher.launch(
                    "aurora_store_apps_${Calendar.getInstance().time.time}.json"
                )

                else -> Log.e(TAG, "Unhandled callback for ${menuItem.name}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBarComposable(
                title = R.string.title_blacklist_manager,
                onNavigateUp = onNavigateUp,
                actions = { if (!packages.isNullOrEmpty()) Menu() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_xxsmall))
        ) {
            items(items = packages ?: emptyList(), key = { p -> p.packageName.hashCode() }) { pkg ->
                val isFiltered = isPackageFiltered(pkg)
                BlackListComposable(
                    icon = PackageUtil.getIconForPackage(ctx, pkg.packageName)!!,
                    displayName = pkg.applicationInfo!!.loadLabel(ctx.packageManager).toString(),
                    packageName = pkg.packageName,
                    versionName = pkg.versionName!!,
                    versionCode = PackageInfoCompat.getLongVersionCode(pkg),
                    isChecked = isPackageBlacklisted(pkg.packageName) || isFiltered,
                    isEnabled = !isFiltered,
                    onClick = { onBlacklist(listOf(pkg.packageName)) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun BlacklistScreenPreview() {
    ScreenContent()
}
