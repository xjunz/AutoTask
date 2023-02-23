/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.graphics.Point
import android.view.MotionEvent
import kotlinx.coroutines.CompletableDeferred

/**
 * @author xjunz 2022/07/21
 */
class A11yInteractionController(
    private val service: AccessibilityService, bridge: CoroutineUiAutomatorBridge
) : CoroutineInteractionController(bridge) {

    private suspend fun dispatchGesture(gesture: GestureDescription): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        service.dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                deferred.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                deferred.complete(false)
            }
        }, null)
        return deferred.await()
    }

    override suspend fun clickNoSync(x: Int, y: Int): Boolean {
        return clickAndSync(x, y, 100)
    }

    override suspend fun clickAndSync(x: Int, y: Int, timeout: Long): Boolean {
        return dispatchGesture(GestureGenerator.makeClickGesture(x, y))
    }

    override suspend fun longTapNoSync(x: Int, y: Int): Boolean {
        return longTapAndSync(x, y, 2000)
    }

    override suspend fun longTapAndSync(x: Int, y: Int, timeout: Long): Boolean {
        return dispatchGesture(GestureGenerator.makeLongClickGesture(x, y))
    }

    override suspend fun swipe(
        downX: Int, downY: Int, upX: Int, upY: Int, steps: Int, drag: Boolean
    ): Boolean {
        return dispatchGesture(
            GestureGenerator.makeSwipeGesture(downX, downY, upX, upY, steps, drag)
        )
    }

    override suspend fun swipe(segments: Array<out Point>, segmentSteps: Int): Boolean {
        return dispatchGesture(GestureGenerator.makeSwipeGesture(segments, segmentSteps))
    }

    override suspend fun performMultiPointerGesture(vararg touches: Array<out MotionEvent.PointerCoords>): Boolean {
        return dispatchGesture(GestureGenerator.convertToStrokes(*touches))
    }
}