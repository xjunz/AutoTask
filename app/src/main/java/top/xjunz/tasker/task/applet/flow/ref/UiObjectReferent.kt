/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow.ref

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.engine.runtime.Referent
import top.xjunz.tasker.task.applet.util.IntValueUtil

/**
 * @author xjunz 2023/02/12
 */
class UiObjectReferent(private val node: AccessibilityNodeInfo) : Referent {

    private val centerCoordinate: Int by lazy {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        IntValueUtil.composeCoordinate(bounds.centerX(), bounds.centerY())
    }

    override fun getReferredValue(which: Int): Any? {
        return when (which) {
            0 -> node
            1 -> node.text?.toString()
            2 -> centerCoordinate
            else -> super.getReferredValue(which)
        }
    }
}