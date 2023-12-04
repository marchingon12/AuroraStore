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

package com.aurora.store.view.ui.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.extensions.isTAndAbove
import com.aurora.store.R
import com.aurora.store.util.Preferences
import com.aurora.store.util.save
import com.aurora.store.view.custom.preference.ListPreferenceMaterialDialogFragmentCompat
import com.aurora.store.view.custom.preference.ListPreferenceMaterialDialogFragmentCompat.Companion.PREFERENCE_DIALOG_FRAGMENT_TAG
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class UIPreference : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_ui, rootKey)

        findPreference<Preference>("PREFERENCE_APP_LANGUAGE")?.apply {
            if (isTAndAbove()) {
                summary = Locale.getDefault().displayName
                setOnPreferenceClickListener {
                    startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                        data = Uri.parse("package:" + requireContext().packageName)
                    })
                    true
                }
            } else {
                isVisible = false
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ListPreference) {
            val dialogFragment = ListPreferenceMaterialDialogFragmentCompat.newInstance(preference.getKey())
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, PREFERENCE_DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.pref_ui_title)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        val themePreference: ListPreference? = findPreference(Preferences.PREFERENCE_THEME_TYPE)
        themePreference?.let {
            it.setOnPreferenceChangeListener { _, newValue ->
                val themeId = Integer.parseInt(newValue.toString())

                save(Preferences.PREFERENCE_THEME_TYPE, themeId)
                requireActivity().recreate()
                true
            }
        }

        val accentPreference: ListPreference? = findPreference(Preferences.PREFERENCE_THEME_ACCENT)
        accentPreference?.let {
            it.setOnPreferenceChangeListener { _, newValue ->
                val accentId = Integer.parseInt(newValue.toString())

                save(Preferences.PREFERENCE_THEME_ACCENT, accentId)
                requireActivity().recreate()
                true
            }
        }
    }
}
