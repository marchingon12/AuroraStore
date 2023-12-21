/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.aurora.store.view.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.aurora.store.R
import com.aurora.store.data.installer.SessionInstaller
import com.aurora.store.databinding.SheetSelfUpdateBinding
import com.aurora.store.util.DownloadWorkerUtil
import com.aurora.store.util.PathUtil
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelfUpdateSheet : BaseBottomSheet() {

    private var _binding: SheetSelfUpdateBinding? = null
    private val binding get() = _binding!!

    private val args: SelfUpdateSheetArgs by navArgs()

    @Inject
    lateinit var downloadWorkerUtil: DownloadWorkerUtil

    override fun onCreateContentView(
        inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?
    ): View {
        _binding = SheetSelfUpdateBinding.inflate(inflater)
        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtLine2.text = ("${args.app.versionName} (${args.app.versionCode})")
        binding.txtChangelog.text = args.app.changes
            .ifEmpty { getString(R.string.details_changelog_unavailable) }
            .trim()

        binding.btnPrimary.setOnClickListener {
            // Check if files are still present, else proceed to re-download the app
            val files = File(
                PathUtil.getAppDownloadDir(
                    requireContext(),
                    args.app.packageName,
                    args.app.versionCode
                ).path
            ).listFiles()

            if (files?.isNotEmpty() == true) {
                val apkFiles = files.filter { it.path.endsWith(".apk") }
                SessionInstaller(requireContext()).install(args.app.packageName, apkFiles)
            } else {
                GlobalScope.launch { downloadWorkerUtil.enqueueApp(args.app) }
                binding.btnPrimary.isEnabled = false
            }
        }

        binding.btnSecondary.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
