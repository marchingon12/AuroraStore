/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.aurora.Constants.PACKAGE_NAME_GMS
import com.aurora.extensions.toast
import com.aurora.store.AuroraApp
import com.aurora.store.ComposeActivity
import com.aurora.store.MainActivity
import com.aurora.store.R
import com.aurora.store.compose.ui.about.AboutScreen
import com.aurora.store.compose.ui.accounts.AccountsScreen
import com.aurora.store.compose.ui.blacklist.BlacklistScreen
import com.aurora.store.compose.ui.commons.PermissionRationaleScreen
import com.aurora.store.compose.ui.commons.StreamBrowseScreen
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.compose.ui.dev.DevProfileScreen
import com.aurora.store.compose.ui.dispenser.DispenserScreen
import com.aurora.store.compose.ui.downloads.DownloadsScreen
import com.aurora.store.compose.ui.favourite.FavouriteScreen
import com.aurora.store.compose.ui.installed.InstalledScreen
import com.aurora.store.compose.ui.onboarding.OnboardingScreen
import com.aurora.store.compose.ui.preferences.installation.InstallerScreen
import com.aurora.store.compose.ui.search.SearchScreen
import com.aurora.store.compose.ui.spoof.SpoofScreen
import com.aurora.store.data.event.InstallerEvent
import com.aurora.store.data.model.AccountType
import com.aurora.store.data.providers.AccountProvider
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences

/**
 * Navigation display for compose screens
 * @param startDestination Starting destination for the activity/app
 */
@Composable
fun NavDisplay(startDestination: NavKey) {
    val backstack = rememberNavBackStack(startDestination)

    // TODO: Rework when migrating splash fragment to compose
    val splashIntent = NavDeepLinkBuilder(LocalContext.current)
        .setGraph(R.navigation.mobile_navigation)
        .setDestination(R.id.splashFragment)
        .setComponentName(MainActivity::class.java)
        .createTaskStackBuilder()
        .intents
        .first()
        .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK) }

    // TODO: Drop this logic once everything is in compose
    val activity = LocalActivity.current
    fun onNavigateUp() {
        if (backstack.size == 1) activity?.finish() else backstack.removeLastOrNull()
    }

    val context = LocalContext.current

    fun isMicroGAuthInvalidated(): Boolean =
        Preferences.getBoolean(context, Preferences.PREFERENCE_AUTH_VIA_MICROG, false) &&
            AccountProvider.getAccountType(context) == AccountType.GOOGLE &&
            !PackageUtil.hasSupportedMicroGVariant(context)

    fun handleMicroGRemoved() {
        context.toast(R.string.microg_removed_auth_warning)
        AccountProvider.logout(context)
        val intent = Intent(context, ComposeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Check every time the screen resumes in case microG was removed while Aurora was in background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isMicroGAuthInvalidated()) handleMicroGRemoved()
    }

    // Also react immediately if the GMS package is uninstalled while Aurora is in the foreground.
    LaunchedEffect(Unit) {
        AuroraApp.events.installerEvent.collect { event ->
            if (event is InstallerEvent.Uninstalled && event.packageName == PACKAGE_NAME_GMS) {
                if (isMicroGAuthInvalidated()) handleMicroGRemoved()
            }
        }
    }

    NavDisplay(
        backStack = backstack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Screen.Blacklist> {
                BlacklistScreen(onNavigateUp = ::onNavigateUp)
            }

            entry<Screen.Search> {
                SearchScreen(onNavigateUp = ::onNavigateUp)
            }

            entry<Screen.AppDetails> { screen ->
                AppDetailsScreen(
                    packageName = screen.packageName,
                    onNavigateUp = ::onNavigateUp,
                    onNavigateToAppDetails = { packageName ->
                        backstack.add(Screen.AppDetails(packageName))
                    }
                )
            }

            entry<Screen.DevProfile> { screen ->
                DevProfileScreen(
                    developerId = screen.developerId,
                    onNavigateUp = ::onNavigateUp,
                    onNavigateToAppDetails = { packageName ->
                        backstack.add(Screen.AppDetails(packageName))
                    }
                )
            }

            entry<Screen.PermissionRationale> { screen ->
                PermissionRationaleScreen(
                    onNavigateUp = ::onNavigateUp,
                    requiredPermissions = screen.requiredPermissions
                )
            }

            entry<Screen.Downloads> {
                DownloadsScreen(
                    onNavigateUp = ::onNavigateUp,
                    onNavigateToAppDetails = { packageName ->
                        backstack.add(Screen.AppDetails(packageName))
                    }
                )
            }

            entry<Screen.Accounts> {
                AccountsScreen(
                    onNavigateUp = ::onNavigateUp,
                    onNavigateToSplash = { activity?.startActivity(splashIntent) }
                )
            }

            entry<Screen.About> {
                AboutScreen(onNavigateUp = ::onNavigateUp)
            }

            entry<Screen.Favourite> {
                FavouriteScreen(
                    onNavigateUp = ::onNavigateUp,
                    onNavigateToAppDetails = { packageName ->
                        backstack.add(Screen.AppDetails(packageName))
                    }
                )
            }

            entry<Screen.Onboarding> {
                OnboardingScreen()
            }

            entry<Screen.Spoof> {
                SpoofScreen(
                    onNavigateUp = ::onNavigateUp,
                    onNavigateToSplash = { activity?.startActivity(splashIntent) }
                )
            }

            entry<Screen.Dispenser> {
                DispenserScreen(onNavigateUp = ::onNavigateUp)
            }

            entry<Screen.Installer> {
                InstallerScreen(onNavigateUp = ::onNavigateUp)
            }

            entry<Screen.Installed> {
                InstalledScreen(
                    onNavigateUp = ::onNavigateUp,
                    onNavigateToAppDetails = { packageName ->
                        backstack.add(Screen.AppDetails(packageName))
                    }
                )
            }

            entry<Screen.StreamBrowse> { screen ->
                StreamBrowseScreen(
                    streamCluster = screen.streamCluster,
                    onNavigateUp = ::onNavigateUp,
                    onNavigateToAppDetails = { packageName ->
                        backstack.add(Screen.AppDetails(packageName))
                    }
                )
            }
        }
    )
}
