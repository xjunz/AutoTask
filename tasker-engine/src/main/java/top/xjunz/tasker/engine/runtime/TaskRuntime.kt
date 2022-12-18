package top.xjunz.tasker.engine.runtime

import androidx.core.util.Pools.SimplePool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.task.XTask


/**
 * The structure storing runtime information of a running [XTask].
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

        fun obtain(
            snapshot: Snapshot,
            coroutineScope: CoroutineScope,
            events: Array<out Event>
        ): TaskRuntime {
            val instance = Pool.acquire() ?: TaskRuntime()
            instance.coroutineScope = coroutineScope
            instance.target = events
            instance._events = events
            instance._snapshot = snapshot
            return instance
        }

        fun drainPool() {
            while (Pool.acquire() != null) {
                /* no-op */
            }
        }
    }

    val isActive get() = coroutineScope?.isActive == true

    fun ensureActive() {
        coroutineScope?.ensureActive()
    }

    fun halt() {
        coroutineScope?.cancel()
    }

    private var _snapshot: Snapshot? = null

    private val snapshot: Snapshot get() = _snapshot!!

    private var _events: Array<out Event>? = null

    val events: Array<out Event> get() = _events!!

    private var coroutineScope: CoroutineScope? = null

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
    fun <V : Any> getEnvironmentVariable(key: Int, initializer: (() -> V)? = null): V {
        if (initializer == null) {
            return snapshot.registry.getValue(key).casted()
        }
        return snapshot.registry.getOrDefault(key, initializer).casted()
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
        _snapshot = null
        _events = null
        coroutineScope = null
        results.clear()
        tracker.reset()
        observer = null
        Pool.release(this)
    }
}