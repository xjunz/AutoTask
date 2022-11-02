package top.xjunz.tasker.engine

import android.app.UiAutomation
import androidx.annotation.MainThread
import androidx.test.uiautomator.UiDevice
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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

    @Transient
    lateinit var rootFlow: Flow

    var label: String? = null

    lateinit var flowPath: String

    @Transient
    lateinit var uiDevice: UiDevice

    /**
     * Whether the task is active or not. Even if set to `false`, the task may continue executing until
     * its latest [Applet] is completed. You can observe [OnStateChangedListener.onTaskStopped] to
     * get notified. Inactive tasks will no longer response to any further [Event] from [launch].
     */
    @Transient
    var isActive = false
        private set

    @Transient
    var onStateChangedListener: OnStateChangedListener = OnStateChangedListener.NoOp

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

    interface OnStateChangedListener {

        fun onTaskStarted() {}

        /**
         * When the flow completes due to an unexpected error.
         */
        fun onAppletError(runtime: FlowRuntime, t: Throwable) {}

        /**
         * When the flow completes due to an applet failure.
         */
        fun onAppletFailure(runtime: FlowRuntime) {}

        /**
         * When the task stops running.
         */
        fun onTaskStopped() {}

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
        onStateChangedListener.onTaskStarted()
    }

    fun deactivate() {
        if (!isActive) {
            error("The task[$name] has already been deactivated!")
        }
        isActive = false
        if (!isExecuting) {
            onStateChangedListener.onTaskStopped()
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
        } catch (t: Throwable) {
            isExecuting = false
            when (t) {
                is FlowFailureException -> onStateChangedListener.onAppletFailure(runtime)
                is TaskCancellationException -> onStateChangedListener.onTaskStopped()
                else -> onStateChangedListener.onAppletError(runtime, t)
            }
            return false
        } finally {
            runtime.recycle()
        }
        return true
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