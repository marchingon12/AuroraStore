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

package com.aurora.store.view.ui.account

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.aurora.Constants.URL_DISCLAIMER
import com.aurora.Constants.URL_LICENSE
import com.aurora.Constants.URL_TOS
import com.aurora.extensions.browse
import com.aurora.store.R
import com.aurora.store.databinding.FragmentAccountBinding
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.viewmodel.account.AccountViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    private val viewModel: AccountViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Chips
        view.context.apply {
            binding.chipDisclaimer.setOnClickListener { browse(URL_DISCLAIMER) }
            binding.chipLicense.setOnClickListener { browse(URL_LICENSE) }
            binding.chipTos.setOnClickListener { browse(URL_TOS) }
        }

        viewModel.authProvider.authData?.userProfile?.let {
            val avatar =
                if (viewModel.authProvider.isAnonymous) R.mipmap.ic_launcher else it.artwork.url
            binding.imgAvatar.load(avatar) {
                placeholder(R.drawable.bg_placeholder)
                transformations(RoundedCornersTransformation(32F))
            }
            binding.txtName.text = if (viewModel.authProvider.isAnonymous) "Anonymous" else it.name
            binding.txtEmail.text =
                if (viewModel.authProvider.isAnonymous) "anonymous@gmail.com" else it.email
        }

        binding.btnLogout.addOnClickListener {
            findNavController().navigate(R.id.logoutDialog)
        }
    }
}
