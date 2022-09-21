package top.xjunz.tasker.task.core

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.ktx.getVisibleBounds
import java.io.BufferedWriter

/**
 * You can treat this as a special [AccessibilityNodeInfo], whose node info would never
 * become stale. Use [StableNode.freeze] to construct an instance.
 *
 * @author xjunz 2021/9/22
 */
class StableNode private constructor(
    val source: AccessibilityNodeInfo, val bounds: Rect,
    var parent: StableNode? = null, var child: StableNode? = null,
    var prev: StableNode? = null, var next: StableNode? = null
) {
    companion object {
        /**
         * Freeze an [AccessibilityNodeInfo] into a [StableNode] preserving its node tree.
         *
         * @param source the source node to be frozen
         * @param visibleBounds the visible area used to intersect the node's bounds in screen
         */
        fun freeze(source: AccessibilityNodeInfo, visibleBounds: Rect): StableNode? {
            val rect = source.getVisibleBounds(visibleBounds) ?: return null
            val node = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                StableNode(AccessibilityNodeInfo(source), rect)
            } else {
                StableNode(AccessibilityNodeInfo.obtain(source), rect)
            }
            var recordedNode: StableNode? = null
            for (i in 0 until source.childCount) {
                val child = source.getChild(i) ?: continue
                val currentNode = freeze(child, visibleBounds)
                currentNode?.parent = node
                currentNode?.prev = recordedNode
                if (i == 0) {
                    node.child = currentNode
                } else {
                    recordedNode?.next = currentNode
                }
                recordedNode = currentNode
            }
            source.recycle()
            return node
        }
    }

    private fun containsPoint(x: Int, y: Int) = bounds.contains(x, y)

    fun findNodeUnder(x: Int, y: Int): StableNode? {
        var targetNode: StableNode? = null
        var nextNode = child
        while (nextNode != null) {
            if (nextNode.containsPoint(x, y)) {
                val possibleNode = nextNode.findNodeUnder(x, y)
                if (possibleNode != null) {
                    // we prefer the smaller one, because it's more likely to be concerned
                    if (targetNode == null || targetNode.bounds.contains(possibleNode.bounds)) {
                        targetNode = possibleNode
                    }
                } else {
                    targetNode = nextNode
                }
            }
            nextNode = nextNode.next
        }
        // neither its brothers nor its children contain the point, check itself then
        if (targetNode == null && containsPoint(x, y)) {
            return this
        }
        return targetNode
    }

    /**
     * Dump the whole node tree with a [writer] in XML style.
     */
    fun dump(writer: BufferedWriter) {
        dump(0, writer)
        writer.flush()
        writer.close()
    }

    private fun dump(indentCount: Int, writer: BufferedWriter) {
        val indent = String(CharArray(indentCount * 2) { ' ' })
        writer.write("<${source.className}")
        if (child != null) {
            writer.write(">")
            writer.newLine()
            child!!.dump(indentCount + 2, writer)
        } else {
            writer.write("/>")
        }
        var next = next
        while (next != null) {
            writer.newLine()
            next.dump(indentCount, writer)
            next = next.next
        }
        if (child != null) {
            writer.newLine()
            writer.write(indent)
            writer.write("</${source.className}>")
        }
    }
}
