/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.splash

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.store.BuildConfig
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.AuthState
import com.aurora.store.util.CertUtil.GOOGLE_ACCOUNT_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_AUTH_TOKEN_TYPE
import com.aurora.store.util.CertUtil.GOOGLE_PLAY_CERT
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.viewmodel.auth.AuthViewModel

@Composable
fun SplashScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    deepLinkPackageName: String? = null,
    deepLinkDevId: String? = null,
    onNavigateTo: (Destination) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    val canMicroGLogin = PackageUtil.hasSupportedMicroGVariant(context) &&
        Preferences.getBoolean(context, Preferences.PREFERENCE_MICROG_AUTH, true)

    var anonymousLoading by remember { mutableStateOf(false) }
    var googleLoading by remember { mutableStateOf(false) }

    val accountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank()) {
            requestAuthTokenForGoogle(viewModel, context, accountName, null, activity)
        } else {
            googleLoading = false
        }
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            AuthState.Valid, AuthState.SignedIn -> {
                anonymousLoading = false
                googleLoading = false
                when {
                    !deepLinkDevId.isNullOrBlank() -> onNavigateTo(
                        Destination.DevProfile(deepLinkDevId)
                    )
                    !deepLinkPackageName.isNullOrBlank() -> onNavigateTo(
                        Destination.AppDetails(deepLinkPackageName)
                    )
                    else -> {
                        val defaultTab = Preferences.getInteger(
                            context,
                            Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
                        )
                        onNavigateTo(Destination.Main(defaultTab))
                    }
                }
            }

            is AuthState.PendingAccountManager -> {
                requestAuthTokenForGoogle(viewModel, context, state.email, state.token, activity)
            }

            is AuthState.Failed -> {
                anonymousLoading = false
                googleLoading = false
            }

            else -> Unit
        }
    }

    val showActions = authState is AuthState.Unavailable ||
        authState is AuthState.Failed ||
        authState is AuthState.SignedOut

    val loading = authState == AuthState.Fetching ||
        authState == AuthState.Available ||
        authState == AuthState.Verifying ||
        authState is AuthState.PendingAccountManager

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = showActions) {
                TopAppBar(
                    actions = {
                        IconButton(onClick = { onNavigateTo(Destination.Settings) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu_settings),
                                contentDescription = stringResource(R.string.title_settings)
                            )
                        }
                        IconButton(onClick = { onNavigateTo(Destination.About) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu_about),
                                contentDescription = stringResource(R.string.title_about)
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dimensionResource(R.dimen.padding_xlarge)),
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.margin_large),
                Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = R.drawable.ic_logo_alt,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(192.dp)
            )
            Text(
                text = statusText(authState),
                textAlign = TextAlign.Center
            )
            if (loading) {
                Spacer(Modifier.height(dimensionResource(R.dimen.margin_large)))
                CircularProgressIndicator()
            }
            AnimatedVisibility(visible = showActions) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.margin_small)
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.width(dimensionResource(R.dimen.width_button)),
                        onClick = {
                            if (canMicroGLogin) {
                                googleLoading = true
                                accountLauncher.launch(
                                    AccountManager.newChooseAccountIntent(
                                        null,
                                        null,
                                        arrayOf(GOOGLE_ACCOUNT_TYPE),
                                        null,
                                        null,
                                        null,
                                        null
                                    )
                                )
                            } else {
                                onNavigateTo(Destination.GoogleLogin)
                            }
                        },
                        enabled = !googleLoading && !anonymousLoading
                    ) {
                        if (googleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.account_google))
                        }
                    }
                    if (BuildConfig.SHOW_ANONYMOUS_LOGIN) {
                        OutlinedButton(
                            modifier = Modifier.width(dimensionResource(R.dimen.width_button)),
                            onClick = {
                                anonymousLoading = true
                                viewModel.buildAnonymousAuthData()
                            },
                            enabled = !googleLoading && !anonymousLoading
                        ) {
                            if (anonymousLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.account_anonymous))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusText(authState: AuthState): String = when (authState) {
    AuthState.Init -> stringResource(R.string.session_init)
    AuthState.Available -> stringResource(R.string.session_verifying)
    AuthState.Fetching -> stringResource(R.string.requesting_new_session)
    AuthState.Verifying -> stringResource(R.string.verifying_new_session)
    AuthState.Valid, AuthState.SignedIn -> stringResource(R.string.session_verifying)
    AuthState.Unavailable, AuthState.SignedOut -> stringResource(R.string.session_login)
    is AuthState.PendingAccountManager -> stringResource(R.string.requesting_new_session)
    is AuthState.Failed -> authState.status
}

private fun requestAuthTokenForGoogle(
    viewModel: AuthViewModel,
    context: android.content.Context,
    accountName: String,
    oldToken: String?,
    activity: ComponentActivity?
) {
    try {
        val accountManager = AccountManager.get(context)
        if (oldToken != null) {
            accountManager.invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, oldToken)
        }
        accountManager.getAuthToken(
            Account(accountName, GOOGLE_ACCOUNT_TYPE),
            GOOGLE_PLAY_AUTH_TOKEN_TYPE,
            Bundle().apply {
                putString("overridePackage", PACKAGE_NAME_PLAY_STORE)
                putByteArray("overrideCertificate", Base64.decode(GOOGLE_PLAY_CERT, Base64.DEFAULT))
            },
            activity,
            { result ->
                viewModel.buildGoogleAuthData(
                    accountName,
                    result.result.getString(AccountManager.KEY_AUTHTOKEN)!!,
                    AuthHelper.Token.AUTH
                )
            },
            Handler(Looper.getMainLooper())
        )
    } catch (_: Exception) {
        Log.e("SplashScreen", "Failed to get authToken for Google login")
    }
}
