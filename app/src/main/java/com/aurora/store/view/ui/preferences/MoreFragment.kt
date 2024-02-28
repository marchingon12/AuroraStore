package com.aurora.store.view.ui.preferences

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aurora.store.R

class MoreFragment : PreferenceFragmentCompat() {

    private val TAG = MoreFragment::class.java.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_more, rootKey)

        findPreference<Preference>("blacklistFragment")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.blacklistFragment)
            true
        }
        findPreference<Preference>("spoofFragment")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.spoofFragment)
            true
        }

        findPreference<Preference>("appsGamesFragment")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.appsGamesFragment)
            true
        }
        findPreference<Preference>("appSalesFragment")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.appSalesFragment)
            true
        }
        findPreference<Preference>("downloadFragment")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.downloadFragment)
            true
        }

        findPreference<Preference>("aboutFragment")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.aboutFragment)
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            title = getString(R.string.title_more)
            navigationIcon = null
            inflateMenu(R.menu.menu_more)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.accountFragment -> findNavController().navigate(R.id.accountFragment)
                    R.id.settingsFragment -> findNavController().navigate(R.id.settingsFragment)
                    R.id.aboutFragment -> findNavController().navigate(R.id.aboutFragment)
                    else -> Log.i(TAG, "Got an unhandled ID: ${it.itemId}")
                }
                true
            }
        }
        view.findViewById<LinearLayout>(R.id.appLogoLayout).visibility = View.VISIBLE
    }
}
