package top.xjunz.tasker.engine

import android.os.Handler
import androidx.annotation.MainThread
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * The abstraction of an automator task. Once an [AutomatorTask] is constructed, its [flow] are
 * immutable.
 *
 * @author xjunz 2022/07/12
 */
class AutomatorTask(val name: String) {

    val handlers = mutableMapOf<Int, Handler>()

    lateinit var flow: Flow

    var label: String? = null

    /**
     * Whether the task is active or not. Even if set to `false`, the task may continue executing until
     * its latest [Applet] is completed. You can observe [OnStateChangedListener.onCancelled] to
     * get notified. Inactive tasks will no longer response to any further [Event] from [launch].
     */
    var isActive = false
        private set

    var onStateChangedListener: OnStateChangedListener? = null

    val id = name.hashCode()

    private var startTimestamp: Long = -1

    /**
     * Whether the task is traversing its [flow].
     */
    private var isExecuting = false

    class FlowFailureException(reason: String) : RuntimeException(reason)

    class TaskCancellationException(task: AutomatorTask) :
        RuntimeException("Task '$task' is cancelled due to user request!")

    interface OnStateChangedListener {

        fun onStarted() {}

        /**
         * When the task completes due to an unexpected error.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onError(runtime: TaskRuntime, t: Throwable) {}

        /**
         * When the flow completes failed.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onFailure(runtime: TaskRuntime) {}

        /**
         * When the task completes successful.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onSuccess(runtime: TaskRuntime) {}

        /**
         * When the task is cancelled.
         */
        fun onCancelled() {}
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
        handlers.forEach {
            it.value.removeCallbacksAndMessages(null)
        }
        handlers.clear()
        isActive = false
        if (!isExecuting) {
            onStateChangedListener?.onCancelled()
        }
    }

    /**
     * Called when an event is received.
     *
     * @return `true` if the task starts executed and `false` otherwise
     */
    @MainThread
    fun launch(events: Array<Event>, observer: TaskRuntime.Observer? = null): Boolean {
        if (!isActive) return false
        if (isExecuting) return false
        isExecuting = true
        val runtime = TaskRuntime.obtain(this, events)
        if (observer != null)
            runtime.observer = observer
        try {
            flow.apply(runtime)
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