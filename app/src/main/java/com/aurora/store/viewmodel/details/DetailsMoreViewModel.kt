package com.aurora.store.viewmodel.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsMoreViewModel @Inject constructor(
    private val appDetailsHelper: AppDetailsHelper
) : ViewModel() {

    private val TAG = DetailsMoreViewModel::class.java.simpleName

    private val dependantAppsStash = mutableMapOf<String, List<App>>()
    private val _dependentApps = MutableSharedFlow<List<App>>()
    val dependentApps = _dependentApps.asSharedFlow()

    fun fetchDependentApps(app: App) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dependantApps = dependantAppsStash.getOrPut(app.packageName) {
                    appDetailsHelper.getAppByPackageName(app.dependencies.dependentPackages)
                }

                _dependentApps.emit(dependantApps)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch dependencies", exception)
                dependantAppsStash[app.packageName] = emptyList()
                _dependentApps.emit(emptyList())
            }
        }
    }
}
