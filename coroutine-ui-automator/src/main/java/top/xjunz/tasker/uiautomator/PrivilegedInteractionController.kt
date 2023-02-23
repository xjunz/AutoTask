/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.graphics.Point
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.ViewConfiguration
import androidx.test.uiautomator.Configurator
import kotlinx.coroutines.delay

/**
 * @author xjunz 2023/02/23
 */
class PrivilegedInteractionController(bridge: CoroutineUiAutomatorBridge) :
    CoroutineInteractionController(bridge) {

    companion object {

        // Inserted after each motion event injection.
        private const val MOTION_EVENT_INJECTION_DELAY_MILLIS = 5

        private fun getMotionEvent(
            downTime: Long,
            eventTime: Long,
            action: Int,
            x: Float,
            y: Float
        ): MotionEvent {
            val properties = PointerProperties()
            properties.id = 0
            properties.toolType = TOOL_TYPE_FINGER
            val coords = PointerCoords()
            coords.pressure = 1f
            coords.size = 1f
            coords.x = x
            coords.y = y
            return obtain(
                downTime, eventTime, action, 1, arrayOf(properties), arrayOf(coords),
                0, 0, 1.0f, 1.0f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
            )
        }
    }

    private var downTime: Long = 0

    private fun injectEventSync(event: InputEvent): Boolean {
        return bridge.uiAutomation.injectInputEvent(event, true)
    }

    private fun touchDown(x: Int, y: Int): Boolean {
        downTime = SystemClock.uptimeMillis()
        val event = getMotionEvent(downTime, downTime, ACTION_DOWN, x.toFloat(), y.toFloat())
        return injectEventSync(event)
    }

    private fun touchUp(x: Int, y: Int): Boolean {
        val eventTime = SystemClock.uptimeMillis()
        val event = getMotionEvent(downTime, eventTime, ACTION_UP, x.toFloat(), y.toFloat())
        downTime = 0
        return injectEventSync(event)
    }

    private fun touchMove(x: Int, y: Int): Boolean {
        val eventTime = SystemClock.uptimeMillis()
        val event = getMotionEvent(downTime, eventTime, ACTION_MOVE, x.toFloat(), y.toFloat())
        return injectEventSync(event)
    }

    /**
     * Clicks at coordinates without waiting for device idle. This may be used for operations
     * that require stressing the target.
     *
     * @return true if the click executed successfully
     */
    override suspend fun clickNoSync(x: Int, y: Int): Boolean {
        return clickAndSync(x, y, ViewConfiguration.getTapTimeout().toLong())
    }

    override suspend fun clickAndSync(x: Int, y: Int, timeout: Long): Boolean {
        if (touchDown(x, y)) {
            delay(timeout)
            if (touchUp(x, y)) return true
        }
        return false
    }

    override suspend fun longTapNoSync(x: Int, y: Int): Boolean {
        return longTapAndSync(x, y, ViewConfiguration.getLongPressTimeout().toLong())
    }

    override suspend fun longTapAndSync(x: Int, y: Int, timeout: Long): Boolean {
        if (touchDown(x, y)) {
            delay(timeout)
            if (touchUp(x, y)) {
                return true
            }
        }
        return false
    }

    suspend fun swipe(downX: Int, downY: Int, upX: Int, upY: Int, steps: Int): Boolean {
        return swipe(downX, downY, upX, upY, steps, false /*drag*/)
    }

    /**
     * Handle swipes/drags in any direction.
     */
    override suspend fun swipe(
        downX: Int,
        downY: Int,
        upX: Int,
        upY: Int,
        steps: Int,
        drag: Boolean
    ): Boolean {
        var ret: Boolean
        var swipeSteps = steps

        // avoid a divide by zero
        if (swipeSteps == 0) swipeSteps = 1
        val xStep: Double = (upX - downX).toDouble() / swipeSteps
        val yStep: Double = (upY - downY).toDouble() / swipeSteps

        // first touch starts exactly at the point requested
        ret = touchDown(downX, downY)
        if (drag) delay(ViewConfiguration.getLongPressTimeout().toLong())
        for (i in 1 until swipeSteps) {
            ret = ret and touchMove(downX + (xStep * i).toInt(), downY + (yStep * i).toInt())
            if (!ret) break
            // set some known constant delay between steps as without it this
            // become completely dependent on the speed of the system and results
            // may vary on different devices. This guarantees at minimum we have
            // a preset delay.
            delay(MOTION_EVENT_INJECTION_DELAY_MILLIS.toLong())
        }
        if (drag) delay(ViewConfiguration.getTapTimeout().toLong())
        ret = ret and touchUp(upX, upY)
        return ret
    }

    /**
     * Performs a swipe between points in the Point array.
     *
     * @param segments     is Point array containing at least one Point object
     * @param segmentSteps steps to inject between two Points
     * @return true on success
     */
    override suspend fun swipe(segments: Array<out Point>, segmentSteps: Int): Boolean {
        var steps = segmentSteps
        var ret: Boolean
        val swipeSteps = steps
        var xStep: Double
        var yStep: Double

        // avoid a divide by zero
        if (steps == 0) steps = 1

        // must have some points
        if (segments.isEmpty()) return false

        // first touch starts exactly at the point requested
        ret = touchDown(segments[0].x, segments[0].y)
        for (seg in segments.indices) {
            if (seg + 1 < segments.size) {
                xStep = (segments[seg + 1].x - segments[seg].x).toDouble() / steps
                yStep = (segments[seg + 1].y - segments[seg].y).toDouble() / steps
                for (i in 1 until swipeSteps) {
                    ret = ret and touchMove(
                        segments[seg].x + (xStep * i).toInt(),
                        segments[seg].y + (yStep * i).toInt()
                    )
                    if (!ret) break
                    // set some known constant delay between steps as without it this
                    // become completely dependent on the speed of the system and results
                    // may vary on different devices. This guarantees at minimum we have
                    // a preset delay.
                    delay(MOTION_EVENT_INJECTION_DELAY_MILLIS.toLong())
                }
            }
        }
        ret = ret and touchUp(segments[segments.size - 1].x, segments[segments.size - 1].y)
        return ret
    }

    /**
     * Performs a multi-touch gesture
     *
     *
     * Takes a series of touch coordinates for at least 2 pointers. Each pointer must have
     * all of its touch steps defined in an array of [PointerCoords]. By having the ability
     * to specify the touch points along the path of a pointer, the caller is able to specify
     * complex gestures like circles, irregular shapes etc, where each pointer may take a
     * different path.
     *
     *
     * To create a single point on a pointer's touch path
     * `
     * PointerCoords p = new PointerCoords();
     * p.x = stepX;
     * p.y = stepY;
     * p.pressure = 1;
     * p.size = 1;
    ` *
     *
     * @param touches each array of [PointerCoords] constitute a single pointer's touch path.
     * Multiple [PointerCoords] arrays constitute multiple pointers, each with its own
     * path. Each [PointerCoords] in an array constitute a point on a pointer's path.
     * @return `true` if all points on all paths are injected successfully, `false
    ` * otherwise
     * @since API Level 18
     */
    override suspend fun performMultiPointerGesture(vararg touches: Array<out PointerCoords>): Boolean {
        var ret: Boolean
        require(touches.size >= 2) { "Must provide coordinates for at least 2 pointers" }

        // Get the pointer with the max steps to inject.
        var maxSteps = 0
        for (touch in touches) maxSteps = Math.max(maxSteps, touch.size)

        // specify the properties for each pointer as finger touch
        val properties = arrayOfNulls<PointerProperties>(touches.size)
        val pointerCoords = arrayOfNulls<PointerCoords>(touches.size)
        for (x in touches.indices) {
            val prop = PointerProperties()
            prop.id = x
            prop.toolType = Configurator.getInstance().toolType
            properties[x] = prop

            // for each pointer set the first coordinates for touch down
            pointerCoords[x] = touches[x][0]
        }

        // Touch down all pointers
        val downTime = SystemClock.uptimeMillis()
        var event: MotionEvent?
        event = obtain(
            downTime, SystemClock.uptimeMillis(), ACTION_DOWN, 1,
            properties, pointerCoords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        ret = injectEventSync(event)
        for (x in 1 until touches.size) {
            event = obtain(
                downTime, SystemClock.uptimeMillis(),
                getPointerAction(ACTION_POINTER_DOWN, x), x + 1, properties,
                pointerCoords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
            )
            ret = ret and injectEventSync(event)
        }

        // Move all pointers
        for (i in 1 until maxSteps - 1) {
            // for each pointer
            for (x in touches.indices) {
                // check if it has coordinates to move
                if (touches[x].size > i) pointerCoords[x] = touches[x][i] else pointerCoords[x] =
                    touches[x][touches[x].size - 1]
            }
            event = obtain(
                downTime, SystemClock.uptimeMillis(),
                ACTION_MOVE, touches.size, properties, pointerCoords, 0, 0, 1f, 1f,
                0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
            )
            ret = ret and injectEventSync(event)
            delay(MOTION_EVENT_INJECTION_DELAY_MILLIS.toLong())
        }

        // For each pointer get the last coordinates
        for (x in touches.indices) pointerCoords[x] = touches[x][touches[x].size - 1]

        // touch up
        for (x in 1 until touches.size) {
            event = obtain(
                downTime, SystemClock.uptimeMillis(),
                getPointerAction(ACTION_POINTER_UP, x), x + 1, properties,
                pointerCoords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
            )
            ret = ret and injectEventSync(event)
        }
        // first to touch down is last up
        event = obtain(
            downTime, SystemClock.uptimeMillis(), ACTION_UP, 1,
            properties, pointerCoords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        ret = ret and injectEventSync(event)
        return ret
    }

    private fun getPointerAction(motionEvent: Int, index: Int): Int {
        return motionEvent + (index shl ACTION_POINTER_INDEX_SHIFT)
    }
}