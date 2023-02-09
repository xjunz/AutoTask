/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui

import android.net.Uri
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.peekActivity
import top.xjunz.tasker.service.OperatingMode
import top.xjunz.tasker.service.controller.ServiceController
import top.xjunz.tasker.service.serviceController
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.storage.TaskStorage
import top.xjunz.tasker.util.Router

/**
 * @author xjunz 2022/07/08
 */
class MainViewModel : ViewModel(), ServiceController.ServiceStateListener {

    companion object {

        fun LifecycleOwner.peekMainViewModel(): MainViewModel {
            return (peekActivity() as MainActivity).viewModels<MainViewModel>().value
        }
    }

    val allTaskLoaded = MutableLiveData<Boolean>()

    val onNewIntent = MutableLiveData<Uri>()

    val stopServiceConfirmation = MutableLiveData<Boolean>()

    val isServiceRunning = MutableLiveData(false)

    val isServiceBinding = MutableLiveData<Boolean>()

    val serviceBindingError = MutableLiveData<Throwable>()

    val operatingMode = MutableLiveData(OperatingMode.CURRENT)

    fun init() {
        AppletOptionFactory.preloadIfNeeded()
        if (TaskStorage.storageTaskLoaded) {
            allTaskLoaded.value = true
        } else viewModelScope.launch {
            TaskStorage.loadAllTasks()
            allTaskLoaded.value = true
        }
    }

    fun setCurrentOperatingMode(mode: OperatingMode) {
        serviceController.removeStateListener()
        Preferences.operatingMode = mode.VALUE
        serviceController.setStateListener(this)
        operatingMode.value = mode
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

    fun doOnRouted(
        lifecycleOwner: LifecycleOwner,
        host: String,
        block: () -> Unit
    ) {
        lifecycleOwner.observeTransient(onNewIntent) {
            if (it.host == host) block()
        }
    }

    fun doOnAction(
        lifecycleOwner: LifecycleOwner,
        actionName: String,
        block: (value: String) -> Unit
    ) {
        lifecycleOwner.observeTransient(onNewIntent) {
            if (it.host == Router.HOST_ACTION
                && it.queryParameterNames.contains(actionName)
            ) {
                block(it.getQueryParameter(actionName)!!)
            }
        }
    }
}