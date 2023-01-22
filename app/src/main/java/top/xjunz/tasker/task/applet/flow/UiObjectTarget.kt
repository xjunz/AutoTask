/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.bridge.DisplayManagerBridge

/**
 * @author xjunz 2022/10/01
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