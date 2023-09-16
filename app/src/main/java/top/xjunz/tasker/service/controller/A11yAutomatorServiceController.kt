/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service.controller

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Observer
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.launchIntentSafely
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.service.A11yAutomatorService.Companion.LAUNCH_ERROR
import top.xjunz.tasker.service.A11yAutomatorService.Companion.RUNNING_STATE

/**
 * @author xjunz 2022/07/23
 */
object A11yAutomatorServiceController : ServiceController<A11yAutomatorService>() {

    override val service: A11yAutomatorService? get() = A11yAutomatorService.get()

    private val statusObserver = Observer<Boolean> {
        if (it) {
            listener?.onServiceStarted()
        } else {
            listener?.onServiceDisconnected()
        }
    }

    private val errorObserver = Observer<Throwable?> {
        if (it != null) {
            listener?.onError(it)
            LAUNCH_ERROR.value = null
        }
    }

    override fun bindService() {
        if (app.launchIntentSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))) {
            A11yAutomatorService.FLAG_REQUEST_INSPECTOR_MODE = false
            toast(R.string.pls_start_a11y_service_manually)
        }
    }

    override fun stopService() {
        service?.destroy()
    }

    override fun unbindService() {
        removeStateListener()
    }

    override fun bindExistingServiceIfExists() {
        /* no-op */
    }

    override fun setStateListener(listener: ServiceStateListener) {
        super.setStateListener(listener)
        RUNNING_STATE.observeForever(statusObserver)
        LAUNCH_ERROR.observeForever(errorObserver)
    }

    override fun removeStateListener() {
        super.removeStateListener()
        RUNNING_STATE.removeObserver(statusObserver)
        LAUNCH_ERROR.removeObserver(errorObserver)
    }

    override fun syncStatus() {
        if (service == null) {
            A11yAutomatorService.FLAG_REQUEST_INSPECTOR_MODE = true
        }
        RUNNING_STATE.value = service != null
    }

    override val isServiceRunning: Boolean get() = service?.isRunning == true

    override val startTimestamp: Long get() = service?.getStartTimestamp() ?: -1L
}