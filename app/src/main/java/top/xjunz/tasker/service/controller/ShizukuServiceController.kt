/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service.controller

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.os.IInterface
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import top.xjunz.shared.utils.runtimeException
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.service.isPremium
import top.xjunz.tasker.util.ShizukuUtil
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException

/**
 * @author xjunz 2022/10/10
 */
abstract class ShizukuServiceController<S : Any> : ServiceController<S>() {

    companion object {
        private const val BINDING_SERVICE_TIMEOUT_MILLS = 3000L
    }

    protected abstract val tag: String

    protected abstract val userServiceStandaloneProcessArgs: Shizuku.UserServiceArgs

    protected abstract fun asInterface(binder: IBinder): IInterface

    protected abstract fun onServiceConnected(remote: IInterface)

    private var bindingJob: Job? = null

    protected var serviceInterface: IInterface? = null

    private val deathRecipient: IBinder.DeathRecipient by lazy {
        IBinder.DeathRecipient {
            Log.w(tag, "The remote service is dead!")
            serviceInterface?.asBinder()?.unlinkToDeath(deathRecipient, 0)
            listener?.onServiceDisconnected()
        }
    }

    private val userServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, ibinder: IBinder?) {
            try {
                bindingJob?.cancel()
                if (ibinder == null || !ibinder.pingBinder()) {
                    runtimeException("Got an invalid binder!")
                } else {
                    ibinder.linkToDeath(deathRecipient, 0)
                    asInterface(ibinder).also {
                        serviceInterface = it
                        onServiceConnected(it)
                    }
                    listener?.onServiceBound()
                }
            } catch (t: Throwable) {
                listener?.onError(t)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    protected open fun onServiceConnectionError() {

    }

    fun bindServiceOnBoot() {
        PremiumMixin.loadPremiumFromFileSafely()
        if (isPremium) {
            bindService()
        }
    }

    override fun bindService() {
        if (bindingJob?.isActive == true) return
        ShizukuUtil.ensureShizukuEnv {
            listener?.onStartBinding()
            bindingJob = async {
                Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
                delay(BINDING_SERVICE_TIMEOUT_MILLS)
                throw TimeoutException()
            }
            bindingJob?.invokeOnCompletion {
                bindingJob = null
                if (it != null && it !is CancellationException) {
                    listener?.onError(it)
                }
            }
        }
    }

    override fun stopService() {
        Shizuku.unbindUserService(
            userServiceStandaloneProcessArgs, userServiceConnection, true
        )
        serviceInterface = null
    }

    override fun bindExistingServiceIfExists() {
        if (ShizukuUtil.isShizukuAvailable &&
            Shizuku.peekUserService(userServiceStandaloneProcessArgs, userServiceConnection)
        ) {
            bindService()
        }
    }

    override fun unbindService() {
        removeStateListener()
        serviceInterface?.asBinder()?.unlinkToDeath(deathRecipient, 0)
        Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, false)
    }

    override val isServiceRunning: Boolean
        get() = serviceInterface != null && serviceInterface?.asBinder()?.pingBinder() == true
}