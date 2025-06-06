/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aurora.store.compose.ui.commons.BlacklistScreen

/**
 * Navigation graph for compose screens
 * @param navHostController [NavHostController] to navigate with compose
 * @param startDestination Starting destination for the activity/app
 */
@Composable
fun NavGraph(navHostController: NavHostController, startDestination: Screen) {
    // TODO: Drop this logic once everything is in compose
    val activity = LocalActivity.current
    fun onNavigateUp() {
        if (navHostController.previousBackStackEntry != null) {
            navHostController.navigateUp()
        } else {
            activity?.finish()
        }
    }

    NavHost(navController = navHostController, startDestination = startDestination) {
        composable<Screen.Blacklist> {
            BlacklistScreen(onNavigateUp = { onNavigateUp() })
        }
    }
}
