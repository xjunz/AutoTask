/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import androidx.test.uiautomator.GestureController
import androidx.test.uiautomator.PointerGesture
import androidx.test.uiautomator.UiDevice

/**
 * @author xjunz 2023/02/15
 */
class ShizukuGestureController(device: UiDevice) : GestureController(device),
    CoroutineGestureController {

    override suspend fun performSinglePointerGesture(gesture: PointerGesture): Boolean {
        super.performGesture(gesture)
        return true
    }
}