/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.net.Uri
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.peekActivity
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.service.OperatingMode
import top.xjunz.tasker.service.controller.ServiceController
import top.xjunz.tasker.service.serviceController
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.storage.TaskStorage

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

    val appbarHeight = MutableLiveData<Int>()

    val paddingBottom = MutableLiveData<Int>()

    val allTaskLoaded = MutableLiveData<Boolean>()

    val onNewIntent = MutableLiveData<Pair<Uri?, Any?>>()

    val stopServiceConfirmation = MutableLiveData<Boolean>()

    val isServiceRunning = MutableLiveData(false)

    val isServiceBinding = MutableLiveData<Boolean>()

    val serviceBindingError = MutableLiveData<Throwable>()

    val operatingMode = MutableLiveData(OperatingMode.CURRENT)

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
    }

    fun toggleService() {
        if (isServiceRunning.isTrue) {
            serviceController.stopService()
        } else {
            serviceController.bindService()
        }
    }

    override fun onStartBinding() {
        isServiceBinding.postValue(true)
    }

    override fun onError(t: Throwable) {
        isServiceBinding.postValue(false)
        isServiceRunning.postValue(false)
        serviceBindingError.postValue(t)
    }

    override fun onServiceBound() {
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

    fun toggleOperatingMode() {
        if (serviceController.isServiceRunning) {
            toast(R.string.error_unable_to_switch_mode)
            return
        }
        if (OperatingMode.CURRENT == OperatingMode.Privilege) {
            setCurrentOperatingMode(OperatingMode.Accessibility)
        } else {
            setCurrentOperatingMode(OperatingMode.Privilege)
        }
    }
}