package top.xjunz.tasker.engine.runtime

import androidx.core.util.Pools.SimplePool
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.Reference


/**
 * The structure storing runtime information of a running [Flow].
 *
 * @author xjunz 2022/08/09
 */
class FlowRuntime private constructor() {

    private object Pool : SimplePool<FlowRuntime>(25)

    interface Observer {

        fun onStarted(victim: Applet, runtime: FlowRuntime) {}

        fun onTerminated(victim: Applet, runtime: FlowRuntime) {}

        fun onSkipped(victim: Applet, runtime: FlowRuntime) {}
    }

    companion object {

        fun obtain(events: Array<Event>): FlowRuntime {
            val instance = Pool.acquire() ?: FlowRuntime()
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

    lateinit var events: Array<Event>

    /**
     * Target is for applet to use in runtime via [FlowRuntime.getTarget].
     */
    private lateinit var target: Any

    val tracker = AppletTracker()

    var observer: Observer? = null

    lateinit var hitEvent: Event

    /**
     * All applets with same id share the same argument.
     */
    val arguments = mutableMapOf<Int, Any>()

    /**
     * Get the argument from the registry or initialize the argument and store it.
     */
    inline fun <T : Any> getOrPutArgument(id: Int, defValue: () -> T): T {
        var arg = arguments[id]
        if (arg == null) {
            arg = defValue()
            arguments[id] = arg
        }
        return arg.casted()
    }

    /**
     * Values are keyed by applets' remarks.
     */
    private val results = mutableMapOf<String, Pair<Applet, Any>>()

    lateinit var currentApplet: Applet

    lateinit var currentFlow: Flow

    /**
     * Whether the applying of current applet is successful.
     */
    var isSuccessful = true

    fun registerResultIfNeeded(applet: Applet, result: Any) {
        if (applet.isReferred)
            results[applet.refid!!] = applet to result
    }

    fun findReferredValue(ref: Reference): Any? {
        val pair = results[ref.id]
        if (pair != null) {
            return pair.first.getReferredValue(ref.which, pair.second)
        }
        return null
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
        arguments.clear()
        tracker.reset()
        observer = null
        Pool.release(this)
    }
}