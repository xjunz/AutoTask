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
import top.xjunz.tasker.service.A11yAutomatorService.Companion.launchError
import top.xjunz.tasker.service.A11yAutomatorService.Companion.runningState

/**
 * @author xjunz 2022/07/23
 */
object A11yAutomatorServiceController : ServiceController<A11yAutomatorService>() {

    override val service: A11yAutomatorService? get() = A11yAutomatorService.get()

    private val statusObserver = Observer<Boolean> {
        if (it) {
            listener?.onServiceBound()
        } else {
            listener?.onServiceDisconnected()
        }
    }

    private val errorObserver = Observer<Throwable?> {
        if (it != null) {
            listener?.onError(it)
            launchError.value = null
        }
    }

    override fun bindService() {
        app.launchIntentSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        toast(R.string.pls_start_a11y_service_manually)
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
        runningState.observeForever(statusObserver)
        launchError.observeForever(errorObserver)
    }

    override fun removeStateListener() {
        super.removeStateListener()
        runningState.removeObserver(statusObserver)
        launchError.removeObserver(errorObserver)
    }

    override fun syncStatus() {
        runningState.value = service != null
    }

    override val isServiceRunning: Boolean get() = service?.isRunning == true

    override val startTimestamp: Long get() = service?.getStartTimestamp() ?: -1L
}