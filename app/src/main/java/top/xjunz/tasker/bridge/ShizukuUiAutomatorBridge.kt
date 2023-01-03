/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import androidx.test.uiautomator.GestureController
import androidx.test.uiautomator.InteractionController
import androidx.test.uiautomator.UiDevice

/**
 * @author xjunz 2022/09/30
 */
class ShizukuUiAutomatorBridge : ContextUiAutomatorBridge() {

    override fun getInteractionController(): InteractionController {
        return InteractionController(this)
    }

    override fun getGestureController(device: UiDevice): GestureController {
        return GestureController(device)
    }
}