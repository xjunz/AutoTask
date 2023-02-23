/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.util

import top.xjunz.shared.utils.unsupportedOperation

/**
 * @author xjunz 2022/10/23
 */
internal object NumberRangeUtil {

    fun <T : Number> contains(start: T?, stop: T?, e: T): Boolean {
        if (start == null && stop == null) return true
        return (start == null || compare(e, start) >= 0)
                && (stop == null || compare(e, stop) <= 0)
    }

    private fun compare(a: Number, b: Number): Int {

        if (a is Int && b is Int)
            return a.compareTo(b)

        if (a is Float && b is Float)
            return a.compareTo(b)

        if (a is Long && b is Long)
            return a.compareTo(b)

        unsupportedOperation("Unsupported comparison types: ${a::class.java} and ${b::class.java}!")
    }
}