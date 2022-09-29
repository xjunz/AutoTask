package top.xjunz.tasker.engine

import android.app.UiAutomation
import androidx.test.uiautomator.UiDevice
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.engine.flow.Flow
import top.xjunz.tasker.trace.logcat

/**
 * The abstraction of an automator task. Once an [AutomatorTask] is constructed, its [rootFlow] are
 * immutable.
 *
 * @author xjunz 2022/07/12
 */
open class AutomatorTask(val name: String) {

    lateinit var uiDevice: UiDevice

    lateinit var rootFlow: Flow

    /**
     * Whether the task is active or not. Even if set to `false`, a task may continue executing until
     * its latest [Applet] is completed. You can observe [OnStateChangedListener.onTaskStopped] to
     * get notified. Inactive tasks will no longer response to any further [Event] from [onEvent].
     */
    var isActive = false
        private set

    private var listener: OnStateChangedListener = OnStateChangedListener.NoOp

    val id = name.hashCode()

    private var startTimestamp: Long = -1

    val uiAutomation: UiAutomation get() = uiDevice.instrumentation.uiAutomation

    /**
     * Whether the task is traversing its [rootFlow].
     */
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
        if (!isActive) {
            throw TaskCancellationException(this)
        }
    }

    fun activate(stateListener: OnStateChangedListener) {
        if (isActive) {
            error("Task[$name] has already been activated!")
        }
        listener = stateListener
        startTimestamp = System.currentTimeMillis()
        isActive = true
        listener.onTaskStarted()
    }

    fun deactivate() {
        if (!isActive) {
            error("The task[$name] has already been deactivated!")
        }
        isActive = false
        if (!isExecuting) {
            listener.onTaskStopped()
        }
    }

    private fun trace(any: Any) {
        logcat(any, tag = "AutomatorTask")
    }

    /**
     * Called when an [Event] is received.
     *
     * @return `true` if the task is successfully executed and `false` otherwise
     */
    fun onEvent(ctx: AppletContext): Boolean {
       if(!isActive) return false
        isExecuting = true
        val runtime = FlowRuntime(ctx.events)
        try {
            rootFlow.apply(ctx, runtime)
        } catch (t: Throwable) {
            isExecuting = false
            when (t) {
                is FlowFailureException -> listener.onAppletFailure(runtime)
                is TaskCancellationException -> listener.onTaskStopped()
                else -> listener.onAppletError(runtime, t)
            }
            return false
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