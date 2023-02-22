/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.gesture

import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.test.uiautomator.PointerGesture
import top.xjunz.tasker.bridge.ContextBridge
import kotlin.math.hypot

/**
 * A gesture recorder by sampling, only supporting recording single pointer gesture currently.
 *
 * @author xjunz 2023/02/14
 */
class GestureRecorder(
    looper: Looper,
    /**
     * In monitor mode, touch events will not be intercepted. Only available in
     * superuser process, which requires permission `android.permission.INPUT_EVENT_MONITOR`.
     */
    private val isMonitorMode: Boolean = false
) {

    companion object {
        const val SAMPLE_INTERVAL_MILLS = 25L
    }

    var extraDelay = 0L

    private val handler = Handler(looper)

    private var previousX = -1

    private var previousY = -1

    private var currentX = -1

    private var currentY = -1

    private var holdingDuration = 0L

    private var gestureDuration = 0L

    private var isPaused = false

    private var callback: Callback? = null

    private var gestureEndTimestamp = -1L

    private var isActive = true

    private var longClickVibrated = false

    private lateinit var currentGesture: PointerGesture

    private val touchSlop by lazy {
        ViewConfiguration.get(ContextBridge.getContext()).scaledTouchSlop
    }

    private val samplingTask: Runnable = object : Runnable {

        override fun run() {
            if (isPaused) return
            if (previousX != -1 &&
                // Not a enough distance to confirm a move
                hypot(
                    (previousX - currentX).toFloat(),
                    (previousY - currentY).toFloat()
                ) <= touchSlop
            ) {
                holdingDuration += SAMPLE_INTERVAL_MILLS
                if (holdingDuration > ViewConfiguration.getLongPressTimeout() && !longClickVibrated) {
                    callback?.onLongClickDetected()
                    longClickVibrated = true
                }
            } else {
                if (holdingDuration > 0L) {
                    currentGesture.pause(holdingDuration)
                    gestureDuration += holdingDuration
                    holdingDuration = 0
                }
                currentGesture.moveWithDuration(Point(currentX, currentY), SAMPLE_INTERVAL_MILLS)
                gestureDuration += SAMPLE_INTERVAL_MILLS
            }
            previousX = currentX
            previousY = currentY
            handler.postDelayed(this, SAMPLE_INTERVAL_MILLS)
        }
    }

    val isActivated get() = isActive

    private fun onTouchDown() {
        isPaused = false
        holdingDuration = 0
        longClickVibrated = false
        previousX = -1
        previousY = -1
        currentGesture = PointerGesture(
            Point(currentX, currentY),
            // First gesture, no delay considered
            if (gestureEndTimestamp == -1L) 0
            // In monitor mode, delay is the gap between two gesture
            else if (isMonitorMode) SystemClock.uptimeMillis() - gestureEndTimestamp
            // Else delay need to minus gesture performing duration
            else SystemClock.uptimeMillis() - gestureEndTimestamp - extraDelay
        )
        callback?.onGestureStarted(currentGesture.delay())
        handler.post(samplingTask)
    }

    private fun onTouchUp(isCancelled: Boolean) {
        isPaused = true
        handler.removeCallbacksAndMessages(null)
        if (isCancelled) {
            callback?.onGestureCancelled()
        } else {
            if (holdingDuration > 0) {
                val duration = holdingDuration
                currentGesture.pause(duration)
                gestureDuration += duration
                holdingDuration = 0
            } else {
                currentGesture.moveWithDuration(Point(currentX, currentY), SAMPLE_INTERVAL_MILLS)
            }
            gestureEndTimestamp = SystemClock.uptimeMillis()
            callback?.onGestureEnded(currentGesture, gestureDuration)
        }
    }

    /**
     * No longer receive touch events and cancel current gesture.
     */
    fun deactivate() {
        if (!isPaused) {
            onTouchUp(true)
        }
        isActive = false
        gestureEndTimestamp = -1
        extraDelay = 0
    }

    /**
     * Allow receiving touch events.
     */
    fun activate() {
        isActive = true
    }

    private fun updateCurrentCoordinate(event: MotionEvent) {
        currentX = event.rawX.toInt()
        currentY = event.rawY.toInt()
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isActive) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                updateCurrentCoordinate(event)
                onTouchDown()
            }
            MotionEvent.ACTION_MOVE -> updateCurrentCoordinate(event)
            MotionEvent.ACTION_UP -> onTouchUp(false)
            MotionEvent.ACTION_CANCEL -> onTouchUp(true)
        }
        return true
    }


    interface Callback {

        fun onLongClickDetected()

        fun onGestureStarted(startDelay: Long)

        fun onGestureEnded(gesture: PointerGesture, duration: Long)

        fun onGestureCancelled()
    }

}