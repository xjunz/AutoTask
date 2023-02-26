/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service.controller

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.lang.ref.WeakReference

/**
 * The abstract service controller manipulating a service.
 *
 * @author xjunz 2022/07/12
 */
abstract class ServiceController<S : Any> : CoroutineScope {

    abstract val service: S?

    /**
     * Start the service if not running and bind to it.
     */
    abstract fun bindService()

    abstract fun stopService()

    /**
     * Unbind the service without killing it. After the service is unbound, you will no longer
     * receive any further event from the [listener].
     */
    abstract fun unbindService()

    /**
     * **Note**: Will not start the service if not exits.
     */
    abstract fun bindExistingServiceIfExists()

    /**
     * Synchronize the service status manually. This is usually called in `Activity.onResume()`.
     */
    open fun syncStatus() {
        /* no-op */
    }

    /**
     * The service state listener.
     *
     * **Note**: the callbacks are not guaranteed to be called in the main thread.
     */
    interface ServiceStateListener {
        fun onStartBinding()
        fun onError(t: Throwable)
        fun onServiceBound()
        fun onServiceDisconnected()
    }

    abstract val isServiceRunning: Boolean

    abstract val startTimestamp: Long

    private var listenerRef: WeakReference<ServiceStateListener>? = null

    protected val listener get() = listenerRef?.get()

    fun requireService(): S {
        return requireNotNull(service) { "The service is not yet started or is dead!" }
    }

    open fun setStateListener(listener: ServiceStateListener) {
        listenerRef = WeakReference(listener)
    }

    open fun removeStateListener() {
        listenerRef?.clear()
        listenerRef = null
    }

    protected inline fun <R> doWhenRunning(block: (S) -> R): R? {
        if (isServiceRunning) {
            return block(service!!)
        }
        return null
    }

    override val coroutineContext = SupervisorJob() + CoroutineName("ServiceControllerScope")

}