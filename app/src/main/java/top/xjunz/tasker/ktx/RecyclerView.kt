/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.scrollPositionToCenterHorizontally(position: Int, smoothly: Boolean) {
    val itemView = findViewHolderForAdapterPosition(position)?.itemView
    if (itemView == null) {
        if (smoothly) {
            smoothScrollToPosition(position)
        } else {
            scrollToPosition(position)
        }
        return
    }
    val delta = itemView.left - (width / 2 - itemView.width / 2)
    if (smoothly) {
        smoothScrollBy(delta, 0, FastOutSlowInInterpolator())
    } else {
        scrollBy(delta, 0)
    }
}

private fun RecyclerView.scrollPositionToCenterVerticallyRecursively(
    position: Int,
    smoothly: Boolean,
    scroll: Boolean,
    onIdle: (View) -> Unit
) {
    val itemView = findViewHolderForAdapterPosition(position)?.itemView
    if (itemView == null) {
        if (scroll) {
            if (smoothly) {
                smoothScrollToPosition(position)
            } else {
                scrollToPosition(position)
            }
        }
        post {
            scrollPositionToCenterVerticallyRecursively(position, smoothly, false, onIdle)
        }
    } else {
        val delta = itemView.top - (height / 2 - itemView.height / 2)
        if (delta == 0) {
            onIdle(itemView)
        } else {
            blockTouch()
            if (smoothly) {
                smoothScrollBy(0, delta, FastOutSlowInInterpolator())
            } else {
                scrollBy(0, delta)
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        removeOnScrollListener(this)
                        setOnTouchListener(null)
                        onIdle(findViewHolderForAdapterPosition(position)!!.itemView)
                    }
                }
            })
        }
    }
}

fun RecyclerView.scrollPositionToCenterVertically(
    position: Int,
    smoothly: Boolean = true,
    onIdle: (View) -> Unit = {}
) {
    scrollPositionToCenterVerticallyRecursively(position, smoothly, true, onIdle)
}