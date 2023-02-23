/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import androidx.test.uiautomator.PointerGesture

/**
 * @author xjunz 2023/02/15
 */
abstract class CoroutineGestureController internal constructor(val bridge: CoroutineUiAutomatorBridge) {

    abstract suspend fun performSinglePointerGesture(gesture: PointerGesture): Boolean

}