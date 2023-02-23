/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.app.UiAutomation
import top.xjunz.tasker.uiautomator.CoroutineGestureController
import top.xjunz.tasker.uiautomator.CoroutineInteractionController
import top.xjunz.tasker.uiautomator.PrivilegedGestureController
import top.xjunz.tasker.uiautomator.PrivilegedInteractionController

/**
 * @author xjunz 2022/09/30
 */
class PrivilegedUiAutomatorBridge(uiAutomation: UiAutomation) :
    ContextUiAutomatorBridge(uiAutomation) {

    override val interactionController: CoroutineInteractionController by lazy {
        PrivilegedInteractionController(this)
    }

    override val gestureController: CoroutineGestureController by lazy {
        PrivilegedGestureController(this)
    }
}