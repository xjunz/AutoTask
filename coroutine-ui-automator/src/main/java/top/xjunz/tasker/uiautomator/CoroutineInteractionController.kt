/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.app.UiAutomation.AccessibilityEventFilter
import android.graphics.Point
import android.os.Build
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import androidx.test.uiautomator.Configurator
import java.util.concurrent.TimeoutException

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

    /**
     * Helper used by methods to perform actions and wait for any accessibility events and return
     * predicated on predefined filter.
     *
     */
    suspend fun runAndWaitForEvents(
        filter: AccessibilityEventFilter, timeout: Long, command: suspend () -> Unit,
    ): AccessibilityEvent? {
        return try {
            bridge.executeAndWaitForEvent(command, filter, timeout)
        } catch (e: TimeoutException) {
            null
        }
    }

    private fun recycleAccessibilityEvents(events: MutableList<AccessibilityEvent>) {
        @Suppress("DEPRECATION")
        for (event in events) event.recycle()
        events.clear()
    }

    private class EventCollectingPredicate(
        var mask: Int,
        var eventsList: MutableList<AccessibilityEvent>
    ) : AccessibilityEventFilter {
        override fun accept(t: AccessibilityEvent): Boolean {
            // check current event in the list
            if (t.eventType and mask != 0) {
                // For the events you need, always store a copy when returning false from
                // predicates since the original will automatically be recycled after the call.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    eventsList.add(AccessibilityEvent(t))
                } else {
                    @Suppress("DEPRECATION")
                    eventsList.add(AccessibilityEvent.obtain(t))
                }
            }

            // get more
            return false
        }
    }

    suspend fun scrollSwipe(downX: Int, downY: Int, upX: Int, upY: Int, steps: Int): Boolean {

        // Collect all accessibility events generated during the swipe command and get the
        // last event
        val events = ArrayList<AccessibilityEvent>()
        runAndWaitForEvents(
            EventCollectingPredicate(AccessibilityEvent.TYPE_VIEW_SCROLLED, events),
            Configurator.getInstance().scrollAcknowledgmentTimeout
        ) {
            swipe(downX, downY, upX, upY, steps, false)
        }
        val event: AccessibilityEvent? = events.findLast {
            it.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        }
        if (event == null) {
            // end of scroll since no new scroll events received
            recycleAccessibilityEvents(events)
            return false
        }

        // AdapterViews have indices we can use to check for the beginning.
        var foundEnd = false
        if (event.fromIndex != -1 && event.toIndex != -1 && event.itemCount != -1) {
            foundEnd = event.fromIndex == 0 ||
                    event.itemCount - 1 == event.toIndex
        } else if (event.scrollX != -1 && event.scrollY != -1) {
            // Determine if we are scrolling vertically or horizontally.
            if (downX == upX) {
                // Vertical
                foundEnd = event.scrollY == 0 ||
                        event.scrollY == event.maxScrollY

            } else if (downY == upY) {
                // Horizontal
                foundEnd = event.scrollX == 0 ||
                        event.scrollX == event.maxScrollX
            }
        }
        recycleAccessibilityEvents(events)
        return !foundEnd
    }

}