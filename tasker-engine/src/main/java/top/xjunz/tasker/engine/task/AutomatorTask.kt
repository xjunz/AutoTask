package top.xjunz.tasker.engine.task

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.Snapshot
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * The abstraction of an automator task. Once an [AutomatorTask] is constructed, its [flow] are
 * immutable.
 *
 * @author xjunz 2022/07/12
 */
class AutomatorTask(val name: String) {

    lateinit var flow: RootFlow

    var label: String? = null

    /**
     * Whether the task is active or not. Even if set to `false`, the task may continue executing until
     * its latest [Applet] is completed. You can observe [OnStateChangedListener.onCancelled] to
     * get notified. Inactive tasks will no longer response to any further [Event] from [launch].
     */
    var isEnabled = false
        private set

    var onStateChangedListener: OnStateChangedListener? = null

    val id = name.hashCode()

    private var startTimestamp: Long = -1

    /**
     * Whether the task is traversing its [flow].
     */
    private val isExecuting get() = currentRuntime?.isActive == true

    private var currentRuntime: TaskRuntime? = null

    class FlowFailureException(reason: String) : RuntimeException(reason)

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

    fun activate(stateListener: OnStateChangedListener) {
        if (isEnabled) {
            error("Task[$name] has already been activated!")
        }
        onStateChangedListener = stateListener
        startTimestamp = System.currentTimeMillis()
        isEnabled = true
        onStateChangedListener?.onStarted()
    }

    fun disable() {
        if (!isEnabled) {
            error("The task[$name] is not enabled!")
        }
        isEnabled = false
        if (!isExecuting) {
            currentRuntime?.halt()
        }
    }

    /**
     * Called when an event is received.
     *
     * @return `true` if the task starts executed and `false` otherwise
     */
    suspend fun launch(
        snapshot: Snapshot,
        scope: CoroutineScope,
        events: Array<out Event>,
        observer: TaskRuntime.Observer? = null
    ): Boolean {
        if (!isEnabled) return false
        // Cancel if still executing //TODO: 单线程情况下，Will this hit?
        if (isExecuting) {
            currentRuntime?.halt()
        }
        val runtime = TaskRuntime.obtain(snapshot, scope, events)
        if (observer != null)
            runtime.observer = observer
        try {
            currentRuntime = runtime
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
                is CancellationException -> onStateChangedListener?.onCancelled()
                else -> onStateChangedListener?.onError(runtime, t)
            }
            return false
        } finally {
            currentRuntime = null
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