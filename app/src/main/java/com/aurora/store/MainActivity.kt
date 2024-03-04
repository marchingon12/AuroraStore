/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2022, The Calyx Institute
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

package com.aurora.store

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.aurora.Constants
import com.aurora.extensions.accentColor
import com.aurora.extensions.applyThemeAccent
import com.aurora.store.data.model.NetworkStatus
import com.aurora.store.data.providers.NetworkProvider
import com.aurora.store.databinding.ActivityMainBinding
import com.aurora.store.util.Log
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.view.ui.sheets.NetworkDialogSheet
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var B: ActivityMainBinding
    private lateinit var navController: NavController

    // TopLevelFragments
    private val topLevelFrags = listOf(
        R.id.appsContainerFragment,
        R.id.gamesContainerFragment,
        R.id.updatesFragment,
        R.id.moreFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeAccent()
        super.onCreate(savedInstanceState)

        B = ActivityMainBinding.inflate(layoutInflater)
        setContentView(B.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        this.lifecycleScope.launch {
            NetworkProvider(applicationContext).networkStatus.collect {
                when (it) {
                    NetworkStatus.AVAILABLE -> {
                        if (!supportFragmentManager.isDestroyed && isIntroDone()) {
                            val fragment = supportFragmentManager
                                .findFragmentByTag(NetworkDialogSheet.TAG)
                            fragment?.let {
                                supportFragmentManager.beginTransaction()
                                    .remove(fragment)
                                    .commitAllowingStateLoss()
                            }
                        }
                    }

                    NetworkStatus.LOST -> {
                        if (!supportFragmentManager.isDestroyed && isIntroDone()) {
                            supportFragmentManager.beginTransaction()
                                .add(NetworkDialogSheet.newInstance(), NetworkDialogSheet.TAG)
                                .commitAllowingStateLoss()
                        }
                    }
                }
            }
        }

        attachNavigation()

        // Handle quick exit from back actions
        val defaultTab = when (Preferences.getInteger(this, PREFERENCE_DEFAULT_SELECTED_TAB)) {
            1 -> R.id.gamesContainerFragment
            2 -> R.id.updatesFragment
            else -> R.id.appsContainerFragment
        }
        onBackPressedDispatcher.addCallback(this) {
            if (navController.currentDestination?.id in topLevelFrags) {
                if (navController.currentDestination?.id == defaultTab) {
                    finish()
                } else {
                    navController.navigate(defaultTab)
                }
            } else if (navHostFragment.childFragmentManager.backStackEntryCount == 0) {
                // We are on either on onboarding or splash fragment
                finish()
            } else {
                navController.navigateUp()
            }
        }

        // Handle intents
        when (intent?.action) {
            Constants.NAVIGATION_UPDATES -> B.navView.selectedItemId = R.id.updatesFragment
            else -> Log.i("Unhandled intent action: ${intent.action}")
        }

        // Handle views on fragments
        navController.addOnDestinationChangedListener { _, navDestination, _ ->
            if (navDestination !is FloatingWindow) {
                when (navDestination.id) {
                    in topLevelFrags -> {
                        B.navView.visibility = View.VISIBLE
                    }

                    else -> {
                        B.navView.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun attachNavigation() {
        val bottomNavigationView: BottomNavigationView = B.navView
        bottomNavigationView.setupWithNavController(navController)

        bottomNavigationView.apply {
            val alphaColor = ColorUtils.setAlphaComponent(this@MainActivity.accentColor(), 100)
            itemActiveIndicatorColor = ColorStateList.valueOf(alphaColor)
        }
    }

    private fun isIntroDone(): Boolean {
        return Preferences.getBoolean(this@MainActivity, Preferences.PREFERENCE_INTRO)
    }
}
