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

package com.aurora.store.viewmodel.homestream

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.StreamHelper
import com.aurora.store.data.ViewState
import com.aurora.store.data.network.HttpClient
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class BaseClusterViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    var authData: AuthData = AuthProvider.with(context).getAuthData()

    var streamHelper: StreamHelper =
        StreamHelper(authData).using(HttpClient.getPreferredClient(context))

    val liveData: MutableLiveData<ViewState> = MutableLiveData()
    var streamBundle: StreamBundle = StreamBundle()

    lateinit var type: StreamHelper.Type
    lateinit var category: StreamHelper.Category

    fun getStreamBundle(category: StreamHelper.Category, type: StreamHelper.Type) {
        this.type = type
        this.category = category
        liveData.postValue(ViewState.Loading)
        observe()
    }

    fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (!streamBundle.hasCluster() || streamBundle.hasNext()) {

                        //Fetch new stream bundle
                        val newBundle = if (streamBundle.streamClusters.isEmpty()) {
                            streamHelper.getNavStream(type, category)
                        } else {
                            streamHelper.next(streamBundle.streamNextPageUrl)
                        }

                        //Update old bundle
                        streamBundle.apply {
                            streamClusters.putAll(newBundle.streamClusters)
                            streamNextPageUrl = newBundle.streamNextPageUrl
                        }

                        //Post updated to UI
                        liveData.postValue(ViewState.Success(streamBundle))
                    } else {
                        Log.i("End of Bundle")
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    fun observeCluster(streamCluster: StreamCluster) {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    if (streamCluster.hasNext()) {
                        val newCluster =
                            streamHelper.getNextStreamCluster(streamCluster.clusterNextPageUrl)
                        updateCluster(streamCluster.id, newCluster)
                        liveData.postValue(ViewState.Success(streamBundle))
                    } else {
                        Log.i("End of cluster")
                        streamCluster.clusterNextPageUrl = String()
                    }
                } catch (e: Exception) {
                    liveData.postValue(ViewState.Error(e.message))
                }
            }
        }
    }

    private fun updateCluster(clusterID: Int, newCluster: StreamCluster) {
        streamBundle.streamClusters[clusterID]?.apply {
            clusterAppList.addAll(newCluster.clusterAppList)
            clusterNextPageUrl = newCluster.clusterNextPageUrl
        }
    }
}
