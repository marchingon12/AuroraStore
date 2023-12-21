package com.aurora.store.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.gplayapi.data.models.App
import com.aurora.store.BuildConfig
import com.aurora.store.data.model.SelfUpdate
import com.aurora.store.util.PathUtil
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : ViewModel() {

    private val TAG = MainViewModel::class.java.simpleName

    private val _selfUpdate = MutableStateFlow(App(packageName = String()))
    val selfUpdate = _selfUpdate.asStateFlow()

    init {
        checkSelfUpdate()
    }

    private fun checkSelfUpdate() {
        val updateFile = PathUtil.getUpdateFile(context)
        if (updateFile.exists()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val update = gson.fromJson(
                        updateFile.bufferedReader().readText(),
                        SelfUpdate::class.java
                    )
                    val app = SelfUpdate.toApp(update, context)
                    if (app.versionCode > BuildConfig.VERSION_CODE) {
                        _selfUpdate.value = app
                    }
                } catch (exception: Exception) {
                    Log.d(TAG, "Failed to parse update file", exception)
                }
            }
        }
    }
}
