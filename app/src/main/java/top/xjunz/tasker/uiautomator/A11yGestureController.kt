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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.uiautomator.GestureGenerator.convertToStrokes

/**
 * @author xjunz 2021/9/20
 */
class A11yGestureController(private val service: A11yAutomatorService, device: UiDevice) :
    GestureController(device), CoroutineGestureController {

    private fun StrokeDescription.buildGesture(): GestureDescription {
        return GestureDescription.Builder().addStroke(this).build()
    }

    /**
     * This implementation will ignore [PointerGesture.delay] and only the first gesture in [gestures]
     * will be performed (single pointer).
     *
     * @see performSinglePointerGesture
     */
    override fun performGesture(vararg gestures: PointerGesture) {
        performSinglePointerGesture(false, gestures[0], null)
    }

    /**
     * **Note**: This implementation will ignore [PointerGesture.delay]. If it matters, use
     * [performSinglePointerGesture] instead.
     */
    fun performSinglePointerGesture(
        notifyDurationChanges: Boolean,
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
                    if (notifyDurationChanges) {
                        resultCallback?.invoke(duration, null)
                    }
                    service.dispatchGesture(strokes[currentIndex].buildGesture(), this, null)
                } else {
                    resultCallback?.invoke(duration, true)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                resultCallback?.invoke(duration, false)
            }
        }
        service.dispatchGesture(strokes[0].buildGesture(), callback, null)
    }

    override suspend fun performSinglePointerGesture(gesture: PointerGesture): Boolean {
        return performSinglePointerGestures(gesture, null)
    }

    suspend fun performSinglePointerGestures(
        gesture: PointerGesture,
        onDurationChanged: ((curDuration: Long, curGestureFinished: Boolean) -> Unit)?
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        delay(gesture.delay())
        onDurationChanged?.invoke(0, false)
        performSinglePointerGesture(
            onDurationChanged != null, gesture
        ) { curDuration, isSucceeded ->
            if (isSucceeded != null) {
                onDurationChanged?.invoke(curDuration, isSucceeded)
                deferred.complete(isSucceeded)
            } else {
                onDurationChanged?.invoke(curDuration, false)
            }
        }
        return deferred.await()
    }
}
