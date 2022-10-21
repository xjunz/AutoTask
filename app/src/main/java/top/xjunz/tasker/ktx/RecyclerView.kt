/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

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

fun RecyclerView.scrollPositionToCenterVertically(position: Int, smoothly: Boolean) {
    val itemView = findViewHolderForAdapterPosition(position)?.itemView
    if (itemView == null) {
        if (smoothly) {
            smoothScrollToPosition(position)
        } else {
            scrollToPosition(position)
        }
        return
    }
    val delta = itemView.top - (height / 2 - itemView.height / 2)
    if (smoothly) {
        smoothScrollBy(0, delta, FastOutSlowInInterpolator())
    } else {
        scrollBy(0, delta)
    }
}