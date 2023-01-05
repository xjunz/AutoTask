/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.app.UiAutomation
import androidx.test.uiautomator.GestureController
import androidx.test.uiautomator.InteractionController
import androidx.test.uiautomator.UiDevice
import top.xjunz.tasker.service.a11yAutomatorService
import top.xjunz.tasker.uiautomator.A11yGestureController
import top.xjunz.tasker.uiautomator.A11yInteractionController


/**
 * @author xjunz 2022/07/23
 */
class A11yUiAutomatorBridge(uiAutomation: UiAutomation) : ContextUiAutomatorBridge(uiAutomation) {

    override fun getInteractionController(): InteractionController {
        return A11yInteractionController(a11yAutomatorService, this)
    }

    override fun getGestureController(device: UiDevice): GestureController {
        return A11yGestureController(a11yAutomatorService, device)
    }
}