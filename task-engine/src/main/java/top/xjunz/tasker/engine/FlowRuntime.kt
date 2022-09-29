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
     * All applets with same id share the same argument.
     */
    val argumentRegistry = mutableMapOf<Int, Any>()

    /**
     * Values are keyed by remarks.
     */
    val resultRegistry = mutableMapOf<String, Any>()

    /**
     * Whether the applying of current applet is successful.
     */
    var isSuccessful = true

    var depth = 0

    lateinit var currentApplet: Applet

    lateinit var currentFlow: Flow

    /**
     * Get the argument from the registry or initialize the argument and store it.
     */
    inline fun <T : Any> getArgument(id: Int, defValue: () -> T): T {
        return argumentRegistry.getOrPut(id, defValue).unsafeCast()
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