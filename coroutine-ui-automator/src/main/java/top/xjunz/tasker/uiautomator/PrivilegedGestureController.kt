/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.graphics.Point
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.PointerGesture
import java.util.*

/**
 * @author xjunz 2023/02/23
 */
class PrivilegedGestureController(bridge: CoroutineUiAutomatorBridge) :
    CoroutineGestureController(bridge) {

    override suspend fun performSinglePointerGesture(gesture: PointerGesture): Boolean {
        return performGesture(gesture)
    }

    /**
     * Comparator for sorting PointerGestures by start times.
     */
    private val startTimeComparator = Comparator<PointerGesture> { o1, o2 ->
        (o1.delay() - o2.delay()).toInt()
    }

    /**
     * Comparator for sorting PointerGestures by end times.
     */
    private val endTimeComparator = Comparator<PointerGesture> { o1, o2 ->
        (o1.delay() + o2.duration() - (o2.delay() + o2.duration())).toInt()
    }

    /**
     * Performs the given gesture as represented by the given [PointerGesture]s.
     *
     *
     * Each [PointerGesture] represents the actions of a single pointer from the time when it
     * is first touched down until the pointer is released. To perform the gesture, this method
     * tracks the locations of each pointer and injects [MotionEvent]s as appropriate.
     *
     * @param gestures One or more [PointerGesture]s which define the gesture to be performed.
     */
    private fun performGesture(vararg gestures: PointerGesture): Boolean {
        // Initialize pointers
        var count = 0
        val pointers: MutableMap<PointerGesture, Pointer> = HashMap()
        for (g in gestures) {
            pointers[g] = Pointer(count++, g.start())
        }

        // Initialize MotionEvent arrays
        val properties = ArrayList<MotionEvent.PointerProperties>()
        val coordinates = ArrayList<MotionEvent.PointerCoords>()

        // Track active and pending gestures
        val active = PriorityQueue(gestures.size, endTimeComparator)
        val pending = PriorityQueue(gestures.size, startTimeComparator)
        pending.addAll(listOf(*gestures))

        // Record the start time
        val startTime = SystemClock.uptimeMillis()

        // Loop
        var event: MotionEvent?
        var elapsedTime: Long = 0
        while (!pending.isEmpty() || !active.isEmpty()) {

            // Touchdown any new pointers
            while (!pending.isEmpty() && elapsedTime > pending.peek()!!.delay()) {
                val gesture = pending.remove()
                val pointer = pointers[gesture]

                // Add the pointer to the MotionEvent arrays
                properties.add(pointer!!.prop)
                coordinates.add(pointer.coords)

                // Touch down
                var action = MotionEvent.ACTION_DOWN
                if (!active.isEmpty()) {
                    // Use ACTION_POINTER_DOWN for secondary pointers. The index is stored at
                    // ACTION_POINTER_INDEX_SHIFT.
                    action = (MotionEvent.ACTION_POINTER_DOWN
                            + (properties.size - 1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT))
                }
                event = getMotionEvent(
                    startTime, startTime + elapsedTime, action, properties, coordinates
                )
                if (!bridge.uiAutomation.injectInputEvent(event, true)) {
                    return false
                }

                // Move the PointerGesture to the active list
                active.add(gesture)
            }

            // Touch up any completed pointers
            while (!active.isEmpty()
                && elapsedTime > active.peek()!!.delay() + active.peek()!!.duration()
            ) {
                val gesture = active.remove()
                val pointer = pointers[gesture]

                // Update pointer positions
                pointer!!.updatePosition(gesture.end())
                for (current in active) {
                    pointers[current]!!.updatePosition(current.pointAt(elapsedTime))
                }
                var action = MotionEvent.ACTION_UP
                val index = properties.indexOf(pointer.prop)
                if (!active.isEmpty()) {
                    action = (MotionEvent.ACTION_POINTER_UP
                            + (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT))
                }
                event = getMotionEvent(
                    startTime, startTime + elapsedTime, action, properties, coordinates
                )
                if (!bridge.uiAutomation.injectInputEvent(event, true)) {
                    return false
                }
                properties.removeAt(index)
                coordinates.removeAt(index)
            }

            // Move any active pointers
            for (gesture in active) {
                val pointer = pointers[gesture]
                pointer!!.updatePosition(gesture.pointAt(elapsedTime - gesture.delay()))
            }
            if (!active.isEmpty()) {
                event = getMotionEvent(
                    startTime, startTime + elapsedTime, MotionEvent.ACTION_MOVE,
                    properties, coordinates
                )
                if (!bridge.uiAutomation.injectInputEvent(event, true)) {
                    return false
                }
            }
            elapsedTime = SystemClock.uptimeMillis() - startTime
        }
        return true
    }

    /**
     * Helper function to obtain a MotionEvent.
     */
    private fun getMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        properties: List<MotionEvent.PointerProperties>,
        coordinates: List<MotionEvent.PointerCoords>
    ): MotionEvent {
        val props = properties.toTypedArray()
        val coords = coordinates.toTypedArray()
        return MotionEvent.obtain(
            downTime, eventTime, action, props.size, props, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
        )
    }

    /**
     * Helper class which tracks an individual pointer as part of a MotionEvent.
     */
    private class Pointer(id: Int, point: Point) {

        var prop: MotionEvent.PointerProperties = MotionEvent.PointerProperties()

        var coords: MotionEvent.PointerCoords

        init {
            prop.id = id
            prop.toolType = Configurator.getInstance().toolType
            coords = MotionEvent.PointerCoords()
            coords.pressure = 1f
            coords.size = 1f
            coords.x = point.x.toFloat()
            coords.y = point.y.toFloat()
        }

        fun updatePosition(point: Point) {
            coords.x = point.x.toFloat()
            coords.y = point.y.toFloat()
        }
    }
}