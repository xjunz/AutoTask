/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.graphics.Point
import android.view.MotionEvent
import androidx.test.uiautomator.InteractionController
import androidx.test.uiautomator.bridge.UiAutomatorBridge
import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.service.A11yAutomatorService

/**
 * @author xjunz 2022/07/21
 */
class A11yInteractionController(
    private val service: A11yAutomatorService, bridge: UiAutomatorBridge
) : InteractionController(bridge) {

    override fun clickNoSync(x: Int, y: Int): Boolean {
        unsupportedOperation("Non-sync click is not implemented. Used sync method instead!")
    }

    override fun clickAndSync(x: Int, y: Int, timeout: Long): Boolean {
        return service.dispatchGesture(
            GestureGenerator.makeClickGesture(x, y), null, null
        )
    }

    override fun longTapNoSync(x: Int, y: Int): Boolean {
        unsupportedOperation("Non-sync long tap is not implemented. Use sync method instead!")
    }

    override fun longTapAndSync(x: Int, y: Int, timeout: Long): Boolean {
        return service.dispatchGesture(
            GestureGenerator.makeLongClickGesture(x, y), null, null
        )
    }

    override fun swipe(
        downX: Int, downY: Int, upX: Int, upY: Int, steps: Int, drag: Boolean
    ): Boolean {
        return service.dispatchGesture(
            GestureGenerator.makeSwipeGesture(downX, downY, upX, upY, steps, drag), null, null
        )
    }

    override fun swipe(segments: Array<out Point>?, segmentSteps: Int): Boolean {
        return service.dispatchGesture(
            GestureGenerator.makeSwipeGesture(segments, segmentSteps), null, null
        )
    }

    override fun performMultiPointerGesture(vararg touches: Array<out MotionEvent.PointerCoords>): Boolean {
        return service.dispatchGesture(
            GestureGenerator.convertToStrokes(*touches), null, null
        )
    }
}