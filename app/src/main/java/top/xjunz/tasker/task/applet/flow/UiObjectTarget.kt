/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.bridge.DisplayManagerBridge

/**
 * @author xjunz 2023/03/13
 */
class UiObjectTarget {

    lateinit var source: AccessibilityNodeInfo

    val density: Float by lazy {
        DisplayManagerBridge.density
    }

    private val realSize by lazy {
        DisplayManagerBridge.size
    }

    val screenWidthPixels get() = realSize.x

    val screenHeightPixels get() = realSize.y
}