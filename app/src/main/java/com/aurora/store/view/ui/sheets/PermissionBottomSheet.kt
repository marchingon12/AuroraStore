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

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.fragment.navArgs
import com.aurora.extensions.hide
import com.aurora.extensions.show
import com.aurora.store.R
import com.aurora.store.data.model.PermissionGroupInfo
import com.aurora.store.databinding.SheetPermissionsBinding
import com.aurora.store.view.custom.layouts.PermissionGroup
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PermissionBottomSheet : BaseBottomSheet() {

    private lateinit var B: SheetPermissionsBinding
    private lateinit var packageManager: PackageManager

    private val args: PermissionBottomSheetArgs by navArgs()

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): View {
        B = SheetPermissionsBinding.inflate(inflater)
        return B.root
    }

    override fun onContentViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inflateData()
    }

    private fun inflateData() {
        packageManager = requireContext().packageManager

        val permissionGroupWidgets: MutableMap<String, PermissionGroup?> =
            HashMap<String, PermissionGroup?>()
        for (permissionName in args.app.permissions) {

            val permissionInfo = getPermissionInfo(permissionName) ?: continue
            val permissionGroupInfo = getPermissionGroupInfo(permissionInfo)
            var permissionGroup: PermissionGroup?

            if (permissionGroupWidgets.containsKey(permissionGroupInfo.name)) {
                permissionGroup = permissionGroupWidgets[permissionGroupInfo.name]
            } else {
                permissionGroup = PermissionGroup(context)
                permissionGroup.setPermissionGroupInfo(permissionGroupInfo)
                permissionGroup.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                permissionGroupWidgets[permissionGroupInfo.name] = permissionGroup
            }

            permissionGroup?.addPermission(permissionInfo)
        }

        B.permissionsContainer.removeAllViews()

        val permissionGroupLabels: List<String> = ArrayList(permissionGroupWidgets.keys)
        permissionGroupLabels.sortedBy { it }.forEach {
            B.permissionsContainer.addView(permissionGroupWidgets[it])
        }

        if (permissionGroupLabels.isEmpty())
            B.permissionsNone.show()
        else
            B.permissionsNone.hide()
    }

    private fun getPermissionInfo(permissionName: String): PermissionInfo? {
        return try {
            packageManager.getPermissionInfo(permissionName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun getPermissionGroupInfo(permissionInfo: PermissionInfo): PermissionGroupInfo {
        val permissionGroupInfo: PermissionGroupInfo = if (null == permissionInfo.group) {
            getFakePermissionGroupInfo(permissionInfo.packageName)
        } else {
            try {
                val platformGroup = packageManager.getPermissionGroupInfo(permissionInfo.group!!, 0)
                PermissionGroupInfo(
                    platformGroup.name,
                    platformGroup.icon,
                    platformGroup.loadLabel(packageManager).toString()
                )
            } catch (e: PackageManager.NameNotFoundException) {
                getFakePermissionGroupInfo(permissionInfo.packageName)
            }
        }
        if (permissionGroupInfo.icon == 0) {
            permissionGroupInfo.icon = R.drawable.ic_permission_android
        }
        return permissionGroupInfo
    }

    private fun getFakePermissionGroupInfo(packageName: String): PermissionGroupInfo {
        return when (packageName) {
            "android" -> PermissionGroupInfo("android", R.drawable.ic_permission_android)
            "com.google.android.gsf",
            "com.android.vending" -> PermissionGroupInfo("google", R.drawable.ic_permission_google)

            else -> PermissionGroupInfo()
        }
    }
}
