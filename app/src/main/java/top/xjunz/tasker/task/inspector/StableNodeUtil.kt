package top.xjunz.tasker.task.inspector

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.ktx.initAccessibilityNodeInfo

/**
 * @author xjunz 2022/10/10
 */
object StableNodeUtil {
    /**
     * Freeze an [AccessibilityNodeInfo] into a [StableNode] preserving its node tree.
     *
     * @param source the source node to be frozen
     */
    fun freeze(source: AccessibilityNodeInfo): StableNode {
        val node = StableNode(initAccessibilityNodeInfo(source))
        var recordedNode: StableNode? = null
        for (i in 0 until source.childCount) {
            val child = source.getChild(i) ?: continue
            val currentNode = freeze(child)
            currentNode.parent = node
            currentNode.prev = recordedNode
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