package top.xjunz.tasker.engine

import android.app.UiAutomation
import androidx.annotation.MainThread
import androidx.test.uiautomator.UiDevice
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.shared.ktx.casted
import top.xjunz.shared.trace.logcat
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.FlowRuntime

/**
 * The abstraction of an automator task. Once an [AutomatorTask] is constructed, its [rootFlow] are
 * immutable.
 *
 * @author xjunz 2022/07/12
 */
@Serializable
open class AutomatorTask(val name: String) {

    companion object {
        private val globalVariableRegistry = mutableMapOf<Int, Any>()

        fun clearGlobalVariables() {
            globalVariableRegistry.clear()
        }
    }

    @Transient
    lateinit var rootFlow: Flow

    var label: String? = null

    @Transient
    lateinit var uiDevice: UiDevice

    /**
     * Whether the task is active or not. Even if set to `false`, the task may continue executing until
     * its latest [Applet] is completed. You can observe [OnStateChangedListener.onCancelled] to
     * get notified. Inactive tasks will no longer response to any further [Event] from [launch].
     */
    @Transient
    var isActive = false
        private set

    @Transient
    var onStateChangedListener: OnStateChangedListener? = null

    @Transient
    val id = name.hashCode()

    @Transient
    private var startTimestamp: Long = -1

    val uiAutomation: UiAutomation get() = uiDevice.instrumentation.uiAutomation

    /**
     * Whether the task is traversing its [rootFlow].
     */
    @Transient
    private var isExecuting = false

    class FlowFailureException(runtime: FlowRuntime) :
        Exception("The whole flow is stopped because [${runtime.currentFlow}:${runtime.currentApplet}] failed!")

    class TaskCancellationException(task: AutomatorTask) :
        Exception("Task '$task' is cancelled due to user request!")

    /**
     * Get or put a global variable if absent. The variable can be shared across tasks.
     */
    fun <V : Any> getOrPutGlobalVariable(key: Int, initializer: () -> V): V {
        return globalVariableRegistry.getOrPut(key, initializer).casted()
    }

    interface OnStateChangedListener {

        fun onStarted() {}

        /**
         * When the task completes due to an unexpected error.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onError(runtime: FlowRuntime, t: Throwable) {}

        /**
         * When the flow completes failed.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onFailure(runtime: FlowRuntime) {}

        /**
         * When the task completes successful.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onSuccess(runtime: FlowRuntime) {}

        /**
         * When the task is cancelled.
         */
        fun onCancelled() {}

        object NoOp : OnStateChangedListener
    }

    /**
     * Throws a [TaskCancellationException] if the task is not [active][isActive] at the moment.
     */
    fun ensureActive() {
        if (!isActive)
            throw TaskCancellationException(this)
    }

    fun activate(stateListener: OnStateChangedListener) {
        if (isActive) {
            error("Task[$name] has already been activated!")
        }
        onStateChangedListener = stateListener
        startTimestamp = System.currentTimeMillis()
        isActive = true
        onStateChangedListener?.onStarted()
    }

    fun deactivate() {
        if (!isActive) {
            error("The task[$name] has already been deactivated!")
        }
        isActive = false
        if (!isExecuting) {
            onStateChangedListener?.onCancelled()
        }
    }


    private fun trace(any: Any) {
        logcat(any, tag = "AutomatorTask")
    }

    /**
     * Called when an event is received.
     *
     * @return `true` if the task is successfully executed and `false` otherwise
     */
    @MainThread
    fun launch(events: Array<Event>, observer: FlowRuntime.Observer? = null): Boolean {
        if (!isActive) return false
        isExecuting = true
        val runtime = FlowRuntime.obtain(events)
        if (observer != null)
            runtime.observer = observer
        try {
            rootFlow.apply(this, runtime)
            if (runtime.isSuccessful) {
                onStateChangedListener?.onSuccess(runtime)
            } else {
                onStateChangedListener?.onFailure(runtime)
            }
            return runtime.isSuccessful
        } catch (t: Throwable) {
            when (t) {
                is FlowFailureException -> onStateChangedListener?.onFailure(runtime)
                is TaskCancellationException -> onStateChangedListener?.onCancelled()
                else -> onStateChangedListener?.onError(runtime, t)
            }
            return false
        } finally {
            isExecuting = false
            runtime.recycle()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AutomatorTask

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}