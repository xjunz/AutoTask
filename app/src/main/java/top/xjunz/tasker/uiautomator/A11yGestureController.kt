/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import androidx.test.uiautomator.GestureController
import androidx.test.uiautomator.PointerGesture
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.uiautomator.GestureGenerator.convertToStrokes

/**
 * @author xjunz 2021/9/20
 */
class A11yGestureController(private val service: A11yAutomatorService, device: UiDevice) :
    GestureController(device) {

    override fun performGesture(vararg gestures: PointerGesture) {
        performSinglePointerGesture(null, gestures[0], null)
    }

    suspend fun performGesture(
        gesture: PointerGesture,
        callback: (currentDuration: Long, succeeded: Boolean?) -> Unit
    ) {
        coroutineScope {
            performSinglePointerGesture(this, gesture) { a, b ->
                callback(a, b)
                cancel()
            }
            awaitCancellation()
        }
    }

    private fun StrokeDescription.buildGesture(): GestureDescription {
        return GestureDescription.Builder().addStroke(this).build()
    }

    private fun performSinglePointerGesture(
        coroutineScope: CoroutineScope?,
        gesture: PointerGesture,
        resultCallback: ((currentDuration: Long, succeeded: Boolean?) -> Unit)?,
    ) {
        val strokes = gesture.convertToStrokes()
        var currentIndex = 0
        var duration = 0L
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                duration += gestureDescription.getStroke(0).duration
                if (++currentIndex <= strokes.lastIndex) {
                    resultCallback?.invoke(duration, null)
                    service.dispatchGesture(strokes[currentIndex].buildGesture(), this, null)
                } else {
                    resultCallback?.invoke(duration, true)
                    coroutineScope?.cancel()
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                resultCallback?.invoke(duration, false)
                coroutineScope?.cancel()
            }
        }
        service.dispatchGesture(strokes[0].buildGesture(), callback, null)
    }
}
