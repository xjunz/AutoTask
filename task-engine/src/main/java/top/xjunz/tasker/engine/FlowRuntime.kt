package top.xjunz.tasker.engine

import androidx.annotation.IntRange
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.engine.flow.Flow


/**
 * The structure storing runtime information of a running [Flow].
 *
 * @author xjunz 2022/08/09
 */
class FlowRuntime(private var argument: Any) {

    /**
     * Values are keyed by applets' remarks.
     */
    private val resultRegistry = mutableMapOf<String, Any>()

    /**
     * The depth of currently executed applet. When the flow is not yet started or has been completed,
     * the depth is -1.
     */
    private var depth = -1

    /**
     * The executing sequence which helps us tracking the exact position of the running track.
     */
    private var sequence: Long = 0

    lateinit var currentApplet: Applet

    lateinit var currentFlow: Flow

    /**
     * Whether the applying of current applet is successful.
     */
    var isSuccessful = true

    fun moveTo(@IntRange(from = 0, to = Applet.MAX_FLOW_CHILD_COUNT - 1L) index: Int) {
        check(index in 0 until Applet.MAX_FLOW_CHILD_COUNT) { "Too many children!" }
        check(depth >= 0) { "Try to track an applet when not even entering any flow!" }
        check(depth <= Applet.MAX_FLOW_NESTED_DEPTH - 1) { "Try to track a too deeply nested applet!" }
        sequence = index.toLong() shl (depth * Applet.FLOW_CHILD_COUNT_BITS) or sequence
    }

    fun jumpIn() {
        check(depth + 1 <= Applet.MAX_FLOW_NESTED_DEPTH) { "Too deeply nested!" }
        depth++
    }

    fun jumpOut() {
        check(depth - 1 >= -1) { "The depth goes down to the void!" }
        // Bit clear the tracked index in this depth
        sequence =
            sequence and ((Applet.MAX_FLOW_CHILD_COUNT - 1L) shl (depth * Applet.FLOW_CHILD_COUNT_BITS)).inv()
        depth--
    }

    fun registerResult(key: String, result: Any) {
        resultRegistry[key] = result
    }

    fun <R> getResult(key: String): R {
        return resultRegistry[key]!!.unsafeCast()
    }

    fun parseSequence(): IntArray {
        check(depth >= -1)
        return IntArray(depth + 1) {
            (sequence ushr depth * Applet.FLOW_CHILD_COUNT_BITS and (Applet.MAX_FLOW_CHILD_COUNT - 1L)).toInt()
        }
    }

    fun printSequence(): String {
        if (depth < 0) {
            return "-"
        }
        return parseSequence().joinToString(">")
    }

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