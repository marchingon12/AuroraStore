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

package com.aurora.store.view.ui.spoof

import android.os.Bundle
import android.util.Log
import android.view.View
import com.aurora.extensions.toast
import com.aurora.store.R
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.databinding.FragmentGenericRecyclerBinding
import com.aurora.store.view.epoxy.views.preference.LocaleViewModel_
import com.aurora.store.view.ui.commons.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LocaleSpoofFragment : BaseFragment<FragmentGenericRecyclerBinding>() {

    private val TAG = LocaleSpoofFragment::class.java.simpleName

    @Inject
    lateinit var spoofProvider: SpoofProvider

    private lateinit var locale: Locale

    companion object {
        @JvmStatic
        fun newInstance(): LocaleSpoofFragment {
            return LocaleSpoofFragment().apply {

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locale = if (spoofProvider.isLocaleSpoofEnabled()) {
            spoofProvider.getSpoofLocale()
        } else {
            Locale.getDefault()
        }

        try {
            updateController(fetchAvailableLocales())
        } catch (exception: Exception) {
            Log.e(TAG, "Could not get available locales", exception)
        }
    }

    private fun updateController(locales: List<Locale>) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            locales
                .sortedBy { it.displayName }
                .forEach {
                add(
                    LocaleViewModel_()
                        .id(it.language)
                        .markChecked(locale == it)
                        .checked { _, checked ->
                            if (checked) {
                                locale = it
                                saveSelection(it)
                                requestModelBuild()
                            }
                        }
                        .locale(it)
                )
            }
        }
    }

    private fun fetchAvailableLocales(): List<Locale> {
        val locales = Locale.getAvailableLocales()
        val localeList: MutableList<Locale> = ArrayList()
        localeList.addAll(locales)
        localeList.add(0, Locale.getDefault())
        return localeList
    }

    private fun saveSelection(locale: Locale) {
        requireContext().toast(R.string.spoof_apply)
        spoofProvider.setSpoofLocale(locale)
    }
}
