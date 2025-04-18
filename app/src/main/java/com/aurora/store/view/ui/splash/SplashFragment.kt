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

package com.aurora.store.view.ui.splash

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.extensions.hide
import com.aurora.extensions.isMAndAbove
import com.aurora.extensions.isNAndAbove
import com.aurora.extensions.navigate
import com.aurora.extensions.show
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.R
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.data.model.AuthState
import com.aurora.store.databinding.FragmentSplashBinding
import com.aurora.store.util.CertUtil.GOOGLE_ACCOUNT_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_AUTH_TOKEN_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_CERT
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_PACKAGE_NAME
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_MICROG_AUTH
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    private val TAG = SplashFragment::class.java.simpleName

    private val viewModel: AuthViewModel by activityViewModels()

    private val canLoginWithMicroG: Boolean
        get() = isMAndAbove && PackageUtil.hasSupportedMicroG(requireContext()) &&
                Preferences.getBoolean(requireContext(), PREFERENCE_MICROG_AUTH, true)

    private val startForAccount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val accountName = it.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                requestAuthTokenForGoogle(accountName)
            } else {
                resetActions()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!Preferences.getBoolean(requireContext(), PREFERENCE_INTRO)) {
            findNavController().navigate(
                SplashFragmentDirections.actionSplashFragmentToOnboardingFragment()
            )
            return
        }

        // Toolbar
        binding.toolbar.apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_blacklist_manager -> {
                        requireContext().navigate(Screen.Blacklist)
                    }

                    R.id.menu_spoof_manager -> {
                        findNavController().navigate(R.id.spoofFragment)
                    }

                    R.id.menu_settings -> {
                        findNavController().navigate(R.id.settingsFragment)
                    }

                    R.id.menu_about -> findNavController().navigate(R.id.aboutFragment)
                }
                true
            }
        }

        attachActions()

        // Show anonymous logins if we have dispenser URL
        binding.btnAnonymous.isVisible = !viewModel.authProvider.dispenserURL.isNullOrBlank()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collectLatest {
                when (it) {
                    AuthState.Init -> updateStatus(getString(R.string.session_init))

                    AuthState.Fetching -> {
                        updateStatus(getString(R.string.requesting_new_session))
                    }

                    AuthState.Valid -> {
                        val packageName = getPackageName(requireActivity().intent)
                        if (packageName.isNullOrBlank()) {
                            navigateToDefaultTab()
                        } else {
                            requireArguments().remove("packageName")
                            findNavController().navigate(
                                SplashFragmentDirections.actionSplashFragmentToAppDetailsFragment(
                                    packageName
                                )
                            )
                        }
                    }

                    AuthState.Available -> {
                        updateStatus(getString(R.string.session_verifying))
                        updateActionLayout(false)
                    }

                    AuthState.Unavailable -> {
                        updateStatus(getString(R.string.session_login))
                        updateActionLayout(true)
                    }

                    AuthState.SignedIn -> {
                        val packageName = getPackageName(requireActivity().intent)
                        if (packageName.isNullOrBlank()) {
                            navigateToDefaultTab()
                        } else {
                            requireArguments().remove("packageName")
                            findNavController().navigate(
                                SplashFragmentDirections.actionSplashFragmentToAppDetailsFragment(
                                    packageName
                                )
                            )
                        }
                    }

                    AuthState.SignedOut -> {
                        updateStatus(getString(R.string.session_scrapped))
                        updateActionLayout(true)
                    }

                    AuthState.Verifying -> {
                        updateStatus(getString(R.string.verifying_new_session))
                    }

                    is AuthState.PendingAccountManager -> {
                        requestAuthTokenForGoogle(it.email, it.token)
                    }

                    is AuthState.Failed -> {
                        updateStatus(it.status)
                        updateActionLayout(true)
                        resetActions()
                    }
                }
            }
        }
    }

    private fun updateStatus(string: String?) {
        activity?.runOnUiThread { binding.txtStatus.text = string }
    }

    private fun updateActionLayout(isVisible: Boolean) {
        if (isVisible) {
            binding.layoutAction.show()
            binding.toolbar.visibility = View.VISIBLE
        } else {
            binding.layoutAction.hide()
            binding.toolbar.visibility = View.GONE
        }
    }

    private fun attachActions() {
        binding.btnAnonymous.addOnClickListener {
            if (viewModel.authState.value != AuthState.Fetching) {
                binding.btnAnonymous.updateProgress(true)
                viewModel.buildAnonymousAuthData()
            }
        }

        binding.btnGoogle.addOnClickListener {
            if (viewModel.authState.value != AuthState.Fetching) {
                binding.btnGoogle.updateProgress(true)
                if (canLoginWithMicroG) {
                    Log.i(TAG, "Found supported microG, trying to request credentials")
                    val accountIntent = AccountManager.newChooseAccountIntent(
                        null,
                        null,
                        arrayOf(GOOGLE_ACCOUNT_TYPE),
                        null,
                        null,
                        null,
                        null
                    )
                    startForAccount.launch(accountIntent)
                } else {
                    findNavController().navigate(R.id.googleFragment)
                }
            }
        }
    }

    private fun resetActions() {
        binding.btnGoogle.apply {
            updateProgress(false)
            isEnabled = true
        }

        binding.btnAnonymous.apply {
            updateProgress(false)
            isEnabled = true
        }
    }

    private fun navigateToDefaultTab() {
        val defaultDestination =
            Preferences.getInteger(requireContext(), PREFERENCE_DEFAULT_SELECTED_TAB)
        val directions =
            when (requireArguments().getInt("destinationId", defaultDestination)) {
                R.id.updatesFragment -> {
                    requireArguments().remove("destinationId")
                    SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                }

                1 -> SplashFragmentDirections.actionSplashFragmentToGamesContainerFragment()
                2 -> SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                else -> SplashFragmentDirections.actionSplashFragmentToNavigationApps()
            }
        requireActivity().viewModelStore.clear() // Clear ViewModelStore to avoid bugs with logout
        findNavController().navigate(directions)
    }

    private fun getPackageName(intent: Intent): String? {
        return when {
            intent.action == Intent.ACTION_VIEW -> {
                intent.data!!.getQueryParameter("id")
            }

            intent.action == Intent.ACTION_SEND -> {
                val clipData = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                UrlQuerySanitizer(clipData).getValue("id")
            }

            isNAndAbove && intent.action == Intent.ACTION_SHOW_APP_INFO -> {
                intent.extras?.getString(Intent.EXTRA_PACKAGE_NAME)
            }

            intent.extras != null -> {
                intent.extras?.getString("packageName")
            }

            else -> requireArguments().getString("packageName")
        }
    }

    private fun requestAuthTokenForGoogle(accountName: String, oldToken: String? = null) {
        try {
            if (oldToken != null) {
                // Invalidate the old token before requesting a new one
                AccountManager.get(requireContext()).invalidateAuthToken(
                    GOOGLE_ACCOUNT_TYPE,
                    oldToken
                )
            }

            AccountManager.get(requireContext())
                .getAuthToken(
                    Account(accountName, GOOGLE_ACCOUNT_TYPE),
                    GOOGLE_PLAY_AUTH_TOKEN_TYPE,
                    bundleOf(
                        "overridePackage" to GOOGLE_PLAY_PACKAGE_NAME,
                        "overrideCertificate" to Base64.decode(GOOGLE_PLAY_CERT, Base64.DEFAULT)
                    ),
                    requireActivity(),
                    {
                        viewModel.buildGoogleAuthData(
                            accountName,
                            it.result.getString(AccountManager.KEY_AUTHTOKEN)!!,
                            AuthHelper.Token.AUTH
                        )
                    },
                    Handler(Looper.getMainLooper())
                )
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get authToken for Google login")
        }
    }
}
