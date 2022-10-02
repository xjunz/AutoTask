package top.xjunz.tasker.engine

import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.engine.flow.Flow


/**
 * The structure storing runtime arguments of a running [Flow].
 *
 * @author xjunz 2022/08/09
 */
class FlowRuntime(events: Array<Event>) {

    /**
     * Values are keyed by remarks.
     */
    private val resultRegistry = mutableMapOf<String, Any>()

    /**
     * Whether the applying of current applet is successful.
     */
    var isSuccessful = true

    var depth = 0

    lateinit var currentApplet: Applet

    lateinit var currentFlow: Flow

    fun registerResult(key: String, result: Any) {
        resultRegistry[key] = result
    }

    fun <R> getResult(key: String): R {
        return resultRegistry[key]!!.unsafeCast()
    }

    /**
     * The value used to
     */
    private var argument: Any = events

    fun setTarget(any: Any) {
        argument = any
    }

    fun getRawTarget(): Any {
        return argument
    }

    fun <T> getTarget(): T {
        return argument.unsafeCast()
    }
}