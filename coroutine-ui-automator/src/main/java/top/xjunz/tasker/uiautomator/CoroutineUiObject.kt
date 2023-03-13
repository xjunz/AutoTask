/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.*

/**
 * A copy of [androidx.test.uiautomator.UiObject2].
 *
 * @author xjunz 2023/02/23
 */
open class CoroutineUiObject internal constructor(
    protected val bridge: CoroutineUiAutomatorBridge,
    protected val source: AccessibilityNodeInfo
) : AutoCloseable {

    companion object {

        private val TAG = CoroutineUiObject::class.java.simpleName

        // Default gesture speeds
        private const val DEFAULT_SWIPE_SPEED = 5000
        private const val DEFAULT_SCROLL_SPEED = 5000
        private const val DEFAULT_FLING_SPEED = 7500
        private const val DEFAULT_DRAG_SPEED = 2500
        private const val DEFAULT_PINCH_SPEED = 2500

        // Short, since we should stop scrolling after the gesture completes.
        private const val SCROLL_TIMEOUT: Long = 1000

        // Longer, since we may continue to scroll after the gesture completes.
        private const val FLING_TIMEOUT: Long = 5000

    }

    private val gestures = Gestures.getInstance()

    private val gestureController get() = bridge.gestureController

    private val displayMetrics get() = bridge.displayMetrics

    // Margins
    private val marginLeft = 5
    private val marginTop = 5
    private val marginRight = 5
    private val marginBottom = 5

    suspend fun click() {
        gestureController.performSinglePointerGesture(
            Gestures.getInstance().click(getVisibleCenter())
        )
    }

    /**
     * Performs a click on this object that lasts for `duration` milliseconds.
     */
    suspend fun click(duration: Long) {
        gestureController.performSinglePointerGesture(gestures.click(getVisibleCenter(), duration))
    }

    suspend fun longClick() {
        gestureController.performSinglePointerGesture(gestures.longClick(getVisibleCenter()))
    }

    /**
     * Drags this object to the specified location.
     *
     * @param dest The end point that this object should be dragged to.
     */
    suspend fun drag(dest: Point) {
        drag(dest, (DEFAULT_DRAG_SPEED * displayMetrics.density).toInt())
    }

    /**
     * Drags this object to the specified location.
     *
     * @param dest  The end point that this object should be dragged to.
     * @param speed The speed at which to perform this gesture in pixels per second.
     */
    private suspend fun drag(dest: Point, speed: Int) {
        require(speed >= 0) { "Speed cannot be negative" }
        gestureController.performSinglePointerGesture(
            gestures.drag(getVisibleCenter(), dest, speed)
        )
    }

    /**
     * Performs a swipe gesture on this object.
     *
     * @param direction The direction in which to swipe.
     * @param percent   The length of the swipe as a percentage of this object's size.
     * @param speed     The speed at which to perform this gesture in pixels per second.
     */
    suspend fun swipe(direction: Direction?, percent: Float, speed: Int) {
        require(!(percent < 0.0f || percent > 1.0f)) { "Percent must be between 0.0f and 1.0f" }
        require(speed >= 0) { "Speed cannot be negative" }
        val bounds: Rect = getVisibleBoundsForGestures()
        gestureController.performSinglePointerGesture(
            gestures.swipeRect(bounds, direction, percent, speed)
        )
    }

    /**
     * Performs a fling gesture on this object.
     *
     * @param direction The direction in which to fling.
     * @return Whether the object can still scroll in the given direction.
     */
    suspend fun fling(direction: Direction): Boolean {
        return fling(direction, (DEFAULT_FLING_SPEED * displayMetrics.density).toInt())
    }

    /**
     * Performs a fling gesture on this object.
     *
     * @param direction The direction in which to fling.
     * @param speed     The speed at which to perform this gesture in pixels per second.
     * @return Whether the object can still scroll in the given direction.
     */
    suspend fun fling(direction: Direction, speed: Int): Boolean {
        require(
            speed >= bridge.scaledMinimumFlingVelocity
        ) { "Speed is less than the minimum fling velocity" }

        // To fling, we swipe in the opposite direction
        val swipeDirection = Direction.reverse(direction)
        val bounds = getVisibleBoundsForGestures()
        val swipe: PointerGesture = gestures.swipeRect(bounds, swipeDirection, 1.0f, speed)

        // Perform the gesture and return true if we did not reach the end
        // TODO: wait for scroll end
        return !gestureController.performSinglePointerGesture(swipe)
    }


    /**
     * Sets the text content if this object is an editable field.
     */
    fun setText(source: CharSequence?) {
        var text = source

        // Per framework convention, setText(null) means clearing it
        if (text == null) {
            text = ""
        }
        // do this for API Level above 19 (exclusive)
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        if (!this.source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            Log.w(TAG, "AccessibilityNodeInfo#performAction(ACTION_SET_TEXT) failed")
        }
    }

    /**
     * Returns the visible bounds of this object with the margins removed.
     */
    private fun getVisibleBoundsForGestures(): Rect {
        val ret = getVisibleBounds()
        ret.left = ret.left + marginLeft
        ret.top = ret.top + marginTop
        ret.right = ret.right - marginRight
        ret.bottom = ret.bottom - marginBottom
        return ret
    }

    /**
     * Returns the visible bounds of this object in screen coordinates.
     */
    private fun getVisibleBounds(): Rect {
        return getVisibleBounds(source)
    }

    /**
     * Returns the visible bounds of `node` in screen coordinates.
     */
    @SuppressLint("CheckResult")
    private fun getVisibleBounds(node: AccessibilityNodeInfo): Rect {
        // Get the object bounds in screen coordinates
        val ret = Rect()
        node.getBoundsInScreen(ret)

        // Trim any portion of the bounds that are not on the screen
        val screen =
            Rect(0, 0, bridge.uiDevice.getDisplayWidth(), bridge.uiDevice.getDisplayHeight())
        ret.intersect(screen)

        // On platforms that give us access to the node's window
        // Trim any portion of the bounds that are outside the window
        val window = Rect()
        if (node.window != null) {
            node.window.getBoundsInScreen(window)
            ret.intersect(window)
        }

        // Find the visible bounds of our first scrollable ancestor
        var ancestor: AccessibilityNodeInfo? = null
        ancestor = node.parent
        while (ancestor != null) {

            // If this ancestor is scrollable
            if (ancestor.isScrollable) {
                // Trim any portion of the bounds that are hidden by the non-visible portion of our
                // ancestor
                val ancestorRect = getVisibleBounds(ancestor)
                ret.intersect(ancestorRect)
                break
            }
            ancestor = ancestor.parent
        }
        return ret
    }

    /**
     * Returns a point in the center of the visible bounds of this object.
     */
    private fun getVisibleCenter(): Point {
        val bounds = getVisibleBounds()
        return Point(bounds.centerX(), bounds.centerY())
    }

    @Suppress("DEPRECATION")
    override fun close() {
        source.recycle()
    }
}