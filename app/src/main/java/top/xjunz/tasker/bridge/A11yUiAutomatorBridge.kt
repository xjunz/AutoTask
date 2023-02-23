/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.app.UiAutomation
import top.xjunz.tasker.service.a11yAutomatorService
import top.xjunz.tasker.uiautomator.A11yGestureController
import top.xjunz.tasker.uiautomator.A11yInteractionController
import top.xjunz.tasker.uiautomator.CoroutineGestureController
import top.xjunz.tasker.uiautomator.CoroutineInteractionController


/**
 * @author xjunz 2022/07/23
 */
class A11yUiAutomatorBridge(uiAutomation: UiAutomation) : ContextUiAutomatorBridge(uiAutomation) {

    override val interactionController: CoroutineInteractionController by lazy {
        A11yInteractionController(a11yAutomatorService, this)
    }

    override val gestureController: CoroutineGestureController by lazy {
        A11yGestureController(a11yAutomatorService, this)
    }
}