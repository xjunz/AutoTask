/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.graphics.Point
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo

/**
 * @author xjunz 2023/02/23
 */
class CoroutineUiDevice internal constructor(internal val bridge: CoroutineUiAutomatorBridge) {

    /**
     * Perform a click at arbitrary coordinates specified by the user
     */
    suspend fun click(x: Int, y: Int): Boolean {
        return if (x >= getDisplayWidth() || y >= getDisplayHeight()) {
            false
        } else {
            bridge.interactionController.clickNoSync(x, y)
        }
    }

    suspend fun longClick(x: Int, y: Int): Boolean {
        return bridge.interactionController.longTapNoSync(x, y)
    }

    fun wrapUiObject(source: AccessibilityNodeInfo): CoroutineUiObject {
        return CoroutineUiObject(bridge, source)
    }

    /**
     * Gets the width of the display, in pixels. The width and height details
     * are reported based on the current orientation of the display.
     *
     * @return width in pixels or zero on failure
     * @since API Level 16
     */
    @Suppress("DEPRECATION")
    fun getDisplayWidth(): Int {
        val p = Point()
        getDefaultDisplay().getSize(p)
        return p.x
    }

    /**
     * Gets the height of the display, in pixels. The size is adjusted based
     * on the current orientation of the display.
     *
     * @return height in pixels or zero on failure
     * @since API Level 16
     */
    @Suppress("DEPRECATION")
    fun getDisplayHeight(): Int {
        val p = Point()
        getDefaultDisplay().getSize(p)
        return p.y
    }

    private fun getDefaultDisplay(): Display {
        return bridge.defaultDisplay
    }
}