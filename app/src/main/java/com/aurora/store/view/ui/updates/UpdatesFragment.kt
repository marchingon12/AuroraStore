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

package com.aurora.store.view.ui.updates

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.aurora.Constants
import com.aurora.extensions.isRAndAbove
import com.aurora.extensions.stackTraceToString
import com.aurora.extensions.toast
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.State
import com.aurora.store.data.downloader.getGroupId
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.UpdateFile
import com.aurora.store.data.service.UpdateService
import com.aurora.store.databinding.FragmentUpdatesBinding
import com.aurora.store.util.Log
import com.aurora.store.util.PathUtil
import com.aurora.store.util.isExternalStorageEnable
import com.aurora.store.view.epoxy.views.UpdateHeaderViewModel_
import com.aurora.store.view.epoxy.views.app.AppUpdateViewModel_
import com.aurora.store.view.epoxy.views.app.NoAppViewModel_
import com.aurora.store.view.epoxy.views.shimmer.AppListViewShimmerModel_
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.view.ui.sheets.AppMenuSheet
import com.aurora.store.viewmodel.all.UpdatesViewModel
import com.tonyodev.fetch2.AbstractFetchGroupListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import nl.komponents.kovenant.task
import java.io.File

class UpdatesFragment : BaseFragment() {

    private lateinit var B: FragmentUpdatesBinding
    private lateinit var VM: UpdatesViewModel
    private lateinit var app: App

    private val startForStorageManagerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isRAndAbove() && Environment.isExternalStorageManager()) {
                updateSingle(app)
            } else {
                toast(R.string.permissions_denied)
            }
        }
    private val startForPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { perm ->
            if (perm) updateSingle(app) else toast(R.string.permissions_denied)
        }

    val listOfActionsWhenServiceAttaches = ArrayList<Runnable>()
    private lateinit var fetchListener: AbstractFetchGroupListener

    private var updateService: UpdateService? = null
    private var attachToServiceCalled = false
    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            updateService = (binder as UpdateService.UpdateServiceBinder).getUpdateService()
            updateService!!.registerFetchListener(fetchListener)
            if (listOfActionsWhenServiceAttaches.isNotEmpty()) {
                val iterator = listOfActionsWhenServiceAttaches.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    next.run()
                    iterator.remove()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            updateService = null
            attachToServiceCalled = false
        }
    }

    private var updateFileMap: MutableMap<Int, UpdateFile> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        B = FragmentUpdatesBinding.bind(
            inflater.inflate(
                R.layout.fragment_updates,
                container,
                false
            )
        )

        VM = ViewModelProvider(requireActivity()).get(UpdatesViewModel::class.java)


        fetchListener = object : AbstractFetchGroupListener() {

            override fun onAdded(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                VM.updateDownload(groupId, fetchGroup)
            }

            override fun onProgress(
                groupId: Int,
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long,
                fetchGroup: FetchGroup
            ) {
                VM.updateDownload(groupId, fetchGroup)
            }

            override fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                if (fetchGroup.groupDownloadProgress == 100) {
                    VM.updateDownload(groupId, fetchGroup, isComplete = true)
                }
            }

            override fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                VM.updateDownload(groupId, fetchGroup, isCancelled = true)
            }

            override fun onDeleted(groupId: Int, download: Download, fetchGroup: FetchGroup) {
                VM.updateDownload(groupId, fetchGroup, isCancelled = true)
            }
        }

        getUpdateServiceInstance()

        return B.root
    }

    override fun onResume() {
        getUpdateServiceInstance()
        super.onResume()
    }

    override fun onPause() {
        if (updateService != null) {
            updateService = null
            attachToServiceCalled = false
            requireContext().unbindService(serviceConnection)
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (updateService != null) {
            updateService = null
            attachToServiceCalled = false
            requireContext().unbindService(serviceConnection)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        VM.liveUpdateData.observe(viewLifecycleOwner) {
            updateFileMap = it
            updateController(updateFileMap)
            B.swipeRefreshLayout.isRefreshing = false
            updateService?.liveUpdateData?.postValue(updateFileMap)
        }

        B.swipeRefreshLayout.setOnRefreshListener {
            VM.observe()
        }

        updateController(null)
    }

    private fun updateController(updateFileMap: MutableMap<Int, UpdateFile>?) {
        B.recycler.withModels {
            setFilterDuplicates(true)
            if (updateFileMap == null) {
                for (i in 1..6) {
                    add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )
                }
            } else {
                if (updateFileMap.isEmpty()) {
                    add(
                        NoAppViewModel_()
                            .id("no_update")
                            .icon(R.drawable.ic_updates)
                            .message(getString(R.string.details_no_updates))
                    )
                } else {
                    add(
                        UpdateHeaderViewModel_()
                            .id("header_all")
                            .title("${updateFileMap.size} " +
                                if (updateFileMap.size == 1)
                                    getString(R.string.update_available)
                                else
                                    getString(R.string.updates_available)
                            )
                            .action(
                                if (VM.updateAllEnqueued)
                                    getString(R.string.action_cancel)
                                else
                                    getString(R.string.action_update_all)
                            )
                            .click { _ ->
                                if (VM.updateAllEnqueued)
                                    cancelAll()
                                else
                                    updateFileMap.values.forEach { updateSingle(it.app, true) }

                                requestModelBuild()
                            }
                    )

                    updateFileMap.values.forEach { updateFile ->
                        add(
                            AppUpdateViewModel_()
                                .id(updateFile.hashCode())
                                .updateFile(updateFile)
                                .click { _ -> openDetailsFragment(updateFile.app) }
                                .longClick { _ ->
                                    openAppMenuSheet(updateFile.app)
                                    false
                                }
                                .positiveAction { _ -> updateSingle(updateFile.app) }
                                .negativeAction { _ -> cancelSingle(updateFile.app) }
                                .installAction { _ ->
                                    install(
                                        updateFile.app.packageName,
                                        updateFile.group?.downloads
                                    )
                                }
                                .state(updateFile.state)
                        )
                    }
                }
            }
        }
    }

    fun runInService(runnable: Runnable) {
        if (updateService == null) {
            listOfActionsWhenServiceAttaches.add(runnable)
            getUpdateServiceInstance()
        } else {
            runnable.run()
        }
    }

    private fun updateSingle(app: App, updateAll: Boolean = false) {
        this.app = app
        runInService {
            VM.updateState(app.getGroupId(requireContext()), State.QUEUED)
            VM.updateAllEnqueued = updateAll

            if (PathUtil.needsStorageManagerPerm(app.fileList) ||
                requireContext().isExternalStorageEnable()) {
                if (isRAndAbove()) {
                    if (!Environment.isExternalStorageManager()) {
                        startForStorageManagerResult.launch(
                            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        )
                    } else {
                        updateService?.updateApp(app)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        updateService?.updateApp(app)
                    } else {
                        startForPermissions.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            } else {
                updateService?.updateApp(app)
            }
        }
    }

    private fun cancelSingle(app: App) {
        runInService {
            updateService?.fetch?.cancelGroup(app.getGroupId(requireContext()))
        }
    }

    private fun cancelAll() {
        runInService {
            VM.updateAllEnqueued = false
            updateFileMap.values.forEach { updateService?.fetch?.cancelGroup(it.app.getGroupId(requireContext())) }
        }
    }

    fun getUpdateServiceInstance() {
        if (updateService == null && !attachToServiceCalled) {
            attachToServiceCalled = true
            val intent = Intent(requireContext(), UpdateService::class.java)
            requireContext().startService(intent)
            requireContext().bindService(
                intent,
                serviceConnection,
                0
            )
        }
    }

    @Synchronized
    private fun install(packageName: String, files: List<Download>?) {
        files?.let { downloads ->
            var filesExist = true

            downloads.forEach { download ->
                filesExist = filesExist && File(download.file).exists()
            }

            if (filesExist) {
                task {
                    AppInstaller.getInstance(requireContext())
                        .getPreferredInstaller()
                        .install(
                            packageName,
                            files
                                .filter { it.file.endsWith(".apk") }
                                .map { it.file }.toList()
                        )
                } fail {
                    Log.e(it.stackTraceToString())
                }
            }
        }
    }

    private fun openAppMenuSheet(app: App) {
        val fragment = childFragmentManager.findFragmentByTag(AppMenuSheet.TAG)
        if (fragment != null)
            childFragmentManager.beginTransaction().remove(fragment)

        AppMenuSheet().apply {
            arguments = Bundle().apply {
                putString(Constants.STRING_EXTRA, gson.toJson(app))
            }
        }.show(
            childFragmentManager,
            AppMenuSheet.TAG
        )
    }
}