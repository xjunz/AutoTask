/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.graphics.Point
import android.view.MotionEvent

/**
 * @author xjunz 2023/02/23
 */
abstract class CoroutineInteractionController(val bridge: CoroutineUiAutomatorBridge) {

    abstract suspend fun clickNoSync(x: Int, y: Int): Boolean

    abstract suspend fun clickAndSync(x: Int, y: Int, timeout: Long): Boolean

    abstract suspend fun longTapNoSync(x: Int, y: Int): Boolean

    abstract suspend fun longTapAndSync(x: Int, y: Int, timeout: Long): Boolean

    abstract suspend fun swipe(
        downX: Int,
        downY: Int,
        upX: Int,
        upY: Int,
        steps: Int,
        drag: Boolean
    ): Boolean

    abstract suspend fun swipe(segments: Array<out Point>, segmentSteps: Int): Boolean

    abstract suspend fun performMultiPointerGesture(vararg touches: Array<out MotionEvent.PointerCoords>): Boolean
}