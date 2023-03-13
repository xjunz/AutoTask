/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * @author xjunz 2023/03/13
 */
class CoroutineUiScrollable(
    bridge: CoroutineUiAutomatorBridge,
    source: AccessibilityNodeInfo,
    private val isVerticalList: Boolean,
) : CoroutineUiObject(bridge, source) {

    companion object {

        private const val DEFAULT_SWIPE_DEAD_ZONE_PCT = 0.1


        // More steps slows the swipe and prevents contents from being flung too far
        private const val DEFAULT_SCROLL_STEPS = 55

        private const val DEFAULT_FLING_STEPS = 5

        private const val SCROLL_TO_MAX_SWIPES = 30

    }

    suspend fun scrollForward(): Boolean {
        return scrollForward(DEFAULT_SCROLL_STEPS)
    }

    suspend fun scrollForward(steps: Int): Boolean {
        val rect = Rect()
        source.getBoundsInScreen(rect)
        val downX: Int
        val downY: Int
        val upX: Int
        val upY: Int

        // scrolling is by default assumed vertically unless the object is explicitly
        // set otherwise by setAsHorizontalContainer()
        if (isVerticalList) {
            val swipeAreaAdjust: Int = (rect.height() * DEFAULT_SWIPE_DEAD_ZONE_PCT).toInt()
            // scroll vertically: swipe down -> up
            downX = rect.centerX()
            downY = rect.bottom - swipeAreaAdjust
            upX = rect.centerX()
            upY = rect.top + swipeAreaAdjust
        } else {
            val swipeAreaAdjust: Int = (rect.width() * DEFAULT_SWIPE_DEAD_ZONE_PCT).toInt()
            // scroll horizontally: swipe right -> left
            // TODO: Assuming device is not in right to left language
            downX = rect.right - swipeAreaAdjust
            downY = rect.centerY()
            upX = rect.left + swipeAreaAdjust
            upY = rect.centerY()
        }
        return bridge.interactionController.scrollSwipe(downX, downY, upX, upY, steps)
    }

    suspend fun scrollBackward(steps: Int): Boolean {
        val rect = Rect()
        source.getBoundsInScreen(rect)
        val downX: Int
        val downY: Int
        val upX: Int
        val upY: Int

        // scrolling is by default assumed vertically unless the object is explicitly
        // set otherwise by setAsHorizontalContainer()
        if (isVerticalList) {
            val swipeAreaAdjust: Int = (rect.height() * DEFAULT_SWIPE_DEAD_ZONE_PCT).toInt()
            // scroll vertically: swipe up -> down
            downX = rect.centerX()
            downY = rect.top + swipeAreaAdjust
            upX = rect.centerX()
            upY = rect.bottom - swipeAreaAdjust
        } else {
            val swipeAreaAdjust: Int = (rect.width() * DEFAULT_SWIPE_DEAD_ZONE_PCT).toInt()
            // scroll horizontally: swipe left -> right
            // TODO: Assuming device is not in right to left language
            downX = rect.left + swipeAreaAdjust
            downY = rect.centerY()
            upX = rect.right - swipeAreaAdjust
            upY = rect.centerY()
        }
        return bridge.interactionController.scrollSwipe(downX, downY, upX, upY, steps)
    }

    suspend fun scrollToEnd(steps: Int): Boolean {
        // protect against potential hanging and return after preset attempts
        for (x in 0 until SCROLL_TO_MAX_SWIPES) {
            if (!scrollForward(steps)) {
                break
            }
        }
        return true
    }

    suspend fun scrollToBeginning(steps: Int): Boolean {
        // protect against potential hanging and return after preset attempts
        for (x in 0 until SCROLL_TO_MAX_SWIPES) {
            if (!scrollBackward(steps)) {
                break
            }
        }
        return true
    }


}