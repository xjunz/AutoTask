package top.xjunz.tasker.ui

import android.net.Uri
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.peekActivity
import top.xjunz.tasker.service.OperatingMode
import top.xjunz.tasker.service.controller.ServiceController
import top.xjunz.tasker.service.serviceController
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

    val onNewIntent = MutableLiveData<Uri>()

    val showStopConfirmation = MutableLiveData<Boolean>()

    val isRunning = MutableLiveData(false)

    val isBinding = MutableLiveData<Boolean>()

    val bindingError = MutableLiveData<Throwable>()

    val operatingMode = MutableLiveData(OperatingMode.CURRENT)

    fun setCurrentOperatingMode(mode: OperatingMode) {
        serviceController.removeStateListener()
        Preferences.operatingMode = mode.VALUE
        serviceController.setStateListener(this)
        operatingMode.value = mode
    }

    fun toggleService() {
        if (isRunning.isTrue) {
            serviceController.stopService()
        } else {
            serviceController.bindService()
        }
    }

    override fun onStartBinding() {
        isBinding.postValue(true)
    }

    override fun onError(t: Throwable) {
        isBinding.postValue(false)
        isRunning.postValue(false)
        bindingError.postValue(t)
    }

    override fun onServiceBound() {
        isBinding.postValue(false)
        isRunning.postValue(true)
    }

    override fun onServiceDisconnected() {
        isRunning.postValue(false)
    }

    inline fun doOnHostRouted(
        lifecycleOwner: LifecycleOwner,
        host: String,
        crossinline block: () -> Unit
    ) {
        lifecycleOwner.observeTransient(onNewIntent) {
            if (it.host == host) block()
        }
    }

    inline fun doOnAction(
        lifecycleOwner: LifecycleOwner,
        actionName: String,
        crossinline block: (value: String) -> Unit
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