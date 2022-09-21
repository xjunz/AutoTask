package top.xjunz.tasker.service.controller

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import top.xjunz.tasker.service.AutomatorService
import top.xjunz.tasker.service.OperatingMode
import java.lang.ref.WeakReference

/**
 * The abstract service controller manipulating an [AutomatorService].
 *
 * @author xjunz 2022/07/12
 */

val currentServiceController get() = OperatingMode.CURRENT.serviceController

val currentService get() = currentServiceController.requireService()

abstract class ServiceController : CoroutineScope {

    abstract val service: AutomatorService?

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
    abstract fun syncStatus()

    /**
     * The service state listener.
     *
     * **Note**: the callbacks are not guaranteed to called in the main thread.
     */
    interface ServiceStateListener {
        fun onStartBinding()
        fun onError(t: Throwable)
        fun onServiceBound()
        fun onServiceDisconnected()
    }

    private val isServiceRunning get() = service?.isRunning == true

    private var listenerRef: WeakReference<ServiceStateListener>? = null

    protected val listener get() = listenerRef?.get()

    val startTimestamp: Long get() = doWhenRunning { it.getStartTimestamp() } ?: -1L

    fun requireService(): AutomatorService {
        return requireNotNull(service) { "The service is not yet started or is dead!" }
    }

    open fun setStateListener(listener: ServiceStateListener) {
        listenerRef = WeakReference(listener)
    }

    open fun removeStateListener() {
        listenerRef?.clear()
        listenerRef = null
    }

    private inline fun <R> doWhenRunning(block: (AutomatorService) -> R): R? {
        if (isServiceRunning) {
            return block(service!!)
        }
        return null
    }

    override val coroutineContext = SupervisorJob() + CoroutineName("ServiceControllerScope")

}