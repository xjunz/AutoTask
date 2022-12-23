package top.xjunz.tasker.engine.runtime

import androidx.annotation.IntRange
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Applet.Companion.FLOW_CHILD_COUNT_BITS
import top.xjunz.tasker.engine.applet.base.Applet.Companion.MAX_FLOW_CHILD_COUNT

/**
 * @author xjunz 2022/11/01
 */
class AppletIndexer {

    /**
     * The depth of currently executed applet starting from 0, which means the flow is not yet started
     * or has completed.
     */
    var depth = 0

    private inline val depthIndex get() = depth - 1

    /**
     * A long value which helps us tracking the exact position of the running track.
     */
    private var trace: Long = 0

    fun moveTo(@IntRange(from = 0, to = MAX_FLOW_CHILD_COUNT - 1L) index: Int) {
        check(index in 0 until MAX_FLOW_CHILD_COUNT) {
            "Too many children!"
        }
        check(depth > 0) {
            "Not in a flow!"
        }
        check(depth <= Applet.MAX_FLOW_NESTED_DEPTH) {
            "Too deeply nested!"
        }
        clearIndex()
        trace = index + 1L shl (depthIndex * FLOW_CHILD_COUNT_BITS) or trace
    }

    /**
     *  Bit clear the tracked index in this depth
     */
    private fun clearIndex() {
        // Bit clear the tracked index in this depth
        trace =
            trace and (MAX_FLOW_CHILD_COUNT.toLong() shl depthIndex * FLOW_CHILD_COUNT_BITS).inv()
    }

    fun jumpIn() {
        check(depth < Applet.MAX_FLOW_NESTED_DEPTH) {
            "Too deeply nested!"
        }
        depth++
    }

    fun jumpOut() {
        check(depth > 0) {
            "Cannot jump out of the void!"
        }
        clearIndex()
        depth--
    }

    fun getIndexInDepth(
        @IntRange(from = 1, to = Applet.MAX_FLOW_NESTED_DEPTH.toLong()) depth: Int
    ): Int {
        check(depth > 0)
        return (trace ushr (depth - 1) * FLOW_CHILD_COUNT_BITS and MAX_FLOW_CHILD_COUNT.toLong()).toInt() - 1
    }

    fun parseIndexes(): IntArray {
        return IntArray(depth) {
            getIndexInDepth(it + 1)
        }
    }

    fun formatHierarchy(): String {
        if (depth <= 0) {
            return "-"
        }
        return parseIndexes().joinToString(" > ")
    }

    fun reset() {
        trace = 0
        depth = 0
    }
}