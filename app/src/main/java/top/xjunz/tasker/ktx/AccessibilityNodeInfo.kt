package top.xjunz.tasker.ktx

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * @author xjunz 2022/07/31
 */
@SuppressLint("CheckResult")
fun AccessibilityNodeInfo.getVisibleBoundsIn(bounds: Rect): Rect {
    val nodeRect: Rect = getVisibleBoundsInScreen(bounds)

    // is the targeted node within a scrollable container?
    // nothing to adjust for so return the node's Rect as is
    val scrollableParentNode = getScrollableParent() ?: return nodeRect

    // Scrollable parent's visible bounds
    val parentRect: Rect = scrollableParentNode.getVisibleBoundsInScreen(bounds)
    // adjust for partial clipping of targeted by parent node if required
    nodeRect.intersect(parentRect)
    return nodeRect
}

@SuppressLint("CheckResult")
private fun AccessibilityNodeInfo.getVisibleBoundsInScreen(bounds: Rect): Rect {
    val nodeRect = Rect()
    getBoundsInScreen(nodeRect)
    val displayRect = Rect(0, 0, bounds.width(), bounds.height())
    nodeRect.offset(-bounds.left, -bounds.top)
    nodeRect.intersect(displayRect)
    return nodeRect
}

fun AccessibilityNodeInfo.dup(): AccessibilityNodeInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        AccessibilityNodeInfo(this)
    } else {
        @Suppress("DEPRECATION")
        AccessibilityNodeInfo.obtain(this)
    }
}

private fun AccessibilityNodeInfo.getScrollableParent(): AccessibilityNodeInfo? {
    var parent: AccessibilityNodeInfo? = this
    while (parent != null) {
        parent = parent.parent
        if (parent != null && parent.isScrollable) {
            return parent
        }
    }
    return null
}