/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.content.Intent
import android.net.Uri
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.api.Client
import top.xjunz.tasker.api.UpdateInfo
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.peekActivity
import top.xjunz.tasker.ktx.setValueIfObserved
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.service.OperatingMode
import top.xjunz.tasker.service.a11yAutomatorService
import top.xjunz.tasker.service.controller.ServiceController
import top.xjunz.tasker.service.serviceController
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.storage.TaskStorage
import java.io.File

/**
 * @author xjunz 2022/07/08
 */
class MainViewModel : ViewModel(), ServiceController.ServiceStateListener {

    companion object {

        fun LifecycleOwner.peekMainViewModel(): MainViewModel {
            return (peekActivity() as MainActivity).viewModels<MainViewModel>().value
        }
    }

    private val onDialogShown = MutableLiveData<DialogStackManager.StackEntry>()

    private val onDialogDismissed = MutableLiveData<DialogStackManager.StackEntry>()

    private val client = Client()

    val taskNumbers = Array<MutableLiveData<Int>>(3) { MutableLiveData() }

    val appbarHeight = MutableLiveData<Int>()

    val paddingBottom = MutableLiveData<Int>()

    val allTaskLoaded = MutableLiveData<Boolean>()

    val onNewIntent = MutableLiveData<Pair<Uri?, Any?>>()

    val stopServiceConfirmation = MutableLiveData<Boolean>()

    val isServiceRunning = MutableLiveData(false)

    val isServiceBinding = MutableLiveData<Boolean>()

    val serviceBindingError = MutableLiveData<Any?>()

    val operatingMode = MutableLiveData(OperatingMode.CURRENT)

    val checkingForUpdates = MutableLiveData<Boolean>()

    val checkingForUpdatesError = MutableLiveData<String>()

    var showUpdateDialog = true

    val requestShareFile = MutableLiveData<File>()

    val requestImportTask = MutableLiveData<Intent>()

    init {
        AppletOptionFactory.preloadIfNeeded()
        if (TaskStorage.storageTaskLoaded) {
            allTaskLoaded.value = true
        } else viewModelScope.launch {
            TaskStorage.loadAllTasks()
            allTaskLoaded.value = true
        }
    }

    fun notifyDialogShown(tag: String?, isFullScreen: Boolean) {
        onDialogShown.value = DialogStackManager.push(
            requireNotNull(tag) { "Tag is null" }, isFullScreen
        )
    }

    fun notifyDialogDismissed() {
        onDialogDismissed.value = DialogStackManager.pop()
    }

    fun doOnDialogShown(
        lifecycleOwner: LifecycleOwner,
        block: (DialogStackManager.StackEntry) -> Unit
    ) {
        lifecycleOwner.observeTransient(onDialogShown, block)
    }

    fun doOnDialogDismissed(
        lifecycleOwner: LifecycleOwner,
        block: (DialogStackManager.StackEntry) -> Unit
    ) {
        lifecycleOwner.observeTransient(onDialogDismissed, block)
    }

    fun setCurrentOperatingMode(mode: OperatingMode) {
        serviceController.removeStateListener()
        Preferences.operatingMode = mode.VALUE
        serviceController.setStateListener(this)
        operatingMode.value = mode
        isServiceRunning.value = serviceController.isServiceRunning
        if (mode == OperatingMode.Accessibility
            && A11yAutomatorService.get()?.isInspectorMode == true
        ) {
            a11yAutomatorService.switchToWorkerMode()
        }
    }

    fun toggleService() {
        if (isServiceRunning.isTrue) {
            serviceController.stopService()
        } else {
            serviceController.bindService()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            checkingForUpdates.setValueIfObserved(true)
            runCatching {
                client.checkForUpdates()
            }.onSuccess { response ->
                if (response.status == HttpStatusCode.OK) {
                    runCatching {
                        Json.decodeFromString<UpdateInfo>(response.bodyAsText())
                    }.onFailure {
                        checkingForUpdatesError.setValueIfObserved(it.message)
                    }.onSuccess {
                        app.updateInfo.value = it
                    }
                } else {
                    checkingForUpdatesError.setValueIfObserved(
                        R.string.format_request_failed.format(response.status)
                    )
                }
            }.onFailure {
                checkingForUpdatesError.value = R.string.format_request_failed.format(it.message)
            }
            checkingForUpdates.setValueIfObserved(false)
        }
    }

    override fun onStartBinding() {
        isServiceBinding.postValue(true)
    }

    override fun onError(t: Any?) {
        isServiceBinding.postValue(false)
        isServiceRunning.postValue(false)
        serviceBindingError.postValue(t)
    }

    override fun onServiceStarted() {
        isServiceBinding.postValue(false)
        isServiceRunning.postValue(true)
    }

    override fun onServiceDisconnected() {
        isServiceRunning.postValue(false)
    }

    fun <V> doOnRouted(
        lifecycleOwner: LifecycleOwner,
        host: String,
        block: (V) -> Unit
    ) {
        lifecycleOwner.observeTransient(onNewIntent) {
            if (it.first?.host == host) {
                block(it.second!!.casted())
            }
        }
    }

    fun doOnRouted(
        lifecycleOwner: LifecycleOwner,
        host: String,
        block: () -> Unit
    ) {
        lifecycleOwner.observeTransient(onNewIntent) {
            if (it.first?.host == host) {
                block()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}