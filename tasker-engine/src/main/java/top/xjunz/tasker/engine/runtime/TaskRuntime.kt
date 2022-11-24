package top.xjunz.tasker.engine.runtime

import androidx.core.util.Pools.SimplePool
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow


/**
 * The structure storing runtime information of a running [AutomatorTask].
 *
 * @author xjunz 2022/08/09
 */
class TaskRuntime private constructor() {

    private object Pool : SimplePool<TaskRuntime>(25)

    interface Observer {

        fun onStarted(victim: Applet, runtime: TaskRuntime) {}

        fun onTerminated(victim: Applet, runtime: TaskRuntime) {}

        fun onSkipped(victim: Applet, runtime: TaskRuntime) {}
    }

    companion object {

        private val globalVariableRegistry = mutableMapOf<Int, Any>()

        fun clearGlobalVariables() {
            globalVariableRegistry.clear()
        }

        fun obtain(task: AutomatorTask, events: Array<Event>): TaskRuntime {
            val instance = Pool.acquire() ?: TaskRuntime()
            instance.task = task
            instance.events = events
            instance.target = events
            return instance
        }

        fun drainPool() {
            while (Pool.acquire() != null) {
                /* no-op */
            }
        }
    }

    lateinit var task: AutomatorTask

    lateinit var events: Array<Event>

    /**
     * Target is for applet to use in runtime via [TaskRuntime.getTarget].
     */
    private lateinit var target: Any

    val tracker = AppletTracker()

    var observer: Observer? = null

    lateinit var hitEvent: Event

    /**
     * Get or put a global variable if absent. The variable can be shared across tasks.
     */
    fun <V : Any> getOrPutCrossTaskVariable(key: Int, initializer: () -> V): V {
        return globalVariableRegistry.getOrPut(key, initializer).casted()
    }

    /**
     * Values are keyed by applets' remarks.
     */
    private val results = mutableMapOf<String, Any?>()

    lateinit var currentApplet: Applet

    lateinit var currentFlow: Flow

    /**
     * Whether the applying of current applet is successful.
     */
    var isSuccessful = true

    fun registerResult(refid: String, result: Any?) {
        results[refid] = result
    }

    fun getResultByRefid(refid: String?): Any? {
        if (refid == null) return null
        return results[refid]
    }

    fun setTarget(any: Any) {
        target = any
    }

    fun getRawTarget(): Any {
        return target
    }

    fun <T> getTarget(): T {
        return target.casted()
    }

    fun recycle() {
        results.clear()
        tracker.reset()
        observer = null
        Pool.release(this)
    }
}